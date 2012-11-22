/**
 *  Self Test application for Invensense's MPU6050/MPU6500/MPU9150.
 */

#include <unistd.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <features.h>
#include <dirent.h>
#include <string.h>
#include <poll.h>
#include <stddef.h>
#include <linux/input.h>
#include <time.h>
#include <linux/time.h>

#include "invensense.h"
#include "ml_math_func.h"
#include "storage_manager.h"
#include "ml_stored_data.h"
#include "ml_sysfs_helper.h"

#ifndef ABS
#define ABS(x)(((x) >= 0) ? (x) : -(x))
#endif

//#define DEBUG_PRINT     /* Uncomment to print Gyro & Accel read from Driver */

#define MAX_SYSFS_NAME_LEN  (100)
#define MAX_SYSFS_ATTRB (sizeof(struct sysfs_attrbs) / sizeof(char*))

/** Change this key if the data being stored by this file changes */
#define INV_DB_SAVE_KEY 53395

#define FALSE   0
#define TRUE    1

#define GYRO_PASS_STATUS_BIT    0x01
#define ACCEL_PASS_STATUS_BIT   0x02
#define COMPASS_PASS_STATUS_BIT 0x04

typedef union {
    long l;
    int i;
} bias_dtype;

char *sysfs_names_ptr;

struct sysfs_attrbs {
    char *enable;
    char *power_state;
    char *dmp_on;
    char *dmp_int_on;
    char *self_test;
    char *temperature;
    char *gyro_enable;
    char *gyro_x_bias;
    char *gyro_y_bias;
    char *gyro_z_bias;
    char *accel_enable;
    char *accel_x_bias;
    char *accel_y_bias;
    char *accel_z_bias;
    char *compass_enable;
} mpu;

struct inv_db_save_t {
    /** Compass Bias in Chip Frame in Hardware units scaled by 2^16 */
    long compass_bias[3];
    /** Gyro Bias in Chip Frame in Hardware units scaled by 2^16 */
    long gyro_bias[3];
    /** Temperature when *gyro_bias was stored. */
    long gyro_temp;
    /** Accel Bias in Chip Frame in Hardware units scaled by 2^16 */
    long accel_bias[3];
    /** Temperature when accel bias was stored. */
    long accel_temp;
    long gyro_temp_slope[3];
    /** Sensor Accuracy */
    int gyro_accuracy;
    int accel_accuracy;
    int compass_accuracy;
};

static struct inv_db_save_t save_data;

/** This function receives the data that was stored in non-volatile memory
    between power off */
static inv_error_t inv_db_load_func(const unsigned char *data)
{
    memcpy(&save_data, data, sizeof(save_data));
    return INV_SUCCESS;
}

/** This function returns the data to be stored in non-volatile memory between
    power off */
static inv_error_t inv_db_save_func(unsigned char *data)
{
    memcpy(data, &save_data, sizeof(save_data));
    return INV_SUCCESS;
}

/** read a sysfs entry that represents an integer */
int read_sysfs_int(char *filename, int *var)
{
    int res=0;
    FILE *fp;

    fp = fopen(filename, "r");
    if (fp != NULL) {
        fscanf(fp, "%d\n", var);
        fclose(fp);
    } else {
        LOGE("HAL:ERR open file to read");
        res= -1;   
    }
    return res;
}

/** write a sysfs entry that represents an integer */
int write_sysfs_int(char *filename, int data)
{
    int res=0;
    FILE  *fp;

    fp = fopen(filename, "w");
    if (fp!=NULL) {
        fprintf(fp, "%d\n", data);
        fclose(fp);
    } else {
        LOGE("HAL:ERR open file to write");
        res= -1;   
    }
    return res;
}

int inv_init_sysfs_attributes(void)
{
    unsigned char i = 0;
    char sysfs_path[MAX_SYSFS_NAME_LEN];
    char *sptr;
    char **dptr;

    sysfs_names_ptr = 
            (char*)malloc(sizeof(char[MAX_SYSFS_ATTRB][MAX_SYSFS_NAME_LEN]));
    sptr = sysfs_names_ptr;
    if (sptr != NULL) {
        dptr = (char**)&mpu;
        do {
            *dptr++ = sptr;
            sptr += sizeof(char[MAX_SYSFS_NAME_LEN]);
        } while (++i < MAX_SYSFS_ATTRB);
    } else {
        LOGE("HAL:couldn't alloc mem for sysfs paths");
        return -1;
    }

    // get proper (in absolute/relative) IIO path & build MPU's sysfs paths
    inv_get_sysfs_path(sysfs_path);

    sprintf(mpu.enable, "%s%s", sysfs_path, "/buffer/enable");
    sprintf(mpu.power_state, "%s%s", sysfs_path, "/power_state");
    sprintf(mpu.dmp_on,"%s%s", sysfs_path, "/dmp_on");
    sprintf(mpu.self_test, "%s%s", sysfs_path, "/self_test");
    sprintf(mpu.temperature, "%s%s", sysfs_path, "/temperature");

    sprintf(mpu.gyro_enable, "%s%s", sysfs_path, "/gyro_enable");
    sprintf(mpu.gyro_x_bias, "%s%s", sysfs_path, "/in_anglvel_x_calibbias");
    sprintf(mpu.gyro_y_bias, "%s%s", sysfs_path, "/in_anglvel_y_calibbias");
    sprintf(mpu.gyro_z_bias, "%s%s", sysfs_path, "/in_anglvel_z_calibbias");

    sprintf(mpu.accel_enable, "%s%s", sysfs_path, "/accl_enable");
    sprintf(mpu.accel_x_bias, "%s%s", sysfs_path, "/in_accel_x_calibbias");
    sprintf(mpu.accel_y_bias, "%s%s", sysfs_path, "/in_accel_y_calibbias");
    sprintf(mpu.accel_z_bias, "%s%s", sysfs_path, "/in_accel_z_calibbias");

    sprintf(mpu.compass_enable, "%s%s", sysfs_path, "/compass_enable");

#if 0
    // test print sysfs paths
    dptr = (char**)&mpu;
    for (i = 0; i < MAX_SYSFS_ATTRB; i++) {
        LOGE("HAL:sysfs path: %s", *dptr++);
    }
#endif
    return 0;
}

/*******************************************************************************
 *                       M a i n  S e l f  T e s t 
 ******************************************************************************/
int main(int argc, char **argv)
{
    FILE *fptr;
    int self_test_status = 0;
    inv_error_t result;
    bias_dtype gyro_bias[3];
    bias_dtype accel_bias[3];
    int axis = 0;
    size_t packet_sz;
    int axis_sign = 1;
    unsigned char *buffer;
    long timestamp;
    int temperature = 0;
    bool compass_present = TRUE;

    result = inv_init_sysfs_attributes();
    if (result)
        return -1;

    inv_init_storage_manager();

    // Clear out data.
    memset(&save_data, 0, sizeof(save_data));
    memset(gyro_bias, 0, sizeof(gyro_bias));
    memset(accel_bias, 0, sizeof(accel_bias));

    // Register packet to be saved.
    result = inv_register_load_store(
                inv_db_load_func, inv_db_save_func,
                sizeof(save_data), INV_DB_SAVE_KEY);

    // Power ON MPUxxxx chip
    if (write_sysfs_int(mpu.power_state, 1) < 0) {
        printf("Self-Test:ERR-Failed to set power state=1\n");
    } else {
        // Note: Driver turns on power automatically when self-test invoked
    }

    // Disable Master enable 
    if (write_sysfs_int(mpu.enable, 0) < 0) {
        printf("Self-Test:ERR-Failed to disable master enable\n");
    }

    // Disable DMP
    if (write_sysfs_int(mpu.dmp_on, 0) < 0) {
        printf("Self-Test:ERR-Failed to disable DMP\n");
    }

    // Enable Accel
    if (write_sysfs_int(mpu.accel_enable, 1) < 0) {
        printf("Self-Test:ERR-Failed to enable accel\n");
    }

    // Enable Gyro
    if (write_sysfs_int(mpu.gyro_enable, 1) < 0) {
        printf("Self-Test:ERR-Failed to enable gyro\n");
    }

    // Enable Compass
    if (write_sysfs_int(mpu.compass_enable, 1) < 0) {
#ifdef DEBUG_PRINT
        printf("Self-Test:ERR-Failed to enable compass\n");
#endif
        compass_present= FALSE;
    }

    fptr = fopen(mpu.self_test, "r");
    if (!fptr) {
        printf("Self-Test:ERR-Couldn't invoke self-test\n");
        result = -1;
        goto free_sysfs_storage;
    }

    // Invoke self-test 
    fscanf(fptr, "%d", &self_test_status);
	if (compass_present == TRUE) {
        printf("Self-Test:Self test result- "
           "Gyro passed= %x, Accel passed= %x, Compass passed= %x\n",
           (self_test_status & GYRO_PASS_STATUS_BIT),
           (self_test_status & ACCEL_PASS_STATUS_BIT) >> 1,
           (self_test_status & COMPASS_PASS_STATUS_BIT) >> 2);
	} else {
		printf("Self-Test:Self test result- "
			   "Gyro passed= %x, Accel passed= %x\n",
			   (self_test_status & GYRO_PASS_STATUS_BIT),
			   (self_test_status & ACCEL_PASS_STATUS_BIT) >> 1);
	}
    fclose(fptr);

    if (self_test_status & GYRO_PASS_STATUS_BIT) {
        // Read Gyro Bias
        if (read_sysfs_int(mpu.gyro_x_bias, &gyro_bias[0].i) < 0 ||
            read_sysfs_int(mpu.gyro_y_bias, &gyro_bias[1].i) < 0 ||
            read_sysfs_int(mpu.gyro_z_bias, &gyro_bias[2].i) < 0) {
            memset(gyro_bias, 0, sizeof(gyro_bias));
            printf("Self-Test:Failed to read Gyro bias\n");
        } else {
            save_data.gyro_accuracy = 3;
#ifdef DEBUG_PRINT
            printf("Self-Test:Gyro bias[0..2]= [%d %d %d]\n", 
                   gyro_bias[0].i, gyro_bias[1].i, gyro_bias[2].i);
#endif
        }
    } else {
        printf("Self-Test:Failed Gyro self-test\n");
    }

    if (self_test_status & ACCEL_PASS_STATUS_BIT) {
        // Read Accel Bias
        if (read_sysfs_int(mpu.accel_x_bias, &accel_bias[0].i) < 0 ||
            read_sysfs_int(mpu.accel_y_bias, &accel_bias[1].i) < 0 ||
            read_sysfs_int(mpu.accel_z_bias, &accel_bias[2].i) < 0) {
            memset(accel_bias,0, sizeof(accel_bias));
            printf("Self-Test:Failed to read Accel bias\n");
        } else {
            save_data.accel_accuracy = 3;
#ifdef DEBUG_PRINT
            printf("Self-Test:Accel bias[0..2]= [%d %d %d]\n",
                   accel_bias[0].i, accel_bias[1].i, accel_bias[2].i);
#endif
       }
    } else {
        printf("Self-Test:Failed Accel self-test\n");
    }

    if (!(self_test_status & (GYRO_PASS_STATUS_BIT | ACCEL_PASS_STATUS_BIT))) {
        printf("Self-Test:Failed Gyro and Accel self-test, "
               "nothing left to do\n");
        result = -1;
        goto free_sysfs_storage;
    }

    // Read temperature
    fptr= fopen(mpu.temperature, "r");
    if (fptr != NULL) {
        fscanf(fptr,"%d %ld", &temperature, &timestamp);
        fclose(fptr);
    } else {
        printf("Self-Test:ERR-Couldn't read temperature\n");
    }

    // When we read gyro bias, the bias is in raw units scaled by 1000.
    // We store the bias in raw units scaled by 2^16
    save_data.gyro_bias[0] = (long)(gyro_bias[0].l * 65536.f / 8000.f);
    save_data.gyro_bias[1] = (long)(gyro_bias[1].l * 65536.f / 8000.f);
    save_data.gyro_bias[2] = (long)(gyro_bias[2].l * 65536.f / 8000.f);

    // Save temperature @ time stored.
    //  Temperature is in degrees Celsius scaled by 2^16
    save_data.gyro_temp = temperature * (1L << 16);
    save_data.accel_temp = save_data.gyro_temp; 

    // When we read accel bias, the bias is in raw units scaled by 1000.
    // and it contains the gravity vector.
    
    // Find the orientation of the device, by looking for gravity
    if (ABS(accel_bias[1].l) > ABS(accel_bias[0].l)) {
        axis = 1;
    }
    if (ABS(accel_bias[2].l) > ABS(accel_bias[axis].l)) {
        axis = 2;
    }
    if (accel_bias[axis].l < 0) {
        axis_sign = -1;
    }
    
    // Remove gravity, gravity in raw units should be 16384 for a 2g setting.
    // We read data scaled by 1000, so 
    accel_bias[axis].l -= axis_sign * 4096L * 1000L;

    // Convert scaling from raw units scaled by 1000 to raw scaled by 2^16
    save_data.accel_bias[0] = (long)(accel_bias[0].l * 65536.f / 1000.f * 4.f);
    save_data.accel_bias[1] = (long)(accel_bias[1].l * 65536.f / 1000.f * 4.f);
    save_data.accel_bias[2] = (long)(accel_bias[2].l * 65536.f / 1000.f * 4.f);

#if 1
    printf("Self-Test:Saved Accel bias[0..2]= [%ld %ld %ld]\n",
           save_data.accel_bias[0], save_data.accel_bias[1], 
           save_data.accel_bias[2]);
    printf("Self-Test:Saved Gyro bias[0..2]= [%ld %ld %ld]\n",
           save_data.gyro_bias[0], save_data.gyro_bias[1], 
           save_data.gyro_bias[2]);
    printf("Self-Test:Gyro temperature @ time stored %ld\n", 
           save_data.gyro_temp);
    printf("Self-Test:Accel temperature @ time stored %ld\n", 
           save_data.accel_temp);
#endif

    // Get size of packet to store.
    inv_get_mpl_state_size(&packet_sz);

    // Create place to store data
    buffer = (unsigned char *)malloc(packet_sz + 10);
    if (buffer == NULL) {
        printf("Self-Test:Can't allocate buffer\n");
        result = -1;
        goto free_sysfs_storage;
    }

    // Store the data
    result = inv_save_mpl_states(buffer, packet_sz);
    if (result) {
        result = -1;
    } else {
        fptr= fopen(MLCAL_FILE, "wb+");
        if (fptr != NULL) {
            fwrite(buffer, 1, packet_sz, fptr);
            fclose(fptr);
        } else {
            printf("Self-Test:ERR- Can't open calibration file to write - %s\n",
                   MLCAL_FILE);
            result = -1;
        }
    }
    free(buffer);

free_sysfs_storage:
    free(sysfs_names_ptr);
    return result;
}


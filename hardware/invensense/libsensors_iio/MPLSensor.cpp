/*
* Copyright (C) 2012 Invensense, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#define LOG_NDEBUG 0
//see also the EXTRA_VERBOSE define in the MPLSensor.h header file

#include <fcntl.h>
#include <errno.h>
#include <math.h>
#include <float.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <stdlib.h>
#include <sys/select.h>
#include <sys/syscall.h>
#include <dlfcn.h>
#include <pthread.h>
#include <cutils/log.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <string.h>
#include <linux/input.h>
#include <utils/Atomic.h>

#include "MPLSensor.h"
#include "MPLSupport.h"
#include "sensor_params.h"

#include "invensense.h"
#include "invensense_adv.h"
#include "ml_stored_data.h"
#include "ml_load_dmp.h"
#include "ml_sysfs_helper.h"

// #define TESTING

#ifdef THIRD_PARTY_ACCEL
#   warning "Third party accel"
#   define USE_THIRD_PARTY_ACCEL        (1)
#else
#   define USE_THIRD_PARTY_ACCEL        (0)
#endif

#define MAX_SYSFS_ATTRB (sizeof(struct sysfs_attrbs) / sizeof(char*))

/******************************************************************************/
/*  MPL interface misc.                                                       */
/******************************************************************************/
static int hertz_request = 200;

#define DEFAULT_MPL_GYRO_RATE           (20000L)     //us
#define DEFAULT_MPL_COMPASS_RATE        (20000L)     //us

#define DEFAULT_HW_GYRO_RATE            (100)        //Hz
#define DEFAULT_HW_ACCEL_RATE           (20)         //ms
#define DEFAULT_HW_COMPASS_RATE         (20000000L)  //ns
#define DEFAULT_HW_AKMD_COMPASS_RATE    (200000000L) //ns

/* convert ns to hardware units */
#define HW_GYRO_RATE_NS                 (1000000000LL / rate_request) // to Hz
#define HW_ACCEL_RATE_NS                (rate_request / (1000000L))   // to ms
#define HW_COMPASS_RATE_NS              (rate_request)                // to ns

/* convert Hz to hardware units */
#define HW_GYRO_RATE_HZ                 (hertz_request)
#define HW_ACCEL_RATE_HZ                (1000 / hertz_request)
#define HW_COMPASS_RATE_HZ              (1000000000LL / hertz_request)

#define RATE_200HZ                      5000000LL
#define RATE_15HZ                       66667000LL
#define RATE_5HZ                        200000000LL

static struct sensor_t sSensorList[] =
{
    {"MPL Gyroscope", "Invensense", 1,
     SENSORS_GYROSCOPE_HANDLE,
     SENSOR_TYPE_GYROSCOPE, 2000.0f, 1.0f, 0.5f, 10000, {}},
    {"MPL Raw Gyro", "Invensense", 1,
     SENSORS_RAW_GYROSCOPE_HANDLE,
     SENSOR_TYPE_GYROSCOPE, 2000.0f, 1.0f, 0.5f, 10000, {}},
    {"MPL Accelerometer", "Invensense", 1,
     SENSORS_ACCELERATION_HANDLE,
     SENSOR_TYPE_ACCELEROMETER, 10240.0f, 1.0f, 0.5f, 10000, {}},
    {"MPL Magnetic Field", "Invensense", 1,
     SENSORS_MAGNETIC_FIELD_HANDLE,
     SENSOR_TYPE_MAGNETIC_FIELD, 10240.0f, 1.0f, 0.5f, 10000, {}},
    {"MPL Orientation", "Invensense", 1,
     SENSORS_ORIENTATION_HANDLE,
     SENSOR_TYPE_ORIENTATION, 360.0f, 1.0f, 9.7f, 10000, {}},
    {"MPL Rotation Vector", "Invensense", 1,
     SENSORS_ROTATION_VECTOR_HANDLE,
     SENSOR_TYPE_ROTATION_VECTOR, 10240.0f, 1.0f, 0.5f, 10000, {}},
    {"MPL Linear Acceleration", "Invensense", 1,
     SENSORS_LINEAR_ACCEL_HANDLE,
     SENSOR_TYPE_LINEAR_ACCELERATION, 10240.0f, 1.0f, 0.5f, 10000, {}},
    {"MPL Gravity", "Invensense", 1,
     SENSORS_GRAVITY_HANDLE,
     SENSOR_TYPE_GRAVITY, 10240.0f, 1.0f, 0.5f, 10000, {}},

#ifdef ENABLE_DMP_SCREEN_AUTO_ROTATION
    {"MPL Screen Orientation", "Invensense ", 1,
     SENSORS_SCREEN_ORIENTATION_HANDLE,
     SENSOR_TYPE_SCREEN_ORIENTATION, 100.0f, 1.0f, 1.1f, 0, {}},
#endif
};

MPLSensor *MPLSensor::gMPLSensor = NULL;

extern "C" {
void procData_cb_wrapper()
{
    if(MPLSensor::gMPLSensor) {
        MPLSensor::gMPLSensor->cbProcData();
    }
}

void setCallbackObject(MPLSensor* gbpt)
{
    MPLSensor::gMPLSensor = gbpt;
}

MPLSensor* getCallbackObject() {
    return MPLSensor::gMPLSensor;
}

} // end of extern C

#ifdef INV_PLAYBACK_DBG
static FILE *logfile = NULL;
#endif

/*******************************************************************************
 * MPLSensor class implementation
 ******************************************************************************/

// following extended initializer list would only be available with -std=c++11 or -std=gnu+11
MPLSensor::MPLSensor(CompassSensor *compass)
                       : SensorBase(NULL, NULL),
                         mMplSensorInitialized(false),
                         mNewData(0),
                         mMasterSensorMask(INV_ALL_SENSORS),
                         mLocalSensorMask(0),
                         mPollTime(-1),
                         mHaveGoodMpuCal(0),
                         mGyroAccuracy(0),
                         mAccelAccuracy(0),
                         mSampleCount(0),
                         dmp_orient_fd(-1),
                         mDmpOrientationEnabled(0),
                         mEnabled(0),
                         mOldEnabledMask(0),
                         mAccelInputReader(4),
                         mGyroInputReader(32),
                         mTempScale(0),
                         mTempOffset(0),
                         mTempCurrentTime(0),
                         mAccelScale(2),
                         mPendingMask(0),
                         mSensorMask(0),
                         mFeatureActiveMask(0) {
    VFUNC_LOG;

    inv_error_t rv;
    int i, fd;
    char *port = NULL;
    char *ver_str;
    unsigned long mSensorMask;
    int res;
    FILE *fptr;

    mCompassSensor = compass;

    LOGV_IF(EXTRA_VERBOSE,
            "HAL:MPLSensor constructor : numSensors = %d", numSensors);

    pthread_mutex_init(&mMplMutex, NULL);
    pthread_mutex_init(&mHALMutex, NULL);
    bzero(mGyroOrientation, sizeof(mGyroOrientation));
    bzero(mAccelOrientation, sizeof(mAccelOrientation));

#ifdef INV_PLAYBACK_DBG
    LOGV_IF(PROCESS_VERBOSE, "HAL:inv_turn_on_data_logging");
    logfile = fopen("/data/playback.bin", "wb");
    if (logfile)
        inv_turn_on_data_logging(logfile);
#endif

    /* setup sysfs paths */
    if(inv_init_sysfs_attributes()) {
        ALOGE("MPLSensor failed to init sysfs attributes");
        return;
    }

    /* get chip name */
    if (inv_get_chip_name(chip_ID) != INV_SUCCESS) {
        LOGE("HAL:ERR- Failed to get chip ID\n");
    } else {
        LOGV_IF(PROCESS_VERBOSE, "HAL:Chip ID= %s\n", chip_ID);
    }

    enable_iio_sysfs();

    /* turn on power state */
    onPower(1);

    /* reset driver master enable */
    masterEnable(0);

    if (isLowPowerQuatEnabled() || isDmpDisplayOrientationOn()) {
        /* Load DMP image if capable, ie. MPU6xxx/9xxx */
        loadDMP();
    }

    /* open temperature fd for temp comp */
    LOGV_IF(EXTRA_VERBOSE, "HAL:gyro temperature path: %s", mpu.temperature);
    gyro_temperature_fd = open(mpu.temperature, O_RDONLY);
    if (gyro_temperature_fd == -1) {
        LOGE("HAL:could not open temperature node");
    } else {
        LOGV_IF(EXTRA_VERBOSE,
                "HAL:temperature_fd opened: %s", mpu.temperature);
    }

    /* read accel FSR to calcuate accel scale later */
    if (USE_THIRD_PARTY_ACCEL == 0) {
        char buf[3];
        int count = 0;
        LOGV_IF(SYSFS_VERBOSE,
                "HAL:sysfs:cat %s (%lld)", mpu.accel_fsr, getTimestamp());

        fd = open(mpu.accel_fsr, O_RDONLY);
        if(fd < 0) {
            LOGE("HAL:Error opening accel FSR");
        } else {
           memset(buf, 0, sizeof(buf));
           count = read_attribute_sensor(fd, buf, sizeof(buf));
           if(count < 1) {
               LOGE("HAL:Error reading accel FSR");
           } else {
               count = sscanf(buf, "%d", &mAccelScale);
               if(count)
                   LOGV_IF(EXTRA_VERBOSE, "HAL:Accel FSR used %d", mAccelScale);
           }
           close(fd);
        }
    }

    /* initialize sensor data */
    memset(mPendingEvents, 0, sizeof(mPendingEvents));

    mPendingEvents[RotationVector].version = sizeof(sensors_event_t);
    mPendingEvents[RotationVector].sensor = ID_RV;
    mPendingEvents[RotationVector].type = SENSOR_TYPE_ROTATION_VECTOR;
    mPendingEvents[RotationVector].acceleration.status
            = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[LinearAccel].version = sizeof(sensors_event_t);
    mPendingEvents[LinearAccel].sensor = ID_LA;
    mPendingEvents[LinearAccel].type = SENSOR_TYPE_LINEAR_ACCELERATION;
    mPendingEvents[LinearAccel].acceleration.status
            = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[Gravity].version = sizeof(sensors_event_t);
    mPendingEvents[Gravity].sensor = ID_GR;
    mPendingEvents[Gravity].type = SENSOR_TYPE_GRAVITY;
    mPendingEvents[Gravity].acceleration.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[Gyro].version = sizeof(sensors_event_t);
    mPendingEvents[Gyro].sensor = ID_GY;
    mPendingEvents[Gyro].type = SENSOR_TYPE_GYROSCOPE;
    mPendingEvents[Gyro].gyro.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[RawGyro].version = sizeof(sensors_event_t);
    mPendingEvents[RawGyro].sensor = ID_RG;
    mPendingEvents[RawGyro].type = SENSOR_TYPE_GYROSCOPE;
    mPendingEvents[RawGyro].gyro.status = SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[Accelerometer].version = sizeof(sensors_event_t);
    mPendingEvents[Accelerometer].sensor = ID_A;
    mPendingEvents[Accelerometer].type = SENSOR_TYPE_ACCELEROMETER;
    mPendingEvents[Accelerometer].acceleration.status
            = SENSOR_STATUS_ACCURACY_HIGH;

    /* Invensense compass calibration */
    mPendingEvents[MagneticField].version = sizeof(sensors_event_t);
    mPendingEvents[MagneticField].sensor = ID_M;
    mPendingEvents[MagneticField].type = SENSOR_TYPE_MAGNETIC_FIELD;
    mPendingEvents[MagneticField].magnetic.status =
        SENSOR_STATUS_ACCURACY_HIGH;

    mPendingEvents[Orientation].version = sizeof(sensors_event_t);
    mPendingEvents[Orientation].sensor = ID_O;
    mPendingEvents[Orientation].type = SENSOR_TYPE_ORIENTATION;
    mPendingEvents[Orientation].orientation.status
            = SENSOR_STATUS_ACCURACY_HIGH;

    mHandlers[RotationVector] = &MPLSensor::rvHandler;
    mHandlers[LinearAccel] = &MPLSensor::laHandler;
    mHandlers[Gravity] = &MPLSensor::gravHandler;
    mHandlers[Gyro] = &MPLSensor::gyroHandler;
    mHandlers[RawGyro] = &MPLSensor::rawGyroHandler;
    mHandlers[Accelerometer] = &MPLSensor::accelHandler;
    mHandlers[MagneticField] = &MPLSensor::compassHandler;
    mHandlers[Orientation] = &MPLSensor::orienHandler;

    for (int i = 0; i < numSensors; i++) {
        mDelays[i] = 0;
    }

    (void)inv_get_version(&ver_str);
    LOGV_IF(PROCESS_VERBOSE, "%s\n", ver_str);

    /* setup MPL */
    inv_constructor_init();

    /* load calibration file from /data/inv_cal_data.bin */
    rv = inv_load_calibration();
    if(rv == INV_SUCCESS)
        LOGV_IF(PROCESS_VERBOSE, "HAL:Calibration file successfully loaded");
    else
        LOGE("HAL:Could not open or load MPL calibration file (%d)", rv);

    inv_set_device_properties();

    /* disable driver master enable the first sensor goes on */
    masterEnable(0);
    enableGyro(0);
    enableAccel(0);
    enableCompass(0);

    if (isLowPowerQuatEnabled()) {
        enableLPQuaternion(0);
    }

    onPower(0);

    enableDmpOrientation(isDmpDisplayOrientationOn());
    enableDmpOrientation(!isDmpScreenAutoRotationOn() && isDmpDisplayOrientationOn());

#ifdef INV_PLAYBACK_DBG
    logfile = fopen("/data/playback.bin", "wb");
    if (logfile)
        inv_turn_on_data_logging(logfile);
#endif

    mMplSensorInitialized = true;
}

void MPLSensor::enable_iio_sysfs() {
    VFUNC_LOG;

    char iio_trigger_name[MAX_CHIP_ID_LEN], iio_device_node[MAX_CHIP_ID_LEN];
    FILE *tempFp = NULL;

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo 1 > %s (%lld)",
            mpu.in_timestamp_en, getTimestamp());
    // Using fopen() here to benefit from fprintf()
    tempFp = fopen(mpu.in_timestamp_en, "w");
    if (tempFp == NULL) {
        LOGE("HAL:could not open timestamp enable");
    } else {
        if(fprintf(tempFp, "%d", 1) < 0) {
            LOGE("HAL:could not enable timestamp");
        }
        fclose(tempFp);
    }

    LOGV_IF(SYSFS_VERBOSE,
            "HAL:sysfs:cat %s (%lld)",
            mpu.trigger_name, getTimestamp());
    tempFp = fopen(mpu.trigger_name, "r");
    if (tempFp == NULL) {
        LOGE("HAL:could not open trigger name");
    } else {
        if (fscanf(tempFp, "%s", iio_trigger_name) < 0) {
            LOGE("HAL:could not read trigger name");
        }
        fclose(tempFp);
    }

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %s > %s (%lld)",
            iio_trigger_name, mpu.current_trigger, getTimestamp());
    tempFp = fopen(mpu.current_trigger, "w");
    if (tempFp == NULL) {
        LOGE("HAL:could not open current trigger");
    } else {
        if (fprintf(tempFp, "%s", iio_trigger_name) < 0) {
            LOGE("HAL:could not write current trigger");
        }
        fclose(tempFp);
    }

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            IIO_BUFFER_LENGTH, mpu.buffer_length, getTimestamp());
    tempFp = fopen(mpu.buffer_length, "w");
    if (tempFp == NULL) {
        LOGE("HAL:could not open buffer length");
    } else {
        if (fprintf(tempFp, "%d", IIO_BUFFER_LENGTH) < 0) {
            LOGE("HAL:could not write buffer length");
        }
        fclose(tempFp);
    }

    inv_get_iio_device_node(iio_device_node);
    iio_fd = open(iio_device_node, O_RDONLY);
    if (iio_fd < 0) {
        LOGE("HAL:could not open iio device node");
    } else {
        LOGV_IF(PROCESS_VERBOSE, "HAL:iio iio_fd opened : %d", iio_fd);
    }
}

int MPLSensor::inv_constructor_init()
{
    VFUNC_LOG;

    inv_error_t result = inv_init_mpl();
    if (result) {
        LOGE("HAL:inv_init_mpl() failed");
        return result;
    }
    result = inv_constructor_default_enable();
    result = inv_start_mpl();
    if (result) {
        LOGE("HAL:inv_start_mpl() failed");
        LOG_RESULT_LOCATION(result);
        return result;
    }

    return result;
}

int MPLSensor::inv_constructor_default_enable()
{
    VFUNC_LOG;

    inv_error_t result;

    result = inv_enable_quaternion();
    if (result) {
        LOGE("HAL:Cannot enable quaternion\n");
        return result;
    }

    result = inv_enable_in_use_auto_calibration();
    if (result) {
        return result;
    }

    // result = inv_enable_motion_no_motion();
    result = inv_enable_fast_nomot();
    if (result) {
        return result;
    }

    result = inv_enable_gyro_tc();
    if (result) {
        return result;
    }

    result = inv_enable_hal_outputs();
    if (result) {
        return result;
    }

    if (!mCompassSensor->providesCalibration()) {
        /* Invensense compass calibration */
        LOGV_IF(PROCESS_VERBOSE, "HAL:Invensense vector compass cal enabled");
        result = inv_enable_vector_compass_cal();
        if (result) {
            LOG_RESULT_LOCATION(result);
            return result;
        } else {
            mFeatureActiveMask |= INV_COMPASS_CAL;
        }

        // specify MPL's trust weight, used by compass algorithms
        inv_vector_compass_cal_sensitivity(3);

        result = inv_enable_compass_bias_w_gyro();
        if (result) {
            LOG_RESULT_LOCATION(result);
            return result;
        }

        result = inv_enable_heading_from_gyro();
        if (result) {
            LOG_RESULT_LOCATION(result);
            return result;
        }

        result = inv_enable_magnetic_disturbance();
        if (result) {
            LOG_RESULT_LOCATION(result);
            return result;
        }
    }

    result = inv_enable_9x_sensor_fusion();
    if (result) {
        LOG_RESULT_LOCATION(result);
        return result;
    } else {
        // 9x sensor fusion enables Compass fit
        mFeatureActiveMask |= INV_COMPASS_FIT;
    }

    result = inv_enable_no_gyro_fusion();
    if (result) {
        LOG_RESULT_LOCATION(result);
        return result;
    }

    result = inv_enable_quat_accuracy_monitor();
    if (result) {
        LOG_RESULT_LOCATION(result);
        return result;
    }

    return result;
}

/* TODO: create function pointers to calculate scale */
void MPLSensor::inv_set_device_properties()
{
    VFUNC_LOG;

    unsigned short orient;

    inv_get_sensors_orientation();

    inv_set_gyro_sample_rate(DEFAULT_MPL_GYRO_RATE);
    inv_set_compass_sample_rate(DEFAULT_MPL_COMPASS_RATE);

    /* gyro setup */
    orient = inv_orientation_matrix_to_scalar(mGyroOrientation);
    inv_set_gyro_orientation_and_scale(orient, 2000L << 15);

    /* accel setup */
    orient = inv_orientation_matrix_to_scalar(mAccelOrientation);
    // BMA250
    //inv_set_accel_orientation_and_scale(orient, 1LL << 22);
    // MPU6050
    inv_set_accel_orientation_and_scale(orient, mAccelScale << 15);

    /* compass setup */
    signed char orientMtx[9];
    mCompassSensor->getOrientationMatrix(orientMtx);
    orient =
        inv_orientation_matrix_to_scalar(orientMtx);
    long sensitivity;
    sensitivity = mCompassSensor->getSensitivity();
    inv_set_compass_orientation_and_scale(orient, sensitivity);
}

void MPLSensor::loadDMP()
{
    int res, fd;
    FILE *fptr;

    if (!strcmp(chip_ID, "mpu3050") || !strcmp(chip_ID, "MPU3050")) {
        //DMP support only for MPU6xxx/9xxx currently
        return;
    }

    /* load DMP firmware */
    LOGV_IF(SYSFS_VERBOSE,
            "HAL:sysfs:cat %s (%lld)", mpu.firmware_loaded, getTimestamp());
    fd = open(mpu.firmware_loaded, O_RDONLY);
    if(fd < 0) {
        LOGE("HAL:could not open dmp state");
    } else {
        if(inv_read_dmp_state(fd) == 0) {
            LOGV_IF(EXTRA_VERBOSE, "HAL:load dmp: %s", mpu.dmp_firmware);
            fptr = fopen(mpu.dmp_firmware, "w");
            if(!fptr) {
                LOGE("HAL:could not write to dmp");
            } else {
                int res = inv_load_dmp(fptr);
                if(res < 0) {
                    LOGE("HAL:load DMP failed");
                } else {
                    LOGV_IF(PROCESS_VERBOSE, "HAL:DMP loaded");
                }
                fclose(fptr);
            }
        } else {
            LOGV_IF(PROCESS_VERBOSE, "HAL:DMP is already loaded");
        }
    }

    // onDMP(1);                //Can't enable here. See note onDMP()
}

void MPLSensor::inv_get_sensors_orientation()
{
    FILE *fptr;

    // get gyro orientation
    LOGV_IF(SYSFS_VERBOSE,
            "HAL:sysfs:cat %s (%lld)", mpu.gyro_orient, getTimestamp());
    fptr = fopen(mpu.gyro_orient, "r");
    if (fptr != NULL) {
        int om[9];
        fscanf(fptr, "%d,%d,%d,%d,%d,%d,%d,%d,%d",
               &om[0], &om[1], &om[2], &om[3], &om[4], &om[5],
               &om[6], &om[7], &om[8]);
        fclose(fptr);

        LOGV_IF(EXTRA_VERBOSE,
                "HAL:gyro mounting matrix: "
                "%+d %+d %+d %+d %+d %+d %+d %+d %+d",
                om[0], om[1], om[2], om[3], om[4], om[5], om[6], om[7], om[8]);

        mGyroOrientation[0] = om[0];
        mGyroOrientation[1] = om[1];
        mGyroOrientation[2] = om[2];
        mGyroOrientation[3] = om[3];
        mGyroOrientation[4] = om[4];
        mGyroOrientation[5] = om[5];
        mGyroOrientation[6] = om[6];
        mGyroOrientation[7] = om[7];
        mGyroOrientation[8] = om[8];
    } else {
        LOGE("HAL:Couldn't read gyro mounting matrix");
    }

    // get accel orientation
    LOGV_IF(SYSFS_VERBOSE,
            "HAL:sysfs:cat %s (%lld)", mpu.accel_orient, getTimestamp());
    fptr = fopen(mpu.accel_orient, "r");
    if (fptr != NULL) {
        int om[9];
        fscanf(fptr, "%d,%d,%d,%d,%d,%d,%d,%d,%d",
               &om[0], &om[1], &om[2], &om[3], &om[4], &om[5],
               &om[6], &om[7], &om[8]);
        fclose(fptr);

        LOGV_IF(EXTRA_VERBOSE,
                "HAL:accel mounting matrix: "
                "%+d %+d %+d %+d %+d %+d %+d %+d %+d",
                om[0], om[1], om[2], om[3], om[4], om[5], om[6], om[7], om[8]);

        mAccelOrientation[0] = om[0];
        mAccelOrientation[1] = om[1];
        mAccelOrientation[2] = om[2];
        mAccelOrientation[3] = om[3];
        mAccelOrientation[4] = om[4];
        mAccelOrientation[5] = om[5];
        mAccelOrientation[6] = om[6];
        mAccelOrientation[7] = om[7];
        mAccelOrientation[8] = om[8];
    } else {
        LOGE("HAL:Couldn't read accel mounting matrix");
    }
}

MPLSensor::~MPLSensor()
{
    VFUNC_LOG;

    mCompassSensor = NULL;

    /* Close open fds */
    if (iio_fd > 0)
        close(iio_fd);
    if( accel_fd > 0 )
        close(accel_fd );
    if (gyro_temperature_fd > 0)
        close(gyro_temperature_fd);
    if (sysfs_names_ptr)
        free(sysfs_names_ptr);

    if (isDmpDisplayOrientationOn()) {
        closeDmpOrientFd();
    }

    /* Turn off Gyro master enable          */
    /* A workaround until driver handles it */
    /* TODO: Turn off and close all sensors */
    LOGV_IF(SYSFS_VERBOSE,
            "HAL:sysfs:echo 0 > %s (%lld)", mpu.chip_enable, getTimestamp());
    if(write_sysfs_int(mpu.chip_enable, 0) < 0) {
        LOGE("HAL:could not disable gyro master enable");
    }

#ifdef INV_PLAYBACK_DBG
    inv_turn_off_data_logging();
    fclose(logfile);
#endif
}

#define GY_ENABLED (((1 << ID_GY) | (1 << ID_RG)) & enabled_sensors)
#define A_ENABLED  ((1 << ID_A)  & enabled_sensors)
#define M_ENABLED  ((1 << ID_M)  & enabled_sensors)
#define O_ENABLED  ((1 << ID_O)  & enabled_sensors)
#define LA_ENABLED ((1 << ID_LA) & enabled_sensors)
#define GR_ENABLED ((1 << ID_GR) & enabled_sensors)
#define RV_ENABLED ((1 << ID_RV) & enabled_sensors)

/* TODO: this step is optional, remove?  */
int MPLSensor::setGyroInitialState()
{
    VFUNC_LOG;

    int res = 0;

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            HW_GYRO_RATE_HZ, mpu.gyro_fifo_rate, getTimestamp());
    int fd = open(mpu.gyro_fifo_rate, O_RDWR);
    res = errno;
    if(fd < 0) {
        LOGE("HAL:open of %s failed with '%s' (%d)",
             mpu.gyro_fifo_rate, strerror(res), res);
        return res;
    }
    res = write_attribute_sensor(fd, HW_GYRO_RATE_HZ);
    if(res < 0) {
        LOGE("HAL:write_attribute_sensor : error writing %s with %d",
             mpu.gyro_fifo_rate, HW_GYRO_RATE_HZ);
        return res;
    }

    // Setting LPF is deprecated

    return 0;
}

/* this applies to BMA250 only */
int MPLSensor::setAccelInitialState()
{
    VFUNC_LOG;

    struct input_absinfo absinfo_x;
    struct input_absinfo absinfo_y;
    struct input_absinfo absinfo_z;
    float value;
    if (!ioctl(accel_fd, EVIOCGABS(EVENT_TYPE_ACCEL_X), &absinfo_x) &&
        !ioctl(accel_fd, EVIOCGABS(EVENT_TYPE_ACCEL_Y), &absinfo_y) &&
        !ioctl(accel_fd, EVIOCGABS(EVENT_TYPE_ACCEL_Z), &absinfo_z)) {
        value = absinfo_x.value;
        mPendingEvents[Accelerometer].data[0] = value * CONVERT_A_X;
        value = absinfo_y.value;
        mPendingEvents[Accelerometer].data[1] = value * CONVERT_A_Y;
        value = absinfo_z.value;
        mPendingEvents[Accelerometer].data[2] = value * CONVERT_A_Z;
        //mHasPendingEvent = true;
    }
    return 0;
}

int MPLSensor::onPower(int en)
{
    VFUNC_LOG;

    int res;

    int count, curr_power_state;

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.power_state, getTimestamp());
    res = read_sysfs_int(mpu.power_state, &curr_power_state);
    if (res < 0) {
        LOGE("HAL:Error reading power state");
        // will set power_state anyway
        curr_power_state = -1;
    }
    if (en != curr_power_state) {
        if((res = write_sysfs_int(mpu.power_state, en)) < 0) {
                LOGE("HAL:Couldn't write power state");
        }
    } else {
        LOGV_IF(EXTRA_VERBOSE,
                "HAL:Power state already enable/disable curr=%d new=%d",
                curr_power_state, en);
    }
    return res;
}

int MPLSensor::onDMP(int en)
{
    VFUNC_LOG;

    int res = -1;
    int status;

    //Sequence to enable DMP
    //1. Turn On power if not already on
    //2. Load DMP image if not already loaded
    //3. Either Gyro or Accel must be enabled/configured before next step
    //4. Enable DMP

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:cat %s (%lld)",
            mpu.firmware_loaded, getTimestamp());
    if(read_sysfs_int(mpu.firmware_loaded, &status) < 0){
        LOGE("HAL:ERR can't get firmware_loaded status");
    } else if (status == 1) {
        //Write only if curr DMP state <> request
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:cat %s (%lld)",
                mpu.dmp_on, getTimestamp());
        if (read_sysfs_int(mpu.dmp_on, &status) < 0) {
            LOGE("HAL:ERR can't read DMP state");
        } else if (status != en) {
            LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                    en, mpu.dmp_on, getTimestamp());
            if (write_sysfs_int(mpu.dmp_on, en) < 0) {
                LOGE("HAL:ERR can't write dmp_on");
            } else {
                res = 0;        //Indicate write successful
            }
            //Enable DMP interrupt
            LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                    en, mpu.dmp_int_on, getTimestamp());
            if (write_sysfs_int(mpu.dmp_int_on, en) < 0) {
                LOGE("HAL:ERR can't en/dis DMP interrupt");
            }
        } else {
            res = 0;    //DMP already set as requested
        }
    } else {
        LOGE("HAL:ERR No DMP image");
    }
    return res;
}

int MPLSensor::checkLPQuaternion(void)
{
    VFUNC_LOG;

    return ((mFeatureActiveMask & INV_DMP_QUATERNION)? 1:0);
}

int MPLSensor::enableLPQuaternion(int en)
{
    VFUNC_LOG;

    if (!en) {
        enableQuaternionData(0);
        onDMP(0);
        mFeatureActiveMask &= ~INV_DMP_QUATERNION;
        LOGV_IF(PROCESS_VERBOSE, "HAL:LP Quat disabled");
    } else {
        if (enableQuaternionData(1) < 0 || onDMP(1) < 0) {
            LOGE("HAL:ERR can't enable LP Quaternion");
        } else {
            mFeatureActiveMask |= INV_DMP_QUATERNION;
            LOGV_IF(PROCESS_VERBOSE, "HAL:LP Quat enabled");
        }
    }
    return 0;
}

int MPLSensor::enableQuaternionData(int en)
{
    int res = 0;
    VFUNC_LOG;

    // Enable DMP quaternion
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.quaternion_on, getTimestamp());
    if (write_sysfs_int(mpu.quaternion_on, en) < 0) {
        LOGE("HAL:ERR can't write DMP quaternion_on");
        res = -1;       //Indicate an err
    }

    if (!en) {
        LOGV_IF(PROCESS_VERBOSE, "HAL:Disabling quat scan elems");
    } else {

        LOGV_IF(PROCESS_VERBOSE, "HAL:Enabling quat scan elems");
    }
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.in_quat_r_en, getTimestamp());
    if (write_sysfs_int(mpu.in_quat_r_en, en) < 0) {
        LOGE("HAL:ERR write in_quat_r_en");
    }
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.in_quat_x_en, getTimestamp());
    if (write_sysfs_int(mpu.in_quat_x_en, en) < 0) {
        LOGE("HAL:ERR write in_quat_x_en");
    }
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.in_quat_y_en, getTimestamp());
    if (write_sysfs_int(mpu.in_quat_y_en, en) < 0) {
        LOGE("HAL:ERR write in_quat_y_en");
    }
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.in_quat_z_en, getTimestamp());

    if (write_sysfs_int(mpu.in_quat_z_en, en) < 0) {
        LOGE("HAL:ERR write in_quat_z_en");
    }

    LOGV_IF(EXTRA_VERBOSE, "HAL:DMP quaternion data was turned off");

    if (!en) {
        inv_quaternion_sensor_was_turned_off();
    }

    return res;
}

int MPLSensor::enableTap(int en)
{
    VFUNC_LOG;

    return 0;
}

int MPLSensor::enableFlick(int en)
{
    VFUNC_LOG;

    return 0;
}

int MPLSensor::enablePedometer(int en)
{
    VFUNC_LOG;

    return 0;
}

int MPLSensor::masterEnable(int en)
{
    VFUNC_LOG;

    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.chip_enable, getTimestamp());
    return write_sysfs_int(mpu.chip_enable, en);
}

int MPLSensor::enableGyro(int en)
{
    VFUNC_LOG;

    /* TODO: FIX error handling. Handle or ignore it appropriately for hw. */
    int res;

    /* need to also turn on/off the master enable */
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.gyro_enable, getTimestamp());
    res = write_sysfs_int(mpu.gyro_enable, en);

    if (!en) {
        LOGV_IF(EXTRA_VERBOSE, "HAL:MPL:inv_gyro_was_turned_off");
        inv_gyro_was_turned_off();
    } else {
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.gyro_x_fifo_enable, getTimestamp());
        write_sysfs_int(mpu.gyro_x_fifo_enable, en);

        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.gyro_y_fifo_enable, getTimestamp());
        write_sysfs_int(mpu.gyro_y_fifo_enable, en);

        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.gyro_z_fifo_enable, getTimestamp());
        res = write_sysfs_int(mpu.gyro_z_fifo_enable, en);
    }

    return res;
}

int MPLSensor::enableAccel(int en)
{
    VFUNC_LOG;

    /* TODO: FIX error handling. Handle or ignore it appropriately for hw. */
    int res;

    /* need to also turn on/off the master enable */
    LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            en, mpu.accel_enable, getTimestamp());
    res = write_sysfs_int(mpu.accel_enable, en);

    if (!en) {
        LOGV_IF(EXTRA_VERBOSE, "HAL:MPL:inv_accel_was_turned_off");
        inv_accel_was_turned_off();
    } else {
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.accel_x_fifo_enable, getTimestamp());
        write_sysfs_int(mpu.accel_x_fifo_enable, en);

        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.accel_y_fifo_enable, getTimestamp());
        write_sysfs_int(mpu.accel_y_fifo_enable, en);

        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.accel_z_fifo_enable, getTimestamp());
        res = write_sysfs_int(mpu.accel_z_fifo_enable, en);
    }

    if(USE_THIRD_PARTY_ACCEL == 1 && en) {
        setAccelInitialState(); // BMA250
    }
    return res;
}

int MPLSensor::enableCompass(int en)
{
    VFUNC_LOG;

    int res = mCompassSensor->enable(ID_M, en);
    if (en == 0) {
        LOGV_IF(EXTRA_VERBOSE, "HAL:MPL:inv_compass_was_turned_off");
        inv_compass_was_turned_off();
    }
    return res;
}

void MPLSensor::computeLocalSensorMask(int enabled_sensors)
{
    VFUNC_LOG;

    do {
        if (LA_ENABLED || GR_ENABLED || RV_ENABLED || O_ENABLED) {
            LOGV_IF(ENG_VERBOSE, "FUSION ENABLED");
            mLocalSensorMask = ALL_MPL_SENSORS_NP;
            break;
        }

        if(!A_ENABLED && !M_ENABLED && !GY_ENABLED) {
            /* Invensense compass cal */
            LOGV_IF(ENG_VERBOSE, "ALL DISABLED");
            mLocalSensorMask = 0;
            break;
        }

        if (GY_ENABLED) {
            LOGV_IF(ENG_VERBOSE, "G ENABLED");
            mLocalSensorMask |= INV_THREE_AXIS_GYRO;
        } else {
            LOGV_IF(ENG_VERBOSE, "G DISABLED");
            mLocalSensorMask &= ~INV_THREE_AXIS_GYRO;
        }

        if (A_ENABLED) {
            LOGV_IF(ENG_VERBOSE, "A ENABLED");
            mLocalSensorMask |= INV_THREE_AXIS_ACCEL;
        } else {
            LOGV_IF(ENG_VERBOSE, "A DISABLED");
            mLocalSensorMask &= ~INV_THREE_AXIS_ACCEL;
        }

        /* Invensense compass calibration */
        if (M_ENABLED) {
            LOGV_IF(ENG_VERBOSE, "M ENABLED");
            mLocalSensorMask |= INV_THREE_AXIS_COMPASS;
        } else {
            LOGV_IF(ENG_VERBOSE, "M DISABLED");
            mLocalSensorMask &= ~INV_THREE_AXIS_COMPASS;
        }
    } while (0);
}

int MPLSensor::enableSensors(unsigned long sensors, int en, uint32_t changed) {
    VFUNC_LOG;

    inv_error_t res = -1;
    int on = 1;
    int off = 0;

    // Sequence to enable or disable a sensor
    // 1. enable Power state
    // 2. reset master enable (=0)
    // 3. enable or disable a sensor
    // 4. set master enable (=1)

    if (isLowPowerQuatEnabled() ||
        changed & ((1 << Gyro) | (1 << RawGyro) | (1 << Accelerometer) |
        (mCompassSensor->isIntegrated() << MagneticField))) {
        /* ensure power state is on */
        onPower(1);

        /* reset master enable */
        res = masterEnable(0);
        if(res < 0) {
            return res;
        }
    }

    LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - sensors: 0x%0x", (unsigned int)sensors);

    if (changed & ((1 << Gyro) | (1 << RawGyro))) {
        if (sensors & INV_THREE_AXIS_GYRO) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - enable gyro");
            res = enableGyro(on);
            if(res < 0) {
                return res;
            }
        } else if ((sensors & INV_THREE_AXIS_GYRO) == 0) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - disable gyro");
            res = enableGyro(off);
            if(res < 0) {
                return res;
            }
        }
    }

    if (changed & (1 << Accelerometer)) {
        if (sensors & INV_THREE_AXIS_ACCEL) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - enable accel");
            res = enableAccel(on);
            if(res < 0) {
                return res;
            }
        } else if ((sensors & INV_THREE_AXIS_ACCEL) == 0) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - disable accel");
            res = enableAccel(off);
            if(res < 0) {
                return res;
            }
        }
    }

    if (changed & (1 << MagneticField)) {
        /* Invensense compass calibration */
        if (sensors & INV_THREE_AXIS_COMPASS) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - enable compass");
            res = enableCompass(on);
            if(res < 0) {
                return res;
            }
        } else if ((sensors & INV_THREE_AXIS_COMPASS) == 0) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:enableSensors - disable compass");
            res = enableCompass(off);
            if(res < 0) {
                return res;
            }
        }
    }

    if ( isLowPowerQuatEnabled() ) {
        // Enable LP Quat
        if ((mEnabled & ((1 << Orientation) | (1 << RotationVector) |
                (1 << LinearAccel) | (1 << Gravity)))) {
            if (!(changed & ((1 << Gyro) | (1 << RawGyro) | (1 << Accelerometer) |
                    (mCompassSensor->isIntegrated() << MagneticField)))) {
                /* ensure power state is on */
                onPower(1);
                /* reset master enable */
                res = masterEnable(0);
                if(res < 0) {
                    return res;
                }
            }
            if (!checkLPQuaternion()) {
                enableLPQuaternion(1);
            } else {
                LOGV_IF(PROCESS_VERBOSE, "HAL:LP Quat already enabled");
            }
        } else if (checkLPQuaternion()) {
            enableLPQuaternion(0);
        }
    }

    if (changed & ((1 << Gyro) | (1 << RawGyro) | (1 << Accelerometer) |
            (mCompassSensor->isIntegrated() << MagneticField))) {
        if (sensors &
            (INV_THREE_AXIS_GYRO
                | INV_THREE_AXIS_ACCEL
                | (INV_THREE_AXIS_COMPASS * mCompassSensor->isIntegrated()))) {
            if (isLowPowerQuatEnabled() || isDmpDisplayOrientationOn()) {
                // disable DMP event interrupt only (w/ data interrupt)
                LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                        0, mpu.dmp_event_int_on, getTimestamp());
                if (write_sysfs_int(mpu.dmp_event_int_on, 0) < 0) {
                    res = -1;
                    LOGE("HAL:ERR can't disable DMP event interrupt");
                    return res;
                }
            }

            if (isDmpDisplayOrientationOn()) {
                // enable DMP
                onDMP(1);

                res = enableAccel(on);
                if(res < 0) {
                    return res;
                }

                if ((sensors & INV_THREE_AXIS_ACCEL) == 0) {
                    res = turnOffAccelFifo();
                }
                if(res < 0) {
                    return res;
                }
            }

            res = masterEnable(1);
            if(res < 0) {
                return res;
            }
        } else { // all sensors idle -> reduce power
            if (isDmpDisplayOrientationOn()) {
                // enable DMP
                onDMP(1);
                // enable DMP event interrupt only (no data interrupt)
                LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                        1, mpu.dmp_event_int_on, getTimestamp());
                if (write_sysfs_int(mpu.dmp_event_int_on, 1) < 0) {
                    res = -1;
                    LOGE("HAL:ERR can't enable DMP event interrupt");
                }
                res = enableAccel(on);
                if(res < 0) {
                    return res;
                }
                if ((sensors & INV_THREE_AXIS_ACCEL) == 0) {
                    res = turnOffAccelFifo();
                }
                if(res < 0) {
                    return res;
                }
                res = masterEnable(1);
                if(res < 0) {
                    return res;
                }
            }
            else {
                res = onPower(0);
                if(res < 0) {
                    return res;
                }
            }

            storeCalibration();
        }
    }

    return res;
}

/* Store calibration file */
void MPLSensor::storeCalibration()
{
    if(mHaveGoodMpuCal == true || mAccelAccuracy >= 2) {
       int res = inv_store_calibration();
       if (res) {
           LOGE("HAL:Cannot store calibration on file");
       } else {
           LOGV_IF(PROCESS_VERBOSE, "HAL:Cal file updated");
       }
    }
}

void MPLSensor::cbProcData()
{
    mNewData = 1;
    mSampleCount++;
    LOGV_IF(EXTRA_VERBOSE, "HAL:new data");
}

/*  these handlers transform mpl data into one of the Android sensor types */
int MPLSensor::gyroHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_gyroscope(s->gyro.v, &s->gyro.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:gyro data : %+f %+f %+f -- %lld - %d",
            s->gyro.v[0], s->gyro.v[1], s->gyro.v[2], s->timestamp, update);
    return update;
}

int MPLSensor::rawGyroHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_gyroscope_raw(s->gyro.v, &s->gyro.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:raw gyro data : %+f %+f %+f -- %lld - %d",
            s->gyro.v[0], s->gyro.v[1], s->gyro.v[2], s->timestamp, update);
    return update;
}

int MPLSensor::accelHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_accelerometer(
        s->acceleration.v, &s->acceleration.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:accel data : %+f %+f %+f -- %lld - %d",
            s->acceleration.v[0], s->acceleration.v[1], s->acceleration.v[2],
            s->timestamp, update);
    mAccelAccuracy = s->acceleration.status;
    return update;
}

int MPLSensor::compassHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_magnetic_field(
        s->magnetic.v, &s->magnetic.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:compass data: %+f %+f %+f -- %lld - %d",
            s->magnetic.v[0], s->magnetic.v[1], s->magnetic.v[2], s->timestamp, update);
    return update;
}

int MPLSensor::rvHandler(sensors_event_t* s)
{
    // rotation vector does not have an accuracy or status
    VHANDLER_LOG;
    int8_t status;
    int update;
    update = inv_get_sensor_type_rotation_vector(s->data, &status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:rv data: %+f %+f %+f %+f - %+lld - %d",
            s->data[0], s->data[1], s->data[2], s->data[3], s->timestamp, update);
    return update;
}

int MPLSensor::laHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_linear_acceleration(
            s->gyro.v, &s->gyro.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:la data: %+f %+f %+f - %lld - %d",
            s->gyro.v[0], s->gyro.v[1], s->gyro.v[2], s->timestamp, update);
    return update;
}

int MPLSensor::gravHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_gravity(s->gyro.v, &s->gyro.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:gr data: %+f %+f %+f - %lld - %d",
            s->gyro.v[0], s->gyro.v[1], s->gyro.v[2], s->timestamp, update);
    return update;
}

int MPLSensor::orienHandler(sensors_event_t* s)
{
    VHANDLER_LOG;
    int update;
    update = inv_get_sensor_type_orientation(
            s->orientation.v, &s->orientation.status, &s->timestamp);
    LOGV_IF(HANDLER_DATA, "HAL:or data: %f %f %f - %lld - %d",
            s->orientation.v[0], s->orientation.v[1], s->orientation.v[2], s->timestamp, update);
    return update;
}

int MPLSensor::enable(int32_t handle, int en)
{
    VFUNC_LOG;

    android::String8 sname;
    int what = -1, err = 0;

    switch (handle) {
    case ID_SO:
        sname = "Screen Orientation";
        LOGV_IF(PROCESS_VERBOSE, "HAL:enable - sensor %s (handle %d) %s -> %s", sname.string(), handle,
            (mDmpOrientationEnabled? "en": "dis"),
            (en? "en" : "dis"));
        enableDmpOrientation(en && isDmpDisplayOrientationOn());
        mDmpOrientationEnabled = !!en;
        return 0;
    case ID_A:
        what = Accelerometer;
        sname = "Accelerometer";
        break;
    case ID_M:
        what = MagneticField;
        sname = "MagneticField";
        break;
    case ID_O:
        what = Orientation;
        sname = "Orientation";
        break;
    case ID_GY:
        what = Gyro;
        sname = "Gyro";
        break;
    case ID_RG:
        what = RawGyro;
        sname = "RawGyro";
        break;
    case ID_GR:
        what = Gravity;
        sname = "Gravity";
        break;
    case ID_RV:
        what = RotationVector;
        sname = "RotationVector";
        break;
    case ID_LA:
        what = LinearAccel;
        sname = "LinearAccel";
        break;
    default: //this takes care of all the gestures
        what = handle;
        sname = "Others";
        break;
    }

    if (uint32_t(what) >= numSensors)
        return -EINVAL;

    int newState = en ? 1 : 0;
    unsigned long sen_mask;

    LOGV_IF(PROCESS_VERBOSE, "HAL:enable - sensor %s (handle %d) %s -> %s", sname.string(), handle,
            ((mEnabled & (1 << what)) ? "en" : "dis"),
            ((uint32_t(newState) << what) ? "en" : "dis"));
    LOGV_IF(PROCESS_VERBOSE,
            "HAL:%s sensor state change what=%d", sname.string(), what);

    // pthread_mutex_lock(&mMplMutex);
    // pthread_mutex_lock(&mHALMutex);

    if ((uint32_t(newState) << what) != (mEnabled & (1 << what))) {
        uint32_t sensor_type;
        short flags = newState;
        uint32_t lastEnabled = mEnabled, changed = 0;

        mEnabled &= ~(1 << what);
        mEnabled |= (uint32_t(flags) << what);

        LOGV_IF(PROCESS_VERBOSE, "HAL:handle = %d", handle);
        LOGV_IF(PROCESS_VERBOSE, "HAL:flags = %d", flags);
        computeLocalSensorMask(mEnabled);
        LOGV_IF(PROCESS_VERBOSE, "HAL:enable : mEnabled = %d", mEnabled);
        sen_mask = mLocalSensorMask & mMasterSensorMask;
        mSensorMask = sen_mask;
        LOGV_IF(PROCESS_VERBOSE, "HAL:sen_mask= 0x%0lx", sen_mask);

        switch (what) {
            case Gyro:
            case RawGyro:
            case Accelerometer:
            case MagneticField:
                if (!(mEnabled & ((1 << Orientation) | (1 << RotationVector) |
                        (1 << LinearAccel) | (1 << Gravity))) &&
                        ((lastEnabled & (1 << what)) != (mEnabled & (1 << what)))) {
                    changed |= (1 << what);
                }
                break;

            case Orientation:
            case RotationVector:
            case LinearAccel:
            case Gravity:
                if ((en && !(lastEnabled & ((1 << Orientation) | (1 << RotationVector) |
                        (1 << LinearAccel) | (1 << Gravity)))) ||
                        (!en && !(mEnabled & ((1 << Orientation) | (1 << RotationVector) |
                        (1 << LinearAccel) | (1 << Gravity))))) {
                    for (int i = Gyro; i <= MagneticField; i++) {
                        if (!(mEnabled & (1 << i))) {
                            changed |= (1 << i);
                        }
                    }
                }
                break;
        }
        LOGV_IF(PROCESS_VERBOSE, "HAL:changed = %d", changed);
        enableSensors(sen_mask, flags, changed);
    }

    // pthread_mutex_unlock(&mMplMutex);
    // pthread_mutex_unlock(&mHALMutex);

#ifdef INV_PLAYBACK_DBG
    /* apparently the logging needs to be go through this sequence
       to properly flush the log file */
    inv_turn_off_data_logging();
    fclose(logfile);
    logfile = fopen("/data/playback.bin", "ab");
    if (logfile)
        inv_turn_on_data_logging(logfile);
#endif

    return err;
}

int MPLSensor::setDelay(int32_t handle, int64_t ns)
{
    VFUNC_LOG;

    android::String8 sname;
    int what = -1;

    switch (handle) {
    case ID_SO:
        return 0;
    case ID_A:
        what = Accelerometer;
        sname = "Accelerometer";
        break;
    case ID_M:
        what = MagneticField;
        sname = "MagneticField";
        break;
    case ID_O:
        what = Orientation;
        sname = "Orientation";
        break;
    case ID_GY:
        what = Gyro;
        sname = "Gyro";
        break;
    case ID_RG:
        what = RawGyro;
        sname = "RawGyro";
        break;
    case ID_GR:
        what = Gravity;
        sname = "Gravity";
        break;
    case ID_RV:
        what = RotationVector;
        sname = "RotationVector";
        break;
    case ID_LA:
        what = LinearAccel;
        sname = "LinearAccel";
        break;
    default: // this takes care of all the gestures
        what = handle;
        sname = "Others";
        break;
    }

#if 0
    // skip the 1st call for enalbing sensors called by ICS/JB sensor service
    static int counter_delay = 0;
    if (!(mEnabled & (1 << what))) {
        counter_delay = 0;
    } else {
        if (++counter_delay == 1) {
            return 0;
        }
        else {
            counter_delay = 0;
        }
    }
#endif

    if (uint32_t(what) >= numSensors)
        return -EINVAL;

    if (ns < 0)
        return -EINVAL;

    LOGV_IF(PROCESS_VERBOSE, "setDelay : %llu ns, (%.2f Hz)", ns, 1000000000.f / ns);

    // limit all rates to reasonable ones */
/*
    if (ns < 10000000LL) {
        ns = 10000000LL;
    }
*/
    if (ns < 5000000LL) {
        ns = 5000000LL;
    }

    /* store request rate to mDelays arrary for each sensor */
    mDelays[what] = ns;

    switch (what) {
        case Gyro:
        case RawGyro:
        case Accelerometer:
            for (int i = Gyro; i <= Accelerometer + mCompassSensor->isIntegrated();
                    i++) {
                if (i != what && (mEnabled & (1 << i)) && ns > mDelays[i]) {
                    LOGV_IF(PROCESS_VERBOSE, "HAL:ignore delay set due to sensor %d", i);
                    return 0;
                }
            }
            break;

        case MagneticField:
            if (mCompassSensor->isIntegrated() &&
                    (((mEnabled & (1 << Gyro)) && ns > mDelays[Gyro]) ||
                    ((mEnabled & (1 << RawGyro)) && ns > mDelays[RawGyro]) ||
                    ((mEnabled & (1 << Accelerometer)) && ns > mDelays[Accelerometer]))) {
                 LOGV_IF(PROCESS_VERBOSE, "HAL:ignore delay set due to gyro/accel");
                 return 0;
            }
            break;

        case Orientation:
        case RotationVector:
        case LinearAccel:
        case Gravity:
            if (isLowPowerQuatEnabled()) {
                LOGV_IF(PROCESS_VERBOSE, "HAL:need to update delay due to LPQ");
                break;
            }

            for (int i = 0; i < numSensors; i++) {
                if (i != what && (mEnabled & (1 << i)) && ns > mDelays[i]) {
                    LOGV_IF(PROCESS_VERBOSE, "HAL:ignore delay set due to sensor %d", i);
                    return 0;
                }
            }
            break;
    }

    // pthread_mutex_lock(&mHALMutex);
    int res = update_delay();
    // pthread_mutex_unlock(&mHALMutex);
    return res;
}

int MPLSensor::update_delay() {
    VHANDLER_LOG;

    int res = 0;
    int64_t got;

    if (mEnabled) {
        int64_t wanted = 1000000000;
        int64_t wanted_3rd_party_sensor = 1000000000;

        // Sequence to change sensor's FIFO rate
        // 1. enable Power state
        // 2. reset master enable
        // 3. Update delay
        // 4. set master enable

        // ensure power on
        onPower(1);

        // reset master enable
        masterEnable(0);

        /* search the minimum delay requested across all enabled sensors */
        for (int i = 0; i < numSensors; i++) {
            if (mEnabled & (1 << i)) {
                int64_t ns = mDelays[i];
                wanted = wanted < ns ? wanted : ns;
            }
        }

        // same delay for 3rd party Accel or Compass
        wanted_3rd_party_sensor = wanted;

        /* mpl rate in us in future maybe different for
           gyro vs compass vs accel */
        int rateInus = (int)wanted / 1000LL;
        int mplGyroRate = rateInus;
        int mplAccelRate = rateInus;
        int mplCompassRate = rateInus;

        LOGV_IF(PROCESS_VERBOSE, "HAL:wanted rate for all sensors : "
             "%llu ns, mpl rate: %d us, (%.2f Hz)",
             wanted, rateInus, 1000000000.f / wanted);

        /* set rate in MPL */
        /* compass can only do 100Hz max */
        inv_set_gyro_sample_rate(mplGyroRate);
        inv_set_accel_sample_rate(mplAccelRate);
        inv_set_compass_sample_rate(mplCompassRate);

        /* TODO: Test 200Hz */
        // inv_set_gyro_sample_rate(5000);
        LOGV_IF(PROCESS_VERBOSE, "HAL:MPL gyro sample rate: %d", mplGyroRate);
        LOGV_IF(PROCESS_VERBOSE, "HAL:MPL accel sample rate: %d", mplAccelRate);
        LOGV_IF(PROCESS_VERBOSE, "HAL:MPL compass sample rate: %d", mplCompassRate);

        int enabled_sensors = mEnabled;
        int tempFd = -1;
        if(LA_ENABLED || GR_ENABLED || RV_ENABLED || O_ENABLED) {
            if (isLowPowerQuatEnabled() || isDmpDisplayOrientationOn()) {
                bool setDMPrate= 0;
                // Set LP Quaternion sample rate if enabled
                if (checkLPQuaternion()) {
                    if (wanted < RATE_200HZ) {
                        enableLPQuaternion(0);
                    } else {
                        inv_set_quat_sample_rate(rateInus);
                        setDMPrate= 1;
                    }
                }

                if (checkDMPOrientation() || setDMPrate==1) {
                    getDmpRate(&wanted);
                }
            }

            int64_t tempRate = wanted;
            LOGV_IF(EXTRA_VERBOSE, "HAL:setDelay - Fusion");
            //nsToHz
            LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %.0f > %s (%lld)",
                    1000000000.f / tempRate, mpu.gyro_fifo_rate,
                    getTimestamp());
            tempFd = open(mpu.gyro_fifo_rate, O_RDWR);
            res = write_attribute_sensor(tempFd, 1000000000.f / tempRate);
            if(res < 0) {
                LOGE("HAL:GYRO update delay error");
            }

            //nsToHz (BMA250)
            if(USE_THIRD_PARTY_ACCEL == 1) {
                LOGV_IF(SYSFS_VERBOSE, "echo %lld > %s (%lld)",
                        wanted_3rd_party_sensor / 1000000L, mpu.accel_fifo_rate,
                        getTimestamp());
                tempFd = open(mpu.accel_fifo_rate, O_RDWR);
                res = write_attribute_sensor(tempFd, wanted_3rd_party_sensor / 1000000L);
                LOGE_IF(res < 0, "HAL:ACCEL update delay error");
            }

            if (!mCompassSensor->isIntegrated()) {
                LOGV_IF(PROCESS_VERBOSE, "HAL:Ext compass rate %.2f Hz", 1000000000.f / wanted_3rd_party_sensor);
                mCompassSensor->setDelay(ID_M, wanted_3rd_party_sensor);
                got = mCompassSensor->getDelay(ID_M);
                inv_set_compass_sample_rate(got / 1000);
            }

            /*
            //nsTons - nothing to be done
            strcpy(&compass_sensor_sysfs_path[compass_sensor_sysfs_path_len],
                   COMPASS_SENSOR_DELAY);
            tempFd = open(compass_sensor_sysfs_path, O_RDWR);
            LOGV_IF(PROCESS_VERBOSE, "setDelay - open path: %s", compass_sensor_sysfs_path);
            wanted = 20000000LLU;
            res = write_attribute_sensor(tempFd, wanted);
            if(res < 0) {
                LOGE("Compass update delay error");
            }
            */

        } else {

            if (GY_ENABLED) {
                wanted = (mDelays[Gyro] <= mDelays[RawGyro]?
                    (mEnabled & (1 << Gyro)? mDelays[Gyro]: mDelays[RawGyro]):
                    (mEnabled & (1 << RawGyro)? mDelays[RawGyro]: mDelays[Gyro]));

                if (isDmpDisplayOrientationOn()) {
                    getDmpRate(&wanted);
                }

                LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %.0f > %s (%lld)",
                        1000000000.f / wanted, mpu.gyro_fifo_rate, getTimestamp());
                tempFd = open(mpu.gyro_fifo_rate, O_RDWR);
                res = write_attribute_sensor(tempFd, 1000000000.f / wanted);
                LOGE_IF(res < 0, "HAL:GYRO update delay error");
            }

            if (A_ENABLED) { /* else if because there is only 1 fifo rate for MPUxxxx */
                if (GY_ENABLED && mDelays[Gyro] < mDelays[Accelerometer]) {
                    wanted = mDelays[Gyro];
                }
                else if (GY_ENABLED && mDelays[RawGyro] < mDelays[Accelerometer]) {
                    wanted = mDelays[RawGyro];

                } else {
                    wanted = mDelays[Accelerometer];
                }

                if (isDmpDisplayOrientationOn()) {
                    getDmpRate(&wanted);
                }

                /* TODO: use function pointers to calculate delay value specific to vendor */
                LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %.0f > %s (%lld)",
                        1000000000.f / wanted, mpu.accel_fifo_rate, getTimestamp());
                tempFd = open(mpu.accel_fifo_rate, O_RDWR);
                //BMA250 in ms
                //res = write_attribute_sensor(tempFd, wanted / 1000000L);
                //MPU6050 in hz
                res = write_attribute_sensor(tempFd, 1000000000.f/wanted);
                LOGE_IF(res < 0, "HAL:ACCEL update delay error");
            }

            /* Invensense compass calibration */
            if (M_ENABLED) {
                if (!mCompassSensor->isIntegrated()) {
                    wanted = mDelays[MagneticField];
                } else {
                    if (GY_ENABLED && mDelays[Gyro] < mDelays[MagneticField]) {
                        wanted = mDelays[Gyro];
                    }
                    else if (GY_ENABLED && mDelays[RawGyro] < mDelays[MagneticField]) {
                        wanted = mDelays[RawGyro];
                    } else if (A_ENABLED && mDelays[Accelerometer] < mDelays[MagneticField]) {
                        wanted = mDelays[Accelerometer];
                    } else {
                        wanted = mDelays[MagneticField];
                    }

                    if (isDmpDisplayOrientationOn()) {
                        getDmpRate(&wanted);
                    }
                }

                mCompassSensor->setDelay(ID_M, wanted);
                got = mCompassSensor->getDelay(ID_M);
                inv_set_compass_sample_rate(got / 1000);
            }

        }

        unsigned long sensors = mLocalSensorMask & mMasterSensorMask;
        if (sensors &
            (INV_THREE_AXIS_GYRO
                | INV_THREE_AXIS_ACCEL
                | (INV_THREE_AXIS_COMPASS * mCompassSensor->isIntegrated()))) {
            res = masterEnable(1);
            if(res < 0) {
                return res;
            }
        } else { // all sensors idle -> reduce power
            res = onPower(0);
            if(res < 0) {
                return res;
            }
        }
    }

    return res;
}

/* use for third party accel */
/* TODO: FIX! data is not used and count not decremented, results is hardcoded to 0 */
int MPLSensor::readAccelEvents(sensors_event_t* data, int count)
{
    VHANDLER_LOG;

    if (count < 1)
        return -EINVAL;

    ssize_t n = mAccelInputReader.fill(accel_fd);
    if (n < 0) {
        LOGE("HAL:missed accel events, exit");
        return n;
    }

    int numEventReceived = 0;
    input_event const* event;
    int nb, done = 0;

    while (done == 0 && count && mAccelInputReader.readEvent(&event)) {
        int type = event->type;
        if (type == EV_ABS) {
            if (event->code == EVENT_TYPE_ACCEL_X) {
                mPendingMask |= 1 << Accelerometer;
                mCachedAccelData[0] = event->value;
            } else if (event->code == EVENT_TYPE_ACCEL_Y) {
                mPendingMask |= 1 << Accelerometer;
                mCachedAccelData[1] = event->value;
            } else if (event->code == EVENT_TYPE_ACCEL_Z) {
                mPendingMask |= 1 << Accelerometer;
                mCachedAccelData[2] =event-> value;
            }
        } else if (type == EV_SYN) {
            done = 1;
            if (mLocalSensorMask & INV_THREE_AXIS_ACCEL) {
                inv_build_accel(mCachedAccelData, 0, getTimestamp());
            }
        } else {
            LOGE("HAL:AccelSensor: unknown event (type=%d, code=%d)",
                    type, event->code);
        }
        mAccelInputReader.next();
    }

    return numEventReceived;
}

/**
 *  Should be called after reading at least one of gyro
 *  compass or accel data. (Also okay for handling all of them).
 *  @returns 0, if successful, error number if not.
 */
/* TODO: This should probably be called "int readEvents(...)"
 *  and readEvents() called "void cacheData(void)".
 */
int MPLSensor::executeOnData(sensors_event_t* data, int count)
{
    VFUNC_LOG;

    inv_execute_on_data();

    int numEventReceived = 0;

    long msg;
    msg = inv_get_message_level_0(1);
    if (msg) {
        if (msg & INV_MSG_MOTION_EVENT) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:**** Motion ****\n");
        }
        if (msg & INV_MSG_NO_MOTION_EVENT) {
            LOGV_IF(PROCESS_VERBOSE, "HAL:***** No Motion *****\n");
            /* after the first no motion, the gyro should be
               calibrated well */
            mGyroAccuracy = SENSOR_STATUS_ACCURACY_HIGH;
            /* if gyros are on and we got a no motion, set a flag
               indicating that the cal file can be written. */
            mHaveGoodMpuCal = true;
        }
    }

    // load up virtual sensors
    for (int i = 0; i < numSensors; i++) {
        int update;
        if (mEnabled & (1 << i)) {
            update = CALL_MEMBER_FN(this, mHandlers[i])(mPendingEvents + i);
            mPendingMask |= (1 << i);

            if (update && (count > 0)) {
                *data++ = mPendingEvents[i];
                count--;
                numEventReceived++;
            }
        }
    }

    return numEventReceived;
}

// collect data for MPL (but NOT sensor service currently), from driver layer
/* TODO: FIX! data and count are not used, results is hardcoded to 0 */
/* TODO: This should probably be called "void cacheEvents(void)"
 * And executeOnData() should be int readEvents(data,count)
 */
int MPLSensor::readEvents(sensors_event_t *data, int count) {


    int lp_quaternion_on, nbyte;
    int i, nb, mask = 0, numEventReceived = 0,
        sensors = ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 1 : 0) +
            ((mLocalSensorMask & INV_THREE_AXIS_ACCEL)? 1 : 0) +
            (((mLocalSensorMask & INV_THREE_AXIS_COMPASS) && mCompassSensor->isIntegrated())? 1 : 0);
    char *rdata = mIIOBuffer;

    nbyte= (8 * sensors + 8) * 1;

    if (isLowPowerQuatEnabled()) {
        lp_quaternion_on = checkLPQuaternion();
        if (lp_quaternion_on == 1) {
            nbyte += sizeof(mCachedQuaternionData);             //currently 16 bytes for Q data
        }
    }

    // pthread_mutex_lock(&mMplMutex);
    // pthread_mutex_lock(&mHALMutex);

    ssize_t rsize = read(iio_fd, rdata, nbyte);
    if (sensors == 0) {
        // read(iio_fd, rdata, nbyte);
        rsize = read(iio_fd, rdata, sizeof(mIIOBuffer));
    }

#ifdef TESTING
    LOGI("get one sample of IIO data with size: %d", rsize);
    LOGI("sensors: %d", sensors);

    LOGI_IF(mLocalSensorMask & INV_THREE_AXIS_GYRO, "gyro x/y/z: %d/%d/%d",
        *((short *) (rdata + 0)), *((short *) (rdata + 2)),
        *((short *) (rdata + 4)));
    LOGI_IF(mLocalSensorMask & INV_THREE_AXIS_ACCEL, "accel x/y/z: %d/%d/%d",
        *((short *) (rdata + 0 + ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0))),
        *((short *) (rdata + 2 + ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0))),
        *((short *) (rdata + 4) + ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0)));

    LOGI_IF(mLocalSensorMask & INV_THREE_AXIS_COMPASS &
        mCompassSensor->isIntegrated(), "compass x/y/z: %d/%d/%d",
        *((short *) (rdata + 0 + ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0) +
            ((mLocalSensorMask & INV_THREE_AXIS_ACCEL)? 6: 0))),
        *((short *) (rdata + 2 + ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0) +
            ((mLocalSensorMask & INV_THREE_AXIS_ACCEL)? 6: 0))),
        *((short *) (rdata + 4) + ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0) +
            ((mLocalSensorMask & INV_THREE_AXIS_ACCEL)? 6: 0)));
#endif

    if (rsize < (nbyte - 8)) {
        LOGE("HAL:ERR Full data packet was not read. rsize=%ld nbyte=%d sensors=%d errno=%d(%s)",
             rsize, nbyte, sensors, errno, strerror(errno));
        return -1;
    }

    if (isLowPowerQuatEnabled() && lp_quaternion_on == 1) {

        for (i=0; i< 4; i++) {
            mCachedQuaternionData[i]= *(long*)rdata;
            rdata += sizeof(long);
        }
    }

    for (i = 0; i < 3; i++) {
        if (mLocalSensorMask & INV_THREE_AXIS_GYRO) {
            mCachedGyroData[i] = *((short *) (rdata + i * 2));
        }
        if (mLocalSensorMask & INV_THREE_AXIS_ACCEL) {
            mCachedAccelData[i] = *((short *) (rdata + i * 2 +
                ((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 6: 0)));
        }
        if ((mLocalSensorMask & INV_THREE_AXIS_COMPASS) && mCompassSensor->isIntegrated()) {
            mCachedCompassData[i] = *((short *) (rdata + i * 2 + 6 * (sensors - 1)));
        }
    }

    mask |= (((mLocalSensorMask & INV_THREE_AXIS_GYRO)? 1 << Gyro: 0) +
        ((mLocalSensorMask & INV_THREE_AXIS_ACCEL)? 1 << Accelerometer: 0));
    if ((mLocalSensorMask & INV_THREE_AXIS_COMPASS) && mCompassSensor->isIntegrated() &&
            (mCachedCompassData[0] != 0 || mCachedCompassData[1] != 0 || mCachedCompassData[0] != 0)) {
        mask |= 1 << MagneticField;
    }

    mSensorTimestamp = *((long long *) (rdata + 8 * sensors));
    if (mCompassSensor->isIntegrated()) {
        mCompassTimestamp = mSensorTimestamp;
    }

    if (mask & (1 << Gyro)) {
        // send down temperature every 0.5 seconds
        // with timestamp measured in "driver" layer
        if(mSensorTimestamp - mTempCurrentTime >= 500000000LL) {
            mTempCurrentTime = mSensorTimestamp;
            long long temperature[2];
            if(inv_read_temperature(temperature) == 0) {
                LOGV_IF(INPUT_DATA,
                        "HAL:inv_read_temperature = %lld, timestamp= %lld",
                        temperature[0], temperature[1]);
                inv_build_temp(temperature[0], temperature[1]);
            }
#ifdef TESTING
            long bias[3], temp, temp_slope[3];
            inv_get_gyro_bias(bias, &temp);
            inv_get_gyro_ts(temp_slope);

            LOGI("T: %.3f "
                 "GB: %+13f %+13f %+13f "
                 "TS: %+13f %+13f %+13f "
                 "\n",
                 (float)temperature[0] / 65536.f,
                 (float)bias[0] / 65536.f / 16.384f,
                 (float)bias[1] / 65536.f / 16.384f,
                 (float)bias[2] / 65536.f / 16.384f,
                 temp_slope[0] / 65536.f,
                 temp_slope[1] / 65536.f,
                 temp_slope[2] / 65536.f);
#endif
        }

        mPendingMask |= 1 << Gyro;
        mPendingMask |= 1 << RawGyro;

        if (mLocalSensorMask & INV_THREE_AXIS_GYRO) {
            inv_build_gyro(mCachedGyroData, mSensorTimestamp);
            LOGV_IF(INPUT_DATA,
                    "HAL:inv_build_gyro: %+8d %+8d %+8d - %lld",
                    mCachedGyroData[0], mCachedGyroData[1],
                    mCachedGyroData[2], mSensorTimestamp);
        }
    }

    if (mask & (1 << Accelerometer)) {
        mPendingMask |= 1 << Accelerometer;
        if (mLocalSensorMask & INV_THREE_AXIS_ACCEL) {
            inv_build_accel(mCachedAccelData, 0, mSensorTimestamp);
             LOGV_IF(INPUT_DATA,
                    "HAL:inv_build_accel: %+8ld %+8ld %+8ld - %lld",
                    mCachedAccelData[0], mCachedAccelData[1],
                    mCachedAccelData[2], mSensorTimestamp);
        }
    }

    if ((mask & (1 << MagneticField)) && mCompassSensor->isIntegrated()) {
        int status = 0;
        if (mCompassSensor->providesCalibration()) {
            status = mCompassSensor->getAccuracy();
            status |= INV_CALIBRATED;
        }
        if (mLocalSensorMask & INV_THREE_AXIS_COMPASS) {
            inv_build_compass(mCachedCompassData, status,
                              mCompassTimestamp);
            LOGV_IF(INPUT_DATA, "HAL:inv_build_compass: %+8ld %+8ld %+8ld - %lld",
                    mCachedCompassData[0], mCachedCompassData[1],
                    mCachedCompassData[2], mCompassTimestamp);
        }
    }

    if (isLowPowerQuatEnabled() && lp_quaternion_on == 1) {

        inv_build_quat(mCachedQuaternionData, 32 /*default 32 for now (16/32bits)*/, mSensorTimestamp);
        LOGV_IF(INPUT_DATA, "HAL:inv_build_quat: %+8ld %+8ld %+8ld %+8ld - %lld",
                    mCachedQuaternionData[0], mCachedQuaternionData[1],
                    mCachedQuaternionData[2], mCachedQuaternionData[3], mSensorTimestamp);
    }

    // pthread_mutex_unlock(&mMplMutex);
    // pthread_mutex_unlock(&mHALMutex);

    return numEventReceived;
}

/* use for both MPUxxxx and third party compass */
int MPLSensor::readCompassEvents(sensors_event_t *data, int count)
{
    VHANDLER_LOG;

    if (count < 1)
        return -EINVAL;

    int numEventReceived = 0;
    int done = 0;
    int nb;

    // pthread_mutex_lock(&mMplMutex);
    // pthread_mutex_lock(&mHALMutex);

    done = mCompassSensor->readSample(mCachedCompassData, &mCompassTimestamp);
#ifdef COMPASS_YAS530
    if (mCompassSensor->checkCoilsReset()==1) {
       //Reset relevant compass settings
       resetCompass();
    }
#endif
    if (done > 0) {
        int status = 0;
        if (mCompassSensor->providesCalibration()) {
            status = mCompassSensor->getAccuracy();
            status |= INV_CALIBRATED;
        }
        if (mLocalSensorMask & INV_THREE_AXIS_COMPASS) {
            inv_build_compass(mCachedCompassData, status,
                              mCompassTimestamp);
            LOGV_IF(INPUT_DATA, "HAL:inv_build_compass: %+8ld %+8ld %+8ld - %lld",
                    mCachedCompassData[0], mCachedCompassData[1],
                    mCachedCompassData[2], mCompassTimestamp);
        }
    }

    // pthread_mutex_unlock(&mMplMutex);
    // pthread_mutex_unlock(&mHALMutex);

    return numEventReceived;
}

#ifdef COMPASS_YAS530
int MPLSensor::resetCompass()
{
    VFUNC_LOG;

    //Reset compass cal if enabled
    if (mFeatureActiveMask & INV_COMPASS_CAL) {
       LOGV_IF(EXTRA_VERBOSE, "HAL:Reset compass cal");
       inv_init_vector_compass_cal();
    }

    //Reset compass fit if enabled
    if (mFeatureActiveMask & INV_COMPASS_FIT) {
       LOGV_IF(EXTRA_VERBOSE, "HAL:Reset compass fit");
       inv_init_compass_fit();
    }

    return 0;
}
#endif

int MPLSensor::getFd() const
{
    VFUNC_LOG;
    LOGV_IF(EXTRA_VERBOSE, "MPLSensor::getFd returning %d", iio_fd);
    return iio_fd;
}

int MPLSensor::getAccelFd() const
{
    VFUNC_LOG;
    LOGV_IF(EXTRA_VERBOSE, "MPLSensor::getAccelFd returning %d", accel_fd);
    return accel_fd;
}

int MPLSensor::getCompassFd() const
{
    VFUNC_LOG;
    int fd = mCompassSensor->getFd();
    LOGV_IF(EXTRA_VERBOSE, "MPLSensor::getCompassFd returning %d", fd);
    return fd;
}

int MPLSensor::turnOffAccelFifo() {
    int i, res, tempFd;
    char *accel_fifo_enable[3] = {mpu.accel_x_fifo_enable,
        mpu.accel_y_fifo_enable, mpu.accel_z_fifo_enable};

    for (i = 0; i < 3; i++) {
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
            0, accel_fifo_enable[i], getTimestamp());
        res = write_sysfs_int(accel_fifo_enable[i], 0);
        if (res < 0) {
            return res;
        }
    }
    return 0;
}

int MPLSensor::enableDmpOrientation(int en)
{
    VFUNC_LOG;
    /* TODO: FIX error handling. Handle or ignore it appropriately for hw. */
    int res = 0;

    // on power if not already On
    onPower(1);
    // reset master enable
    masterEnable(0);

    if (en == 1) {
        //Enable DMP orientation
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                en, mpu.display_orientation_on, getTimestamp());
        if (write_sysfs_int(mpu.display_orientation_on, en) < 0) {
            LOGE("HAL:ERR can't enable Android orientation");
            res = -1;    //Indicate an err
        }

        //Open DMP Orient Fd
        openDmpOrientFd();

        //Enable DMP
        onDMP(1);

        //Default DMP output rate to FIFO
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                5, mpu.dmp_output_rate, getTimestamp());
        if (write_sysfs_int(mpu.dmp_output_rate, 5) < 0) {
            LOGE("HAL:ERR can't default DMP output rate");
        }

        //Set DMP rate to 200Hz
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                200, mpu.accel_fifo_rate, getTimestamp());
        if (write_sysfs_int(mpu.accel_fifo_rate, 200) < 0) {
            res = -1;
            LOGE("HAL:ERR can't set DMP rate to 200Hz");
        }

        // enable accel engine
        enableAccel(1);

        // disable accel FIFO
        res = turnOffAccelFifo();

        mFeatureActiveMask |=INV_DMP_DISPL_ORIENTATION;
    } else {
        // disable DMP
        onDMP(0);
        // disable accel engine
        enableAccel(0);
    }

    res = masterEnable(1);
    return res;
}

int MPLSensor::openDmpOrientFd()
{
    VFUNC_LOG;

    if (!isDmpDisplayOrientationOn() || dmp_orient_fd >= 0) {
        LOGV_IF(PROCESS_VERBOSE, "HAL:DMP display orientation disabled or file desc opened");
        return -1;
    }

    dmp_orient_fd = open(mpu.event_display_orientation, O_RDONLY| O_NONBLOCK);
    if (dmp_orient_fd < 0) {
        LOGE("HAL:ERR couldn't open dmpOrient node");
        return -1;
    } else {
        LOGV_IF(PROCESS_VERBOSE, "HAL:dmp_orient_fd opened : %d", dmp_orient_fd);
        return 0;
    }
}

int MPLSensor::closeDmpOrientFd()
{
    VFUNC_LOG;
    if (dmp_orient_fd >= 0)
        close(dmp_orient_fd);
    return 0;
}

int MPLSensor::dmpOrientHandler(int orient)
{
    VFUNC_LOG;

    LOGV_IF(PROCESS_VERBOSE, "HAL:orient %x", orient);
    return 0;
}

int MPLSensor::readDmpOrientEvents(sensors_event_t* data, int count) {
    VFUNC_LOG;

    char dummy[4];
    int screen_orientation = 0;
    FILE *fp;

    fp = fopen(mpu.event_display_orientation, "r");
    if (fp == NULL) {
        LOGE("HAL:cannot open event_display_orientation");
        return 0;
    }
    fscanf(fp, "%d\n", &screen_orientation);
    fclose(fp);

    int numEventReceived = 0;

    if (mDmpOrientationEnabled && count > 0) {
        sensors_event_t temp;

        bzero(&temp, sizeof(temp));  /* Let's hope that SENSOR_TYPE_NONE is 0 */
        temp.version = sizeof(sensors_event_t);
        temp.sensor = ID_SO;
#ifdef ENABLE_DMP_SCREEN_AUTO_ROTATION
        temp.type = SENSOR_TYPE_SCREEN_ORIENTATION;
        temp.screen_orientation = screen_orientation;
#endif
        struct timespec ts;
        clock_gettime(CLOCK_MONOTONIC, &ts);
        temp.timestamp = (int64_t) ts.tv_sec * 1000000000 + ts.tv_nsec;

        *data++ = temp;
        count--;
        numEventReceived++;
    }

    // read dummy data per driver's request
    dmpOrientHandler(screen_orientation);
    read(dmp_orient_fd, dummy, 4);

    return numEventReceived;
}

int MPLSensor::getDmpOrientFd()
{
    VFUNC_LOG;

    LOGV_IF(EXTRA_VERBOSE, "MPLSensor::getDmpOrientFd returning %d", dmp_orient_fd);
    return dmp_orient_fd;

}

int MPLSensor::checkDMPOrientation()
{
    VFUNC_LOG;
    return ((mFeatureActiveMask & INV_DMP_DISPL_ORIENTATION) ? 1 : 0);
}

int MPLSensor::getDmpRate(int64_t *wanted)
{
    if (checkDMPOrientation() || checkLPQuaternion()) {
        // set DMP output rate to FIFO
        LOGV_IF(SYSFS_VERBOSE, "HAL:sysfs:echo %d > %s (%lld)",
                int(1000000000.f / *wanted), mpu.dmp_output_rate,
                getTimestamp());
        write_sysfs_int(mpu.dmp_output_rate, 1000000000.f / *wanted);
        LOGV_IF(PROCESS_VERBOSE, "HAL:DMP FIFO rate %.2f Hz", 1000000000.f / *wanted);

        //DMP running rate must be @ 200Hz
        *wanted= RATE_200HZ;
        LOGV_IF(PROCESS_VERBOSE, "HAL:DMP rate= %.2f Hz", 1000000000.f / *wanted);
    }
    return 0;
}

int MPLSensor::getPollTime()
{
    VHANDLER_LOG;
    return mPollTime;
}

bool MPLSensor::hasPendingEvents() const
{
    VHANDLER_LOG;
    // if we are using the polling workaround, force the main
    // loop to check for data every time
    return (mPollTime != -1);
}

/* TODO: support resume suspend when we gain more info about them*/
void MPLSensor::sleepEvent()
{
    VFUNC_LOG;
}

void MPLSensor::wakeEvent()
{
    VFUNC_LOG;
}

int MPLSensor::inv_float_to_q16(float *fdata, long *ldata)
{
    VHANDLER_LOG;

    if (!fdata || !ldata)
        return -1;
    ldata[0] = (long)(fdata[0] * 65536.f);
    ldata[1] = (long)(fdata[1] * 65536.f);
    ldata[2] = (long)(fdata[2] * 65536.f);
    return 0;
}

int MPLSensor::inv_long_to_q16(long *fdata, long *ldata)
{
    VHANDLER_LOG;

    if (!fdata || !ldata)
        return -1;
    ldata[0] = (fdata[1] * 65536.f);
    ldata[1] = (fdata[2] * 65536.f);
    ldata[2] = (fdata[3] * 65536.f);
    return 0;
}

int MPLSensor::inv_float_to_round(float *fdata, long *ldata)
{
    VHANDLER_LOG;

    if (!fdata || !ldata)
            return -1;
    ldata[0] = (long)fdata[0];
    ldata[1] = (long)fdata[1];
    ldata[2] = (long)fdata[2];
    return 0;
}

int MPLSensor::inv_float_to_round2(float *fdata, short *ldata)
{
    VHANDLER_LOG;

    if (!fdata || !ldata)
        return -1;
    ldata[0] = (short)fdata[0];
    ldata[1] = (short)fdata[1];
    ldata[2] = (short)fdata[2];
    return 0;
}

int MPLSensor::inv_read_temperature(long long *data)
{
    VHANDLER_LOG;

    int count = 0;
    char raw_buf[40];
    long raw = 0;

    long long timestamp = 0;

    memset(raw_buf, 0, sizeof(raw_buf));
    count = read_attribute_sensor(gyro_temperature_fd, raw_buf,
                                  sizeof(raw_buf));
    if(count < 1) {
        LOGE("HAL:error reading gyro temperature");
        return -1;
    }

    count = sscanf(raw_buf, "%ld%lld", &raw, &timestamp);

    if(count < 0) {
        return -1;
    }

    LOGV_IF(ENG_VERBOSE,
            "HAL:temperature raw = %ld, timestamp = %lld, count = %d",
            raw, timestamp, count);
    data[0] = raw;
    data[1] = timestamp;

    return 0;
}

int MPLSensor::inv_read_dmp_state(int fd)
{
    VFUNC_LOG;

    if(fd < 0)
        return -1;

    int count = 0;
    char raw_buf[10];
    short raw = 0;

    memset(raw_buf, 0, sizeof(raw_buf));
    count = read_attribute_sensor(fd, raw_buf, sizeof(raw_buf));
    if(count < 1) {
        LOGE("HAL:error reading dmp state");
        close(fd);
        return -1;
    }
    count = sscanf(raw_buf, "%hd", &raw);
    if(count < 0) {
        LOGE("HAL:dmp state data is invalid");
        close(fd);
        return -1;
    }
    LOGV_IF(EXTRA_VERBOSE, "HAL:dmp state = %d, count = %d", raw, count);
    close(fd);
    return (int)raw;
}

int MPLSensor::inv_read_sensor_bias(int fd, long *data)
{
    VFUNC_LOG;

    if(fd == -1) {
        return -1;
    }

    char buf[50];
    char x[15], y[15], z[15];

    memset(buf, 0, sizeof(buf));
    int count = read_attribute_sensor(fd, buf, sizeof(buf));
    if(count < 1) {
        LOGE("HAL:Error reading gyro bias");
        return -1;
    }
    count = sscanf(buf, "%[^','],%[^','],%[^',']", x, y, z);
    if(count) {
        /* scale appropriately for MPL */
        LOGV_IF(ENG_VERBOSE,
                "HAL:pre-scaled bias: X:Y:Z (%ld, %ld, %ld)",
                atol(x), atol(y), atol(z));

        data[0] = (long)(atol(x) / 10000 * (1L << 16));
        data[1] = (long)(atol(y) / 10000 * (1L << 16));
        data[2] = (long)(atol(z) / 10000 * (1L << 16));

        LOGV_IF(ENG_VERBOSE,
                "HAL:scaled bias: X:Y:Z (%ld, %ld, %ld)",
                data[0], data[1], data[2]);
    }
    return 0;
}

/** fill in the sensor list based on which sensors are configured.
 *  return the number of configured sensors.
 *  parameter list must point to a memory region of at least 7*sizeof(sensor_t)
 *  parameter len gives the length of the buffer pointed to by list
 */
int MPLSensor::populateSensorList(struct sensor_t *list, int len)
{
    VFUNC_LOG;

    int numsensors;

    if(len < (int)((sizeof(sSensorList) / sizeof(sensor_t)) * sizeof(sensor_t))) {
        LOGE("HAL:sensor list too small, not populating.");
        return -(sizeof(sSensorList) / sizeof(sensor_t));
    }

    /* fill in the base values */
    memcpy(list, sSensorList, sizeof (struct sensor_t) * (sizeof(sSensorList) / sizeof(sensor_t)));

    /* first add gyro, accel and compass to the list */

    /* fill in gyro/accel values */
    if(chip_ID == NULL) {
        LOGE("HAL:Can not get gyro/accel id");
    }
    fillGyro(chip_ID, list);
    fillAccel(chip_ID, list);

    // TODO: need fixes for unified HAL and 3rd-party solution
    mCompassSensor->fillList(&list[MagneticField]);

    if(1) {
        numsensors = (sizeof(sSensorList) / sizeof(sensor_t));
        /* all sensors will be added to the list
           fill in orientation values */
        fillOrientation(list);
        /* fill in rotation vector values */
        fillRV(list);
        /* fill in gravity values */
        fillGravity(list);
        /* fill in Linear accel values */
        fillLinearAccel(list);
    } else {
        /* no 9-axis sensors, zero fill that part of the list */
        numsensors = 3;
        memset(list + 3, 0, 4 * sizeof(struct sensor_t));
    }

    return numsensors;
}

void MPLSensor::fillAccel(const char* accel, struct sensor_t *list)
{
    VFUNC_LOG;

    if (accel) {
        if(accel != NULL && strcmp(accel, "BMA250") == 0) {
            list[Accelerometer].maxRange = ACCEL_BMA250_RANGE;
            list[Accelerometer].resolution = ACCEL_BMA250_RESOLUTION;
            list[Accelerometer].power = ACCEL_BMA250_POWER;
            list[Accelerometer].minDelay = ACCEL_BMA250_MINDELAY;
            return;
        } else if (accel != NULL && strcmp(accel, "MPU6050") == 0) {
            list[Accelerometer].maxRange = ACCEL_MPU6050_RANGE;
            list[Accelerometer].resolution = ACCEL_MPU6050_RESOLUTION;
            list[Accelerometer].power = ACCEL_MPU6050_POWER;
            list[Accelerometer].minDelay = ACCEL_MPU6050_MINDELAY;
            return;
        } else if (accel != NULL && strcmp(accel, "MPU6500") == 0) {
            list[Accelerometer].maxRange = ACCEL_MPU6500_RANGE;
            list[Accelerometer].resolution = ACCEL_MPU6500_RESOLUTION;
            list[Accelerometer].power = ACCEL_MPU6500_POWER;
            list[Accelerometer].minDelay = ACCEL_MPU6500_MINDELAY;

            return;
        } else if (accel != NULL && strcmp(accel, "MPU9150") == 0) {
            list[Accelerometer].maxRange = ACCEL_MPU9150_RANGE;
            list[Accelerometer].resolution = ACCEL_MPU9150_RESOLUTION;
            list[Accelerometer].power = ACCEL_MPU9150_POWER;
            list[Accelerometer].minDelay = ACCEL_MPU9150_MINDELAY;
            return;
        } else if (accel != NULL && strcmp(accel, "MPU3050") == 0) {
            list[Accelerometer].maxRange = ACCEL_BMA250_RANGE;
            list[Accelerometer].resolution = ACCEL_BMA250_RESOLUTION;
            list[Accelerometer].power = ACCEL_BMA250_POWER;
            list[Accelerometer].minDelay = ACCEL_BMA250_MINDELAY;
            return;
        }
    }

    LOGE("HAL:unknown accel id %s -- "
         "params default to bma250 and might be wrong.",
         accel);
    list[Accelerometer].maxRange = ACCEL_BMA250_RANGE;
    list[Accelerometer].resolution = ACCEL_BMA250_RESOLUTION;
    list[Accelerometer].power = ACCEL_BMA250_POWER;
    list[Accelerometer].minDelay = ACCEL_BMA250_MINDELAY;
}

void MPLSensor::fillGyro(const char* gyro, struct sensor_t *list)
{
    VFUNC_LOG;

    if ( gyro != NULL && strcmp(gyro, "MPU3050") == 0) {
        list[Gyro].maxRange = GYRO_MPU3050_RANGE;
        list[Gyro].resolution = GYRO_MPU3050_RESOLUTION;
        list[Gyro].power = GYRO_MPU3050_POWER;
        list[Gyro].minDelay = GYRO_MPU3050_MINDELAY;
    } else if( gyro != NULL && strcmp(gyro, "MPU6050") == 0) {
        list[Gyro].maxRange = GYRO_MPU6050_RANGE;
        list[Gyro].resolution = GYRO_MPU6050_RESOLUTION;
        list[Gyro].power = GYRO_MPU6050_POWER;
        list[Gyro].minDelay = GYRO_MPU6050_MINDELAY;
    } else if( gyro != NULL && strcmp(gyro, "MPU6500") == 0) {
        list[Gyro].maxRange = GYRO_MPU6500_RANGE;
        list[Gyro].resolution = GYRO_MPU6500_RESOLUTION;
        list[Gyro].power = GYRO_MPU6500_POWER;
        list[Gyro].minDelay = GYRO_MPU6500_MINDELAY;
    } else if( gyro != NULL && strcmp(gyro, "MPU9150") == 0) {
        list[Gyro].maxRange = GYRO_MPU9150_RANGE;
        list[Gyro].resolution = GYRO_MPU9150_RESOLUTION;
        list[Gyro].power = GYRO_MPU9150_POWER;
        list[Gyro].minDelay = GYRO_MPU9150_MINDELAY;
    } else {
        LOGE("HAL:unknown gyro id -- gyro params will be wrong.");
        LOGE("HAL:default to use mpu3050 params");
        list[Gyro].maxRange = GYRO_MPU3050_RANGE;
        list[Gyro].resolution = GYRO_MPU3050_RESOLUTION;
        list[Gyro].power = GYRO_MPU3050_POWER;
        list[Gyro].minDelay = GYRO_MPU3050_MINDELAY;
    }

    list[RawGyro].maxRange = list[Gyro].maxRange;
    list[RawGyro].resolution = list[Gyro].resolution;
    list[RawGyro].power = list[Gyro].power;
    list[RawGyro].minDelay = list[Gyro].minDelay;

    return;
}

/* fillRV depends on values of accel and compass in the list */
void MPLSensor::fillRV(struct sensor_t *list)
{
    VFUNC_LOG;

    /* compute power on the fly */
    list[RotationVector].power = list[Gyro].power +
                                 list[Accelerometer].power +
                                 list[MagneticField].power;
    list[RotationVector].resolution = .00001;
    list[RotationVector].maxRange = 1.0;
    list[RotationVector].minDelay = 5000;

    return;
}

void MPLSensor::fillOrientation(struct sensor_t *list)
{
    VFUNC_LOG;

    list[Orientation].power = list[Gyro].power +
                              list[Accelerometer].power +
                              list[MagneticField].power;
    list[Orientation].resolution = .00001;
    list[Orientation].maxRange = 360.0;
    list[Orientation].minDelay = 5000;

    return;
}

void MPLSensor::fillGravity( struct sensor_t *list)
{
    VFUNC_LOG;

    list[Gravity].power = list[Gyro].power +
                          list[Accelerometer].power +
                          list[MagneticField].power;
    list[Gravity].resolution = .00001;
    list[Gravity].maxRange = 9.81;
    list[Gravity].minDelay = 5000;

    return;
}

void MPLSensor::fillLinearAccel(struct sensor_t *list)
{
    VFUNC_LOG;

    list[LinearAccel].power = list[Gyro].power +
                          list[Accelerometer].power +
                          list[MagneticField].power;
    list[LinearAccel].resolution = list[Accelerometer].resolution;
    list[LinearAccel].maxRange = list[Accelerometer].maxRange;
    list[LinearAccel].minDelay = 5000;

    return;
}

int MPLSensor::inv_init_sysfs_attributes(void)
{
    VFUNC_LOG;

    unsigned char i = 0;
    char sysfs_path[MAX_SYSFS_NAME_LEN], iio_trigger_path[MAX_SYSFS_NAME_LEN], tbuf[2];
    char *sptr;
    char **dptr;
    int num;

    // get proper (in absolute/relative) IIO path & build MPU's sysfs paths
    // inv_get_sysfs_abs_path(sysfs_path);
    if(INV_SUCCESS != inv_get_sysfs_path(sysfs_path)) {
        ALOGE("MPLSensor failed get sysfs path");
        return -1;
    }
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

    inv_get_iio_trigger_path(iio_trigger_path);

    sprintf(mpu.key, "%s%s", sysfs_path, "/key");
    sprintf(mpu.chip_enable, "%s%s", sysfs_path, "/buffer/enable");
    sprintf(mpu.buffer_length, "%s%s", sysfs_path, "/buffer/length");
    sprintf(mpu.power_state, "%s%s", sysfs_path, "/power_state");
    sprintf(mpu.in_timestamp_en, "%s%s", sysfs_path, "/scan_elements/in_timestamp_en");
    sprintf(mpu.trigger_name, "%s%s", iio_trigger_path, "/name");
    sprintf(mpu.current_trigger, "%s%s", sysfs_path, "/trigger/current_trigger");

    sprintf(mpu.dmp_firmware, "%s%s", sysfs_path,"/dmp_firmware");
    sprintf(mpu.firmware_loaded,"%s%s", sysfs_path, "/firmware_loaded");
    sprintf(mpu.dmp_on,"%s%s", sysfs_path, "/dmp_on");
    sprintf(mpu.dmp_int_on,"%s%s", sysfs_path, "/dmp_int_on");
    sprintf(mpu.dmp_event_int_on,"%s%s", sysfs_path, "/dmp_event_int_on");
    sprintf(mpu.dmp_output_rate,"%s%s", sysfs_path, "/dmp_output_rate");
    sprintf(mpu.tap_on, "%s%s", sysfs_path, "/tap_on");

    // TODO: for self test
    sprintf(mpu.self_test, "%s%s", sysfs_path, "/self_test");

    sprintf(mpu.temperature, "%s%s", sysfs_path, "/temperature");
    sprintf(mpu.gyro_enable, "%s%s", sysfs_path, "/gyro_enable");
    sprintf(mpu.gyro_fifo_rate, "%s%s", sysfs_path, "/sampling_frequency");
    sprintf(mpu.gyro_orient, "%s%s", sysfs_path, "/gyro_matrix");
    sprintf(mpu.gyro_x_fifo_enable, "%s%s", sysfs_path, "/scan_elements/in_anglvel_x_en");
    sprintf(mpu.gyro_y_fifo_enable, "%s%s", sysfs_path, "/scan_elements/in_anglvel_y_en");
    sprintf(mpu.gyro_z_fifo_enable, "%s%s", sysfs_path, "/scan_elements/in_anglvel_z_en");

#ifdef THIRD_PARTY_ACCEL    //BMA250
    /* same as 'mpu.accel_enable' */
    sprintf(mpu.accel_enable, "%s%s", sysfs_path, "/accl_enable");
    sprintf(mpu.accel_fifo_rate, "%s%s", sysfs_path, "/sampling_frequency");
    sprintf(mpu.accel_fsr, "%s%s", sysfs_path, "/NA");
    sprintf(mpu.accel_bias, "%s%s", sysfs_path, "/NA");
    sprintf(mpu.accel_orient, "%s%s", sysfs_path, "/accl_matrix");
#else
    sprintf(mpu.accel_enable, "%s%s", sysfs_path, "/accl_enable");
    sprintf(mpu.accel_fifo_rate, "%s%s", sysfs_path, "/sampling_frequency");
    sprintf(mpu.accel_fsr, "%s%s", sysfs_path, "/in_accel_scale");

    // TODO: for bias settings
    sprintf(mpu.accel_bias, "%s%s", sysfs_path, "/accl_bias");

    sprintf(mpu.accel_orient, "%s%s", sysfs_path, "/accl_matrix");
#endif

    sprintf(mpu.accel_x_fifo_enable, "%s%s", sysfs_path, "/scan_elements/in_accel_x_en");
    sprintf(mpu.accel_y_fifo_enable, "%s%s", sysfs_path, "/scan_elements/in_accel_y_en");
    sprintf(mpu.accel_z_fifo_enable, "%s%s", sysfs_path, "/scan_elements/in_accel_z_en");

    sprintf(mpu.quaternion_on, "%s%s", sysfs_path, "/quaternion_on");
    sprintf(mpu.in_quat_r_en, "%s%s", sysfs_path, "/scan_elements/in_quaternion_r_en");
    sprintf(mpu.in_quat_x_en, "%s%s", sysfs_path, "/scan_elements/in_quaternion_x_en");
    sprintf(mpu.in_quat_y_en, "%s%s", sysfs_path, "/scan_elements/in_quaternion_y_en");
    sprintf(mpu.in_quat_z_en, "%s%s", sysfs_path, "/scan_elements/in_quaternion_z_en");

    sprintf(mpu.display_orientation_on, "%s%s", sysfs_path, "/display_orientation_on");
    sprintf(mpu.event_display_orientation, "%s%s", sysfs_path, "/event_display_orientation");

#if SYSFS_VERBOSE
    // test print sysfs paths
    dptr = (char**)&mpu;
    for (i = 0; i < MAX_SYSFS_ATTRB; i++) {
        LOGE("HAL:sysfs path: %s", *dptr++);
    }
#endif
    return 0;
}

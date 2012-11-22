/* Industrialio buffer test code.
 *
 * Copyright (c) 2008 Jonathan Cameron
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * This program is primarily intended as an example application.
 * Reads the current buffer setup from sysfs and starts a short capture
 * from the specified device, pretty printing the result after appropriate
 * conversion.
 *
 * Command line parameters
 * generic_buffer -n <device_name> -t <trigger_name>
 * If trigger name is not specified the program assumes you want a dataready
 * trigger associated with the device and goes looking for it.
 *
 */

#include <unistd.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>
#include <sys/stat.h>
#include <dirent.h>
#include <linux/types.h>
#include <string.h>
#include <poll.h>
#include "iio_utils.h"
#include "ml_load_dmp.h"
#include "ml_sysfs_helper.h"
#include "authenticate.h"

/**
 * size_from_channelarray() - calculate the storage size of a scan
 * @channels: the channel info array
 * @num_channels: size of the channel info array
 *
 * Has the side effect of filling the channels[i].location values used
 * in processing the buffer output.
 **/
int size_from_channelarray(struct iio_channel_info *channels, int num_channels)
{
	int bytes = 0;
	int i = 0;
	while (i < num_channels) {
		if (bytes % channels[i].bytes == 0)
			channels[i].location = bytes;
		else
			channels[i].location = bytes - bytes%channels[i].bytes
				+ channels[i].bytes;
		bytes = channels[i].location + channels[i].bytes;
		i++;
	}
	return bytes;
}

void print2byte(int input, struct iio_channel_info *info)
{
	/* shift before conversion to avoid sign extension
	   of left aligned data */
	input = input >> info->shift;
	if (info->is_signed) {
		int16_t val = input;
		val &= (1 << info->bits_used) - 1;
		val = (int16_t)(val << (16 - info->bits_used)) >>
			(16 - info->bits_used);
		/*printf("%d, %05f, scale=%05f", val,
		       (float)(val + info->offset)*info->scale, info->scale);*/
		printf("%d, ", val);

	} else {
		uint16_t val = input;
		val &= (1 << info->bits_used) - 1;
		printf("%05f ", ((float)val + info->offset)*info->scale);
	}
}
/**
 * process_scan() - print out the values in SI units
 * @data:		pointer to the start of the scan
 * @infoarray:		information about the channels. Note
 *  size_from_channelarray must have been called first to fill the
 *  location offsets.
 * @num_channels:	the number of active channels
 **/
void process_scan(char *data,
		  struct iio_channel_info *infoarray,
		  int num_channels)
{
	int k;
	//char *tmp;
	for (k = 0; k < num_channels; k++) {
		switch (infoarray[k].bytes) {
			/* only a few cases implemented so far */
		case 2:
			print2byte(*(uint16_t *)(data + infoarray[k].location),
				   &infoarray[k]);
			//tmp = data + infoarray[k].location;
			break;
		case 4:
			if (infoarray[k].is_signed) {
				int32_t val = *(int32_t *)
					(data +
					 infoarray[k].location);
				if ((val >> infoarray[k].bits_used) & 1)
					val = (val & infoarray[k].mask) |
						~infoarray[k].mask;
				/* special case for timestamp */
				printf(" %d ", val);
			}
			break;
		case 8:
			if (infoarray[k].is_signed) {
				int64_t val = *(int64_t *)
					(data +
					 infoarray[k].location);
				if ((val >> infoarray[k].bits_used) & 1)
					val = (val & infoarray[k].mask) |
						~infoarray[k].mask;
				/* special case for timestamp */
				if (infoarray[k].scale == 1.0f &&
				    infoarray[k].offset == 0.0f)
					printf(" %lld", val);
				else
					printf("%05f ", ((float)val +
							 infoarray[k].offset)*
					       infoarray[k].scale);
			}
			break;
		default:
			break;
		}
	}
	printf("\n");
}

void enable_flick(char *p){
	int ret;
	printf("flick:%s\n", p);
	ret = write_sysfs_int_and_verify("flick_int_on", p, 1);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("flick_upper", p, 3147790);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("flick_lower", p, -3147790);
	if (ret < 0)
		return;

	ret = write_sysfs_int_and_verify("flick_counter", p, 50);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("flick_message_on", p, 0);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("flick_axis", p, 0);
}
void HandleOrient(int orient)
{    
    if (orient & 0x01)
	printf("INV_X_UP\n");
    if (orient & 0x02) 
	printf("INV_X_DOWN\n");
    if (orient & 0x04) 
	printf("INV_Y_UP\n");
    if (orient & 0x08) 
	printf("INV_Y_DOWN\n");
    if (orient & 0x10) 
	printf("INV_Z_UP\n");
    if (orient & 0x20) 
	printf("INV_Z_DOWN\n");
    if (orient & 0x40) 
	printf("INV_ORIENTATION_FLIP\n");
}

void HandleTap(int tap)
{
    int tap_dir = tap/8;
    int tap_num = tap%8 + 1;

    switch (tap_dir) {
        case 1:
            printf("INV_TAP_AXIS_X_POS\n");
            break;
        case 2:
            printf("INV_TAP_AXIS_X_NEG\n");
            break;
        case 3:
            printf("INV_TAP_AXIS_Y_POS\n");
            break;
        case 4:
            printf("INV_TAP_AXIS_Y_NEG\n");
            break;
        case 5:
            printf("INV_TAP_AXIS_Z_POS\n");
            break;
        case 6:
            printf("INV_TAP_AXIS_Z_NEG\n");
            break;
        default:
            break;
    }
    printf("Tap number: %d\n", tap_num);
}
void setup_dmp(char *dev_path){
	char sysfs_path[200];
	char dmp_path[200];
	int  ret;
	FILE *fd;
	sprintf(sysfs_path, "%s", dev_path);
	printf("sysfs: %s\n", sysfs_path);
	ret = write_sysfs_int_and_verify("power_state", sysfs_path, 1);
	if (ret < 0)
		return;

	ret = write_sysfs_int("in_accel_scale", dev_path, 0);
	if (ret < 0)
		return;
	ret = write_sysfs_int("in_anglvel_scale", dev_path, 3);
	if (ret < 0)
		return;	
	ret = write_sysfs_int("sampling_frequency", sysfs_path, 200);
	if (ret < 0)
		return;	
	ret = write_sysfs_int_and_verify("firmware_loaded", sysfs_path, 0);
	if (ret < 0)
		return;
	sprintf(dmp_path, "%s/dmp_firmware", dev_path);
	if ((fd = fopen(dmp_path, "wb")) < 0 ) {
		perror("dmp fail");
	}	
	inv_load_dmp(fd);
	fclose(fd);
	printf("firmware_loaded=%d\n", read_sysfs_posint("firmware_loaded", sysfs_path));
	ret = write_sysfs_int_and_verify("dmp_on", sysfs_path, 1);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("dmp_int_on", sysfs_path, 1);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("display_orientation_on", sysfs_path, 1);
	if (ret < 0)
		return;
	enable_flick(sysfs_path);
	ret = write_sysfs_int_and_verify("tap_on", sysfs_path, 1);
	if (ret < 0)
		return;
	ret = write_sysfs_int_and_verify("orientation_on", sysfs_path, 1);
	if (ret < 0)
		return;
}

void get_dmp_event(char *dev_dir_name) {
	char file_name[100];
	int i;
	int fp_flick, fp_tap, fp_orient, fp_disp;
	int data;
	char d[4];
	FILE *fp;
	struct pollfd pfd[4];
	printf("%s\n", dev_dir_name);
	while(1) {
		sprintf(file_name, "%s/event_flick", dev_dir_name);
		fp_flick = open(file_name, O_RDONLY | O_NONBLOCK);
		sprintf(file_name, "%s/event_tap", dev_dir_name);
		fp_tap = open(file_name, O_RDONLY | O_NONBLOCK);
		sprintf(file_name, "%s/event_orientation", dev_dir_name);
		fp_orient = open(file_name, O_RDONLY | O_NONBLOCK);
		sprintf(file_name, "%s/event_display_orientation", dev_dir_name);
		fp_disp = open(file_name, O_RDONLY | O_NONBLOCK);

		pfd[0].fd = fp_flick;
		pfd[0].events = POLLPRI|POLLERR,
		pfd[0].revents = 0;			

		pfd[1].fd = fp_tap;
		pfd[1].events = POLLPRI|POLLERR,
		pfd[1].revents = 0;			

		pfd[2].fd = fp_orient;
		pfd[2].events = POLLPRI|POLLERR,
		pfd[2].revents = 0;			

		pfd[3].fd = fp_disp;
		pfd[3].events = POLLPRI|POLLERR,
		pfd[3].revents = 0;			

		read(fp_flick, d, 4);
		read(fp_tap, d, 4);
		read(fp_orient, d, 4);
		read(fp_disp, d, 4);

		poll(pfd, 4, -1);
		close(fp_flick);
		close(fp_tap);
		close(fp_orient);
		close(fp_disp);

		for (i=0; i< ARRAY_SIZE(pfd); i++) {
			if(pfd[i].revents != 0) {
				switch (i){
				case 0:
					sprintf(file_name, "%s/event_flick", dev_dir_name);
					fp = fopen(file_name, "rt");
					fscanf(fp, "%d\n", &data);
					printf("flick=%x\n", data);
					fclose(fp);
				break;
				case 1:
					sprintf(file_name, "%s/event_tap", dev_dir_name);
					fp = fopen(file_name, "rt");
					fscanf(fp, "%d\n", &data);
					printf("tap=%x\n", data);
					HandleTap(data);
					fclose(fp);
				break;
				case 2:
					sprintf(file_name, "%s/event_orientation", dev_dir_name);
					fp = fopen(file_name, "rt");
					fscanf(fp, "%d\n", &data);
					printf("orient=%x\n", data);
					HandleOrient(data);
					fclose(fp);
				break;
				case 3:
					sprintf(file_name, "%s/event_display_orientation", dev_dir_name);
					fp = fopen(file_name, "rt");
					fscanf(fp, "%d\n", &data);
					printf("display_orient=%x\n", data);
					fclose(fp);
				break;
				}
			}
		}						
	}
}


int main(int argc, char **argv)
{
	unsigned long num_loops = 2;
	unsigned long timedelay = 100000;
	unsigned long buf_len = 128;

	int ret, c, i, j, toread;
	int fp;

	int num_channels;
	char *trigger_name = NULL;
	char *dev_dir_name, *buf_dir_name;

	int datardytrigger = 1;
	char *data;
	int read_size;
	int dev_num, trig_num;
	char *buffer_access;
	int scan_size;
	int noevents = 0;
	int p_event = 0, nodmp = 0;
	char *dummy;
	char chip_name[10];
	char device_name[10];
	char sysfs[100];

	struct iio_channel_info *infoarray;
	/*set r means no DMP is enabled. should be used for mpu3050.
	  set p means no print of data*/
	/* when p is set, 1 means orientation, 2 means tap, 3 means flick*/
	while ((c = getopt(argc, argv, "l:w:c:pret:")) != -1) {
		switch (c) {
		case 't':
			trigger_name = optarg;
			datardytrigger = 0;
			break;
		case 'e':
			noevents = 1;
			break;
		case 'p':
			p_event = 1;
			break;
		case 'r':
			nodmp = 1;
			break;
		case 'c':
			num_loops = strtoul(optarg, &dummy, 10);
			break;
		case 'w':
			timedelay = strtoul(optarg, &dummy, 10);
			break;
		case 'l':
			buf_len = strtoul(optarg, &dummy, 10);
			break;
		case '?':
			return -1;
		}
	}
	inv_get_sysfs_path(sysfs);
	printf("sss:::%s\n", sysfs);
	if (inv_get_chip_name(chip_name) != INV_SUCCESS) {
		printf("get chip name fail\n");
		exit(0);
	}
	printf("chip_name=%s\n", chip_name);
	if (INV_SUCCESS != inv_check_key())
        	printf("key check fail\n");
	else
        	printf("key authenticated\n");

	for (i=0; i<strlen(chip_name); i++) {
		device_name[i] = tolower(chip_name[i]);
	}
	device_name[strlen(chip_name)] = '\0';
	printf("device name: %s\n", device_name);

	/* Find the device requested */
	dev_num = find_type_by_name(device_name, "iio:device");
	if (dev_num < 0) {
		printf("Failed to find the %s\n", device_name);
		ret = -ENODEV;
		goto error_ret;
	}
	printf("iio device number being used is %d\n", dev_num);

	asprintf(&dev_dir_name, "%siio:device%d", iio_dir, dev_num);
	if (trigger_name == NULL) {
		/*
		 * Build the trigger name. If it is device associated it's
		 * name is <device_name>_dev[n] where n matches the device
		 * number found above
		 */
		ret = asprintf(&trigger_name,
			       "%s-dev%d", device_name, dev_num);
		if (ret < 0) {
			ret = -ENOMEM;
			goto error_ret;
		}
	}
	ret = write_sysfs_int_and_verify("gyro_enable", dev_dir_name, 1);
	ret = write_sysfs_int_and_verify("accl_enable", dev_dir_name, 1);
	ret = write_sysfs_int_and_verify("compass_enable", dev_dir_name, 1);
	ret = write_sysfs_int_and_verify("firmware_loaded", dev_dir_name, 0);

	/* Verify the trigger exists */
	trig_num = find_type_by_name(trigger_name, "trigger");
	if (trig_num < 0) {
		printf("Failed to find the trigger %s\n", trigger_name);
		ret = -ENODEV;
		goto error_free_triggername;
	}
	printf("iio trigger number being used is %d\n", trig_num);
	/*
	 * Parse the files in scan_elements to identify what channels are
	 * present
	 */
	ret = 0;
	ret = enable(dev_dir_name, &infoarray, &num_channels);
	if (ret) {
		printf("error enable\n");
		goto error_free_triggername;
	}

	if (nodmp ==0)
		setup_dmp(dev_dir_name);

	/*
	 * Construct the directory name for the associated buffer.
	 * As we know that the lis3l02dq has only one buffer this may
	 * be built rather than found.
	 */
	ret = asprintf(&buf_dir_name, "%siio:device%d/buffer", iio_dir, dev_num);
	if (ret < 0) {
		ret = -ENOMEM;
		goto error_free_triggername;
	}
	printf("%s %s\n", dev_dir_name, trigger_name);

	/* Set the device trigger to be the data rdy trigger found above */
	ret = write_sysfs_string_and_verify("trigger/current_trigger",
					dev_dir_name,
					trigger_name);
	if (ret < 0) {
		printf("Failed to write current_trigger file\n");
		goto error_free_buf_dir_name;
	}
	/* Setup ring buffer parameters */
	/* length must be even number because iio_store_to_sw_ring is expecting 
		half pointer to be equal to the read pointer, which is impossible
		when buflen is odd number. This is actually a bug in the code */
	ret = write_sysfs_int("length", buf_dir_name, buf_len*2);
	if (ret < 0)
		goto exit_here;
	ret = write_sysfs_int_and_verify("gyro_enable", dev_dir_name, 1);
	ret = write_sysfs_int_and_verify("accl_enable", dev_dir_name, 1);
	ret = write_sysfs_int_and_verify("compass_enable", dev_dir_name, 1);
	if (nodmp == 0)
		ret = write_sysfs_int_and_verify("quaternion_on", dev_dir_name, 1);
	else
		ret = disable_q_out(dev_dir_name, &infoarray, &num_channels);		
	ret = build_channel_array(dev_dir_name, &infoarray, &num_channels);
	if (ret) {
		printf("Problem reading scan element information\n");
		goto exit_here;
	}

	/* Enable the buffer */
	ret = write_sysfs_int("enable", buf_dir_name, 1);
	if (ret < 0)
		goto exit_here;
	scan_size = size_from_channelarray(infoarray, num_channels);
	data = malloc(scan_size*buf_len);
	if (!data) {
		ret = -ENOMEM;
		goto exit_here;
	}

	ret = asprintf(&buffer_access,
		       "/dev/iio:device%d",
		       dev_num);
	if (ret < 0) {
		ret = -ENOMEM;
		goto error_free_data;
	}
	if (p_event) {
		get_dmp_event(dev_dir_name);
		goto error_free_buffer_access;
	}
	/* Attempt to open non blocking the access dev */
	fp = open(buffer_access, O_RDONLY | O_NONBLOCK);
	if (fp == -1) { /*If it isn't there make the node */
		printf("Failed to open %s\n", buffer_access);
		ret = -errno;
		goto error_free_buffer_access;
	}
	/* Wait for events 10 times */
	for (j = 0; j < num_loops; j++) {
		if (!noevents) {
			struct pollfd pfd = {
				.fd = fp,
				.events = POLLIN,
			};
			poll(&pfd, 1, -1);
			toread = 1;
			if ((j%128)==0)
				usleep(timedelay);

		} else {
			usleep(timedelay);
			toread = 1;
		}
		read_size = read(fp,
				 data,
				 toread*scan_size);
		if (read_size == -EAGAIN) {
			printf("nothing available\n");
			continue;
		}
		if (0 == p_event) {
			for (i = 0; i < read_size/scan_size; i++)
				process_scan(data + scan_size*i,
					     infoarray,
					     num_channels);
		}
	}
	close(fp);
error_free_buffer_access:
	free(buffer_access);
error_free_data:
	free(data);
exit_here:
	/* Stop the ring buffer */
	ret = write_sysfs_int("enable", buf_dir_name, 0);

error_free_buf_dir_name:
	free(buf_dir_name);
error_free_triggername:
	if (datardytrigger)
		free(trigger_name);
error_ret:
	return ret;
}

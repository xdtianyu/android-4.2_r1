/*
 * WPA Supplicant - driver_wext exported functions
 * Copyright (c) 2003-2005, Jouni Malinen <j@w1.fi>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Alternatively, this software may be distributed under the terms of BSD
 * license.
 *
 * See README and COPYING for more details.
 */

#ifndef DRIVER_WEXT_H
#define DRIVER_WEXT_H

#include <net/if.h>

struct wpa_driver_wext_data {
	void *ctx;
	int event_sock;
	int ioctl_sock;
	int mlme_sock;
	char ifname[IFNAMSIZ + 1];
	int ifindex;
	int ifindex2;
	int if_removed;
	u8 *assoc_req_ies;
	size_t assoc_req_ies_len;
	u8 *assoc_resp_ies;
	size_t assoc_resp_ies_len;
	struct wpa_driver_capa capa;
	int has_capability;
	int we_version_compiled;

	/* for set_auth_alg fallback */
	int use_crypt;
	int auth_alg_fallback;

	int operstate;

	char mlmedev[IFNAMSIZ + 1];

	int scan_complete_events;
#ifdef ANDROID
	int errors;
	int driver_is_started;
	int skip_disconnect;
	int bgscan_enabled;
#endif
};

int wpa_driver_wext_get_ifflags(struct wpa_driver_wext_data *drv, int *flags);
int wpa_driver_wext_set_ifflags(struct wpa_driver_wext_data *drv, int flags);
int wpa_driver_wext_get_bssid(void *priv, u8 *bssid);
int wpa_driver_wext_set_bssid(void *priv, const u8 *bssid);
int wpa_driver_wext_get_ssid(void *priv, u8 *ssid);
int wpa_driver_wext_set_ssid(void *priv, const u8 *ssid, size_t ssid_len);
int wpa_driver_wext_set_freq(void *priv, int freq);
int wpa_driver_wext_set_mode(void *priv, int mode);
int wpa_driver_wext_set_key(void *priv, wpa_alg alg,
			    const u8 *addr, int key_idx,
			    int set_tx, const u8 *seq, size_t seq_len,
			    const u8 *key, size_t key_len);
int wpa_driver_wext_scan(void *priv, const u8 *ssid, size_t ssid_len);
int wpa_driver_wext_combo_scan(void *priv, struct wpa_ssid **ssid_ptr,
			       struct wpa_ssid *ssid_conf);
struct wpa_scan_results * wpa_driver_wext_get_scan_results(void *priv);

void wpa_driver_wext_scan_timeout(void *eloop_ctx, void *timeout_ctx);

int wpa_driver_wext_alternative_ifindex(struct wpa_driver_wext_data *drv,
					const char *ifname);

void * wpa_driver_wext_init(void *ctx, const char *ifname);
void wpa_driver_wext_deinit(void *priv);

int wpa_driver_wext_set_operstate(void *priv, int state);
int wpa_driver_wext_get_version(struct wpa_driver_wext_data *drv);

int wpa_driver_wext_associate(void *priv,
			      struct wpa_driver_associate_params *params);
int wpa_driver_wext_get_capa(void *priv, struct wpa_driver_capa *capa);
int wpa_driver_wext_set_auth_param(struct wpa_driver_wext_data *drv,
				   int idx, u32 value);
int wpa_driver_wext_cipher2wext(int cipher);
int wpa_driver_wext_keymgmt2wext(int keymgmt);

#ifdef ANDROID
#define WEXT_NUMBER_SCAN_CHANNELS_FCC	11
#define WEXT_NUMBER_SCAN_CHANNELS_ETSI	13
#define WEXT_NUMBER_SCAN_CHANNELS_MKK1	14

#define WPA_DRIVER_WEXT_WAIT_US		400000
#define MAX_DRV_CMD_SIZE		248
#define WEXT_NUMBER_SEQUENTIAL_ERRORS	4
#define WEXT_CSCAN_AMOUNT		9
#define WEXT_CSCAN_BUF_LEN		360
#define WEXT_CSCAN_HEADER		"CSCAN S\x01\x00\x00S\x00"
#define WEXT_CSCAN_HEADER_SIZE		12
#define WEXT_CSCAN_SSID_SECTION		'S'
#define WEXT_CSCAN_CHANNEL_SECTION	'C'
#define WEXT_CSCAN_NPROBE_SECTION	'N'
#define WEXT_CSCAN_ACTV_DWELL_SECTION	'A'
#define WEXT_CSCAN_PASV_DWELL_SECTION	'P'
#define WEXT_CSCAN_HOME_DWELL_SECTION	'H'
#define WEXT_CSCAN_TYPE_SECTION		'T'
#define WEXT_CSCAN_TYPE_DEFAULT		0
#define WEXT_CSCAN_TYPE_PASSIVE		1
#define WEXT_CSCAN_PASV_DWELL_TIME	130
#define WEXT_CSCAN_PASV_DWELL_TIME_DEF	250
#define WEXT_CSCAN_PASV_DWELL_TIME_MAX	3000
#define WEXT_CSCAN_HOME_DWELL_TIME	130

#define WEXT_PNOSETUP_HEADER            "PNOSETUP "
#define WEXT_PNOSETUP_HEADER_SIZE       9
#define WEXT_PNO_TLV_VERSION            '1'
#define WEXT_PNO_TLV_SUBVERSION         '2'
#define WEXT_PNO_TLV_RESERVED           '0'
#define WEXT_PNO_VERSION_SIZE           4
#define WEXT_PNO_AMOUNT                 16
#define WEXT_PNO_SSID_SECTION           'S'
/* SSID header size is SSID section type above + SSID length */
#define WEXT_PNO_SSID_HEADER_SIZE       2
#define WEXT_PNO_SCAN_INTERVAL_SECTION  'T'
#define WEXT_PNO_SCAN_INTERVAL_LENGTH   2
#define WEXT_PNO_SCAN_INTERVAL          30
/* Scan interval size is scan interval section type + scan interval length above*/
#define WEXT_PNO_SCAN_INTERVAL_SIZE     (1 + WEXT_PNO_SCAN_INTERVAL_LENGTH)
#define WEXT_PNO_REPEAT_SECTION         'R'
#define WEXT_PNO_REPEAT_LENGTH          1
#define WEXT_PNO_REPEAT                 4
/* Repeat section size is Repeat section type + Repeat value length above*/
#define WEXT_PNO_REPEAT_SIZE            (1 + WEXT_PNO_REPEAT_LENGTH)
#define WEXT_PNO_MAX_REPEAT_SECTION     'M'
#define WEXT_PNO_MAX_REPEAT_LENGTH      1
#define WEXT_PNO_MAX_REPEAT             3
/* Max Repeat section size is Max Repeat section type + Max Repeat value length above*/
#define WEXT_PNO_MAX_REPEAT_SIZE        (1 + WEXT_PNO_MAX_REPEAT_LENGTH)
/* This corresponds to the size of all sections expect SSIDs */
#define WEXT_PNO_NONSSID_SECTIONS_SIZE  (WEXT_PNO_SCAN_INTERVAL_SIZE + WEXT_PNO_REPEAT_SIZE + WEXT_PNO_MAX_REPEAT_SIZE)
/* PNO Max command size is total of header, version, ssid and other sections + Null termination */
#define WEXT_PNO_MAX_COMMAND_SIZE       (WEXT_PNOSETUP_HEADER_SIZE + WEXT_PNO_VERSION_SIZE \
					+ WEXT_PNO_AMOUNT * (WEXT_PNO_SSID_HEADER_SIZE + IW_ESSID_MAX_SIZE) \
					+ WEXT_PNO_NONSSID_SECTIONS_SIZE + 1)
#endif

#endif /* DRIVER_WEXT_H */

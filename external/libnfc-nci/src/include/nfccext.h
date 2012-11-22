/******************************************************************************
 *
 *  Copyright (C) 1999-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
/****************************************************************************/
/*  NFCC global definitions                                                 */
/****************************************************************************/

#ifndef NFCC_EXT_H
#define NFCC_EXT_H

#include "bt_target.h"
#include "nci_cmsgs.h"
#include "nci_defs.h"

extern void    nfcc_init (void);
extern void * nfcc_find_conn_cb_by_conn_id(UINT8 conn_id);
extern void     nfcc_proc_nfcee_discover(void);
extern void     nfcc_proc_nfcee_uicc_vse_test(UINT8 ee_handle, UINT8 mode);
extern void    lm_process_nfc (BT_HDR *p_msg);

/*
** Definitions for events sent from NFCC to LMP
*/
#define NFCC_EVENT_START_DISCOVER       1    /* forward Start Discover cmd to peer NFCC */
#define NFCC_EVENT_DISCOVER_RESP        2    /* A response to a Discover cmd       */
#define NFCC_EVENT_DATA                 3    /* A response to a Discover cmd       */
#define NFCC_EVENT_DISCOVER_SELECT      4    /* forward Start Discover cmd to peer NFCC */
#define NFCC_EVENT_DEACTIVATE           5    /* forward deactivate cmd to peer NFCC */


#define NFCC_MAX_DISCOVER_PARAMS        7

/* this does not work if more than 329 */
#define NFCC_MAX_PARAM_TLV_LEN          328

/* Discovery Type Masks - not in spec; for convenience/based on NCI_DISCOVERY_TYPE* */
#define NCI_DISCOVERY_MASK_POLL_A       0x0001
#define NCI_DISCOVERY_MASK_POLL_B       0x0002
#define NCI_DISCOVERY_MASK_POLL_F       0x0004
#define NCI_DISCOVERY_MASK_LISTEN_A     0x0100
#define NCI_DISCOVERY_MASK_LISTEN_B     0x0200
#define NCI_DISCOVERY_MASK_LISTEN_F     0x0400
#define NCI_DISCOVERY_MASK_MAX          0x070F

typedef UINT16 tNCI_DISCOVERY_MASK;

#define NFCC_NUM_NFCEE      3
/*
** Define a buffer that is used for data descriptors from HCID to LC
*/
typedef struct
{
    BT_HDR                  hdr;        /* Standard BT header               */
    tNCI_DISCOVERY_MASK     mask;       /* sender is looking for anything in this mask */
    UINT8                   num_params;
    tNCI_DISCOVER_PARAMS    params[NFCC_MAX_DISCOVER_PARAMS];
    UINT8                   param_tlv[NFCC_MAX_PARAM_TLV_LEN];
    UINT16                  param_tlv_len;
} tNFCC_START_DISCOVER;

typedef struct
{
    BT_HDR                  hdr;        /* Standard BT header               */
    tNCI_DISCOVERY_MASK     mask;       /* sender is looking for anything in this mask */
    UINT8                   num_params;
    tNCI_DISCOVER_PARAMS    params[NFCC_MAX_DISCOVER_PARAMS];
    UINT8                   param_tlv[NFCC_MAX_PARAM_TLV_LEN];
    UINT16                  param_tlv_len;
} tNFCC_DISCOVER_RESP;

typedef struct
{
    BT_HDR                  hdr;        /* Standard BT header               */
    UINT8                   target_handle;
    UINT8                   protocol;
} tNFCC_DISCOVER_SELECT;

/********************************************************************************
**
** Structures used for NFCC Simulation to communicate with peer NFCC Simulation
*/
typedef union
{
    BT_HDR                  hdr;
    tNFCC_START_DISCOVER    start_discover;
    tNFCC_DISCOVER_RESP     discover_resp;
    tNFCC_DISCOVER_SELECT   discover_select;
} tNFCC_TO_PEER;


#endif


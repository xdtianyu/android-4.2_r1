/******************************************************************************
 *
 *  Copyright (C) 2012 Broadcom Corporation
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

/******************************************************************************
 *
 *  NFC Hardware Abstraction Layer API
 *
 ******************************************************************************/
#ifndef NFC_HAL_API_H
#define NFC_HAL_API_H
#include <hardware/nfc.h>
#include "data_types.h"

/****************************************************************************
** NFC_HDR header definition for NFC messages
*****************************************************************************/
typedef struct
{
    UINT16          event;
    UINT16          len;
    UINT16          offset;
    UINT16          layer_specific;
} NFC_HDR;
#define NFC_HDR_SIZE (sizeof (NFC_HDR))

typedef UINT8 tHAL_NFC_STATUS;

typedef void (tHAL_NFC_STATUS_CBACK) (tHAL_NFC_STATUS status);
typedef void (tHAL_NFC_CBACK) (UINT8 event, tHAL_NFC_STATUS status);
typedef void (tHAL_NFC_DATA_CBACK) (UINT16 data_len, UINT8   *p_data);

/*******************************************************************************
** tHAL_NFC_ENTRY HAL entry-point lookup table
*******************************************************************************/

typedef void (tHAL_API_INITIALIZE) (void);
typedef void (tHAL_API_TERMINATE) (void);
typedef void (tHAL_API_OPEN) (tHAL_NFC_CBACK *p_hal_cback, tHAL_NFC_DATA_CBACK *p_data_cback);
typedef void (tHAL_API_CLOSE) (void);
typedef void (tHAL_API_CORE_INITIALIZED) (UINT8 *p_core_init_rsp_params);
typedef void (tHAL_API_WRITE) (UINT16 data_len, UINT8 *p_data);
typedef BOOLEAN (tHAL_API_PREDISCOVER) (void);
typedef void (tHAL_API_CONTROL_GRANTED) (void);
typedef void (tHAL_API_POWER_CYCLE) (void);


typedef struct
{
    tHAL_API_INITIALIZE *initialize;
    tHAL_API_TERMINATE *terminate;
    tHAL_API_OPEN *open;
    tHAL_API_CLOSE *close;
    tHAL_API_CORE_INITIALIZED *core_initialized;
    tHAL_API_WRITE *write;
    tHAL_API_PREDISCOVER *prediscover;
    tHAL_API_CONTROL_GRANTED *control_granted;
    tHAL_API_POWER_CYCLE *power_cycle;


} tHAL_NFC_ENTRY;


/*******************************************************************************
** HAL API Function Prototypes
*******************************************************************************/
#ifdef __cplusplus
extern "C"
{
#endif

/* Toolset-specific macro for exporting API funcitons */
#if (defined(NFC_HAL_TARGET) && (NFC_HAL_TARGET == TRUE)) && (defined(_WINDLL))
#define EXPORT_HAL_API  __declspec(dllexport)
#else
#define EXPORT_HAL_API
#endif

/*******************************************************************************
**
** Function         HAL_NfcInitialize
**
** Description      Called when HAL library is loaded.
**
**                  Initialize GKI and start the HCIT task
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcInitialize(void);

/*******************************************************************************
**
** Function         HAL_NfcTerminate
**
** Description      Called to terminate NFC HAL
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcTerminate(void);

/*******************************************************************************
**
** Function         HAL_NfcOpen
**
** Description      Open transport and intialize the NFCC, and
**                  Register callback for HAL event notifications,
**
**                  HAL_OPEN_CPLT_EVT will notify when operation is complete.
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcOpen (tHAL_NFC_CBACK *p_hal_cback, tHAL_NFC_DATA_CBACK *p_data_cback);

/*******************************************************************************
**
** Function         HAL_NfcClose
**
** Description      Prepare for shutdown. A HAL_CLOSE_CPLT_EVT will be
**                  reported when complete.
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcClose (void);

/*******************************************************************************
**
** Function         HAL_NfcCoreInitialized
**
** Description      Called after the CORE_INIT_RSP is received from the NFCC.
**                  At this time, the HAL can do any chip-specific configuration,
**                  and when finished signal the libnfc-nci with event
**                  HAL_POST_INIT_CPLT_EVT.
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcCoreInitialized (UINT8 *p_core_init_rsp_params);

/*******************************************************************************
**
** Function         HAL_NfcWrite
**
** Description      Send an NCI control message or data packet to the
**                  transport. If an NCI command message exceeds the transport
**                  size, HAL is responsible for fragmenting it, Data packets
**                  must be of the correct size.
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcWrite (UINT16 data_len, UINT8 *p_data);

/*******************************************************************************
**
** Function         HAL_NfcPreDiscover
**
** Description      Perform any vendor-specific pre-discovery actions (if needed)
**                  If any actions were performed TRUE will be returned, and
**                  HAL_PRE_DISCOVER_CPLT_EVT will notify when actions are
**                  completed.
**
** Returns          TRUE if vendor-specific pre-discovery actions initialized
**                  FALSE if no vendor-specific pre-discovery actions are needed.
**
*******************************************************************************/
EXPORT_HAL_API BOOLEAN HAL_NfcPreDiscover (void);

/*******************************************************************************
**
** Function         HAL_NfcControlGranted
**
** Description      Grant control to HAL control for sending NCI commands.
**
**                  Call in response to HAL_REQUEST_CONTROL_EVT.
**
**                  Must only be called when there are no NCI commands pending.
**
**                  HAL_RELEASE_CONTROL_EVT will notify when HAL no longer
**                  needs control of NCI.
**
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcControlGranted (void);

/*******************************************************************************
**
** Function         HAL_NfcPowerCycle
**
** Description      Restart NFCC by power cyle
**
**                  HAL_OPEN_CPLT_EVT will notify when operation is complete.
**
** Returns          void
**
*******************************************************************************/
EXPORT_HAL_API void HAL_NfcPowerCycle (void);


#ifdef __cplusplus
}
#endif

#endif /* NFC_HAL_API_H  */

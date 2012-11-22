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
 *  HAL Adaptation Interface (HAI). This interface regulates the interaction
 *  between standard Android HAL and Broadcom-specific HAL.  It adapts
 *  Broadcom-specific features to the Android framework.
 *
 ******************************************************************************/
#define LOG_TAG "NfcNciHal"
#include "OverrideLog.h"
#include "HalAdaptation.h"
#include "SyncEvent.h"
#include "config.h"
#include "nfc_hal_int.h"
#include "nfc_hal_post_reset.h"
#include <errno.h>
#include <pthread.h>


///////////////////////////////////////
// private declaration, definition


static nfc_stack_callback_t* gAndroidHalCallback = NULL;
static nfc_stack_data_callback_t* gAndroidHalDataCallback = NULL;
static SyncEvent gOpenCompletedEvent;
static SyncEvent gPostInitCompletedEvent;
static SyncEvent gCloseCompletedEvent;

UINT32 ScrProtocolTraceFlag = SCR_PROTO_TRACE_ALL; //0x017F00;

static void BroadcomHalCallback (UINT8 event, tHAL_NFC_STATUS status);
static void BroadcomHalDataCallback (UINT16 data_len, UINT8* p_data);


///////////////////////////////////////


int HaiInitializeLibrary (const bcm2079x_dev_t* device)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;
    unsigned long freq = 0;
    unsigned long num = 0;

    InitializeGlobalAppLogLevel ();

    //initialize the crystal frequency
    if (GetNumValue((char*)NAME_XTAL_FREQUENCY, &freq, sizeof(freq)))
    {
        ALOGD("%s: setting xtal frequency=%lu", __FUNCTION__, freq);
        nfc_post_reset_cb.dev_init_config.xtal_freq = (UINT16) freq;
        nfc_post_reset_cb.dev_init_config.flags |= NFC_HAL_DEV_INIT_FLAGS_SET_XTAL_FREQ;
    }

    // Initialize protocol logging level
    if ( GetNumValue ( NAME_PROTOCOL_TRACE_LEVEL, &num, sizeof ( num ) ) )
        ScrProtocolTraceFlag = num;

    HAL_NfcInitialize ();

    // Initialize appliation logging level
    if ( GetNumValue ( NAME_APPL_TRACE_LEVEL, &num, sizeof ( num ) ) ) {
        HAL_NfcSetTraceLevel(num);
    }

    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiTerminateLibrary ()
{
    int retval = EACCES;
    ALOGD ("%s: enter", __FUNCTION__);

    HAL_NfcTerminate ();
    gAndroidHalCallback = NULL;
    gAndroidHalDataCallback = NULL;
    GKI_shutdown ();
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiOpen (const bcm2079x_dev_t* device, nfc_stack_callback_t* halCallbackFunc, nfc_stack_data_callback_t* halDataCallbackFunc)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;

    gAndroidHalCallback = halCallbackFunc;
    gAndroidHalDataCallback = halDataCallbackFunc;

    SyncEventGuard guard (gOpenCompletedEvent);
    HAL_NfcOpen (BroadcomHalCallback, BroadcomHalDataCallback);
    gOpenCompletedEvent.wait ();

    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


void BroadcomHalCallback (UINT8 event, tHAL_NFC_STATUS status)
{
    ALOGD ("%s: enter; event=0x%X", __FUNCTION__, event);
    switch (event)
    {
    case HAL_NFC_OPEN_CPLT_EVT:
        {
            ALOGD ("%s: HAL_NFC_OPEN_CPLT_EVT; status=0x%X", __FUNCTION__, status);
            SyncEventGuard guard (gOpenCompletedEvent);
            gOpenCompletedEvent.notifyOne ();
            break;
        }

    case HAL_NFC_POST_INIT_CPLT_EVT:
        {
            ALOGD ("%s: HAL_NFC_POST_INIT_CPLT_EVT", __FUNCTION__);
            SyncEventGuard guard (gPostInitCompletedEvent);
            gPostInitCompletedEvent.notifyOne ();
            break;
        }

    case HAL_NFC_CLOSE_CPLT_EVT:
        {
            ALOGD ("%s: HAL_NFC_CLOSE_CPLT_EVT", __FUNCTION__);
            SyncEventGuard guard (gCloseCompletedEvent);
            gCloseCompletedEvent.notifyOne ();
            break;
        }

    case HAL_NFC_ERROR_EVT:
        {
            ALOGD ("%s: HAL_NFC_ERROR_EVT", __FUNCTION__);
            {
                SyncEventGuard guard (gOpenCompletedEvent);
                gOpenCompletedEvent.notifyOne ();
            }
            {
                SyncEventGuard guard (gPostInitCompletedEvent);
                gPostInitCompletedEvent.notifyOne ();
            }
            {
                SyncEventGuard guard (gCloseCompletedEvent);
                gCloseCompletedEvent.notifyOne ();
            }
            break;
        }
    }
    gAndroidHalCallback (event, status);
    ALOGD ("%s: exit; event=0x%X", __FUNCTION__, event);
}


void BroadcomHalDataCallback (UINT16 data_len, UINT8* p_data)
{
    ALOGD ("%s: enter; len=%u", __FUNCTION__, data_len);
    gAndroidHalDataCallback (data_len, p_data);
}


int HaiClose (const bcm2079x_dev_t* device)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;

    SyncEventGuard guard (gCloseCompletedEvent);
    HAL_NfcClose ();
    gCloseCompletedEvent.wait ();
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiCoreInitialized (const bcm2079x_dev_t* device, uint8_t* coreInitResponseParams)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;

    SyncEventGuard guard (gPostInitCompletedEvent);
    HAL_NfcCoreInitialized (coreInitResponseParams);
    gPostInitCompletedEvent.wait ();
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiWrite (const bcm2079x_dev_t* dev, uint16_t dataLen, const uint8_t* data)
{
    ALOGD ("%s: enter; len=%u", __FUNCTION__, dataLen);
    int retval = EACCES;

    HAL_NfcWrite (dataLen, const_cast<UINT8*> (data));
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiPreDiscover (const bcm2079x_dev_t* device)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;

    HAL_NfcPreDiscover ();
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiControlGranted (const bcm2079x_dev_t* device)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;

    HAL_NfcControlGranted ();
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}


int HaiPowerCycle (const bcm2079x_dev_t* device)
{
    ALOGD ("%s: enter", __FUNCTION__);
    int retval = EACCES;

    HAL_NfcPowerCycle ();
    retval = 0;
    ALOGD ("%s: exit %d", __FUNCTION__, retval);
    return retval;
}

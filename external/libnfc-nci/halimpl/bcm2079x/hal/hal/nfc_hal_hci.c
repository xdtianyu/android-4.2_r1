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
 *  Vendor-specific handler for HCI events
 *
 ******************************************************************************/
#include "gki.h"
#include "nfc_hal_api.h"
#include "nfc_hal_int.h"
#include "nfc_hal_nv_ci.h"
#include "nfc_hal_nv_co.h"

#include <string.h>
#include "nfc_hal_nv_co.h"

#ifndef NFC_HAL_HCI_NV_READ_TIMEOUT
#define NFC_HAL_HCI_NV_READ_TIMEOUT    1000
#endif

#ifndef NFC_HAL_HCI_NFCC_RSP_TIMEOUT
#define NFC_HAL_HCI_NFCC_RSP_TIMEOUT   3000
#endif

static void nfc_hal_hci_set_next_hci_netwk_config (UINT8 block);
static void nfc_hal_hci_handle_nv_read (UINT8 block, tHAL_NFC_STATUS status, UINT16 size);
static void nfc_hal_hci_init_complete (tHAL_NFC_STATUS status);
static void nfc_hal_hci_vsc_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data);

/*******************************************************************************
**
** Function         nfc_hal_hci_evt_hdlr
**
** Description      Processing event for NFA HCI
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_evt_hdlr (tNFC_HAL_HCI_EVENT_DATA *p_evt_data)
{
    switch (p_evt_data->hdr.event)
    {
    case NFC_HAL_HCI_RSP_NV_READ_EVT:
        nfc_hal_hci_handle_nv_read (p_evt_data->nv_read.block, p_evt_data->nv_read.status, p_evt_data->nv_read.size);
        break;

    case NFC_HAL_HCI_RSP_NV_WRITE_EVT:
        /* NV Ram write completed - nothing to do... */
        break;

    default:
        break;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_enable
**
** Description      Program nv data on to controller
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_hci_enable (void)
{

    UINT8 *p_hci_netwk_cmd;

    if (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf = NULL;
    }

    if (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_info_buf = NULL;
    }

    if ((p_hci_netwk_cmd = (UINT8 *) GKI_getbuf (NCI_MSG_HDR_SIZE + NFC_HAL_HCI_NETWK_INFO_SIZE)) == NULL)
    {
        NCI_TRACE_ERROR0 ("nfc_hal_hci_enable: unable to allocate buffer for reading hci network info from nvram");
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
    }
    else
    {
        nfc_hal_cb.hci_cb.p_hci_netwk_info_buf   = (UINT8 *) (p_hci_netwk_cmd + NCI_MSG_HDR_SIZE);
        nfc_hal_cb.hci_cb.hci_netwk_config_block = 0;
        memset (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, 0, NFC_HAL_HCI_NETWK_INFO_SIZE);
        nfc_hal_nv_co_read ((UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, NFC_HAL_HCI_NETWK_INFO_SIZE, HC_F3_NV_BLOCK);
        nfc_hal_main_start_quick_timer (&nfc_hal_cb.hci_cb.hci_timer, NFC_HAL_HCI_VSC_TIMEOUT_EVT, NFC_HAL_HCI_NV_READ_TIMEOUT);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_hci_netwk_info
**
** Description      Handler function for HCI Network Notification
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_handle_hci_netwk_info (UINT8 *p_data)
{
    UINT8  *p = p_data;
    UINT16 data_len;
    UINT8  target_handle;
    UINT8   hci_netwk_cmd[1 + NFC_HAL_HCI_SESSION_ID_LEN];

    NCI_TRACE_DEBUG0 ("nfc_hal_hci_handle_hci_netwk_info()");

    /* skip NCI header byte0 (MT,GID), byte1 (OID) */
    p += 2;

    STREAM_TO_UINT8 (data_len, p);
    target_handle = *(UINT8 *) p;

    if (target_handle == NFC_HAL_HCI_DH_TARGET_HANDLE)
        nfc_hal_nv_co_write (p, data_len,HC_DH_NV_BLOCK);

    else if (target_handle == NFC_HAL_HCI_UICC0_TARGET_HANDLE)
    {
        if (p[12] & 0x80)
        {
            /* HCI Network notification received for UICC 0, Update nv data */
            nfc_hal_nv_co_write (p, data_len,HC_F3_NV_BLOCK);
        }
        else
        {
            NCI_TRACE_DEBUG1 ("nfc_hal_hci_handle_hci_netwk_info(): Type A Card Emulation invalid, Reset nv file: 0x%02x", p[15]);
            hci_netwk_cmd[0] = NFC_HAL_HCI_UICC0_TARGET_HANDLE;
            memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F3_NV_BLOCK);
        }
    }
    else if (target_handle == NFC_HAL_HCI_UICC1_TARGET_HANDLE)
    {
        if (p[12] & 0x80)
        {
            /* HCI Network notification received for UICC 1, Update nv data */
            nfc_hal_nv_co_write (p, data_len,HC_F4_NV_BLOCK);
        }
        else
        {
            NCI_TRACE_DEBUG1 ("nfc_hal_hci_handle_hci_netwk_info(): Type A Card Emulation invalid, Reset nv file: 0x%02x", p[15]);
            hci_netwk_cmd[0] = NFC_HAL_HCI_UICC1_TARGET_HANDLE;
            /* Reset Session ID */
            memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F4_NV_BLOCK);
        }
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_hcp_pkt
**
** Description      Handle HCP Packet
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_handle_hcp_pkt (UINT8 *p_data)
{
    UINT8   chaining_bit;
    UINT8   pipe;
    UINT8   type;
    UINT8   inst;
    UINT8   hci_netwk_cmd[1 + NFC_HAL_HCI_SESSION_ID_LEN];
    UINT8   source_host;

    chaining_bit = ((*p_data) >> 0x07) & 0x01;
    pipe = (*p_data++) & 0x7F;

    if (  (chaining_bit)
        &&(pipe == NFC_HAL_HCI_ADMIN_PIPE)  )
    {
        type  = ((*p_data) >> 0x06) & 0x03;

        if (type == NFC_HAL_HCI_COMMAND_TYPE)
        {
            inst  = (*p_data++ & 0x3F);

            if (inst == NFC_HAL_HCI_ADM_NOTIFY_ALL_PIPE_CLEARED)
            {

                STREAM_TO_UINT8 (source_host, p_data);

                NCI_TRACE_DEBUG1 ("nfc_hal_hci_handle_hcp_pkt(): Received Clear All pipe command for UICC: 0x%02x", source_host);
                if (source_host == NFC_HAL_HCI_HOST_ID_UICC0)
                {
                    hci_netwk_cmd[0] = NFC_HAL_HCI_UICC0_TARGET_HANDLE;
                    /* Reset Session ID */
                    memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
                    nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F3_NV_BLOCK);
                    NCI_TRACE_DEBUG1 ("nfc_hal_hci_handle_hcp_pkt(): Sent command to reset nv file for block: 0x%02x", HC_F3_NV_BLOCK);
                }
                else if (source_host == NFC_HAL_HCI_HOST_ID_UICC1)
                {
                    hci_netwk_cmd[0] = NFC_HAL_HCI_UICC1_TARGET_HANDLE;
                    /* Reset Session ID */
                    memset (&hci_netwk_cmd[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
                    nfc_hal_nv_co_write (hci_netwk_cmd, 1, HC_F4_NV_BLOCK);
                    NCI_TRACE_DEBUG1 ("nfc_hal_hci_handle_hcp_pkt(): Sent command to reset nv file for block: 0x%02x", HC_F4_NV_BLOCK);
                }
            }
        }
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_handle_nv_read
**
** Description      handler function for nv read complete event
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_handle_nv_read (UINT8 block, tHAL_NFC_STATUS status, UINT16 size)
{
    NFC_HDR *p_data = NULL;
    UINT8   *p;
    UINT8   *p_hci_netwk_info = NULL;

    /* Stop timer as NVDATA Read Completed */
    nfc_hal_main_stop_quick_timer (&nfc_hal_cb.hci_cb.hci_timer);

    switch (block)
    {
    case HC_F3_NV_BLOCK:
    case HC_F4_NV_BLOCK:
        if (  (status != HAL_NFC_STATUS_OK)
            ||(size > NFC_HAL_HCI_NETWK_INFO_SIZE)  )
        {
            NCI_TRACE_DEBUG0 ("nfc_hal_hci_handle_nv_read: Invalid data from nv memory, Set DEFAULT Configuration!");
            memset (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, 0, NFC_HAL_HCI_NETWK_INFO_SIZE);
            nfc_hal_cb.hci_cb.p_hci_netwk_info_buf[0] = (block == HC_F3_NV_BLOCK) ? NFC_HAL_HCI_UICC0_TARGET_HANDLE : NFC_HAL_HCI_UICC1_TARGET_HANDLE;
            memset (&nfc_hal_cb.hci_cb.p_hci_netwk_info_buf[1], 0xFF, NFC_HAL_HCI_SESSION_ID_LEN);
            size = NFC_HAL_HCI_NETWK_INFO_SIZE;
        }

        p_hci_netwk_info = (UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_info_buf - NCI_MSG_HDR_SIZE;
        break;

    case HC_DH_NV_BLOCK:
        if (  (status == HAL_NFC_STATUS_OK)
            &&(size <= NFC_HAL_HCI_DH_NETWK_INFO_SIZE)  )
        {
            p_hci_netwk_info = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE);
        }
        else
        {
            NCI_TRACE_ERROR0 ("nfc_hal_hci_handle_nv_read: Invalid data from nv memory, Skip DH Configuration!");
        }
        break;

    default:
        return;
    }

    if (p_hci_netwk_info)
    {
        p = p_hci_netwk_info;
        /* Send HCI Network ntf command using nv data */
        NCI_MSG_BLD_HDR0 (p, NCI_MT_CMD, NCI_GID_PROP);
        NCI_MSG_BLD_HDR1 (p, NCI_MSG_HCI_NETWK);
        UINT8_TO_STREAM (p, (UINT8) size);

        nfc_hal_dm_send_nci_cmd (p_hci_netwk_info, (UINT16) (NCI_MSG_HDR_SIZE + size), nfc_hal_hci_vsc_cback);

        nfc_hal_cb.hci_cb.hci_netwk_config_block = block;
    }
    else
    {
        /* Set next HCI Network configuration */
        nfc_hal_hci_set_next_hci_netwk_config (block);
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_init_complete
**
** Description      Notify VSC initialization is complete
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_init_complete (tHAL_NFC_STATUS status)
{
    UINT8 *p_hci_netwk_cmd;

    if (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf = NULL;
    }

    if (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf)
    {
        p_hci_netwk_cmd = (UINT8 *) (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf - NCI_MSG_HDR_SIZE);
        GKI_freebuf (p_hci_netwk_cmd);
        nfc_hal_cb.hci_cb.p_hci_netwk_info_buf = NULL;
    }

    NFC_HAL_SET_INIT_STATE (NFC_HAL_INIT_STATE_IDLE);
    nfc_hal_cb.p_stack_cback (HAL_NFC_POST_INIT_CPLT_EVT, HAL_NFC_STATUS_OK);
}

/*******************************************************************************
**
** Function         nfc_hal_hci_set_next_hci_netwk_config
**
** Description      set next hci network configuration
**
** Returns          None
**
*******************************************************************************/
void nfc_hal_hci_set_next_hci_netwk_config (UINT8 block)
{
    UINT8 *p_hci_netwk_cmd;

    switch (block)
    {
    case HC_F3_NV_BLOCK:
        /* Send command to read nvram data for 0xF4 */
        memset (nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, 0, NFC_HAL_HCI_NETWK_INFO_SIZE);
        nfc_hal_nv_co_read ((UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_info_buf, NFC_HAL_HCI_NETWK_INFO_SIZE, HC_F4_NV_BLOCK);
        nfc_hal_main_start_quick_timer (&nfc_hal_cb.hci_cb.hci_timer, NFC_HAL_HCI_VSC_TIMEOUT_EVT, NFC_HAL_HCI_NV_READ_TIMEOUT);
        break;

    case HC_F4_NV_BLOCK:
        if ((p_hci_netwk_cmd = (UINT8 *) GKI_getbuf (NCI_MSG_HDR_SIZE + NFC_HAL_HCI_DH_NETWK_INFO_SIZE)) == NULL)
        {
            NCI_TRACE_ERROR0 ("nfc_hal_hci_set_next_hci_netwk_config: unable to allocate buffer for reading hci network info from nvram");
            nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
        }
        else
        {
            nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf   = (UINT8 *) (p_hci_netwk_cmd + NCI_MSG_HDR_SIZE);
            /* Send command to read nvram data for 0xF2 */
            memset (nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf, 0, NFC_HAL_HCI_DH_NETWK_INFO_SIZE);
            nfc_hal_nv_co_read ((UINT8 *) nfc_hal_cb.hci_cb.p_hci_netwk_dh_info_buf, NFC_HAL_HCI_DH_NETWK_INFO_SIZE, HC_DH_NV_BLOCK);
            nfc_hal_main_start_quick_timer (&nfc_hal_cb.hci_cb.hci_timer, NFC_HAL_HCI_VSC_TIMEOUT_EVT, NFC_HAL_HCI_NV_READ_TIMEOUT);
        }
        break;

    case HC_DH_NV_BLOCK:
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_OK);
        break;

    default:
        NCI_TRACE_ERROR1 ("nfc_hal_hci_set_next_hci_netwk_config: unable to allocate buffer to send VSC 0x%02x", block);
        /* Brcm initialization failed */
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
        break;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_hci_vsc_cback
**
** Description      process VS callback event from stack
**
** Returns          none
**
*******************************************************************************/
static void nfc_hal_hci_vsc_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data)
{
    UINT8 *p_ret = NULL;
    UINT8 status;

    p_ret  = p_data + NCI_MSG_HDR_SIZE;
    status = *p_ret;

    if (event  != NFC_VS_HCI_NETWK_RSP)
        return;

    if (status != HAL_NFC_STATUS_OK)
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);

    switch (nfc_hal_cb.hci_cb.hci_netwk_config_block)
    {
    case HC_F3_NV_BLOCK:
    case HC_F4_NV_BLOCK:
    case HC_DH_NV_BLOCK:
        nfc_hal_hci_set_next_hci_netwk_config (nfc_hal_cb.hci_cb.hci_netwk_config_block);
        break;

    default:
        /* Ignore the event */
        break;
    }
}

/*******************************************************************************
**
** Function         nfc_hal_nci_cmd_timeout_cback
**
** Description      callback function for timeout
**
** Returns          void
**
*******************************************************************************/
void nfc_hal_hci_timeout_cback (void *p_tle)
{
    TIMER_LIST_ENT  *p_tlent = (TIMER_LIST_ENT *)p_tle;

    NCI_TRACE_DEBUG0 ("nfc_hal_hci_timeout_cback ()");

    if (p_tlent->event == NFC_HAL_HCI_VSC_TIMEOUT_EVT)
    {
        NCI_TRACE_ERROR0 ("nfc_hal_hci_timeout_cback: Timeout - NFC HAL HCI BRCM Initialization Failed!");
        nfc_hal_hci_init_complete (HAL_NFC_STATUS_FAILED);
    }
}


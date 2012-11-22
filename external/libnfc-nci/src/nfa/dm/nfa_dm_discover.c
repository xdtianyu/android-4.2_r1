/******************************************************************************
 *
 *  Copyright (C) 2010-2012 Broadcom Corporation
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
 *  This file contains the action functions for device manager discovery
 *  function.
 *
 ******************************************************************************/
#include <string.h>
#include "nfa_sys.h"
#include "nfa_api.h"
#include "nfa_dm_int.h"
#include "nfa_p2p_int.h"
#include "nfa_sys_int.h"
#if (NFC_NFCEE_INCLUDED == TRUE)
#include "nfa_ee_api.h"
#include "nfa_ee_int.h"
#endif
#include "nfa_rw_int.h"

/*
**  static functions
*/

static UINT8 nfa_dm_get_rf_discover_config (tNFA_DM_DISC_TECH_PROTO_MASK dm_disc_mask,
                                            tNFC_DISCOVER_PARAMS disc_params[],
                                            UINT8 max_params);
static tNFA_STATUS nfa_dm_set_rf_listen_mode_config (tNFA_DM_DISC_TECH_PROTO_MASK tech_proto_mask);
static void nfa_dm_set_rf_listen_mode_raw_config (tNFA_DM_DISC_TECH_PROTO_MASK *p_disc_mask);
static tNFA_DM_DISC_TECH_PROTO_MASK nfa_dm_disc_get_disc_mask (tNFC_RF_TECH_N_MODE tech_n_mode,
                                                               tNFC_PROTOCOL       protocol);
static void nfa_dm_notify_discovery (tNFA_DM_RF_DISC_DATA *p_data);
static tNFA_STATUS nfa_dm_disc_notify_activation (tNFC_DISCOVER *p_data);
static void nfa_dm_disc_notify_deactivation (tNFA_DM_RF_DISC_SM_EVENT sm_event, tNFC_DISCOVER *p_data);
static void nfa_dm_disc_data_cback (UINT8 conn_id, tNFC_CONN_EVT event, tNFC_CONN *p_data);

#if (BT_TRACE_VERBOSE == TRUE)
static char *nfa_dm_disc_state_2_str (UINT8 state);
static char *nfa_dm_disc_event_2_str (UINT8 event);
#endif


/*******************************************************************************
**
** Function         nfa_dm_get_rf_discover_config
**
** Description      Build RF discovery configurations from tNFA_DM_DISC_TECH_PROTO_MASK
**
** Returns          number of RF discovery configurations
**
*******************************************************************************/
static UINT8 nfa_dm_get_rf_discover_config (tNFA_DM_DISC_TECH_PROTO_MASK dm_disc_mask,
                                            tNFC_DISCOVER_PARAMS         disc_params[],
                                            UINT8 max_params)
{
    UINT8 num_params = 0;

    /* Check polling A */
    if (dm_disc_mask & ( NFA_DM_DISC_MASK_PA_T1T
                        |NFA_DM_DISC_MASK_PA_T2T
                        |NFA_DM_DISC_MASK_PA_ISO_DEP
                        |NFA_DM_DISC_MASK_PA_NFC_DEP
                        |NFA_DM_DISC_MASK_P_LEGACY) )
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_A;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling B */
    if (dm_disc_mask & NFA_DM_DISC_MASK_PB_ISO_DEP)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_B;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling F */
    if (dm_disc_mask & ( NFA_DM_DISC_MASK_PF_T3T
                        |NFA_DM_DISC_MASK_PF_NFC_DEP) )
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_F;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling A Active mode  */
    if (dm_disc_mask & NFA_DM_DISC_MASK_PAA_NFC_DEP)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_A_ACTIVE;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling F Active mode  */
    if (dm_disc_mask & NFA_DM_DISC_MASK_PFA_NFC_DEP)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_F_ACTIVE;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening A */
    if (dm_disc_mask & ( NFA_DM_DISC_MASK_LA_T1T
                        |NFA_DM_DISC_MASK_LA_T2T
                        |NFA_DM_DISC_MASK_LA_ISO_DEP
                        |NFA_DM_DISC_MASK_LA_NFC_DEP) )
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_A;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening B */
    if (dm_disc_mask & NFA_DM_DISC_MASK_LB_ISO_DEP)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_B;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening F */
    if (dm_disc_mask & ( NFA_DM_DISC_MASK_LF_T3T
                        |NFA_DM_DISC_MASK_LF_NFC_DEP) )
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_F;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening A Active mode */
    if (dm_disc_mask & NFA_DM_DISC_MASK_LAA_NFC_DEP)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening F Active mode */
    if (dm_disc_mask & NFA_DM_DISC_MASK_LFA_NFC_DEP)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_F_ACTIVE;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling ISO 15693 */
    if (dm_disc_mask & NFA_DM_DISC_MASK_P_ISO15693)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_ISO15693;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling B' */
    if (dm_disc_mask & NFA_DM_DISC_MASK_P_B_PRIME)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_B_PRIME;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check polling KOVIO */
    if (dm_disc_mask & NFA_DM_DISC_MASK_P_KOVIO)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_POLL_KOVIO;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening ISO 15693 */
    if (dm_disc_mask & NFA_DM_DISC_MASK_L_ISO15693)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_ISO15693;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    /* Check listening B' */
    if (dm_disc_mask & NFA_DM_DISC_MASK_L_B_PRIME)
    {
        disc_params[num_params].type      = NFC_DISCOVERY_TYPE_LISTEN_B_PRIME;
        disc_params[num_params].frequency = 1;
        num_params++;

        if (num_params >= max_params)
            return num_params;
    }

    return num_params;
}

/*******************************************************************************
**
** Function         nfa_dm_set_rf_listen_mode_config
**
** Description      Update listening protocol to NFCC
**
** Returns          NFA_STATUS_OK if success
**
*******************************************************************************/
static tNFA_STATUS nfa_dm_set_rf_listen_mode_config (tNFA_DM_DISC_TECH_PROTO_MASK tech_proto_mask)
{
    UINT8 params[40], *p;
    UINT8 platform  = 0;
    UINT8 sens_info = 0;

    NFA_TRACE_DEBUG1 ("nfa_dm_set_rf_listen_mode_config () tech_proto_mask = 0x%08X",
                       tech_proto_mask);

    /*
    ** T1T listen     LA_PROT 0x80, LA_SENS_RES byte1:0x00 byte2:0x0C
    ** T2T listen     LA_PROT 0x00
    ** T3T listen     No bit for T3T in LF_PROT (CE T3T set listen parameters, system code, NFCID2, etc.)
    ** ISO-DEP listen LA_PROT 0x01, LB_PROT 0x01
    ** NFC-DEP listen LA_PROT 0x02, LF_PROT 0x02
    */

    if (tech_proto_mask & NFA_DM_DISC_MASK_LA_T1T)
    {
        platform = NCI_PARAM_PLATFORM_T1T;
    }
    else if (tech_proto_mask & NFA_DM_DISC_MASK_LA_T2T)
    {
        /* platform = 0 and sens_info = 0 */
    }
    else
    {
        if (tech_proto_mask & NFA_DM_DISC_MASK_LA_ISO_DEP)
        {
            sens_info |= NCI_PARAM_SEL_INFO_ISODEP;
        }

        if (tech_proto_mask & NFA_DM_DISC_MASK_LA_NFC_DEP)
        {
            sens_info |= NCI_PARAM_SEL_INFO_NFCDEP;
        }
    }

    p = params;

    /*
    ** for Listen A
    **
    ** Set ATQA 0x0C00 for T1T listen
    ** If the ATQA values are 0x0000, then the FW will use 0x0400
    ** which works for ISODEP, T2T and NFCDEP.
    */
    if (nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_A] == NFA_DM_DISC_HOST_ID_DH)
    {
        UINT8_TO_STREAM (p, NFC_PMID_LA_BIT_FRAME_SDD);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LA_BIT_FRAME_SDD);
        UINT8_TO_STREAM (p, 0x04);
        UINT8_TO_STREAM (p, NFC_PMID_LA_PLATFORM_CONFIG);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LA_PLATFORM_CONFIG);
        UINT8_TO_STREAM (p, platform);
        UINT8_TO_STREAM (p, NFC_PMID_LA_SEL_INFO);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LA_SEL_INFO);
        UINT8_TO_STREAM (p, sens_info);
    }
    else /* Let NFCC use UICC configuration by configuring with length = 0 */
    {
        UINT8_TO_STREAM (p, NFC_PMID_LA_BIT_FRAME_SDD);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LA_PLATFORM_CONFIG);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LA_SEL_INFO);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LA_NFCID1);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LA_HIST_BY);
        UINT8_TO_STREAM (p, 0);
    }

    /* for Listen B */
    if (nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_B] == NFA_DM_DISC_HOST_ID_DH)
    {
        UINT8_TO_STREAM (p, NFC_PMID_LB_SENSB_INFO);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LB_SENSB_INFO);
        if (tech_proto_mask & NFA_DM_DISC_MASK_LB_ISO_DEP)
        {
            UINT8_TO_STREAM (p, NCI_LISTEN_PROTOCOL_ISO_DEP);
        }
        else
        {
            UINT8_TO_STREAM (p,  0x00);
        }
    }
    else /* Let NFCC use UICC configuration by configuring with length = 0 */
    {
        UINT8_TO_STREAM (p, NFC_PMID_LB_SENSB_INFO);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LB_NFCID0);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LB_APPDATA);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LB_ADC_FO);
        UINT8_TO_STREAM (p, 0);
        UINT8_TO_STREAM (p, NFC_PMID_LB_H_INFO);
        UINT8_TO_STREAM (p, 0);
    }

    /* for Listen F */
    if (nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_F] == NFA_DM_DISC_HOST_ID_DH)
    {
        UINT8_TO_STREAM (p, NFC_PMID_LF_PROTOCOL);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LF_PROTOCOL);
        if (tech_proto_mask & NFA_DM_DISC_MASK_LF_NFC_DEP)
        {
            UINT8_TO_STREAM (p, NCI_LISTEN_PROTOCOL_NFC_DEP);
        }
        else
        {
            UINT8_TO_STREAM (p, 0x00);
        }
    }
    else
    {
        /* If DH is not listening on T3T, let NFCC use UICC configuration by configuring with length = 0 */
        if ((tech_proto_mask & NFA_DM_DISC_MASK_LF_T3T) == 0)
        {
            UINT8_TO_STREAM (p, NFC_PMID_LF_PROTOCOL);
            UINT8_TO_STREAM (p, 0);
            UINT8_TO_STREAM (p, NFC_PMID_LF_T3T_FLAGS2);
            UINT8_TO_STREAM (p, 0);
        }
    }

    if (p > params)
    {
        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);
    }

    return NFA_STATUS_OK;
}

/*******************************************************************************
**
** Function         nfa_dm_set_total_duration
**
** Description      Update total duration to NFCC
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_set_total_duration (void)
{
    UINT8 params[10], *p;

    NFA_TRACE_DEBUG0 ("nfa_dm_set_total_duration ()");

    p = params;

    /* for total duration */
    UINT8_TO_STREAM (p, NFC_PMID_TOTAL_DURATION);
    UINT8_TO_STREAM (p, NCI_PARAM_LEN_TOTAL_DURATION);
    UINT16_TO_STREAM (p, nfa_dm_cb.disc_cb.disc_duration);

    if (p > params)
    {
        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);
    }
}

/*******************************************************************************
**
** Function         nfa_dm_set_rf_listen_mode_raw_config
**
** Description      Set raw listen parameters
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_set_rf_listen_mode_raw_config (tNFA_DM_DISC_TECH_PROTO_MASK *p_disc_mask)
{
    tNFA_DM_DISC_TECH_PROTO_MASK disc_mask = 0;
    tNFA_LISTEN_CFG  *p_cfg = &nfa_dm_cb.disc_cb.excl_listen_config;
    UINT8 params[250], *p, xx;

    NFA_TRACE_DEBUG0 ("nfa_dm_set_rf_listen_mode_raw_config ()");

    /*
    ** Discovery Configuration Parameters for Listen A
    */
    if (  (nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_A] == NFA_DM_DISC_HOST_ID_DH)
        &&(p_cfg->la_enable)  )
    {
        p = params;

        UINT8_TO_STREAM (p, NFC_PMID_LA_BIT_FRAME_SDD);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LA_BIT_FRAME_SDD);
        UINT8_TO_STREAM (p, p_cfg->la_bit_frame_sdd);

        UINT8_TO_STREAM (p, NFC_PMID_LA_PLATFORM_CONFIG);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LA_PLATFORM_CONFIG);
        UINT8_TO_STREAM (p, p_cfg->la_platform_config);

        UINT8_TO_STREAM (p, NFC_PMID_LA_SEL_INFO);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LA_SEL_INFO);
        UINT8_TO_STREAM (p, p_cfg->la_sel_info);

        if (p_cfg->la_platform_config == NCI_PARAM_PLATFORM_T1T)
        {
            disc_mask |= NFA_DM_DISC_MASK_LA_T1T;
        }
        else
        {
            /* If T4T or NFCDEP */
            if (p_cfg->la_sel_info & NCI_PARAM_SEL_INFO_ISODEP)
            {
                disc_mask |= NFA_DM_DISC_MASK_LA_ISO_DEP;
            }

            if (p_cfg->la_sel_info & NCI_PARAM_SEL_INFO_NFCDEP)
            {
                disc_mask |= NFA_DM_DISC_MASK_LA_NFC_DEP;
            }

            /* If neither, T4T nor NFCDEP, then its T2T */
            if (disc_mask == 0)
            {
                disc_mask |= NFA_DM_DISC_MASK_LA_T2T;
            }
        }

        UINT8_TO_STREAM (p, NFC_PMID_LA_NFCID1);
        UINT8_TO_STREAM (p, p_cfg->la_nfcid1_len);
        ARRAY_TO_STREAM (p, p_cfg->la_nfcid1, p_cfg->la_nfcid1_len);

        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);
    }

    /*
    ** Discovery Configuration Parameters for Listen B
    */
    if (  (nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_B] == NFA_DM_DISC_HOST_ID_DH)
        &&(p_cfg->lb_enable)  )
    {
        p = params;

        UINT8_TO_STREAM (p, NFC_PMID_LB_SENSB_INFO);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LB_SENSB_INFO);
        UINT8_TO_STREAM (p, p_cfg->lb_sensb_info);

        UINT8_TO_STREAM (p, NFC_PMID_LB_NFCID0);
        UINT8_TO_STREAM (p, p_cfg->lb_nfcid0_len);
        ARRAY_TO_STREAM (p, p_cfg->lb_nfcid0, p_cfg->lb_nfcid0_len);

        UINT8_TO_STREAM (p, NFC_PMID_LB_APPDATA);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LB_APPDATA);
        ARRAY_TO_STREAM (p, p_cfg->lb_app_data, NCI_PARAM_LEN_LB_APPDATA);

        UINT8_TO_STREAM (p, NFC_PMID_LB_SFGI);
        UINT8_TO_STREAM (p, 1);
        UINT8_TO_STREAM (p, p_cfg->lb_adc_fo);

        UINT8_TO_STREAM (p, NFC_PMID_LB_ADC_FO);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LB_ADC_FO);
        UINT8_TO_STREAM (p, p_cfg->lb_adc_fo);

        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);

        if (p_cfg->lb_sensb_info & NCI_LISTEN_PROTOCOL_ISO_DEP)
        {
            disc_mask |= NFA_DM_DISC_MASK_LB_ISO_DEP;
        }
    }

    /*
    ** Discovery Configuration Parameters for Listen F
    */
    if (  (nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_F] == NFA_DM_DISC_HOST_ID_DH)
        &&(p_cfg->lf_enable)  )
    {
        p = params;

        UINT8_TO_STREAM (p, NFC_PMID_LF_CON_BITR_F);
        UINT8_TO_STREAM (p, 1);
        UINT8_TO_STREAM (p, p_cfg->lf_con_bitr_f);

        UINT8_TO_STREAM (p, NFC_PMID_LF_PROTOCOL);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LF_PROTOCOL);
        UINT8_TO_STREAM (p, p_cfg->lf_protocol_type);

        UINT8_TO_STREAM (p, NFC_PMID_LF_T3T_FLAGS2);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_LF_T3T_FLAGS2);
        UINT16_TO_STREAM(p, p_cfg->lf_t3t_flags);

        /* if the bit at position X is set to 0, SC/NFCID2 with index X shall be ignored */
        for (xx = 0; xx < NFA_LF_MAX_SC_NFCID2; xx++)
        {
            if ((p_cfg->lf_t3t_flags & (0x0001 << xx)) != 0x0000)
            {
                UINT8_TO_STREAM (p, NFC_PMID_LF_T3T_ID1 + xx);
                UINT8_TO_STREAM (p, NCI_SYSTEMCODE_LEN + NCI_NFCID2_LEN);
                ARRAY_TO_STREAM (p, p_cfg->lf_t3t_identifier[xx], NCI_SYSTEMCODE_LEN + NCI_NFCID2_LEN);
            }
        }

        UINT8_TO_STREAM (p,  NFC_PMID_LF_T3T_PMM);
        UINT8_TO_STREAM (p,  NCI_PARAM_LEN_LF_T3T_PMM);
        ARRAY_TO_STREAM (p,  p_cfg->lf_t3t_pmm, NCI_PARAM_LEN_LF_T3T_PMM);

        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);

        if (p_cfg->lf_t3t_flags != NCI_LF_T3T_FLAGS2_ALL_DISABLED)
        {
            disc_mask |= NFA_DM_DISC_MASK_LF_T3T;
        }
        if (p_cfg->lf_protocol_type & NCI_LISTEN_PROTOCOL_NFC_DEP)
        {
            disc_mask |= NFA_DM_DISC_MASK_LF_NFC_DEP;
        }
    }

    /*
    ** Discovery Configuration Parameters for Listen ISO-DEP
    */
    if ((disc_mask & (NFA_DM_DISC_MASK_LA_ISO_DEP|NFA_DM_DISC_MASK_LB_ISO_DEP))
      &&(p_cfg->li_enable))
    {
        p = params;

        UINT8_TO_STREAM (p, NFC_PMID_FWI);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_FWI);
        UINT8_TO_STREAM (p, p_cfg->li_fwi);

        if (disc_mask & NFA_DM_DISC_MASK_LA_ISO_DEP)
        {
            UINT8_TO_STREAM (p, NFC_PMID_LA_HIST_BY);
            UINT8_TO_STREAM (p, p_cfg->la_hist_bytes_len);
            ARRAY_TO_STREAM (p, p_cfg->la_hist_bytes, p_cfg->la_hist_bytes_len);
        }

        if (disc_mask & NFA_DM_DISC_MASK_LB_ISO_DEP)
        {
            UINT8_TO_STREAM (p, NFC_PMID_LB_H_INFO);
            UINT8_TO_STREAM (p, p_cfg->lb_h_info_resp_len);
            ARRAY_TO_STREAM (p, p_cfg->lb_h_info_resp, p_cfg->lb_h_info_resp_len);
        }

        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);
    }

    /*
    ** Discovery Configuration Parameters for Listen NFC-DEP
    */
    if (  (disc_mask & (NFA_DM_DISC_MASK_LA_NFC_DEP|NFA_DM_DISC_MASK_LF_NFC_DEP))
        &&(p_cfg->ln_enable))
    {
        p = params;

        UINT8_TO_STREAM (p, NFC_PMID_WT);
        UINT8_TO_STREAM (p, NCI_PARAM_LEN_WT);
        UINT8_TO_STREAM (p, p_cfg->ln_wt);

        UINT8_TO_STREAM (p, NFC_PMID_ATR_RES_GEN_BYTES);
        UINT8_TO_STREAM (p, p_cfg->ln_atr_res_gen_bytes_len);
        ARRAY_TO_STREAM (p, p_cfg->ln_atr_res_gen_bytes, p_cfg->ln_atr_res_gen_bytes_len);

        UINT8_TO_STREAM (p, NFC_PMID_ATR_RSP_CONFIG);
        UINT8_TO_STREAM (p, 1);
        UINT8_TO_STREAM (p, p_cfg->ln_atr_res_config);

        nfa_dm_check_set_config ((UINT8) (p - params), params, FALSE);
    }

    *p_disc_mask = disc_mask;

    NFA_TRACE_DEBUG1 ("nfa_dm_set_rf_listen_mode_raw_config () disc_mask = 0x%x", disc_mask);
}

/*******************************************************************************
**
** Function         nfa_dm_disc_get_disc_mask
**
** Description      Convert RF technology, mode and protocol to bit mask
**
** Returns          tNFA_DM_DISC_TECH_PROTO_MASK
**
*******************************************************************************/
static tNFA_DM_DISC_TECH_PROTO_MASK nfa_dm_disc_get_disc_mask (tNFC_RF_TECH_N_MODE tech_n_mode,
                                                               tNFC_PROTOCOL       protocol)
{
    /* Set initial disc_mask to legacy poll or listen */
    tNFA_DM_DISC_TECH_PROTO_MASK disc_mask = ((tech_n_mode & 0x80) ? NFA_DM_DISC_MASK_L_LEGACY : NFA_DM_DISC_MASK_P_LEGACY);

    switch (tech_n_mode)
    {
    case NFC_DISCOVERY_TYPE_POLL_A:
        switch (protocol)
        {
        case NFC_PROTOCOL_T1T:
            disc_mask = NFA_DM_DISC_MASK_PA_T1T;
            break;
        case NFC_PROTOCOL_T2T:
            disc_mask = NFA_DM_DISC_MASK_PA_T2T;
            break;
        case NFC_PROTOCOL_ISO_DEP:
            disc_mask = NFA_DM_DISC_MASK_PA_ISO_DEP;
            break;
        case NFC_PROTOCOL_NFC_DEP:
            disc_mask = NFA_DM_DISC_MASK_PA_NFC_DEP;
            break;
        }
        break;
    case NFC_DISCOVERY_TYPE_POLL_B:
        if (protocol == NFC_PROTOCOL_ISO_DEP)
            disc_mask = NFA_DM_DISC_MASK_PB_ISO_DEP;
        break;
    case NFC_DISCOVERY_TYPE_POLL_F:
        if (protocol == NFC_PROTOCOL_T3T)
            disc_mask = NFA_DM_DISC_MASK_PF_T3T;
        else if (protocol == NFC_PROTOCOL_NFC_DEP)
            disc_mask = NFA_DM_DISC_MASK_PF_NFC_DEP;
        break;
    case NFC_DISCOVERY_TYPE_POLL_ISO15693:
        disc_mask = NFA_DM_DISC_MASK_P_ISO15693;
        break;
    case NFC_DISCOVERY_TYPE_POLL_B_PRIME:
        disc_mask = NFA_DM_DISC_MASK_P_B_PRIME;
        break;
    case NFC_DISCOVERY_TYPE_POLL_KOVIO:
        disc_mask = NFA_DM_DISC_MASK_P_KOVIO;
        break;
    case NFC_DISCOVERY_TYPE_POLL_A_ACTIVE:
        disc_mask = NFA_DM_DISC_MASK_PAA_NFC_DEP;
        break;
    case NFC_DISCOVERY_TYPE_POLL_F_ACTIVE:
        disc_mask = NFA_DM_DISC_MASK_PFA_NFC_DEP;
        break;

    case NFC_DISCOVERY_TYPE_LISTEN_A:
        switch (protocol)
        {
        case NFC_PROTOCOL_T1T:
            disc_mask = NFA_DM_DISC_MASK_LA_T1T;
            break;
        case NFC_PROTOCOL_T2T:
            disc_mask = NFA_DM_DISC_MASK_LA_T2T;
            break;
        case NFC_PROTOCOL_ISO_DEP:
            disc_mask = NFA_DM_DISC_MASK_LA_ISO_DEP;
            break;
        case NFC_PROTOCOL_NFC_DEP:
            disc_mask = NFA_DM_DISC_MASK_LA_NFC_DEP;
            break;
        }
        break;
    case NFC_DISCOVERY_TYPE_LISTEN_B:
        if (protocol == NFC_PROTOCOL_ISO_DEP)
            disc_mask = NFA_DM_DISC_MASK_LB_ISO_DEP;
        break;
    case NFC_DISCOVERY_TYPE_LISTEN_F:
        if (protocol == NFC_PROTOCOL_T3T)
            disc_mask = NFA_DM_DISC_MASK_LF_T3T;
        else if (protocol == NFC_PROTOCOL_NFC_DEP)
            disc_mask = NFA_DM_DISC_MASK_LF_NFC_DEP;
        break;
    case NFC_DISCOVERY_TYPE_LISTEN_ISO15693:
        disc_mask = NFA_DM_DISC_MASK_L_ISO15693;
        break;
    case NFC_DISCOVERY_TYPE_LISTEN_B_PRIME:
        disc_mask = NFA_DM_DISC_MASK_L_B_PRIME;
        break;
    case NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE:
        disc_mask = NFA_DM_DISC_MASK_LAA_NFC_DEP;
        break;
    case NFC_DISCOVERY_TYPE_LISTEN_F_ACTIVE:
        disc_mask = NFA_DM_DISC_MASK_LFA_NFC_DEP;
        break;
    }

    NFA_TRACE_DEBUG3 ("nfa_dm_disc_get_disc_mask (): tech_n_mode:0x%X, protocol:0x%X, disc_mask:0x%X",
                       tech_n_mode, protocol, disc_mask);
    return (disc_mask);
}

/*******************************************************************************
**
** Function         nfa_dm_disc_discovery_cback
**
** Description      Discovery callback event from NFC
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_discovery_cback (tNFC_DISCOVER_EVT event, tNFC_DISCOVER *p_data)
{
    tNFA_DM_RF_DISC_SM_EVENT dm_disc_event = NFA_DM_DISC_SM_MAX_EVENT;

    NFA_TRACE_DEBUG1 ("nfa_dm_disc_discovery_cback (): event:0x%X", event);

    switch (event)
    {
    case NFC_START_DEVT:
        dm_disc_event = NFA_DM_RF_DISCOVER_RSP;
        break;
    case NFC_RESULT_DEVT:
        dm_disc_event = NFA_DM_RF_DISCOVER_NTF;
        break;
    case NFC_SELECT_DEVT:
        dm_disc_event = NFA_DM_RF_DISCOVER_SELECT_RSP;
        break;
    case NFC_ACTIVATE_DEVT:
        dm_disc_event = NFA_DM_RF_INTF_ACTIVATED_NTF;
        break;
    case NFC_DEACTIVATE_DEVT:
        if (p_data->deactivate.is_ntf)
            dm_disc_event = NFA_DM_RF_DEACTIVATE_NTF;
        else
            dm_disc_event = NFA_DM_RF_DEACTIVATE_RSP;
        break;
    default:
        NFA_TRACE_ERROR0 ("Unexpected event");
        return;
    }

    nfa_dm_disc_sm_execute (dm_disc_event, (tNFA_DM_RF_DISC_DATA *) p_data);
}

/*******************************************************************************
**
** Function         nfa_dm_disc_notify_started
**
** Description      Report NFA_EXCLUSIVE_RF_CONTROL_STARTED_EVT or
**                  NFA_RF_DISCOVERY_STARTED_EVT, if needed
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_notify_started (tNFA_STATUS status)
{
    tNFA_CONN_EVT_DATA      evt_data;

    if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_NOTIFY)
    {
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_NOTIFY;

        evt_data.status = status;

        if (nfa_dm_cb.disc_cb.excl_disc_entry.in_use)
            nfa_dm_conn_cback_event_notify (NFA_EXCLUSIVE_RF_CONTROL_STARTED_EVT, &evt_data);
        else
            nfa_dm_conn_cback_event_notify (NFA_RF_DISCOVERY_STARTED_EVT, &evt_data);
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_conn_event_notify
**
** Description      Notify application of CONN_CBACK event, using appropriate
**                  callback
**
** Returns          nothing
**
*******************************************************************************/
void nfa_dm_disc_conn_event_notify (UINT8 event, tNFA_STATUS status)
{
    tNFA_CONN_EVT_DATA      evt_data;

    if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_NOTIFY)
    {
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_NOTIFY;
        evt_data.status               = status;

        if (nfa_dm_cb.flags & NFA_DM_FLAGS_EXCL_RF_ACTIVE)
        {
            /* Use exclusive RF mode callback */
            if (nfa_dm_cb.p_excl_conn_cback)
                (*nfa_dm_cb.p_excl_conn_cback) (event, &evt_data);
        }
        else
        {
            (*nfa_dm_cb.p_conn_cback) (event, &evt_data);
        }
    }
}

/*******************************************************************************
**
** Function         nfa_dm_send_deactivate_cmd
**
** Description      Send deactivate command to NFCC, if needed.
**
** Returns          NFC_STATUS_OK             - deactivate cmd is sent
**                  NCI_STATUS_FAILED         - no buffers
**                  NFC_STATUS_SEMANTIC_ERROR - this function does not attempt
**                                              to send deactivate cmd
**
*******************************************************************************/
static tNFC_STATUS nfa_dm_send_deactivate_cmd (tNFC_DEACT_TYPE deactivate_type)
{
    tNFC_STATUS status = NFC_STATUS_SEMANTIC_ERROR;
    tNFA_DM_DISC_FLAGS w4_flags = nfa_dm_cb.disc_cb.disc_flags & (NFA_DM_DISC_FLAGS_W4_RSP|NFA_DM_DISC_FLAGS_W4_NTF);

    if (!w4_flags)
    {
        /* if deactivate CMD was not sent to NFCC */
        nfa_dm_cb.disc_cb.disc_flags |= (NFA_DM_DISC_FLAGS_W4_RSP|NFA_DM_DISC_FLAGS_W4_NTF);

        status = NFC_Deactivate (deactivate_type);
    }
    else if ((w4_flags == NFA_DM_DISC_FLAGS_W4_NTF) && (deactivate_type == NFC_DEACTIVATE_TYPE_IDLE))
    {
        nfa_dm_cb.disc_cb.disc_flags &= ~(NFA_DM_DISC_FLAGS_W4_NTF);
        nfa_dm_cb.disc_cb.disc_flags |= (NFA_DM_DISC_FLAGS_W4_RSP);
        nfa_dm_disc_new_state (NFA_DM_RFST_DISCOVERY);
        status = NFC_Deactivate (deactivate_type);
    }

    return status;
}

/*******************************************************************************
**
** Function         nfa_dm_start_rf_discover
**
** Description      Start RF discovery
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_start_rf_discover (void)
{
    tNFC_DISCOVER_PARAMS    disc_params[NFA_DM_MAX_DISC_PARAMS];
    tNFA_DM_DISC_TECH_PROTO_MASK dm_disc_mask = 0, poll_mask, listen_mask;
    UINT8                   num_params, xx;

    NFA_TRACE_DEBUG0 ("nfa_dm_start_rf_discover ()");
    /* Make sure that RF discovery was enabled, or some app has exclusive control */
    if (  (!(nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_ENABLED))
        &&(nfa_dm_cb.disc_cb.excl_disc_entry.in_use == FALSE)  )
    {
        return;
    }

    /* get listen mode routing table for technology */
    nfa_ee_get_tech_route (NFA_EE_PWR_STATE_ON, nfa_dm_cb.disc_cb.listen_RT);

    if (nfa_dm_cb.disc_cb.excl_disc_entry.in_use)
    {
        nfa_dm_set_rf_listen_mode_raw_config (&dm_disc_mask);
        dm_disc_mask |= (nfa_dm_cb.disc_cb.excl_disc_entry.requested_disc_mask & NFA_DM_DISC_MASK_POLL);
        nfa_dm_cb.disc_cb.excl_disc_entry.selected_disc_mask = dm_disc_mask;
    }
    else
    {
        /* Collect RF discovery request from sub-modules */
        for (xx = 0; xx < NFA_DM_DISC_NUM_ENTRIES; xx++)
        {
            if (nfa_dm_cb.disc_cb.entry[xx].in_use)
            {
                poll_mask = (nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask & NFA_DM_DISC_MASK_POLL);

                /* clear poll mode technolgies and protocols which are already used by others */
                poll_mask &= ~(dm_disc_mask & NFA_DM_DISC_MASK_POLL);

                listen_mask = 0;

                /*
                ** add listen mode technolgies and protocols if host ID is matched to listen mode routing table
                */

                /* NFC-A */
                if (nfa_dm_cb.disc_cb.entry[xx].host_id == nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_A])
                {
                    listen_mask |= nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask
                                   & ( NFA_DM_DISC_MASK_LA_T1T
                                      |NFA_DM_DISC_MASK_LA_T2T
                                      |NFA_DM_DISC_MASK_LA_ISO_DEP
                                      |NFA_DM_DISC_MASK_LA_NFC_DEP
                                      |NFA_DM_DISC_MASK_LAA_NFC_DEP );
                }
                else
                {
                    /* host can listen ISO-DEP based on AID routing */
                    listen_mask |= (nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask  & NFA_DM_DISC_MASK_LA_ISO_DEP);
                    listen_mask |= (nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask  & NFA_DM_DISC_MASK_LA_NFC_DEP);
                    listen_mask |= (nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask  & NFA_DM_DISC_MASK_LAA_NFC_DEP);
                }

                /* NFC-B */
                /* multiple hosts can listen ISO-DEP based on AID routing */
                listen_mask |= nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask
                               & NFA_DM_DISC_MASK_LB_ISO_DEP;

                /* NFC-F */
                if (nfa_dm_cb.disc_cb.entry[xx].host_id == nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_F])
                {
                    listen_mask |= nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask
                                   & ( NFA_DM_DISC_MASK_LF_T3T
                                      |NFA_DM_DISC_MASK_LF_NFC_DEP
                                      |NFA_DM_DISC_MASK_LFA_NFC_DEP );
                }
                else
                {
                    /* NFCC can listen T3T based on NFCID routing */
                    listen_mask |= (nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask  & NFA_DM_DISC_MASK_LF_T3T);
                }

                /* NFC-B Prime */
                if (nfa_dm_cb.disc_cb.entry[xx].host_id == nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_BP])
                {
                    listen_mask |= nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask
                                   & NFA_DM_DISC_MASK_L_B_PRIME;
                }

                /*
                ** clear listen mode technolgies and protocols which are already used by others
                */

                /* Check if other modules are listening T1T or T2T */
                if (dm_disc_mask & (NFA_DM_DISC_MASK_LA_T1T|NFA_DM_DISC_MASK_LA_T2T))
                {
                    listen_mask &= ~( NFA_DM_DISC_MASK_LA_T1T
                                     |NFA_DM_DISC_MASK_LA_T2T
                                     |NFA_DM_DISC_MASK_LA_ISO_DEP
                                     |NFA_DM_DISC_MASK_LA_NFC_DEP );
                }

                /* T1T/T2T has priority on NFC-A */
                if (  (dm_disc_mask & (NFA_DM_DISC_MASK_LA_ISO_DEP|NFA_DM_DISC_MASK_LA_NFC_DEP))
                    &&(listen_mask & (NFA_DM_DISC_MASK_LA_T1T|NFA_DM_DISC_MASK_LA_T2T)))
                {
                    dm_disc_mask &= ~( NFA_DM_DISC_MASK_LA_ISO_DEP
                                      |NFA_DM_DISC_MASK_LA_NFC_DEP );
                }

                /* Don't remove ISO-DEP because multiple hosts can listen ISO-DEP based on AID routing */

                /* Check if other modules are listening NFC-DEP */
                if (dm_disc_mask & (NFA_DM_DISC_MASK_LA_NFC_DEP | NFA_DM_DISC_MASK_LAA_NFC_DEP))
                {
                    listen_mask &= ~( NFA_DM_DISC_MASK_LA_NFC_DEP
                                     |NFA_DM_DISC_MASK_LAA_NFC_DEP );
                }

                nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask = poll_mask | listen_mask;

                NFA_TRACE_DEBUG2 ("nfa_dm_cb.disc_cb.entry[%d].selected_disc_mask = 0x%x",
                                   xx, nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask);

                dm_disc_mask |= nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask;
            }
        }

        /* Let P2P set GEN bytes for LLCP to NFCC */
        if (dm_disc_mask & ( NFA_DM_DISC_MASK_PA_NFC_DEP
                            |NFA_DM_DISC_MASK_PF_NFC_DEP
                            |NFA_DM_DISC_MASK_LA_NFC_DEP
                            |NFA_DM_DISC_MASK_LF_NFC_DEP
                            |NFA_DM_DISC_MASK_PAA_NFC_DEP
                            |NFA_DM_DISC_MASK_PFA_NFC_DEP
                            |NFA_DM_DISC_MASK_LAA_NFC_DEP
                            |NFA_DM_DISC_MASK_LFA_NFC_DEP ))
        {
            nfa_p2p_set_config (dm_disc_mask);
        }
    }

    NFA_TRACE_DEBUG1 ("dm_disc_mask = 0x%x", dm_disc_mask);

    /* Get Discovery Technology parameters */
    num_params = nfa_dm_get_rf_discover_config (dm_disc_mask, disc_params, NFA_DM_MAX_DISC_PARAMS);

    if (num_params)
    {
        /*
        ** NFCC will abort programming personality slots if not available.
        ** NFCC programs the personality slots in the following order of RF technologies:
        **      NFC-A, NFC-B, NFC-BP, NFC-I93
        */

        /* if this is not for exclusive control */
        if (!nfa_dm_cb.disc_cb.excl_disc_entry.in_use)
        {
            /* update listening protocols in each NFC technology */
            nfa_dm_set_rf_listen_mode_config (dm_disc_mask);
        }

        /* Set polling duty cycle */
        nfa_dm_set_total_duration ();
        nfa_dm_cb.disc_cb.dm_disc_mask = dm_disc_mask;

        NFC_DiscoveryStart (num_params, disc_params, nfa_dm_disc_discovery_cback);
        /* set flag about waiting for response in IDLE state */
        nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;

        /* register callback to get interface error NTF */
        NFC_SetStaticRfCback (nfa_dm_disc_data_cback);
    }
    else
    {
        /* RF discovery is started but there is no valid technology or protocol to discover */
        nfa_dm_disc_notify_started (NFA_STATUS_OK);
    }
}

/*******************************************************************************
**
** Function         nfa_dm_notify_discovery
**
** Description      Send RF discovery notification to upper layer
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_notify_discovery (tNFA_DM_RF_DISC_DATA *p_data)
{
    tNFA_CONN_EVT_DATA conn_evt;

    /* let application select a device */
    conn_evt.disc_result.status = NFA_STATUS_OK;
    memcpy (&(conn_evt.disc_result.discovery_ntf),
            &(p_data->nfc_discover.result),
            sizeof (tNFC_RESULT_DEVT));

    nfa_dm_conn_cback_event_notify (NFA_DISC_RESULT_EVT, &conn_evt);
}

/*******************************************************************************
**
** Function         nfa_dm_disc_notify_activation
**
** Description      Send RF activation notification to sub-module
**
** Returns          NFA_STATUS_OK if success
**
*******************************************************************************/
static tNFA_STATUS nfa_dm_disc_notify_activation (tNFC_DISCOVER *p_data)
{
    UINT8   xx, host_id_in_LRT;
    UINT8   iso_dep_t3t__listen = NFA_DM_DISC_NUM_ENTRIES;

    tNFC_RF_TECH_N_MODE tech_n_mode = p_data->activate.rf_tech_param.mode;
    tNFC_PROTOCOL       protocol    = p_data->activate.protocol;

    tNFA_DM_DISC_TECH_PROTO_MASK activated_disc_mask;

    NFA_TRACE_DEBUG2 ("nfa_dm_disc_notify_activation (): tech_n_mode:0x%X, proto:0x%X",
                       tech_n_mode, protocol);

    if (nfa_dm_cb.disc_cb.excl_disc_entry.in_use)
    {
        nfa_dm_cb.disc_cb.activated_tech_mode    = tech_n_mode;
        nfa_dm_cb.disc_cb.activated_rf_disc_id   = p_data->activate.rf_disc_id;
        nfa_dm_cb.disc_cb.activated_rf_interface = p_data->activate.intf_param.type;
        nfa_dm_cb.disc_cb.activated_protocol     = protocol;
        nfa_dm_cb.disc_cb.activated_handle       = NFA_HANDLE_INVALID;

        if (nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback)
            (*(nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback)) (NFA_DM_RF_DISC_ACTIVATED_EVT, p_data);

        return (NFA_STATUS_OK);
    }

    /* if this is NFCEE direct RF interface, notify activation to whoever listening UICC */
    if (p_data->activate.intf_param.type == NFC_INTERFACE_EE_DIRECT_RF)
    {
        for (xx = 0; xx < NFA_DM_DISC_NUM_ENTRIES; xx++)
        {
            if (  (nfa_dm_cb.disc_cb.entry[xx].in_use)
                &&(nfa_dm_cb.disc_cb.entry[xx].host_id != NFA_DM_DISC_HOST_ID_DH))
            {
                nfa_dm_cb.disc_cb.activated_rf_disc_id   = p_data->activate.rf_disc_id;
                nfa_dm_cb.disc_cb.activated_rf_interface = p_data->activate.intf_param.type;
                nfa_dm_cb.disc_cb.activated_protocol     = NFC_PROTOCOL_UNKNOWN;
                nfa_dm_cb.disc_cb.activated_handle       = xx;

                NFA_TRACE_DEBUG2 ("activated_rf_interface:0x%x, activated_handle: 0x%x",
                                   nfa_dm_cb.disc_cb.activated_rf_interface,
                                   nfa_dm_cb.disc_cb.activated_handle);

                if (nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)
                    (*(nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)) (NFA_DM_RF_DISC_ACTIVATED_EVT, p_data);

                return (NFA_STATUS_OK);
            }
        }
        return (NFA_STATUS_FAILED);
    }

    /* get bit mask of technolgies/mode and protocol */
    activated_disc_mask = nfa_dm_disc_get_disc_mask (tech_n_mode, protocol);

    /* get host ID of technology from listen mode routing table */
    if (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_A)
    {
        host_id_in_LRT = nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_A];
    }
    else if (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B)
    {
        host_id_in_LRT = nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_B];
    }
    else if (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_F)
    {
        host_id_in_LRT = nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_F];
    }
    else if (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B_PRIME)
    {
        host_id_in_LRT = nfa_dm_cb.disc_cb.listen_RT[NFA_DM_DISC_LRT_NFC_BP];
    }
    else    /* DH only */
    {
        host_id_in_LRT = NFA_DM_DISC_HOST_ID_DH;
    }

    if (protocol == NFC_PROTOCOL_NFC_DEP) {
        // Force NFC-DEP to the host
        host_id_in_LRT = NFA_DM_DISC_HOST_ID_DH;
    }

    for (xx = 0; xx < NFA_DM_DISC_NUM_ENTRIES; xx++)
    {
        /* if any matching NFC technology and protocol */
        if (nfa_dm_cb.disc_cb.entry[xx].in_use)
        {
            if (nfa_dm_cb.disc_cb.entry[xx].host_id == host_id_in_LRT)
            {
                if (nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask & activated_disc_mask)
                    break;
            }
            else
            {
                /* check ISO-DEP listening even if host in LRT is not matched */
                if (protocol == NFC_PROTOCOL_ISO_DEP)
                {
                    if (  (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_A)
                        &&(nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask & NFA_DM_DISC_MASK_LA_ISO_DEP))
                    {
                        iso_dep_t3t__listen = xx;
                    }
                    else if (  (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_B)
                             &&(nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask & NFA_DM_DISC_MASK_LB_ISO_DEP))
                    {
                        iso_dep_t3t__listen = xx;
                    }
                }
                /* check T3T listening even if host in LRT is not matched */
                else if (protocol == NFC_PROTOCOL_T3T)
                {
                    if (  (tech_n_mode == NFC_DISCOVERY_TYPE_LISTEN_F)
                        &&(nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask & NFA_DM_DISC_MASK_LF_T3T))
                    {
                        iso_dep_t3t__listen = xx;
                    }
                }
            }
        }
    }

    if (xx >= NFA_DM_DISC_NUM_ENTRIES)
    {
        /* if any ISO-DEP or T3T listening even if host in LRT is not matched */
        xx = iso_dep_t3t__listen;
    }

    if (xx < NFA_DM_DISC_NUM_ENTRIES)
    {
        nfa_dm_cb.disc_cb.activated_tech_mode    = tech_n_mode;
        nfa_dm_cb.disc_cb.activated_rf_disc_id   = p_data->activate.rf_disc_id;
        nfa_dm_cb.disc_cb.activated_rf_interface = p_data->activate.intf_param.type;
        nfa_dm_cb.disc_cb.activated_protocol     = protocol;
        nfa_dm_cb.disc_cb.activated_handle       = xx;

        NFA_TRACE_DEBUG2 ("activated_protocol:0x%x, activated_handle: 0x%x",
                           nfa_dm_cb.disc_cb.activated_protocol,
                           nfa_dm_cb.disc_cb.activated_handle);

        if (nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)
            (*(nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)) (NFA_DM_RF_DISC_ACTIVATED_EVT, p_data);

        return (NFA_STATUS_OK);
    }
    else
    {
        nfa_dm_cb.disc_cb.activated_protocol = NFA_PROTOCOL_INVALID;
        nfa_dm_cb.disc_cb.activated_handle   = NFA_HANDLE_INVALID;
        return (NFA_STATUS_FAILED);
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_notify_deactivation
**
** Description      Send deactivation notification to sub-module
**
** Returns          None
**
*******************************************************************************/
static void nfa_dm_disc_notify_deactivation (tNFA_DM_RF_DISC_SM_EVENT sm_event,
                                             tNFC_DISCOVER *p_data)
{
    tNFA_HANDLE         xx;
    tNFA_DM_RF_DISC_EVT disc_evt;

    NFA_TRACE_DEBUG1 ("nfa_dm_disc_notify_deactivation (): activated_handle=%d",
                       nfa_dm_cb.disc_cb.activated_handle);

    if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_CHECKING)
    {
        NFA_TRACE_DEBUG0 ("nfa_dm_disc_notify_deactivation (): for presence check");
        return;
    }

    if (sm_event == NFA_DM_RF_DEACTIVATE_RSP)
    {
        /*
        ** Activation has been aborted by upper layer in NFA_DM_RFST_W4_HOST_SELECT
        ** or NFA_DM_RFST_LISTEN_SLEEP
        */
        disc_evt = NFA_DM_RF_DISC_CMD_IDLE_CMPL_EVT;
    }
    else
    {
        /* Received deactivation NTF */
        disc_evt = NFA_DM_RF_DISC_DEACTIVATED_EVT;
    }

    if (nfa_dm_cb.disc_cb.excl_disc_entry.in_use)
    {
        if (nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback)
            (*(nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback)) (disc_evt, p_data);
    }
    else
    {
        /* notify event to activated module */
        xx = nfa_dm_cb.disc_cb.activated_handle;

        if (  (sm_event == NFA_DM_RF_DEACTIVATE_RSP)
            ||(p_data->deactivate.type == NFC_DEACTIVATE_TYPE_IDLE)  )
        {
            /* Don't remove handle in NFA_DM_RFST_W4_HOST_SELECT or NFA_DM_RFST_LISTEN_SLEEP */
            nfa_dm_cb.disc_cb.activated_handle = NFA_HANDLE_INVALID;
        }

        if ((xx < NFA_DM_DISC_NUM_ENTRIES) && (nfa_dm_cb.disc_cb.entry[xx].in_use))
        {
            if (nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)
                (*(nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)) (disc_evt, p_data);
        }
    }

    /* clear activated information */
    nfa_dm_cb.disc_cb.activated_tech_mode    = 0;
    nfa_dm_cb.disc_cb.activated_rf_disc_id   = 0;
    nfa_dm_cb.disc_cb.activated_rf_interface = 0;
    nfa_dm_cb.disc_cb.activated_protocol     = NFA_PROTOCOL_INVALID;
}

/*******************************************************************************
**
** Function         nfa_dm_disc_presence_check
**
** Description      Perform legacy presence check (put tag to sleep, then
**                  wake it up to see if tag is present)
**
** Returns          TRUE if operation started
**
*******************************************************************************/
tNFC_STATUS nfa_dm_disc_presence_check (void)
{
    tNFC_STATUS status = NFC_STATUS_FAILED;

    if (nfa_dm_cb.disc_cb.disc_state == NFA_DM_RFST_POLL_ACTIVE)
    {
        /* Deactivate to sleep mode */
        status = nfa_dm_send_deactivate_cmd(NFC_DEACTIVATE_TYPE_SLEEP);
        if (status == NFC_STATUS_OK)
        {
            /* deactivate to sleep is sent on behave of presence check.
             * set the presence check information in control block */
            nfa_dm_cb.disc_cb.disc_flags          |= NFA_DM_DISC_FLAGS_CHECKING;
            nfa_dm_cb.presence_check_deact_pending = FALSE;
        }
    }

    return (status);
}

/*******************************************************************************
**
** Function         nfa_dm_disc_end_presence_check
**
** Description      Perform legacy presence check (put tag to sleep, then
**                  wake it up to see if tag is present)
**
** Returns          TRUE if operation started
**
*******************************************************************************/
static void nfa_dm_disc_end_presence_check (tNFC_STATUS status)
{
    if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_CHECKING)
    {
        /* notify RW module that presence checking is finished */
        nfa_rw_handle_presence_check_rsp (status);
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_CHECKING;
        if (nfa_dm_cb.presence_check_deact_pending)
        {
            nfa_dm_cb.presence_check_deact_pending = FALSE;
            nfa_dm_disc_sm_execute (NFA_DM_RF_DEACTIVATE_CMD,
                                          (tNFA_DM_RF_DISC_DATA *) &nfa_dm_cb.presence_check_deact_type);
        }
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_data_cback
**
** Description      Monitoring interface error through data callback
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_data_cback (UINT8 conn_id, tNFC_CONN_EVT event, tNFC_CONN *p_data)
{
    NFA_TRACE_DEBUG0 ("nfa_dm_disc_data_cback ()");

    /* if selection failed */
    if (event == NFC_ERROR_CEVT)
    {
        nfa_dm_disc_sm_execute (NFA_DM_CORE_INTF_ERROR_NTF, NULL);
    }
    else if (event == NFC_DATA_CEVT)
    {
        GKI_freebuf (p_data->data.p_data);
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_new_state
**
** Description      Processing discovery events in NFA_DM_RFST_IDLE state
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_disc_new_state (tNFA_DM_RF_DISC_STATE new_state)
{
    tNFA_CONN_EVT_DATA      evt_data;
    tNFA_DM_RF_DISC_STATE   old_state = nfa_dm_cb.disc_cb.disc_state;

#if (BT_TRACE_VERBOSE == TRUE)
    NFA_TRACE_DEBUG5 ("nfa_dm_disc_new_state (): old_state: %s (%d), new_state: %s (%d) disc_flags: 0x%x",
                       nfa_dm_disc_state_2_str (nfa_dm_cb.disc_cb.disc_state), nfa_dm_cb.disc_cb.disc_state,
                       nfa_dm_disc_state_2_str (new_state), new_state, nfa_dm_cb.disc_cb.disc_flags);
#else
    NFA_TRACE_DEBUG3 ("nfa_dm_disc_new_state(): old_state: %d, new_state: %d disc_flags: 0x%x",
                       nfa_dm_cb.disc_cb.disc_state, new_state, nfa_dm_cb.disc_cb.disc_flags);
#endif
    nfa_dm_cb.disc_cb.disc_state = new_state;
    if (new_state == NFA_DM_RFST_IDLE)
    {
        if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_STOPPING)
        {
            nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_STOPPING;

            /* if exclusive RF control is stopping */
            if (nfa_dm_cb.flags & NFA_DM_FLAGS_EXCL_RF_ACTIVE)
            {
                if (old_state > NFA_DM_RFST_DISCOVERY)
                {
                    /* notify deactivation to application */
                    evt_data.deactivated.type = NFA_DEACTIVATE_TYPE_IDLE;
                    nfa_dm_conn_cback_event_notify (NFA_DEACTIVATED_EVT, &evt_data);
                }

                nfa_dm_rel_excl_rf_control_and_notify ();
            }
            else
            {
                evt_data.status = NFA_STATUS_OK;
                nfa_dm_conn_cback_event_notify (NFA_RF_DISCOVERY_STOPPED_EVT, &evt_data);
            }
        }
        if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_DISABLING)
        {
            nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_DISABLING;
            nfa_sys_check_disabled ();
        }
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_idle
**
** Description      Processing discovery events in NFA_DM_RFST_IDLE state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_idle (tNFA_DM_RF_DISC_SM_EVENT event,
                                 tNFA_DM_RF_DISC_DATA *p_data)
{
    UINT8              xx;

    switch (event)
    {
    case NFA_DM_RF_DISCOVER_CMD:
        nfa_dm_start_rf_discover ();
        break;

    case NFA_DM_RF_DISCOVER_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;

        if (p_data->nfc_discover.status == NFC_STATUS_OK)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_DISCOVERY);

            /* if RF discovery was stopped while waiting for response */
            if (nfa_dm_cb.disc_cb.disc_flags & (NFA_DM_DISC_FLAGS_STOPPING|NFA_DM_DISC_FLAGS_DISABLING))
            {
                /* stop discovery */
                nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;
                NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
                break;
            }

            if (nfa_dm_cb.disc_cb.excl_disc_entry.in_use)
            {
                if (nfa_dm_cb.disc_cb.excl_disc_entry.disc_flags & NFA_DM_DISC_FLAGS_NOTIFY)
                {
                    nfa_dm_cb.disc_cb.excl_disc_entry.disc_flags &= ~NFA_DM_DISC_FLAGS_NOTIFY;

                    if (nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback)
                        (*(nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback)) (NFA_DM_RF_DISC_START_EVT, (tNFC_DISCOVER*) p_data);
                }
            }
            else
            {
                /* notify event to each module which is waiting for start */
                for (xx = 0; xx < NFA_DM_DISC_NUM_ENTRIES; xx++)
                {
                    /* if registered module is waiting for starting discovery */
                    if (  (nfa_dm_cb.disc_cb.entry[xx].in_use)
                        &&(nfa_dm_cb.disc_cb.dm_disc_mask & nfa_dm_cb.disc_cb.entry[xx].selected_disc_mask)
                        &&(nfa_dm_cb.disc_cb.entry[xx].disc_flags & NFA_DM_DISC_FLAGS_NOTIFY)  )
                    {
                        nfa_dm_cb.disc_cb.entry[xx].disc_flags &= ~NFA_DM_DISC_FLAGS_NOTIFY;

                        if (nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)
                            (*(nfa_dm_cb.disc_cb.entry[xx].p_disc_cback)) (NFA_DM_RF_DISC_START_EVT, (tNFC_DISCOVER*) p_data);
                    }
                }

            }
            nfa_dm_disc_notify_started (p_data->nfc_discover.status);
        }
        else
        {
            /* in rare case that the discovery states of NFCC and DH mismatch and NFCC rejects Discover Cmd
             * deactivate idle and then start disvocery when got deactivate rsp */
            nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;

    case NFA_DM_RF_DEACTIVATE_RSP:
        /* restart discovery */
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_DISABLING)
        {
            nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_DISABLING;
            nfa_sys_check_disabled ();
        }
        else
            nfa_dm_start_rf_discover ();
        break;

    case NFA_DM_LP_LISTEN_CMD:
        nfa_dm_disc_new_state (NFA_DM_RFST_LP_LISTEN);
        break;

    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_idle (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_discovery
**
** Description      Processing discovery events in NFA_DM_RFST_DISCOVERY state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_discovery (tNFA_DM_RF_DISC_SM_EVENT event,
                                      tNFA_DM_RF_DISC_DATA *p_data)
{
    switch (event)
    {
    case NFA_DM_RF_DEACTIVATE_CMD:
        /* if deactivate CMD was not sent to NFCC */
        if (!(nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_W4_RSP))
        {
            nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;
            NFC_Deactivate (p_data->deactivate_type);
        }
        break;
    case NFA_DM_RF_DEACTIVATE_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
        nfa_dm_start_rf_discover ();
        break;
    case NFA_DM_RF_DISCOVER_NTF:
        nfa_dm_disc_new_state (NFA_DM_RFST_W4_ALL_DISCOVERIES);
        nfa_dm_notify_discovery (p_data);
        break;
    case NFA_DM_RF_INTF_ACTIVATED_NTF:
        if (p_data->nfc_discover.activate.intf_param.type == NFC_INTERFACE_EE_DIRECT_RF)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_LISTEN_ACTIVE);
        }
        else if (p_data->nfc_discover.activate.rf_tech_param.mode & 0x80)
        {
            /* Listen mode */
            nfa_dm_disc_new_state (NFA_DM_RFST_LISTEN_ACTIVE);
        }
        else
        {
            /* Poll mode */
            nfa_dm_disc_new_state (NFA_DM_RFST_POLL_ACTIVE);
        }

        if (nfa_dm_disc_notify_activation (&(p_data->nfc_discover)) == NFA_STATUS_FAILED)
        {
            NFA_TRACE_DEBUG0 ("Not matched, restart discovery after receiving deactivate ntf");

            /* after receiving deactivate event, restart discovery */
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;

    case NFA_DM_LP_LISTEN_CMD:
        break;
    case NFA_DM_CORE_INTF_ERROR_NTF:
        break;
    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_discovery (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_w4_all_discoveries
**
** Description      Processing discovery events in NFA_DM_RFST_W4_ALL_DISCOVERIES state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_w4_all_discoveries (tNFA_DM_RF_DISC_SM_EVENT event,
                                               tNFA_DM_RF_DISC_DATA *p_data)
{
    switch (event)
    {
    case NFA_DM_RF_DEACTIVATE_CMD:
        /* if deactivate CMD was not sent to NFCC */
        if (!(nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_W4_RSP))
        {
            nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;
            /* only IDLE mode is allowed */
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;
    case NFA_DM_RF_DEACTIVATE_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
        nfa_dm_start_rf_discover ();
        break;
    case NFA_DM_RF_DISCOVER_NTF:
        /* Notification Type = NCI_DISCOVER_NTF_LAST or NCI_DISCOVER_NTF_LAST_ABORT */
        if (p_data->nfc_discover.result.more != NCI_DISCOVER_NTF_MORE)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_W4_HOST_SELECT);
        }
        nfa_dm_notify_discovery (p_data);
        break;
    case NFA_DM_RF_INTF_ACTIVATED_NTF:
        /*
        ** This is only for ISO15693.
        ** FW sends activation NTF when all responses are received from tags without host selecting.
        */
        nfa_dm_disc_new_state (NFA_DM_RFST_POLL_ACTIVE);

        if (nfa_dm_disc_notify_activation (&(p_data->nfc_discover)) == NFA_STATUS_FAILED)
        {
            NFA_TRACE_DEBUG0 ("Not matched, restart discovery after receiving deactivate ntf");

            /* after receiving deactivate event, restart discovery */
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;
    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_w4_all_discoveries (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_w4_host_select
**
** Description      Processing discovery events in NFA_DM_RFST_W4_HOST_SELECT state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_w4_host_select (tNFA_DM_RF_DISC_SM_EVENT event,
                                           tNFA_DM_RF_DISC_DATA *p_data)
{
    tNFA_CONN_EVT_DATA conn_evt;
    tNFA_DM_DISC_FLAGS  old_pres_check_flag = (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_CHECKING);
    BOOLEAN             pres_check_event = FALSE;
    BOOLEAN             pres_check_event_processed = FALSE;

    switch (event)
    {
    case NFA_DM_RF_DISCOVER_SELECT_CMD:
        /* if not waiting to deactivate */
        if (!(nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_W4_RSP))
        {
            NFC_DiscoverySelect (p_data->select.rf_disc_id,
                                 p_data->select.protocol,
                                 p_data->select.rf_interface);
        }
        else
        {
            nfa_dm_disc_conn_event_notify (NFA_SELECT_RESULT_EVT, NFA_STATUS_FAILED);
        }
        break;

    case NFA_DM_RF_DISCOVER_SELECT_RSP:
        pres_check_event = TRUE;
        /* notify application status of selection */
        if (p_data->nfc_discover.status == NFC_STATUS_OK)
        {
            pres_check_event_processed = TRUE;
            conn_evt.status = NFA_STATUS_OK;
            /* register callback to get interface error NTF */
            NFC_SetStaticRfCback (nfa_dm_disc_data_cback);
        }
        else
            conn_evt.status = NFA_STATUS_FAILED;

        if (!old_pres_check_flag)
        {
            nfa_dm_disc_conn_event_notify (NFA_SELECT_RESULT_EVT, p_data->nfc_discover.status);
        }
        break;
    case NFA_DM_RF_INTF_ACTIVATED_NTF:
        nfa_dm_disc_new_state (NFA_DM_RFST_POLL_ACTIVE);
        if (old_pres_check_flag)
        {
            /* notify RW module of presence of tag */
            nfa_rw_handle_presence_check_rsp (NFC_STATUS_OK);
            if (nfa_dm_cb.presence_check_deact_pending)
            {
                nfa_dm_cb.presence_check_deact_pending = FALSE;
                nfa_dm_disc_sm_execute (NFA_DM_RF_DEACTIVATE_CMD,
                                       (tNFA_DM_RF_DISC_DATA *) &nfa_dm_cb.presence_check_deact_type);
            }
        }

        else if (nfa_dm_disc_notify_activation (&(p_data->nfc_discover)) == NFA_STATUS_FAILED)
        {
            NFA_TRACE_DEBUG0 ("Not matched, restart discovery after receiving deactivate ntf");

            /* after receiving deactivate event, restart discovery */
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;
    case NFA_DM_RF_DEACTIVATE_CMD:
        if (old_pres_check_flag)
        {
            nfa_dm_cb.presence_check_deact_pending = TRUE;
            nfa_dm_cb.presence_check_deact_type    = p_data->deactivate_type;
        }
        /* if deactivate CMD was not sent to NFCC */
        else if (!(nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_W4_RSP))
        {
            nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_W4_RSP;
            /* only IDLE mode is allowed */
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;
    case NFA_DM_RF_DEACTIVATE_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
        nfa_dm_start_rf_discover ();
        /* notify exiting from host select state */
        nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_RSP, &(p_data->nfc_discover));
        break;

    case NFA_DM_CORE_INTF_ERROR_NTF:
        pres_check_event    = TRUE;
        if (!old_pres_check_flag)
        {
            /* target activation failed, upper layer may deactivate or select again */
            conn_evt.status = NFA_STATUS_FAILED;
            nfa_dm_conn_cback_event_notify (NFA_SELECT_RESULT_EVT, &conn_evt);
        }
        break;
    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_w4_host_select (): Unexpected discovery event");
        break;
    }

    if (old_pres_check_flag && pres_check_event && !pres_check_event_processed)
    {
        /* performing presence check for unknow protocol and exception conditions happened
         * clear presence check information and report failure */
        nfa_dm_disc_end_presence_check (NFC_STATUS_FAILED);
        if (nfa_dm_cb.presence_check_deact_pending)
        {
            nfa_dm_cb.presence_check_deact_pending = FALSE;

            NFC_Deactivate (nfa_dm_cb.presence_check_deact_type);
        }
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_poll_active
**
** Description      Processing discovery events in NFA_DM_RFST_POLL_ACTIVE state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_poll_active (tNFA_DM_RF_DISC_SM_EVENT event,
                                        tNFA_DM_RF_DISC_DATA *p_data)
{
    tNFC_STATUS status;
    tNFA_DM_DISC_FLAGS  old_pres_check_flag = (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_CHECKING);
    BOOLEAN             pres_check_event = FALSE;
    BOOLEAN             pres_check_event_processed = FALSE;

    switch (event)
    {
    case NFA_DM_RF_DEACTIVATE_CMD:
        if (old_pres_check_flag)
        {
            /* presence check is already enabled when deactivate cmd is requested,
             * keep the information in control block to issue it later */
            nfa_dm_cb.presence_check_deact_pending = TRUE;
            nfa_dm_cb.presence_check_deact_type    = p_data->deactivate_type;
        }
        else
        {
            status = nfa_dm_send_deactivate_cmd(p_data->deactivate_type);
        }

        break;
    case NFA_DM_RF_DEACTIVATE_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        /* register callback to get interface error NTF */
        NFC_SetStaticRfCback (nfa_dm_disc_data_cback);
        break;
    case NFA_DM_RF_DEACTIVATE_NTF:
        pres_check_event    = TRUE;
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_NTF;
        if (  (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_SLEEP)
            ||(p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_SLEEP_AF)  )
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_W4_HOST_SELECT);
            if (old_pres_check_flag)
            {
                pres_check_event_processed  = TRUE;
                /* process pending deactivate request */
                if (nfa_dm_cb.presence_check_deact_pending)
                {
                    nfa_dm_cb.presence_check_deact_pending = FALSE;
                    /* notify RW module that presence checking is finished */
                    nfa_dm_disc_end_presence_check (NFC_STATUS_OK);

                    if (nfa_dm_cb.presence_check_deact_type == NFC_DEACTIVATE_TYPE_IDLE)
                        NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
                    else
                        nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_NTF, &(p_data->nfc_discover));
                }
                else
                {
                    /* Successfully went to sleep mode for presence check */
                    /* Now wake up the tag to see if it is present */
                    NFC_DiscoverySelect (nfa_dm_cb.disc_cb.activated_rf_disc_id,
                                         nfa_dm_cb.disc_cb.activated_protocol,
                                         nfa_dm_cb.disc_cb.activated_rf_interface);
                }

            }
        }
        else if (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_IDLE)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
            nfa_dm_start_rf_discover ();
        }
        else if (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_DISCOVERY)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_DISCOVERY);
            if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_STOPPING)
            {
                /* stop discovery */
                NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
            }
        }
        nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_NTF, &(p_data->nfc_discover));
        break;

    case NFA_DM_CORE_INTF_ERROR_NTF:
        pres_check_event    = TRUE;
        NFC_Deactivate (NFC_DEACTIVATE_TYPE_DISCOVERY);
        break;

    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_poll_active (): Unexpected discovery event");
        break;
    }

    if (old_pres_check_flag && pres_check_event && !pres_check_event_processed)
    {
        /* performing presence check for unknow protocol and exception conditions happened
         * clear presence check information and report failure */
        nfa_dm_disc_end_presence_check (NFC_STATUS_FAILED);
        if (nfa_dm_cb.presence_check_deact_pending)
        {
            nfa_dm_cb.presence_check_deact_pending = FALSE;

            NFC_Deactivate (nfa_dm_cb.presence_check_deact_type);
        }
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_listen_active
**
** Description      Processing discovery events in NFA_DM_RFST_LISTEN_ACTIVE state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_listen_active (tNFA_DM_RF_DISC_SM_EVENT event,
                                          tNFA_DM_RF_DISC_DATA     *p_data)
{
    switch (event)
    {
    case NFA_DM_RF_DEACTIVATE_CMD:
        nfa_dm_send_deactivate_cmd(p_data->deactivate_type);
        break;
    case NFA_DM_RF_DEACTIVATE_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        break;
    case NFA_DM_RF_DEACTIVATE_NTF:
        /* clear both W4_RSP and W4_NTF because of race condition between deactivat CMD and link loss */
        nfa_dm_cb.disc_cb.disc_flags &= ~(NFA_DM_DISC_FLAGS_W4_RSP|NFA_DM_DISC_FLAGS_W4_NTF);

        if (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_IDLE)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
            nfa_dm_start_rf_discover ();
        }
        else if (  (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_SLEEP)
                 ||(p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_SLEEP_AF)  )
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_LISTEN_SLEEP);
        }
        else if (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_DISCOVERY)
        {
            /* Discovery */
            nfa_dm_disc_new_state (NFA_DM_RFST_DISCOVERY);
            if (nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_STOPPING)
            {
                /* stop discovery */
                NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
            }
        }
        nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_NTF, &(p_data->nfc_discover));
        break;

    case NFA_DM_CORE_INTF_ERROR_NTF:
        break;
    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_listen_active (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_listen_sleep
**
** Description      Processing discovery events in NFA_DM_RFST_LISTEN_SLEEP state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_listen_sleep (tNFA_DM_RF_DISC_SM_EVENT event,
                                         tNFA_DM_RF_DISC_DATA *p_data)
{
    switch (event)
    {
    case NFA_DM_RF_DEACTIVATE_CMD:
        nfa_dm_send_deactivate_cmd (p_data->deactivate_type);
        break;
    case NFA_DM_RF_DEACTIVATE_RSP:
        nfa_dm_cb.disc_cb.disc_flags &= ~NFA_DM_DISC_FLAGS_W4_RSP;
        /* if deactivate type in CMD was IDLE */
        if (!(nfa_dm_cb.disc_cb.disc_flags & NFA_DM_DISC_FLAGS_W4_NTF))
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
            nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_RSP, &(p_data->nfc_discover));
        }
        break;
    case NFA_DM_RF_DEACTIVATE_NTF:
        /* clear both W4_RSP and W4_NTF because of race condition between deactivat CMD and link loss */
        nfa_dm_cb.disc_cb.disc_flags &= ~(NFA_DM_DISC_FLAGS_W4_RSP|NFA_DM_DISC_FLAGS_W4_NTF);
        if (p_data->nfc_discover.deactivate.type == NFC_DEACTIVATE_TYPE_IDLE)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
            nfa_dm_start_rf_discover ();
        }
        else if (p_data->nfc_discover.deactivate.type == NFA_DEACTIVATE_TYPE_DISCOVERY)
        {
            nfa_dm_disc_new_state (NFA_DM_RFST_DISCOVERY);
        }
        else
        {
            NFA_TRACE_ERROR0 ("Unexpected deactivation type");
            nfa_dm_disc_new_state (NFA_DM_RFST_IDLE);
            nfa_dm_start_rf_discover ();
        }
        nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_NTF, &(p_data->nfc_discover));
        break;
    case NFA_DM_RF_INTF_ACTIVATED_NTF:
        nfa_dm_disc_new_state (NFA_DM_RFST_LISTEN_ACTIVE);
        if (nfa_dm_disc_notify_activation (&(p_data->nfc_discover)) == NFA_STATUS_FAILED)
        {
            NFA_TRACE_DEBUG0 ("Not matched, restart discovery after receiving deactivate ntf");

            /* after receiving deactivate event, restart discovery */
            NFC_Deactivate (NFA_DEACTIVATE_TYPE_IDLE);
        }
        break;
    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_listen_sleep (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_lp_listen
**
** Description      Processing discovery events in NFA_DM_RFST_LP_LISTEN state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_lp_listen (tNFA_DM_RF_DISC_SM_EVENT event,
                                           tNFA_DM_RF_DISC_DATA *p_data)
{
    switch (event)
    {
    case NFA_DM_RF_INTF_ACTIVATED_NTF:
        nfa_dm_disc_new_state (NFA_DM_RFST_LP_ACTIVE);
        nfa_dm_disc_notify_activation (&(p_data->nfc_discover));
        break;

    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_lp_listen (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_lp_active
**
** Description      Processing discovery events in NFA_DM_RFST_LP_ACTIVE state
**
** Returns          void
**
*******************************************************************************/
static void nfa_dm_disc_sm_lp_active (tNFA_DM_RF_DISC_SM_EVENT event,
                                           tNFA_DM_RF_DISC_DATA *p_data)
{
    switch (event)
    {
    case NFA_DM_RF_DEACTIVATE_NTF:
        nfa_dm_disc_new_state (NFA_DM_RFST_LP_LISTEN);
        nfa_dm_disc_notify_deactivation (NFA_DM_RF_DEACTIVATE_NTF, &(p_data->nfc_discover));
        break;
    default:
        NFA_TRACE_ERROR0 ("nfa_dm_disc_sm_lp_active (): Unexpected discovery event");
        break;
    }
}

/*******************************************************************************
**
** Function         nfa_dm_disc_sm_execute
**
** Description      Processing discovery related events
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_disc_sm_execute (tNFA_DM_RF_DISC_SM_EVENT event, tNFA_DM_RF_DISC_DATA *p_data)
{
#if (BT_TRACE_VERBOSE == TRUE)
    NFA_TRACE_DEBUG5 ("nfa_dm_disc_sm_execute (): state: %s (%d), event: %s(%d) disc_flags: 0x%x",
                       nfa_dm_disc_state_2_str (nfa_dm_cb.disc_cb.disc_state), nfa_dm_cb.disc_cb.disc_state,
                       nfa_dm_disc_event_2_str (event), event, nfa_dm_cb.disc_cb.disc_flags);
#else
    NFA_TRACE_DEBUG3 ("nfa_dm_disc_sm_execute(): state: %d, event:%d disc_flags: 0x%x",
                       nfa_dm_cb.disc_cb.disc_state, event, nfa_dm_cb.disc_cb.disc_flags);
#endif

    switch (nfa_dm_cb.disc_cb.disc_state)
    {
    /*  RF Discovery State - Idle */
    case NFA_DM_RFST_IDLE:
        nfa_dm_disc_sm_idle (event, p_data);
        break;

    /* RF Discovery State - Discovery */
    case NFA_DM_RFST_DISCOVERY:
        nfa_dm_disc_sm_discovery (event, p_data);
        break;

    /*RF Discovery State - Wait for all discoveries */
    case NFA_DM_RFST_W4_ALL_DISCOVERIES:
        nfa_dm_disc_sm_w4_all_discoveries (event, p_data);
        break;

    /* RF Discovery State - Wait for host selection */
    case NFA_DM_RFST_W4_HOST_SELECT:
        nfa_dm_disc_sm_w4_host_select (event, p_data);
        break;

    /* RF Discovery State - Poll mode activated */
    case NFA_DM_RFST_POLL_ACTIVE:
        nfa_dm_disc_sm_poll_active (event, p_data);
        break;

    /* RF Discovery State - listen mode activated */
    case NFA_DM_RFST_LISTEN_ACTIVE:
        nfa_dm_disc_sm_listen_active (event, p_data);
        break;

    /* RF Discovery State - listen mode sleep */
    case NFA_DM_RFST_LISTEN_SLEEP:
        nfa_dm_disc_sm_listen_sleep (event, p_data);
        break;

    /* Listening in Low Power mode    */
    case NFA_DM_RFST_LP_LISTEN:
        nfa_dm_disc_sm_lp_listen (event, p_data);
        break;

    /* Activated in Low Power mode    */
    case NFA_DM_RFST_LP_ACTIVE:
        nfa_dm_disc_sm_lp_active (event, p_data);
        break;
    }
#if (BT_TRACE_VERBOSE == TRUE)
    NFA_TRACE_DEBUG3 ("nfa_dm_disc_sm_execute (): new state: %s (%d), disc_flags: 0x%x",
                       nfa_dm_disc_state_2_str (nfa_dm_cb.disc_cb.disc_state), nfa_dm_cb.disc_cb.disc_state,
                       nfa_dm_cb.disc_cb.disc_flags);
#else
    NFA_TRACE_DEBUG2 ("nfa_dm_disc_sm_execute(): new state: %d,  disc_flags: 0x%x",
                       nfa_dm_cb.disc_cb.disc_state, nfa_dm_cb.disc_cb.disc_flags);
#endif
}

/*******************************************************************************
**
** Function         nfa_dm_add_rf_discover
**
** Description      Add discovery configuration and callback function
**
** Returns          valid handle if success
**
*******************************************************************************/
tNFA_HANDLE nfa_dm_add_rf_discover (tNFA_DM_DISC_TECH_PROTO_MASK disc_mask,
                                    tNFA_DM_DISC_HOST_ID         host_id,
                                    tNFA_DISCOVER_CBACK         *p_disc_cback)
{
    UINT8       xx;

    NFA_TRACE_DEBUG1 ("nfa_dm_add_rf_discover () disc_mask=0x%x", disc_mask);

    for (xx = 0; xx < NFA_DM_DISC_NUM_ENTRIES; xx++)
    {
        if (!nfa_dm_cb.disc_cb.entry[xx].in_use)
        {
            nfa_dm_cb.disc_cb.entry[xx].in_use              = TRUE;
            nfa_dm_cb.disc_cb.entry[xx].requested_disc_mask = disc_mask;
            nfa_dm_cb.disc_cb.entry[xx].host_id             = host_id;
            nfa_dm_cb.disc_cb.entry[xx].p_disc_cback        = p_disc_cback;
            nfa_dm_cb.disc_cb.entry[xx].disc_flags          = NFA_DM_DISC_FLAGS_NOTIFY;
            return xx;
        }
    }

    return NFA_HANDLE_INVALID;
}

/*******************************************************************************
**
** Function         nfa_dm_start_excl_discovery
**
** Description      Start exclusive RF discovery
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_start_excl_discovery (tNFA_TECHNOLOGY_MASK poll_tech_mask,
                                  tNFA_LISTEN_CFG *p_listen_cfg,
                                  tNFA_DISCOVER_CBACK  *p_disc_cback)
{
    tNFA_DM_DISC_TECH_PROTO_MASK poll_disc_mask = 0;

    NFA_TRACE_DEBUG0 ("nfa_dm_start_excl_discovery ()");

    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_A)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_PA_T1T;
        poll_disc_mask |= NFA_DM_DISC_MASK_PA_T2T;
        poll_disc_mask |= NFA_DM_DISC_MASK_PA_ISO_DEP;
        poll_disc_mask |= NFA_DM_DISC_MASK_PA_NFC_DEP;
        poll_disc_mask |= NFA_DM_DISC_MASK_P_LEGACY;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_A_ACTIVE)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_PAA_NFC_DEP;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_B)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_PB_ISO_DEP;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_F)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_PF_T3T;
        poll_disc_mask |= NFA_DM_DISC_MASK_PF_NFC_DEP;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_F_ACTIVE)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_PFA_NFC_DEP;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_ISO15693)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_P_ISO15693;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_B_PRIME)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_P_B_PRIME;
    }
    if (poll_tech_mask & NFA_TECHNOLOGY_MASK_KOVIO)
    {
        poll_disc_mask |= NFA_DM_DISC_MASK_P_KOVIO;
    }

    nfa_dm_cb.disc_cb.excl_disc_entry.in_use              = TRUE;
    nfa_dm_cb.disc_cb.excl_disc_entry.requested_disc_mask = poll_disc_mask;
    nfa_dm_cb.disc_cb.excl_disc_entry.host_id             = NFA_DM_DISC_HOST_ID_DH;
    nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback        = p_disc_cback;
    nfa_dm_cb.disc_cb.excl_disc_entry.disc_flags          = NFA_DM_DISC_FLAGS_NOTIFY;

    memcpy (&nfa_dm_cb.disc_cb.excl_listen_config, p_listen_cfg, sizeof (tNFA_LISTEN_CFG));

    nfa_dm_disc_sm_execute (NFA_DM_RF_DISCOVER_CMD, NULL);
}

/*******************************************************************************
**
** Function         nfa_dm_stop_excl_discovery
**
** Description      Stop exclusive RF discovery
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_stop_excl_discovery (void)
{
    NFA_TRACE_DEBUG0 ("nfa_dm_stop_excl_discovery ()");

    nfa_dm_cb.disc_cb.excl_disc_entry.in_use       = FALSE;
    nfa_dm_cb.disc_cb.excl_disc_entry.p_disc_cback = NULL;
}

/*******************************************************************************
**
** Function         nfa_dm_delete_rf_discover
**
** Description      Remove discovery configuration and callback function
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_delete_rf_discover (tNFA_HANDLE handle)
{
    NFA_TRACE_DEBUG1 ("nfa_dm_delete_rf_discover () handle=0x%x", handle);

    if (handle < NFA_DM_DISC_NUM_ENTRIES)
    {
        nfa_dm_cb.disc_cb.entry[handle].in_use = FALSE;
    }
    else
    {
        NFA_TRACE_ERROR0 ("Invalid discovery handle");
    }
}

/*******************************************************************************
**
** Function         nfa_dm_rf_discover_select
**
** Description      Select target, protocol and RF interface
**
** Returns          void
**
*******************************************************************************/
void nfa_dm_rf_discover_select (UINT8             rf_disc_id,
                                       tNFA_NFC_PROTOCOL protocol,
                                       tNFA_INTF_TYPE    rf_interface)
{
    tNFA_DM_DISC_SELECT_PARAMS select_params;
    tNFA_CONN_EVT_DATA conn_evt;

    NFA_TRACE_DEBUG3 ("nfa_dm_disc_select () rf_disc_id:0x%X, protocol:0x%X, rf_interface:0x%X",
                       rf_disc_id, protocol, rf_interface);

    if (nfa_dm_cb.disc_cb.disc_state == NFA_DM_RFST_W4_HOST_SELECT)
    {
        /* state is OK: notify the status when the response is received from NFCC */
        select_params.rf_disc_id   = rf_disc_id;
        select_params.protocol     = protocol;
        select_params.rf_interface = rf_interface;

        nfa_dm_cb.disc_cb.disc_flags |= NFA_DM_DISC_FLAGS_NOTIFY;
        nfa_dm_disc_sm_execute (NFA_DM_RF_DISCOVER_SELECT_CMD, (tNFA_DM_RF_DISC_DATA *) &select_params);
    }
    else
    {
        /* Wrong state: notify failed status right away */
        conn_evt.status = NFA_STATUS_FAILED;
        nfa_dm_conn_cback_event_notify (NFA_SELECT_RESULT_EVT, &conn_evt);
    }
}

/*******************************************************************************
**
** Function         nfa_dm_rf_deactivate
**
** Description      Deactivate NFC link
**
** Returns          NFA_STATUS_OK if success
**
*******************************************************************************/
tNFA_STATUS nfa_dm_rf_deactivate (tNFA_DEACTIVATE_TYPE deactivate_type)
{
    NFA_TRACE_DEBUG1 ("nfa_dm_rf_deactivate () deactivate_type:0x%X", deactivate_type);

    if (deactivate_type == NFA_DEACTIVATE_TYPE_SLEEP)
    {
        if (nfa_dm_cb.disc_cb.activated_protocol == NFA_PROTOCOL_NFC_DEP)
            deactivate_type = NFC_DEACTIVATE_TYPE_SLEEP_AF;
        else
            deactivate_type = NFC_DEACTIVATE_TYPE_SLEEP;
    }

    if (nfa_dm_cb.disc_cb.disc_state == NFA_DM_RFST_IDLE)
    {
        return NFA_STATUS_FAILED;
    }
    else
    {
        nfa_dm_disc_sm_execute (NFA_DM_RF_DEACTIVATE_CMD, (tNFA_DM_RF_DISC_DATA *) &deactivate_type);
        return NFA_STATUS_OK;
    }
}

#if (BT_TRACE_VERBOSE == TRUE)
/*******************************************************************************
**
** Function         nfa_dm_disc_state_2_str
**
** Description      convert nfc discovery state to string
**
*******************************************************************************/
static char *nfa_dm_disc_state_2_str (UINT8 state)
{
    switch (state)
    {
    case NFA_DM_RFST_IDLE:
        return "IDLE";

    case NFA_DM_RFST_DISCOVERY:
        return "DISCOVERY";

    case NFA_DM_RFST_W4_ALL_DISCOVERIES:
        return "W4_ALL_DISCOVERIES";

    case NFA_DM_RFST_W4_HOST_SELECT:
        return "W4_HOST_SELECT";

    case NFA_DM_RFST_POLL_ACTIVE:
        return "POLL_ACTIVE";

    case NFA_DM_RFST_LISTEN_ACTIVE:
        return "LISTEN_ACTIVE";

    case NFA_DM_RFST_LISTEN_SLEEP:
        return "LISTEN_SLEEP";

    case NFA_DM_RFST_LP_LISTEN:
        return "LP_LISTEN";

    case NFA_DM_RFST_LP_ACTIVE:
        return "LP_ACTIVE";
    }
    return "Unknown";
}

/*******************************************************************************
**
** Function         nfa_dm_disc_event_2_str
**
** Description      convert nfc discovery RSP/NTF to string
**
*******************************************************************************/
static char *nfa_dm_disc_event_2_str (UINT8 event)
{
    switch (event)
    {
    case NFA_DM_RF_DISCOVER_CMD:
        return "DISCOVER_CMD";

    case NFA_DM_RF_DISCOVER_RSP:
        return "DISCOVER_RSP";

    case NFA_DM_RF_DISCOVER_NTF:
        return "DISCOVER_NTF";

    case NFA_DM_RF_DISCOVER_SELECT_CMD:
        return "SELECT_CMD";

    case NFA_DM_RF_DISCOVER_SELECT_RSP:
        return "SELECT_RSP";

    case NFA_DM_RF_INTF_ACTIVATED_NTF:
        return "ACTIVATED_NTF";

    case NFA_DM_RF_DEACTIVATE_CMD:
        return "DEACTIVATE_CMD";

    case NFA_DM_RF_DEACTIVATE_RSP:
        return "DEACTIVATE_RSP";

    case NFA_DM_RF_DEACTIVATE_NTF:
        return "DEACTIVATE_NTF";

    case NFA_DM_LP_LISTEN_CMD:
        return "NFA_DM_LP_LISTEN_CMD";

    case NFA_DM_CORE_INTF_ERROR_NTF:
        return "INTF_ERROR_NTF";

    }
    return "Unknown";
}
#endif /* BT_TRACE_VERBOSE */

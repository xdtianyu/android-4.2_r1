/* Copyright (C) 2010 The Android Open Source Project.
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

package com.android.exchange.adapter;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import com.android.emailcommon.provider.Policy;
import com.android.exchange.EasSyncService;
import com.android.exchange.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Parse the result of the Provision command
 */
public class ProvisionParser extends Parser {
    private final EasSyncService mService;
    private Policy mPolicy = null;
    private String mSecuritySyncKey = null;
    private boolean mRemoteWipe = false;
    private boolean mIsSupportable = true;
    private boolean smimeRequired = false;
    private final Resources mResources;

    public ProvisionParser(InputStream in, EasSyncService service) throws IOException {
        super(in);
        mService = service;
        mResources = service.mContext.getResources();
    }

    public Policy getPolicy() {
        return mPolicy;
    }

    public String getSecuritySyncKey() {
        return mSecuritySyncKey;
    }

    public void setSecuritySyncKey(String securitySyncKey) {
        mSecuritySyncKey = securitySyncKey;
    }

    public boolean getRemoteWipe() {
        return mRemoteWipe;
    }

    public boolean hasSupportablePolicySet() {
        return (mPolicy != null) && mIsSupportable;
    }

    public void clearUnsupportablePolicies() {
        mIsSupportable = true;
        mPolicy.mProtocolPoliciesUnsupported = null;
    }

    private void addPolicyString(StringBuilder sb, int res) {
        sb.append(mResources.getString(res));
        sb.append(Policy.POLICY_STRING_DELIMITER);
    }

    /**
     * Complete setup of a Policy; we normalize it first (removing inconsistencies, etc.) and then
     * generate the tokenized "protocol policies enforced" string.  Note that unsupported policies
     * must have been added prior to calling this method (this is only a possibility with wbxml
     * policy documents, as all versions of the OS support the policies in xml documents).
     */
    private void setPolicy(Policy policy) {
        policy.normalize();
        StringBuilder sb = new StringBuilder();
        if (policy.mDontAllowAttachments) {
            addPolicyString(sb, R.string.policy_dont_allow_attachments);
        }
        if (policy.mRequireManualSyncWhenRoaming) {
            addPolicyString(sb, R.string.policy_require_manual_sync_roaming);
        }
        policy.mProtocolPoliciesEnforced = sb.toString();
        mPolicy = policy;
    }

    private boolean deviceSupportsEncryption() {
        DevicePolicyManager dpm = (DevicePolicyManager)
                mService.mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        int status = dpm.getStorageEncryptionStatus();
        return status != DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
    }

    private void parseProvisionDocWbxml() throws IOException {
        Policy policy = new Policy();
        ArrayList<Integer> unsupportedList = new ArrayList<Integer>();
        boolean passwordEnabled = false;

        while (nextTag(Tags.PROVISION_EAS_PROVISION_DOC) != END) {
            boolean tagIsSupported = true;
            int res = 0;
            switch (tag) {
                case Tags.PROVISION_DEVICE_PASSWORD_ENABLED:
                    if (getValueInt() == 1) {
                        passwordEnabled = true;
                        if (policy.mPasswordMode == Policy.PASSWORD_MODE_NONE) {
                            policy.mPasswordMode = Policy.PASSWORD_MODE_SIMPLE;
                        }
                    }
                    break;
                case Tags.PROVISION_MIN_DEVICE_PASSWORD_LENGTH:
                    policy.mPasswordMinLength = getValueInt();
                    break;
                case Tags.PROVISION_ALPHA_DEVICE_PASSWORD_ENABLED:
                    if (getValueInt() == 1) {
                        policy.mPasswordMode = Policy.PASSWORD_MODE_STRONG;
                    }
                    break;
                case Tags.PROVISION_MAX_INACTIVITY_TIME_DEVICE_LOCK:
                    // EAS gives us seconds, which is, happily, what the PolicySet requires
                    policy.mMaxScreenLockTime = getValueInt();
                    break;
                case Tags.PROVISION_MAX_DEVICE_PASSWORD_FAILED_ATTEMPTS:
                    policy.mPasswordMaxFails = getValueInt();
                    break;
                case Tags.PROVISION_DEVICE_PASSWORD_EXPIRATION:
                    policy.mPasswordExpirationDays = getValueInt();
                    break;
                case Tags.PROVISION_DEVICE_PASSWORD_HISTORY:
                    policy.mPasswordHistory = getValueInt();
                    break;
                case Tags.PROVISION_ALLOW_CAMERA:
                    policy.mDontAllowCamera = (getValueInt() == 0);
                    break;
                case Tags.PROVISION_ALLOW_SIMPLE_DEVICE_PASSWORD:
                    // Ignore this unless there's any MSFT documentation for what this means
                    // Hint: I haven't seen any that's more specific than "simple"
                    getValue();
                    break;
                // The following policies, if false, can't be supported at the moment
                case Tags.PROVISION_ALLOW_STORAGE_CARD:
                case Tags.PROVISION_ALLOW_UNSIGNED_APPLICATIONS:
                case Tags.PROVISION_ALLOW_UNSIGNED_INSTALLATION_PACKAGES:
                case Tags.PROVISION_ALLOW_WIFI:
                case Tags.PROVISION_ALLOW_TEXT_MESSAGING:
                case Tags.PROVISION_ALLOW_POP_IMAP_EMAIL:
                case Tags.PROVISION_ALLOW_IRDA:
                case Tags.PROVISION_ALLOW_HTML_EMAIL:
                case Tags.PROVISION_ALLOW_BROWSER:
                case Tags.PROVISION_ALLOW_CONSUMER_EMAIL:
                case Tags.PROVISION_ALLOW_INTERNET_SHARING:
                    if (getValueInt() == 0) {
                        tagIsSupported = false;
                        switch(tag) {
                            case Tags.PROVISION_ALLOW_STORAGE_CARD:
                                res = R.string.policy_dont_allow_storage_cards;
                                break;
                            case Tags.PROVISION_ALLOW_UNSIGNED_APPLICATIONS:
                                res = R.string.policy_dont_allow_unsigned_apps;
                                break;
                            case Tags.PROVISION_ALLOW_UNSIGNED_INSTALLATION_PACKAGES:
                                res = R.string.policy_dont_allow_unsigned_installers;
                                break;
                            case Tags.PROVISION_ALLOW_WIFI:
                                res = R.string.policy_dont_allow_wifi;
                                break;
                            case Tags.PROVISION_ALLOW_TEXT_MESSAGING:
                                res = R.string.policy_dont_allow_text_messaging;
                                break;
                            case Tags.PROVISION_ALLOW_POP_IMAP_EMAIL:
                                res = R.string.policy_dont_allow_pop_imap;
                                break;
                            case Tags.PROVISION_ALLOW_IRDA:
                                res = R.string.policy_dont_allow_irda;
                                break;
                            case Tags.PROVISION_ALLOW_HTML_EMAIL:
                                res = R.string.policy_dont_allow_html;
                                policy.mDontAllowHtml = true;
                                break;
                            case Tags.PROVISION_ALLOW_BROWSER:
                                res = R.string.policy_dont_allow_browser;
                                break;
                            case Tags.PROVISION_ALLOW_CONSUMER_EMAIL:
                                res = R.string.policy_dont_allow_consumer_email;
                                break;
                            case Tags.PROVISION_ALLOW_INTERNET_SHARING:
                                res = R.string.policy_dont_allow_internet_sharing;
                                break;
                        }
                        if (res > 0) {
                            unsupportedList.add(res);
                        }
                    }
                    break;
                case Tags.PROVISION_ATTACHMENTS_ENABLED:
                    policy.mDontAllowAttachments = getValueInt() != 1;
                    break;
                // Bluetooth: 0 = no bluetooth; 1 = only hands-free; 2 = allowed
                case Tags.PROVISION_ALLOW_BLUETOOTH:
                    if (getValueInt() != 2) {
                        tagIsSupported = false;
                        unsupportedList.add(R.string.policy_bluetooth_restricted);
                    }
                    break;
                // We may now support device (internal) encryption; we'll check this capability
                // below with the call to SecurityPolicy.isSupported()
                case Tags.PROVISION_REQUIRE_DEVICE_ENCRYPTION:
                    if (getValueInt() == 1) {
                         if (!deviceSupportsEncryption()) {
                            tagIsSupported = false;
                            unsupportedList.add(R.string.policy_require_encryption);
                        } else {
                            policy.mRequireEncryption = true;
                        }
                    }
                    break;
                // Note that DEVICE_ENCRYPTION_ENABLED refers to SD card encryption, which the OS
                // does not yet support.
                case Tags.PROVISION_DEVICE_ENCRYPTION_ENABLED:
                    if (getValueInt() == 1) {
                        log("Policy requires SD card encryption");
                        // Let's see if this can be supported on our device...
                        if (deviceSupportsEncryption()) {
                            StorageManager sm = (StorageManager)mService.mContext.getSystemService(
                                    Context.STORAGE_SERVICE);
                            // NOTE: Private API!
                            // Go through volumes; if ANY are removable, we can't support this
                            // policy.
                            StorageVolume[] volumeList = sm.getVolumeList();
                            for (StorageVolume volume: volumeList) {
                                if (volume.isRemovable()) {
                                    tagIsSupported = false;
                                    log("Removable: " + volume.getDescription(mService.mContext));
                                    break;  // Break only from the storage volume loop
                                } else {
                                    log("Not Removable: " + volume.getDescription(mService.mContext));
                                }
                            }
                            if (tagIsSupported) {
                                // If this policy is requested, we MUST also require encryption
                                log("Device supports SD card encryption");
                                policy.mRequireEncryption = true;
                                break;
                            }
                        } else {
                            log("Device doesn't support encryption; failing");
                            tagIsSupported = false;
                        }
                        // If we fall through, we can't support the policy
                        unsupportedList.add(R.string.policy_require_sd_encryption);
                    }
                    break;
                    // Note this policy; we enforce it in ExchangeService
                case Tags.PROVISION_REQUIRE_MANUAL_SYNC_WHEN_ROAMING:
                    policy.mRequireManualSyncWhenRoaming = getValueInt() == 1;
                    break;
                // We are allowed to accept policies, regardless of value of this tag
                // TODO: When we DO support a recovery password, we need to store the value in
                // the account (so we know to utilize it)
                case Tags.PROVISION_PASSWORD_RECOVERY_ENABLED:
                    // Read, but ignore, value
                    policy.mPasswordRecoveryEnabled = getValueInt() == 1;
                    break;
                // The following policies, if true, can't be supported at the moment
                case Tags.PROVISION_REQUIRE_SIGNED_SMIME_MESSAGES:
                case Tags.PROVISION_REQUIRE_ENCRYPTED_SMIME_MESSAGES:
                case Tags.PROVISION_REQUIRE_SIGNED_SMIME_ALGORITHM:
                case Tags.PROVISION_REQUIRE_ENCRYPTION_SMIME_ALGORITHM:
                    if (getValueInt() == 1) {
                        tagIsSupported = false;
                        if (!smimeRequired) {
                            unsupportedList.add(R.string.policy_require_smime);
                            smimeRequired = true;
                        }
                    }
                    break;
                case Tags.PROVISION_MAX_ATTACHMENT_SIZE:
                    int max = getValueInt();
                    if (max > 0) {
                        policy.mMaxAttachmentSize = max;
                    }
                    break;
                // Complex characters are supported
                case Tags.PROVISION_MIN_DEVICE_PASSWORD_COMPLEX_CHARS:
                    policy.mPasswordComplexChars = getValueInt();
                    break;
                // The following policies are moot; they allow functionality that we don't support
                case Tags.PROVISION_ALLOW_DESKTOP_SYNC:
                case Tags.PROVISION_ALLOW_SMIME_ENCRYPTION_NEGOTIATION:
                case Tags.PROVISION_ALLOW_SMIME_SOFT_CERTS:
                case Tags.PROVISION_ALLOW_REMOTE_DESKTOP:
                    skipTag();
                    break;
                // We don't handle approved/unapproved application lists
                case Tags.PROVISION_UNAPPROVED_IN_ROM_APPLICATION_LIST:
                case Tags.PROVISION_APPROVED_APPLICATION_LIST:
                    // Parse and throw away the content
                    if (specifiesApplications(tag)) {
                        tagIsSupported = false;
                        if (tag == Tags.PROVISION_UNAPPROVED_IN_ROM_APPLICATION_LIST) {
                            unsupportedList.add(R.string.policy_app_blacklist);
                        } else {
                            unsupportedList.add(R.string.policy_app_whitelist);
                        }
                    }
                    break;
                // We accept calendar age, since we never ask for more than two weeks, and that's
                // the most restrictive policy
                case Tags.PROVISION_MAX_CALENDAR_AGE_FILTER:
                    policy.mMaxCalendarLookback = getValueInt();
                    break;
                // We handle max email lookback
                case Tags.PROVISION_MAX_EMAIL_AGE_FILTER:
                    policy.mMaxEmailLookback = getValueInt();
                    break;
                // We currently reject these next two policies
                case Tags.PROVISION_MAX_EMAIL_BODY_TRUNCATION_SIZE:
                case Tags.PROVISION_MAX_EMAIL_HTML_BODY_TRUNCATION_SIZE:
                    String value = getValue();
                    // -1 indicates no required truncation
                    if (!value.equals("-1")) {
                        max = Integer.parseInt(value);
                        if (tag == Tags.PROVISION_MAX_EMAIL_BODY_TRUNCATION_SIZE) {
                            policy.mMaxTextTruncationSize = max;
                            unsupportedList.add(R.string.policy_text_truncation);
                        } else {
                            policy.mMaxHtmlTruncationSize = max;
                            unsupportedList.add(R.string.policy_html_truncation);
                        }
                        tagIsSupported = false;
                    }
                    break;
                default:
                    skipTag();
            }

            if (!tagIsSupported) {
                log("Policy not supported: " + tag);
                mIsSupportable = false;
            }
        }

        // Make sure policy settings are valid; password not enabled trumps other password settings
        if (!passwordEnabled) {
            policy.mPasswordMode = Policy.PASSWORD_MODE_NONE;
        }

        if (!unsupportedList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int res: unsupportedList) {
                addPolicyString(sb, res);
            }
            policy.mProtocolPoliciesUnsupported = sb.toString();
        }

        setPolicy(policy);
    }

    /**
     * Return whether or not either of the application list tags specifies any applications
     * @param endTag the tag whose children we're walking through
     * @return whether any applications were specified (by name or by hash)
     * @throws IOException
     */
    private boolean specifiesApplications(int endTag) throws IOException {
        boolean specifiesApplications = false;
        while (nextTag(endTag) != END) {
            switch (tag) {
                case Tags.PROVISION_APPLICATION_NAME:
                case Tags.PROVISION_HASH:
                    specifiesApplications = true;
                    break;
                default:
                    skipTag();
            }
        }
        return specifiesApplications;
    }

    /*package*/ void parseProvisionDocXml(String doc) throws IOException {
        Policy policy = new Policy();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new ByteArrayInputStream(doc.getBytes()), "UTF-8");
            int type = parser.getEventType();
            if (type == XmlPullParser.START_DOCUMENT) {
                type = parser.next();
                if (type == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("wap-provisioningdoc")) {
                        parseWapProvisioningDoc(parser, policy);
                    }
                }
            }
        } catch (XmlPullParserException e) {
           throw new IOException();
        }

        setPolicy(policy);
    }

    /**
     * Return true if password is required; otherwise false.
     */
    private boolean parseSecurityPolicy(XmlPullParser parser, Policy policy)
            throws XmlPullParserException, IOException {
        boolean passwordRequired = true;
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("parm")) {
                    String name = parser.getAttributeValue(null, "name");
                    if (name.equals("4131")) {
                        String value = parser.getAttributeValue(null, "value");
                        if (value.equals("1")) {
                            passwordRequired = false;
                        }
                    }
                }
            }
        }
        return passwordRequired;
    }

    private void parseCharacteristic(XmlPullParser parser, Policy policy)
            throws XmlPullParserException, IOException {
        boolean enforceInactivityTimer = true;
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                if (parser.getName().equals("parm")) {
                    String name = parser.getAttributeValue(null, "name");
                    String value = parser.getAttributeValue(null, "value");
                    if (name.equals("AEFrequencyValue")) {
                        if (enforceInactivityTimer) {
                            if (value.equals("0")) {
                                policy.mMaxScreenLockTime = 1;
                            } else {
                                policy.mMaxScreenLockTime = 60*Integer.parseInt(value);
                            }
                        }
                    } else if (name.equals("AEFrequencyType")) {
                        // "0" here means we don't enforce an inactivity timeout
                        if (value.equals("0")) {
                            enforceInactivityTimer = false;
                        }
                    } else if (name.equals("DeviceWipeThreshold")) {
                        policy.mPasswordMaxFails = Integer.parseInt(value);
                    } else if (name.equals("CodewordFrequency")) {
                        // Ignore; has no meaning for us
                    } else if (name.equals("MinimumPasswordLength")) {
                        policy.mPasswordMinLength = Integer.parseInt(value);
                    } else if (name.equals("PasswordComplexity")) {
                        if (value.equals("0")) {
                            policy.mPasswordMode = Policy.PASSWORD_MODE_STRONG;
                        } else {
                            policy.mPasswordMode = Policy.PASSWORD_MODE_SIMPLE;
                        }
                    }
                }
            }
        }
    }

    private void parseRegistry(XmlPullParser parser, Policy policy)
            throws XmlPullParserException, IOException {
      while (true) {
          int type = parser.nextTag();
          if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
              break;
          } else if (type == XmlPullParser.START_TAG) {
              String name = parser.getName();
              if (name.equals("characteristic")) {
                  parseCharacteristic(parser, policy);
              }
          }
      }
    }

    private void parseWapProvisioningDoc(XmlPullParser parser, Policy policy)
            throws XmlPullParserException, IOException {
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("wap-provisioningdoc")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.equals("characteristic")) {
                    String atype = parser.getAttributeValue(null, "type");
                    if (atype.equals("SecurityPolicy")) {
                        // If a password isn't required, stop here
                        if (!parseSecurityPolicy(parser, policy)) {
                            return;
                        }
                    } else if (atype.equals("Registry")) {
                        parseRegistry(parser, policy);
                        return;
                    }
                }
            }
        }
    }

    private void parseProvisionData() throws IOException {
        while (nextTag(Tags.PROVISION_DATA) != END) {
            if (tag == Tags.PROVISION_EAS_PROVISION_DOC) {
                parseProvisionDocWbxml();
            } else {
                skipTag();
            }
        }
    }

    private void parsePolicy() throws IOException {
        String policyType = null;
        while (nextTag(Tags.PROVISION_POLICY) != END) {
            switch (tag) {
                case Tags.PROVISION_POLICY_TYPE:
                    policyType = getValue();
                    mService.userLog("Policy type: ", policyType);
                    break;
                case Tags.PROVISION_POLICY_KEY:
                    mSecuritySyncKey = getValue();
                    break;
                case Tags.PROVISION_STATUS:
                    mService.userLog("Policy status: ", getValue());
                    break;
                case Tags.PROVISION_DATA:
                    if (policyType.equalsIgnoreCase(EasSyncService.EAS_2_POLICY_TYPE)) {
                        // Parse the old style XML document
                        parseProvisionDocXml(getValue());
                    } else {
                        // Parse the newer WBXML data
                        parseProvisionData();
                    }
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void parsePolicies() throws IOException {
        while (nextTag(Tags.PROVISION_POLICIES) != END) {
            if (tag == Tags.PROVISION_POLICY) {
                parsePolicy();
            } else {
                skipTag();
            }
        }
    }

    private void parseDeviceInformation() throws IOException {
        while (nextTag(Tags.SETTINGS_DEVICE_INFORMATION) != END) {
            if (tag == Tags.SETTINGS_STATUS) {
                mService.userLog("DeviceInformation status: " + getValue());
            } else {
                skipTag();
            }
        }
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.PROVISION_PROVISION) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            switch (tag) {
                case Tags.PROVISION_STATUS:
                    int status = getValueInt();
                    mService.userLog("Provision status: ", status);
                    res = (status == 1);
                    break;
                case Tags.SETTINGS_DEVICE_INFORMATION:
                    parseDeviceInformation();
                    break;
                case Tags.PROVISION_POLICIES:
                    parsePolicies();
                    break;
                case Tags.PROVISION_REMOTE_WIPE:
                    // Indicate remote wipe command received
                    mRemoteWipe = true;
                    break;
                default:
                    skipTag();
            }
        }
        return res;
    }
}

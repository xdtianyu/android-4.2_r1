/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.exchange;

/**
 * ActiveSync command error status definitions (EAS 14.0 and later); these are in addition to the
 * command-specific errors defined for earlier protocol versions
 */
public class CommandStatusException extends EasException {
    private static final long serialVersionUID = 1L;

    // A status response to an EAS account. Responses < 16 correspond to command-specific errors as
    // reported by EAS versions < 14.0; responses > 100 correspond to generic errors as reported
    // by EAS versions 14.0 and greater
    public final int mStatus;
    // If the error refers to a specific data item, that item's id (as provided by the server) is
    // stored here
    public final String mItemId;

    public static class CommandStatus {
        private static final long serialVersionUID = 1L;

        // Fatal user/provisioning issues (put on security hold)
        public static final int USER_DISABLED_FOR_SYNC = 126;
        public static final int USERS_DISABLED_FOR_SYNC = 127;
        public static final int USER_ON_LEGACY_SERVER_CANT_SYNC = 128;
        public static final int DEVICE_QUARANTINED = 129;
        public static final int ACCESS_DENIED = 130;
        public static final int USER_ACCOUNT_DISABLED = 131;
        public static final int NOT_PROVISIONABLE_PARTIAL = 139;
        public static final int NOT_PROVISIONABLE_LEGACY_DEVICE = 141;
        public static final int TOO_MANY_PARTNERSHIPS = 177;

        // Sync state problems (bad key, multiple client conflict, etc.)
        public static final int SYNC_STATE_LOCKED = 133;
        public static final int SYNC_STATE_CORRUPT = 134;
        public static final int SYNC_STATE_EXISTS = 135;
        public static final int SYNC_STATE_INVALID = 136;

        // Soft provisioning errors, we need to send Provision command
        public static final int NEEDS_PROVISIONING_WIPE = 140;
        public static final int NEEDS_PROVISIONING = 142;
        public static final int NEEDS_PROVISIONING_REFRESH = 143;
        public static final int NEEDS_PROVISIONING_INVALID = 144;

        // WTF issues (really shouldn't happen in our implementation)
        public static final int WTF_INVALID_COMMAND = 137;
        public static final int WTF_INVALID_PROTOCOL = 138;
        public static final int WTF_DEVICE_CLAIMS_EXTERNAL_MANAGEMENT = 145;
        public static final int WTF_UNKNOWN_ITEM_TYPE = 147;
        public static final int WTF_REQUIRES_PROXY_WITHOUT_SSL = 148;

        // For SmartReply/SmartForward
        public static final int ITEM_NOT_FOUND = 150;

        // Transient or possibly transient errors
        public static final int SERVER_ERROR_RETRY = 111;
        public static final int SYNC_STATE_NOT_FOUND = 132;

        // String version of error status codes (for logging only)
        private static final int STATUS_TEXT_START = 101;
        private static final int STATUS_TEXT_END = 150;
        private static final String[] STATUS_TEXT = {
            "InvalidContent", "InvalidWBXML", "InvalidXML", "InvalidDateTime", "InvalidIDCombo",
            "InvalidIDs", "InvalidMIME", "DeviceIdError", "DeviceTypeError", "ServerError",
            "ServerErrorRetry", "ADAccessDenied", "Quota", "ServerOffline", "SendQuota",
            "RecipientUnresolved", "ReplyNotAllowed", "SentPreviously", "NoRecipient", "SendFailed",
            "ReplyFailed", "AttsTooLarge", "NoMailbox", "CantBeAnonymous", "UserNotFound",
            "UserDisabled", "NewMailbox", "LegacyMailbox", "DeviceBlocked", "AccessDenied",
            "AcctDisabled", "SyncStateNF", "SyncStateLocked", "SyncStateCorrupt", "SyncStateExists",
            "SyncStateInvalid", "BadCommand", "BadVersion", "NotFullyProvisionable", "RemoteWipe",
            "LegacyDevice", "NotProvisioned", "PolicyRefresh", "BadPolicyKey", "ExternallyManaged",
            "NoRecurrence", "UnexpectedClass", "RemoteHasNoSSL", "InvalidRequest", "ItemNotFound"
        };

        public static boolean isNeedsProvisioning(int status) {
            return (status == CommandStatus.NEEDS_PROVISIONING ||
                    status == CommandStatus.NEEDS_PROVISIONING_REFRESH ||
                    status == CommandStatus.NEEDS_PROVISIONING_INVALID ||
                    status == CommandStatus.NEEDS_PROVISIONING_WIPE);
        }

        public static boolean isBadSyncKey(int status) {
            return (status == CommandStatus.SYNC_STATE_CORRUPT ||
                    status == CommandStatus.SYNC_STATE_INVALID);
        }

        public static boolean isDeniedAccess(int status) {
            return (status == CommandStatus.USER_DISABLED_FOR_SYNC ||
                    status == CommandStatus.USERS_DISABLED_FOR_SYNC ||
                    status == CommandStatus.USER_ON_LEGACY_SERVER_CANT_SYNC ||
                    status == CommandStatus.DEVICE_QUARANTINED ||
                    status == CommandStatus.ACCESS_DENIED ||
                    status == CommandStatus.USER_ACCOUNT_DISABLED ||
                    status == CommandStatus.NOT_PROVISIONABLE_LEGACY_DEVICE ||
                    status == CommandStatus.NOT_PROVISIONABLE_PARTIAL ||
                    status == CommandStatus.TOO_MANY_PARTNERSHIPS);
        }

        public static boolean isTransientError(int status) {
            return status == CommandStatus.SYNC_STATE_NOT_FOUND ||
                status == CommandStatus.SERVER_ERROR_RETRY;
        }

        public static String toString(int status) {
            StringBuilder sb = new StringBuilder();
            sb.append(status);
            sb.append(" (");
            if (status < STATUS_TEXT_START || status > STATUS_TEXT_END) {
                sb.append("unknown");
            } else {
                int offset = status - STATUS_TEXT_START;
                if (offset <= STATUS_TEXT.length) {
                    sb.append(STATUS_TEXT[offset]);
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }

    public CommandStatusException(int status) {
        mStatus = status;
        mItemId = null;
    }

    public CommandStatusException(int status, String itemId) {
        mStatus = status;
        mItemId = itemId;
    }
}

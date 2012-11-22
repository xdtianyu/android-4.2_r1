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

import android.security.KeyChain;

import com.android.emailcommon.utility.CertificateRequestor;

/**
 * A subclass of the {@link CertificateRequestor} so that the Exchange process
 * can request access to a certificate.
 *
 * They {@link KeyChain} API works in such a way that the host
 * activity requesting the certificate must be running in the process with the UID of who will
 * actually use the certificate. Since the Exchange process needs to establish connections and use
 * certificates for EAS accounts, requests for certificates must be delegated by an Activity in this
 * process.
 */
public class EasCertificateRequestor extends CertificateRequestor {
    // Intentionally blank - no behavior overridden.
}

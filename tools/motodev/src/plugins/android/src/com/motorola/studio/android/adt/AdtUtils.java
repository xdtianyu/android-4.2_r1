/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.motorola.studio.android.adt;

import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.android.ide.eclipse.adt.internal.project.ExportHelper;

/**
 * Methods to interface with ADT plug-in.
 */
public class AdtUtils
{

    /**
     * Exports APK in release mode, signing the package with the given certificate/key
     * @throws CoreException 
     */
    public static void exportReleaseApk(IProject project, File outputFile, PrivateKey key,
            X509Certificate certificate, IProgressMonitor monitor) throws CoreException
    {
        ExportHelper exportHelper = new ExportHelper();
        ExportHelper.exportReleaseApk(project, outputFile, key, certificate, monitor);
    }

    /**
     * Exports APK in release mode, without signing
     * @throws CoreException 
     */
    public static void exportUnsignedReleaseApk(IProject project, File outputFile,
            IProgressMonitor monitor) throws CoreException
    {
        ExportHelper exportHelper = new ExportHelper();
        ExportHelper.exportReleaseApk(project, outputFile, null, null, monitor);
    }

}

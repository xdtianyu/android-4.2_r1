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
package com.motorola.studio.android.emulator.device;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IStartup;

import com.motorola.studio.android.AndroidPlugin;
import com.motorola.studio.android.adt.SdkUtils;
import com.motorola.studio.android.common.preferences.DialogWithToggleUtils;
import com.motorola.studio.android.emulator.device.handlers.OpenNewDeviceWizardHandler;
import com.motorola.studio.android.emulator.i18n.EmulatorNLS;

public class CreateAVDOnStartupListener implements IStartup
{
    private static final String SDK_CREATE_NEW_AVD_KEY = "create.avd.on.startup";

    private static final Lock lock = new ReentrantReadWriteLock().writeLock();

    private static boolean executed = false;

    public void earlyStartup()
    {
        AndroidPlugin.getDefault().addSDKLoaderListener(new Runnable()
        {
            public void run()
            {
                lock.lock();
                if (!executed
                        && ((SdkUtils.getAllTargets() != null) && (SdkUtils.getAllTargets().length > 0))
                        && ((SdkUtils.getAllValidVms() != null) && (SdkUtils.getAllValidVms().length == 0)))
                {
                    if (DialogWithToggleUtils.showQuestion(SDK_CREATE_NEW_AVD_KEY,
                            EmulatorNLS.UI_SdkSetup_CreateAVD_Title,
                            EmulatorNLS.UI_SdkSetup_CreateAVD_Message))
                    {
                        OpenNewDeviceWizardHandler handler = new OpenNewDeviceWizardHandler();
                        try
                        {
                            handler.execute(new ExecutionEvent());
                        }
                        catch (ExecutionException e)
                        {
                            //do nothing
                            lock.unlock();
                        }
                    }
                    executed = true;
                }
                lock.unlock();
            }
        });

    }

}

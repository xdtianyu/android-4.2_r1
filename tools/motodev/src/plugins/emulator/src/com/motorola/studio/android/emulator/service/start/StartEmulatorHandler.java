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
package com.motorola.studio.android.emulator.service.start;

import static com.motorola.studio.android.common.log.StudioLogger.debug;
import static com.motorola.studio.android.common.log.StudioLogger.error;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;

import com.motorola.studio.android.common.log.StudioLogger;
import com.motorola.studio.android.emulator.EmulatorPlugin;
import com.motorola.studio.android.emulator.core.exception.StartCancelledException;
import com.motorola.studio.android.emulator.device.instance.AndroidDeviceInstance;
import com.motorola.studio.android.emulator.i18n.EmulatorNLS;
import com.motorola.studio.android.emulator.logic.AndroidLogicUtils;
import com.motorola.studio.android.emulator.logic.start.AndroidEmulatorStarter;

/**
 * DESCRIPTION: This class handles the start Android Emulator action, which
 * transitions from both stopped states to started
 * 
 * RESPONSIBILITY: Delegate the action to the AndroidEmulatorStarter, which
 * contains the business layer logic for starting Android Emulators.
 * 
 * COLABORATORS: None.
 * 
 * USAGE: This class is intended to be used by Eclipse only
 */
public class StartEmulatorHandler extends ServiceHandler
{

    /**
     * @see IServiceHandler#newInstance()
     */
    @Override
    public IServiceHandler newInstance()
    {
        return new StartEmulatorHandler();
    }

    /**
     * @see ServiceHandler#runService(IInstance, Map, IProgressMonitor)
     */
    @Override
    public IStatus runService(IInstance instance, Map<Object, Object> arguments,
            IProgressMonitor monitor)
    {
        IStatus status = Status.OK_STATUS;
        try
        {

            String description = "";

            // Tests if the given instance is a Android instance
            if (!(instance instanceof AndroidDeviceInstance))
            {
                error("Aborting start service. This is not an Android Emulator instance...");
                status =
                        new Status(IStatus.ERROR, EmulatorPlugin.PLUGIN_ID,
                                EmulatorNLS.ERR_StartEmulatorHandler_NotAnAndroidEmulator);
            }
            else
            {
                description = ((AndroidDeviceInstance) instance).getTarget();
                try
                {
                    AndroidLogicUtils.testCanceled(monitor);

                    status =
                            AndroidEmulatorStarter.startInstance((AndroidDeviceInstance) instance,
                                    arguments, monitor);

                    debug("Finished Execution of the start emulator service... :" + instance
                            + " Status => " + status);
                }
                catch (StartCancelledException e)
                {
                    monitor.done();
                    status = Status.CANCEL_STATUS;
                }
            }

            description = StudioLogger.KEY_TARGET + description;
            StudioLogger.collectUsageData(StudioLogger.WHAT_EMULATOR_START,
                    StudioLogger.KIND_EMULATOR, description, EmulatorPlugin.PLUGIN_ID,
                    EmulatorPlugin.getDefault().getBundle().getVersion().toString());
        }
        catch (Throwable t)
        {
            StudioLogger.error(StartEmulatorHandler.class.toString(),
                    "An exception ocurred during emulator start up process.", t);
            status =
                    new Status(Status.ERROR, EmulatorPlugin.PLUGIN_ID,
                            "An exception ocurred during emulator start up process.");
        }
        return status;
    }

    /**
     * @see ServiceHandler#updatingService(IInstance, IProgressMonitor)
     */
    @Override
    public IStatus updatingService(IInstance instance, IProgressMonitor monitor)
    {
        return Status.OK_STATUS;
    }

}

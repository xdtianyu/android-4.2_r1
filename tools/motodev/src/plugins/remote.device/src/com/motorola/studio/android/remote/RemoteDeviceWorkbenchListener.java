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
package com.motorola.studio.android.remote;

import java.util.Collection;

import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;

import com.motorola.studio.android.adt.ISerialNumbered;
import com.motorola.studio.android.common.log.StudioLogger;
import com.motorola.studio.android.common.preferences.DialogWithToggleUtils;
import com.motorola.studio.android.devices.DevicesManager;
import com.motorola.studio.android.remote.i18n.RemoteDeviceNLS;
import com.motorola.studio.android.remote.instance.RemoteDeviceInstance;

/**
 * Class to implement the IWorkbenchListener that will check if
 * there are connected Remote Devices when the uses tries to close Studio.
 * The user will be prompted whether he wants to disconnect them or not.
 */
public class RemoteDeviceWorkbenchListener implements IWorkbenchListener
{

    public void postShutdown(IWorkbench workbench)
    {
        // Nothing to do.   
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchListener#preShutdown(org.eclipse.ui.IWorkbench, boolean)
     */
    public boolean preShutdown(IWorkbench workbench, boolean forced)
    {

        Collection<ISerialNumbered> connectedDevices =
                DevicesManager.getInstance().getOnlineDevicesByType(RemoteDeviceInstance.class);

        if (connectedDevices.size() > 0)
        {

            boolean disconnectRemoteInstances =
                    DialogWithToggleUtils
                            .showQuestion(
                                    RemoteDeviceConstants.DISCONNECT_ALL_REMOTE_DEVICES_IN_SHUTDOWN_KEY_PREFERENCE,
                                    RemoteDeviceNLS.QUESTION_ConnectedRemoteDevicesOnClose_Title,
                                    RemoteDeviceNLS.QUESTION_ConnectedRemoteDevicesOnClose_Text);

            if (disconnectRemoteInstances)
            {
                for (ISerialNumbered device : connectedDevices)
                {
                    try
                    {
                        RemoteDevicePlugin.getDisconnectServiceHandler().run(
                                (RemoteDeviceInstance) device);
                    }
                    catch (SequoyahException e)
                    {
                        StudioLogger
                                .error("Error when trying to disconnect Remote Devices on Studio shutdown: "
                                        + e.getMessage());
                    }
                }
            }

        }

        return true;
    }
}

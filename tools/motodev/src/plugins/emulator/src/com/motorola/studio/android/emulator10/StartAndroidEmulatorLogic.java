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
package com.motorola.studio.android.emulator10;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.motorola.studio.android.adt.SdkUtils;
import com.motorola.studio.android.common.utilities.PluginUtils;
import com.motorola.studio.android.emulator.EmulatorPlugin;
import com.motorola.studio.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import com.motorola.studio.android.emulator.logic.ConnectVncLogic;
import com.motorola.studio.android.emulator.logic.ForwardVncPortLogic;
import com.motorola.studio.android.emulator.logic.IAndroidLogic;
import com.motorola.studio.android.emulator.logic.IAndroidLogicInstance;
import com.motorola.studio.android.emulator.logic.StartEmulatorProcessLogic;
import com.motorola.studio.android.emulator.logic.StartVncServerLogic;
import com.motorola.studio.android.emulator.logic.TransferFilesLogic;

public class StartAndroidEmulatorLogic extends AbstractStartAndroidEmulatorLogic
{
    @SuppressWarnings("incomplete-switch")
    @Override
    public Collection<IAndroidLogic> getLogicCollection(IAndroidLogicInstance instance,
            LogicMode mode)
    {
        // When starting, all steps must be done. When restarting, only VNC server launch 
        // step will be performed.
        Collection<IAndroidLogic> logicCollection = new LinkedList<IAndroidLogic>();

        switch (mode)
        {
            case START_MODE:
                logicCollection.add(new StartEmulatorProcessLogic());
                break;
            case TRANSFER_AND_CONNECT_VNC:
                logicCollection.add(createTransferFilesLogic());
                logicCollection.add(new ForwardVncPortLogic());
                StartVncServerLogic startVncServerLogic = createStartVncServerLogic();
                logicCollection.add(startVncServerLogic);
                logicCollection.add(getConnectVncClientLogic(startVncServerLogic));
                break;
            case RESTART_VNC_SERVER:
                logicCollection.add(createTransferFilesLogic());
                logicCollection.add(new ForwardVncPortLogic());
                logicCollection.add(createStartVncServerLogic());
                break;
        }

        return logicCollection;
    }

    private String getResourceDir()
    {
        String resDir = "res";
        if (SdkUtils.isOphoneSDK())
        {
            resDir = "res_OPhone";
        }

        return resDir;
    }

    protected IAndroidLogic createTransferFilesLogic()
    {
        File localDirParent = PluginUtils.getPluginInstallationPath(EmulatorPlugin.getDefault());
        File localDir = new File(localDirParent, getResourceDir());

        TransferFilesLogic transferLogic = new TransferFilesLogic();
        transferLogic.setLocalDir(localDir.getAbsolutePath());
        transferLogic.setRemoteDir("/data/local");
        transferLogic.addFilename("fbvncserver");
        return transferLogic;
    }

    protected StartVncServerLogic createStartVncServerLogic()
    {
        StartVncServerLogic logic = new StartVncServerLogic();
        logic.addRemoteCommand("chmod 700 /data/local/fbvncserver");
        logic.addRemoteCommand("/data/local/fbvncserver");
        return logic;
    }

    protected IAndroidLogic getConnectVncClientLogic(StartVncServerLogic startVncServerLogic)
    {
        final ConnectVncLogic startVncClientLogic = new ConnectVncLogic();

        startVncServerLogic.addVncServerJobListener(new JobChangeAdapter()
        {
            @Override
            public void done(IJobChangeEvent ijobchangeevent)
            {
                startVncClientLogic.setVncServerDoneEvent(ijobchangeevent);
            }
        });

        return startVncClientLogic;
    }

}

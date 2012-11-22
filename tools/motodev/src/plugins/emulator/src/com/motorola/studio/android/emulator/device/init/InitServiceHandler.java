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
package com.motorola.studio.android.emulator.device.init;

import static com.motorola.studio.android.common.log.StudioLogger.info;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent.InstanceEventType;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.sequoyah.device.framework.model.IService;
import org.eclipse.sequoyah.device.framework.model.handler.IServiceHandler;

import com.motorola.studio.android.emulator.EmulatorPlugin;
import com.motorola.studio.android.emulator.logic.IAndroidLogicInstance;

/**
 * DESCRIPTION:
 * This class plugs the init procedure to a TmL service. This service implements the
 * interface directly, because it causes the instance to have a particular behavior
 * at the state machine.   
 *
 * RESPONSIBILITY:
 * Provide the initialization procedure to apply to every instance that
 * is loaded at TmL device framework
 *
 * COLABORATORS:
 * None.
 *
 * USAGE:
 * This class is intended to be used by Eclipse only
 */
public class InitServiceHandler implements IServiceHandler
{
    /**
     * The parent service handler
     */
    private IServiceHandler parent;

    /**
     * The service that launches the handler 
     */
    private IService service;

    /**
     * @see IServiceHandler#run(IInstance)
     */
    public void run(IInstance instance)
    {
        if (instance instanceof IAndroidLogicInstance)
        {
            // The service definition defined (by convention) that 
            // stopped-dirty is the success state, and not available 
            // is the failure state. The exception is being thrown for
            // the framework to set the state correctly. 
            instance.setStatus(EmulatorPlugin.STATUS_NOT_AVAILABLE);
            InstanceEventManager.getInstance().notifyListeners(
                    new InstanceEvent(InstanceEventType.INSTANCE_TRANSITIONED, instance));
        }

    }

    /**
     * @see IServiceHandler#newInstance()
     */
    public IServiceHandler newInstance()
    {
        return new InitServiceHandler();
    }

    /**
     * @see IServiceHandler#setParent(IServiceHandler)
     */
    public void setParent(IServiceHandler handler)
    {
        this.parent = handler;
    }

    /**
     * @see IServiceHandler#setService(IService)
     */
    public void setService(IService service)
    {
        this.service = service;
    }

    /**
     * @see IServiceHandler#updatingService(IInstance)
     */
    public void updatingService(IInstance instance)
    {
        info("Updating init emulator service");
    }

    /**
     * @see IServiceHandler#clone()
     * @see Cloneable#clone()
     */
    @Override
    public Object clone()
    {
        IServiceHandler newHandler = newInstance();
        newHandler.setParent(parent);
        newHandler.setService(service);
        return newHandler;
    }

    /**
     * @see IServiceHandler#getParent()
     */
    public IServiceHandler getParent()
    {
        return parent;
    }

    /**
     * @see IServiceHandler#getService()
     */
    public IService getService()
    {
        return service;
    }

    public IStatus singleInit(List<IInstance> instances)
    {
        return Status.OK_STATUS;
    }
}

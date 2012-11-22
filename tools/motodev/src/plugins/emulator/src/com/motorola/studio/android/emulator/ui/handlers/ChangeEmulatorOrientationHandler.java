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
package com.motorola.studio.android.emulator.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IViewPart;

import com.motorola.studio.android.common.utilities.EclipseUtils;
import com.motorola.studio.android.emulator.core.model.IAndroidEmulatorInstance;
import com.motorola.studio.android.emulator.core.skin.IAndroidSkin;
import com.motorola.studio.android.emulator.ui.view.AbstractAndroidView;
import com.motorola.studio.android.emulator.ui.view.AndroidView;
import com.motorola.studio.android.nativeos.IDevicePropertiesOSConstants;
import com.motorola.studio.android.nativeos.NativeUIUtils;

/**
 * This class executes the operation of setting an Emulator Layout with the Landscape orientation
 **/
public class ChangeEmulatorOrientationHandler extends AbstractHandler
{

    /**
     * Sets a layout with the Landscape orientation for the active Emulator.
     */
    public Object execute(ExecutionEvent event)
    {
        IAndroidEmulatorInstance instance = AbstractAndroidView.getActiveInstance();

        if (instance != null)
        {
            String viewId = event.getParameter(IHandlerConstants.ACTIVE_VIEW_PARAMETER);
            if ((instance != null) && (viewId != null))
            {
                IViewPart viewPart = EclipseUtils.getActiveView(viewId);
                if (viewPart instanceof AndroidView)
                {
                    if ((instance.getProperties().getProperty(IDevicePropertiesOSConstants.useVnc,
                            NativeUIUtils.getDefaultUseVnc())).equals("true"))
                    {
                        String nextLayout =
                                AbstractAndroidView.getPreviousOrNextLayout(viewId,
                                        AbstractAndroidView.LayoutOpp.NEXT);
                        AbstractAndroidView.changeLayout(nextLayout);

                        IAndroidSkin skin = ((AbstractAndroidView) viewPart).getSkin(instance);
                        String cmd = skin.getLayoutScreenCommand(instance.getCurrentLayout());
                        instance.changeOrientation(cmd);
                    }
                    else
                    {
                        AndroidView androidView = (AndroidView) viewPart;
                        androidView.changeToNextLayout();
                    }
                }
            }

        }

        return null;
    }

    /**
     * Determines when the Landscape command can be executed.
     */
    @Override
    public boolean isEnabled()
    {
        return AbstractAndroidView.getActiveInstance() != null;
    }

    //    /**
    //     * Determines what layout is the current one and checks it
    //     */
    //    @SuppressWarnings("unchecked")
    //    public void updateElement(UIElement element, Map parameters)
    //    {
    //        boolean checked = false;
    //
    //        IAndroidEmulatorInstance instance = AbstractAndroidView.getActiveInstance();
    //        String viewId = (String) parameters.get(IHandlerConstants.ACTIVE_VIEW_PARAMETER);
    //
    //        if ((instance != null) && (viewId != null))
    //        {
    //            IViewPart viewPart = EclipseUtils.getActiveView(viewId);
    //            if (viewPart instanceof AbstractAndroidView)
    //            {
    //                AbstractAndroidView view = (AbstractAndroidView) viewPart;
    //                IAndroidSkin skin = view.getSkin(instance);
    //
    //                if (skin != null)
    //                {
    //                    String currentLayout = instance.getCurrentLayout();
    //                    String setLayoutName =
    //                            (String) parameters.get(IHandlerConstants.LAYOUT_TO_SET_PARAMETER);
    //                    if ((setLayoutName != null) && (setLayoutName.equals(currentLayout)))
    //                    {
    //                        checked = true;
    //                    }
    //                }
    //            }
    //        }
    //
    //        element.setChecked(checked);
    //    }
}

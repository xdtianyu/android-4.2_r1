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
package com.motorolamobility.preflighting.ui.tabs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractAppValidatorTabComposite extends Composite
{

    private final Set<UIChangedListener> listeners;

    /**
     * @param parent
     * @param style
     */
    public AbstractAppValidatorTabComposite(Composite parent, int style)
    {
        super(parent, style);
        listeners = new HashSet<UIChangedListener>();

    }

    public void removeUIChangedListener(UIChangedListener listener)
    {
        listeners.remove(listener);
    }

    public void notifyListener()
    {
        for (UIChangedListener listener : listeners)
        {
            listener.uiChanged(this);
        }
    }

    abstract public IStatus isValid();

    abstract public void performDefaults();

    abstract public void performOk(IPreferenceStore preferenceStore);

    abstract public String commandLineBuilder();

    public void addUIChangedListener(UIChangedListener listener)
    {
        listeners.add(listener);

    }

}

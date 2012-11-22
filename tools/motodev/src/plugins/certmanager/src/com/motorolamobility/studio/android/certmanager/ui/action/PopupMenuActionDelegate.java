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

package com.motorolamobility.studio.android.certmanager.ui.action;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.motorola.studio.android.common.log.StudioLogger;
import com.motorola.studio.android.common.utilities.EclipseUtils;
import com.motorolamobility.studio.android.certmanager.command.BackupHandler;
import com.motorolamobility.studio.android.certmanager.command.CertificatePropertiesHandler;
import com.motorolamobility.studio.android.certmanager.command.ChangeKeyStoreTypeHandler;
import com.motorolamobility.studio.android.certmanager.command.ChangePasswordKeyHandler;
import com.motorolamobility.studio.android.certmanager.command.ChangePasswordKeystoreHandler;
import com.motorolamobility.studio.android.certmanager.command.CreateKeyHandler;
import com.motorolamobility.studio.android.certmanager.command.DeleteKeyHandler;
import com.motorolamobility.studio.android.certmanager.command.DeleteKeystoreHandler;
import com.motorolamobility.studio.android.certmanager.command.ImportKeyStoreEntriesHandler;
import com.motorolamobility.studio.android.certmanager.command.RefreshHandler;
import com.motorolamobility.studio.android.certmanager.command.SignExternalPackagesHandler;

public class PopupMenuActionDelegate implements IObjectActionDelegate
{

    /**
     * Enum type for ActionHandlers. If you need to add a new ActionHandler, just include 
     * a new type to this enum with the action id that you defined on your action extension point 
     */
    enum ActionHandlers
    {
        SIGN_PACKAGE("com.motorolamobility.studio.android.certmanager.core.ui.action.addSignature") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new SignExternalPackagesHandler();
            }
        },
        BACKUP_KEYSTORE(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.backupKeystore") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                //this handler will delegate the operation to another action that, in turn,
                //will retrieve the current selection
                //therefore, the parameter treeNode is not used here.
                return new BackupHandler();
            }

        },
        DELETE_KEYSTORE(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.deleteKeystore") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new DeleteKeystoreHandler();
            }

        },
        PROPERTIES_KEYSTORE(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.certificateProperties") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new CertificatePropertiesHandler();
            }

        },
        CHANGE_KEYSTORE_PASSWORD(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.changeKeystorePassword") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new ChangePasswordKeystoreHandler();
            }

        },
        CHANGE_KEY_PASSWORD(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.changeKeyPassword") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new ChangePasswordKeyHandler();
            }

        },
        CREATE_KEY("com.motorolamobility.studio.android.certmanager.core.ui.action.createKey") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new CreateKeyHandler();
            }

        },
        DELETE_KEY("com.motorolamobility.studio.android.certmanager.core.ui.action.deleteEntry") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new DeleteKeyHandler();
            }

        },
        REFRESH("com.motorolamobility.studio.android.certmanager.core.ui.action.refresh") //$NON-NLS-1$
        {
            @Override
            public IHandler getHandler()
            {
                return new RefreshHandler();
            }

        },
        CHANGE_KEYSTORE_TYPE(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.changeKeystoreType")
        {
            @Override
            public IHandler getHandler()
            {
                return new ChangeKeyStoreTypeHandler();
            }
        },
        IMPORT_KEYSTORE_ENTRIES(
                "com.motorolamobility.studio.android.certmanager.core.ui.action.importKeystoreEntries")
        {
            @Override
            public IHandler getHandler()
            {
                return new ImportKeyStoreEntriesHandler();
            }
        };

        private final String actionId;

        private ActionHandlers(String actionId)
        {
            this.actionId = actionId;
        }

        public abstract IHandler getHandler();

        public static ActionHandlers getActionHandlerbyId(String id)
        {

            Object ret = null;
            for (ActionHandlers h : ActionHandlers.values())
            {
                if (h.actionId.equals(id))
                {
                    ret = h;
                    break;
                }
            }

            return (ActionHandlers) ret;
        }
    }

    @Override
    public void run(IAction action)
    {

        ActionHandlers type = ActionHandlers.getActionHandlerbyId(action.getId());

        IHandler handler = null;

        if (type != null)
        {
            handler = type.getHandler();
        }

        if (handler != null)
        {
            ExecutionEvent event = new ExecutionEvent();
            try
            {
                handler.execute(event);
            }
            catch (ExecutionException e)
            {
                StudioLogger.error(PopupMenuActionDelegate.class, e.getMessage(), e);
                EclipseUtils.showErrorDialog("Execution error", e.getMessage());
            }
        }

    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        //selection is retrieved by the handlers
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart)
    {
        //do nothing
    }

}

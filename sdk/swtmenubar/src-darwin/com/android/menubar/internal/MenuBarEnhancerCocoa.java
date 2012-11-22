/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * History:
 * Original code by the <a href="http://www.simidude.com/blog/2008/macify-a-swt-application-in-a-cross-platform-way/">CarbonUIEnhancer from Agynami</a>
 * with the implementation being modified from the <a href="http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.ui.cocoa/src/org/eclipse/ui/internal/cocoa/CocoaUIEnhancer.java">org.eclipse.ui.internal.cocoa.CocoaUIEnhancer</a>,
 * then modified by http://www.transparentech.com/opensource/cocoauienhancer to use reflection
 * rather than 'link' to SWT cocoa, and finally modified to be usable by the SwtMenuBar project.
 */

package com.android.menubar.internal;

import com.android.menubar.IMenuBarCallback;
import com.android.menubar.IMenuBarEnhancer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MenuBarEnhancerCocoa implements IMenuBarEnhancer {

    private static final long kAboutMenuItem = 0;
    private static final long kPreferencesMenuItem = 2;
    // private static final long kServicesMenuItem = 4;
    // private static final long kHideApplicationMenuItem = 6;
    private static final long kQuitMenuItem = 10;

    static long mSelPreferencesMenuItemSelected;
    static long mSelAboutMenuItemSelected;
    static Callback mProc3Args;

    private String mAppName;

    /**
     * Class invoked via the Callback object to run the about and preferences
     * actions.
     * <p>
     * If you don't use JFace in your application (SWT only), change the
     * {@link org.eclipse.jface.action.IAction}s to
     * {@link org.eclipse.swt.widgets.Listener}s.
     * </p>
     */
    private static class ActionProctarget {
        private final IMenuBarCallback mCallbacks;

        public ActionProctarget(IMenuBarCallback callbacks) {
            mCallbacks = callbacks;
        }

        /**
         * Will be called on 32bit SWT.
         */
        @SuppressWarnings("unused")
        public int actionProc(int id, int sel, int arg0) {
            return (int) actionProc((long) id, (long) sel, (long) arg0);
        }

        /**
         * Will be called on 64bit SWT.
         */
        public long actionProc(long id, long sel, long arg0) {
            if (sel == mSelAboutMenuItemSelected) {
                mCallbacks.onAboutMenuSelected();
            } else if (sel == mSelPreferencesMenuItemSelected) {
                mCallbacks.onPreferencesMenuSelected();
            } else {
                // Unknown selection!
            }
            // Return value is not used.
            return 0;
        }
    }

    /**
     * Construct a new CocoaUIEnhancer.
     *
     * @param mAppName The name of the application. It will be used to customize
     *            the About and Quit menu items. If you do not wish to customize
     *            the About and Quit menu items, just pass <tt>null</tt> here.
     */
    public MenuBarEnhancerCocoa() {
    }

    public MenuBarMode getMenuBarMode() {
        return MenuBarMode.MAC_OS;
    }

    /**
     * Setup the About and Preferences native menut items with the
     * given application name and links them to the callback.
     *
     * @param appName The application name.
     * @param display The SWT display. Must not be null.
     * @param callbacks The callbacks invoked by the menus.
     */
    public void setupMenu(
            String appName,
            Display display,
            IMenuBarCallback callbacks) {

        mAppName = appName;

        // This is our callback object whose 'actionProc' method will be called
        // when the About or Preferences menuItem is invoked.
        ActionProctarget target = new ActionProctarget(callbacks);

        try {
            // Initialize the menuItems.
            initialize(target);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        // Schedule disposal of callback object
        display.disposeExec(new Runnable() {
            public void run() {
                invoke(mProc3Args, "dispose");
            }
        });
    }

    private void initialize(Object callbackObject)
            throws Exception {

        Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");

        // Register names in objective-c.
        if (mSelAboutMenuItemSelected == 0) {
            mSelPreferencesMenuItemSelected = registerName(osCls, "preferencesMenuItemSelected:"); //$NON-NLS-1$
            mSelAboutMenuItemSelected = registerName(osCls, "aboutMenuItemSelected:");             //$NON-NLS-1$
        }

        // Create an SWT Callback object that will invoke the actionProc method
        // of our internal callback Object.
        mProc3Args = new Callback(callbackObject, "actionProc", 3); //$NON-NLS-1$
        Method getAddress = Callback.class.getMethod("getAddress", new Class[0]);
        Object object = getAddress.invoke(mProc3Args, (Object[]) null);
        long proc3 = convertToLong(object);
        if (proc3 == 0) {
            SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
        }

        Class<?> nsMenuCls        = classForName("org.eclipse.swt.internal.cocoa.NSMenu");
        Class<?> nsMenuitemCls    = classForName("org.eclipse.swt.internal.cocoa.NSMenuItem");
        Class<?> nsStringCls      = classForName("org.eclipse.swt.internal.cocoa.NSString");
        Class<?> nsApplicationCls = classForName("org.eclipse.swt.internal.cocoa.NSApplication");

        // Instead of creating a new delegate class in objective-c,
        // just use the current SWTApplicationDelegate. An instance of this
        // is a field of the Cocoa Display object and is already the target
        // for the menuItems. So just get this class and add the new methods
        // to it.
        object = invoke(osCls, "objc_lookUpClass", new Object[] {
            "SWTApplicationDelegate"
        });
        long cls = convertToLong(object);

        // Add the action callbacks for Preferences and About menu items.
        invoke(osCls, "class_addMethod",
                new Object[] {
                    wrapPointer(cls),
                    wrapPointer(mSelPreferencesMenuItemSelected),
                    wrapPointer(proc3), "@:@"}); //$NON-NLS-1$
        invoke(osCls,  "class_addMethod",
                new Object[] {
                    wrapPointer(cls),
                    wrapPointer(mSelAboutMenuItemSelected),
                    wrapPointer(proc3), "@:@"}); //$NON-NLS-1$

        // Get the Mac OS X Application menu.
        Object sharedApplication = invoke(nsApplicationCls, "sharedApplication");
        Object mainMenu = invoke(sharedApplication, "mainMenu");
        Object mainMenuItem = invoke(nsMenuCls, mainMenu, "itemAtIndex", new Object[] {
            wrapPointer(0)
        });
        Object appMenu = invoke(mainMenuItem, "submenu");

        // Create the About <application-name> menu command
        Object aboutMenuItem =
                invoke(nsMenuCls, appMenu, "itemAtIndex", new Object[] {
                    wrapPointer(kAboutMenuItem)
                });
        if (mAppName != null) {
            Object nsStr = invoke(nsStringCls, "stringWith", new Object[] {
                "About " + mAppName
            });
            invoke(nsMenuitemCls, aboutMenuItem, "setTitle", new Object[] {
                nsStr
            });
        }
        // Rename the quit action.
        if (mAppName != null) {
            Object quitMenuItem =
                    invoke(nsMenuCls, appMenu, "itemAtIndex", new Object[] {
                        wrapPointer(kQuitMenuItem)
                    });
            Object nsStr = invoke(nsStringCls, "stringWith", new Object[] {
                "Quit " + mAppName
            });
            invoke(nsMenuitemCls, quitMenuItem, "setTitle", new Object[] {
                nsStr
            });
        }

        // Enable the Preferences menuItem.
        Object prefMenuItem =
                invoke(nsMenuCls, appMenu, "itemAtIndex", new Object[] {
                    wrapPointer(kPreferencesMenuItem)
                });
        invoke(nsMenuitemCls, prefMenuItem, "setEnabled", new Object[] {
            true
        });

        // Set the action to execute when the About or Preferences menuItem is
        // invoked.
        //
        // We don't need to set the target here as the current target is the
        // SWTApplicationDelegate and we have registerd the new selectors on
        // it. So just set the new action to invoke the selector.
        invoke(nsMenuitemCls, prefMenuItem, "setAction",
                new Object[] {
                    wrapPointer(mSelPreferencesMenuItemSelected)
                });
        invoke(nsMenuitemCls, aboutMenuItem, "setAction",
                new Object[] {
                    wrapPointer(mSelAboutMenuItemSelected)
                });
    }

    private long registerName(Class<?> osCls, String name)
            throws IllegalArgumentException, SecurityException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        Object object = invoke(osCls, "sel_registerName", new Object[] {
            name
        });
        return convertToLong(object);
    }

    private long convertToLong(Object object) {
        if (object instanceof Integer) {
            Integer i = (Integer) object;
            return i.longValue();
        }
        if (object instanceof Long) {
            Long l = (Long) object;
            return l.longValue();
        }
        return 0;
    }

    private static Object wrapPointer(long value) {
        Class<?> PTR_CLASS = C.PTR_SIZEOF == 8 ? long.class : int.class;
        if (PTR_CLASS == long.class) {
            return new Long(value);
        } else {
            return new Integer((int) value);
        }
    }

    private static Object invoke(Class<?> clazz, String methodName, Object[] args) {
        return invoke(clazz, null, methodName, args);
    }

    private static Object invoke(Class<?> clazz, Object target, String methodName, Object[] args) {
        try {
            Class<?>[] signature = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                Class<?> thisClass = args[i].getClass();
                if (thisClass == Integer.class)
                    signature[i] = int.class;
                else if (thisClass == Long.class)
                    signature[i] = long.class;
                else if (thisClass == Byte.class)
                    signature[i] = byte.class;
                else if (thisClass == Boolean.class)
                    signature[i] = boolean.class;
                else
                    signature[i] = thisClass;
            }
            Method method = clazz.getMethod(methodName, signature);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Class<?> classForName(String classname) {
        try {
            Class<?> cls = Class.forName(classname);
            return cls;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object invoke(Class<?> cls, String methodName) {
        return invoke(cls, methodName, (Class<?>[]) null, (Object[]) null);
    }

    private Object invoke(Class<?> cls, String methodName, Class<?>[] paramTypes,
            Object... arguments) {
        try {
            Method m = cls.getDeclaredMethod(methodName, paramTypes);
            return m.invoke(null, arguments);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Object invoke(Object obj, String methodName) {
        return invoke(obj, methodName, (Class<?>[]) null, (Object[]) null);
    }

    private Object invoke(Object obj, String methodName, Class<?>[] paramTypes, Object... arguments) {
        try {
            Method m = obj.getClass().getDeclaredMethod(methodName, paramTypes);
            return m.invoke(obj, arguments);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

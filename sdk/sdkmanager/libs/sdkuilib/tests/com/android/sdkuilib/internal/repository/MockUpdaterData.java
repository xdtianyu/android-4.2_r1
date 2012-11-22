/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.repository.DownloadCache;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskFactory;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.MockEmptySdkManager;
import com.android.sdklib.internal.repository.NullTaskMonitor;
import com.android.sdklib.internal.repository.archives.ArchiveInstaller;
import com.android.sdklib.internal.repository.archives.ArchiveReplacement;
import com.android.sdklib.internal.repository.sources.SdkSourceCategory;
import com.android.sdklib.internal.repository.sources.SdkSources;
import com.android.sdklib.mock.MockLog;
import com.android.sdkuilib.internal.repository.SettingsController.Settings;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.utils.ILogger;
import com.android.utils.NullLogger;

import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** A mock UpdaterData that simply records what would have been installed. */
public class MockUpdaterData extends UpdaterData {

    public final static String SDK_PATH = "/tmp/SDK";

    private final List<ArchiveReplacement> mInstalled = new ArrayList<ArchiveReplacement>();

    private DownloadCache mMockDownloadCache = new MockDownloadCache();

    private final SdkSources mMockSdkSources = new SdkSources() {
        @Override
        public void loadUserAddons(ILogger log) {
            // This source does not load user addons.
            removeAll(SdkSourceCategory.USER_ADDONS);
        };
    };

    /** Creates a {@link MockUpdaterData} using a {@link MockEmptySdkManager}. */
    public MockUpdaterData() {
        super(SDK_PATH, new MockLog());

        setTaskFactory(new MockTaskFactory());
        setImageFactory(new NullImageFactory());
    }

    /** Creates a {@link MockUpdaterData} using the given {@link SdkManager}. */
    public MockUpdaterData(SdkManager sdkManager) {
        super(sdkManager.getLocation(), new MockLog());
        setSdkManager(sdkManager);
        setTaskFactory(new MockTaskFactory());
        setImageFactory(new NullImageFactory());
    }

    /** Gives access to the internal {@link #installArchives(List, int)}. */
    public void _installArchives(List<ArchiveInfo> result) {
        installArchives(result, 0/*flags*/);
    }

    public ArchiveReplacement[] getInstalled() {
        return mInstalled.toArray(new ArchiveReplacement[mInstalled.size()]);
    }

    /** Overrides the sdk manager with our mock instance. */
    @Override
    protected void initSdk() {
        setSdkManager(new MockEmptySdkManager(SDK_PATH));
    }

    /** Overrides the settings controller with our mock instance. */
    @Override
    protected SettingsController initSettingsController() {
        return createSettingsController(getSdkLog());
    }

    /** Override original implementation to do nothing. */
    @Override
    public void reloadSdk() {
        // nop
    }

    /**
     * Override original implementation to return a mock SdkSources that
     * does not load user add-ons from the local .android/repository.cfg file.
     */
    @Override
    public SdkSources getSources() {
        return mMockSdkSources;
    }

    /** Returns a mock installer that simply records what would have been installed. */
    @Override
    protected ArchiveInstaller createArchiveInstaler() {
        return new ArchiveInstaller() {
            @Override
            public boolean install(
                    ArchiveReplacement archiveInfo,
                    String osSdkRoot,
                    boolean forceHttp,
                    SdkManager sdkManager,
                    DownloadCache cache,
                    ITaskMonitor monitor) {
                mInstalled.add(archiveInfo);
                return true;
            }
        };
    }

    /** Returns a mock download cache. */
    @Override
    public DownloadCache getDownloadCache() {
        return mMockDownloadCache;
    }

    /** Overrides the mock download cache. */
    public void setMockDownloadCache(DownloadCache mockDownloadCache) {
        mMockDownloadCache = mockDownloadCache;
    }

    public void overrideSetting(String key, boolean boolValue) {
        SettingsController sc = getSettingsController();
        assert sc instanceof MockSettingsController;
        ((MockSettingsController)sc).overrideSetting(key, boolValue);
    }

    //------------

    public static SettingsController createSettingsController(ILogger sdkLog) {
        Properties props = new Properties();
        Settings settings = new Settings(props) {}; // this constructor is protected
        MockSettingsController controller = new MockSettingsController(sdkLog, settings);
        controller.setProperties(props);
        return controller;
    }

    static class MockSettingsController extends SettingsController {

        private Properties mProperties;

        MockSettingsController(ILogger sdkLog, Settings settings) {
            super(sdkLog, settings);
        }

        void setProperties(Properties properties) {
            mProperties = properties;
        }

        public void overrideSetting(String key, boolean boolValue) {
            mProperties.setProperty(key, Boolean.valueOf(boolValue).toString());
        }

        @Override
        public void loadSettings() {
            // This mock setting controller does not load live file settings.
        }

        @Override
        public void saveSettings() {
            // This mock setting controller does not save live file settings.
        }
    }

    //------------

    private class MockTaskFactory implements ITaskFactory {
        @Override
        public void start(String title, ITask task) {
            start(title, null /*parentMonitor*/, task);
        }

        @SuppressWarnings("unused") // works by side-effect of creating a new MockTask.
        @Override
        public void start(String title, ITaskMonitor parentMonitor, ITask task) {
            new MockTask(task);
        }
    }

    //------------

    private static class MockTask extends NullTaskMonitor {
        public MockTask(ITask task) {
            super(NullLogger.getLogger());
            task.run(this);
        }
    }

    //------------

    private static class NullImageFactory extends ImageFactory {
        public NullImageFactory() {
            // pass
            super(null /*display*/);
        }

        @Override
        public Image getImageByName(String imageName) {
            return null;
        }

        @Override
        public Image getImageForObject(Object object) {
            return null;
        }

        @Override
        public void dispose() {
            // pass
        }

    }
}

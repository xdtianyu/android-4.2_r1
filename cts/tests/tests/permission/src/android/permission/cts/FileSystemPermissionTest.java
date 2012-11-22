/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.permission.cts;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.LargeTest;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verify certain permissions on the filesystem
 *
 * TODO: Combine this file with {@link android.os.cts.FileAccessPermissionTest}
 */
public class FileSystemPermissionTest extends AndroidTestCase {

    @MediumTest
    public void testCreateFileHasSanePermissions() throws Exception {
        File myFile = new File(getContext().getFilesDir(), "hello");
        FileOutputStream stream = new FileOutputStream(myFile);
        stream.write("hello world".getBytes());
        stream.close();
        try {
            FileUtils.FileStatus status = new FileUtils.FileStatus();
            FileUtils.getFileStatus(myFile.getAbsolutePath(), status, false);
            int expectedPerms = FileUtils.S_IFREG
                    | FileUtils.S_IWUSR
                    | FileUtils.S_IRUSR;
            assertEquals(
                    "Newly created files should have 0600 permissions",
                    Integer.toOctalString(expectedPerms),
                    Integer.toOctalString(status.mode));
        } finally {
            assertTrue(myFile.delete());
        }
    }

    @MediumTest
    public void testCreateDirectoryHasSanePermissions() throws Exception {
        File myDir = new File(getContext().getFilesDir(), "helloDirectory");
        assertTrue(myDir.mkdir());
        try {
            FileUtils.FileStatus status = new FileUtils.FileStatus();
            FileUtils.getFileStatus(myDir.getAbsolutePath(), status, false);
            int expectedPerms = FileUtils.S_IFDIR
                    | FileUtils.S_IWUSR
                    | FileUtils.S_IRUSR
                    | FileUtils.S_IXUSR;
            assertEquals(
                    "Newly created directories should have 0700 permissions",
                    Integer.toOctalString(expectedPerms),
                    Integer.toOctalString(status.mode));

        } finally {
            assertTrue(myDir.delete());
        }
    }

    @MediumTest
    public void testOtherApplicationDirectoriesAreNotWritable() throws Exception {
        Set<File> writableDirs = new HashSet<File>();
        List<ApplicationInfo> apps = getContext()
                .getPackageManager()
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        String myAppDirectory = getContext().getApplicationInfo().dataDir;
        for (ApplicationInfo app : apps) {
            if (!myAppDirectory.equals(app.dataDir)) {
                writableDirs.addAll(getWritableDirectoryiesAndSubdirectoriesOf(new File(app.dataDir)));
            }
        }

        assertTrue("Found writable directories: " + writableDirs.toString(),
                writableDirs.isEmpty());
    }

    @MediumTest
    public void testApplicationParentDirectoryNotWritable() throws Exception {
        String myDataDir = getContext().getApplicationInfo().dataDir;
        File parentDir = new File(myDataDir).getParentFile();
        assertFalse(parentDir.toString(), isDirectoryWritable(parentDir));
    }

    @MediumTest
    public void testDataDirectoryNotWritable() throws Exception {
        assertFalse(isDirectoryWritable(Environment.getDataDirectory()));
    }

    @MediumTest
    public void testAndroidRootDirectoryNotWritable() throws Exception {
        assertFalse(isDirectoryWritable(Environment.getRootDirectory()));
    }

    @MediumTest
    public void testDownloadCacheDirectoryNotWritable() throws Exception {
        assertFalse(isDirectoryWritable(Environment.getDownloadCacheDirectory()));
    }

    @MediumTest
    public void testRootDirectoryNotWritable() throws Exception {
        assertFalse(isDirectoryWritable(new File("/")));
    }

    @MediumTest
    public void testDevDirectoryNotWritable() throws Exception {
        assertFalse(isDirectoryWritable(new File("/dev")));
    }

    @MediumTest
    public void testProcDirectoryNotWritable() throws Exception {
        assertFalse(isDirectoryWritable(new File("/proc")));
    }

    @MediumTest
    public void testDevMemSane() throws Exception {
        File f = new File("/dev/mem");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    @MediumTest
    public void testDevkmemSane() throws Exception {
        File f = new File("/dev/kmem");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    @MediumTest
    public void testDevPortSane() throws Exception {
        File f = new File("/dev/port");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    @MediumTest
    public void testPn544Sane() throws Exception {
        File f = new File("/dev/pn544");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());

        assertFileOwnedBy(f, "nfc");
        assertFileOwnedByGroup(f, "nfc");
    }

    @MediumTest
    public void testBcm2079xSane() throws Exception {
        File f = new File("/dev/bcm2079x");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());

        assertFileOwnedBy(f, "nfc");
        assertFileOwnedByGroup(f, "nfc");
    }

    @MediumTest
    public void testBcm2079xi2cSane() throws Exception {
        File f = new File("/dev/bcm2079x-i2c");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());

        assertFileOwnedBy(f, "nfc");
        assertFileOwnedByGroup(f, "nfc");
    }

    /**
     * Assert that a file is owned by a specific owner. This is a noop if the
     * file does not exist.
     *
     * @param file The file to check.
     * @param expectedOwner The owner of the file.
     */
    private static void assertFileOwnedBy(File file, String expectedOwner) {
        FileUtils.FileStatus status = new FileUtils.FileStatus();
        String path = file.getAbsolutePath();
        if (file.exists() && FileUtils.getFileStatus(path, status, true)) {
            String actualOwner = FileUtils.getUserName(status.uid);
            if (!expectedOwner.equals(actualOwner)) {
                String msg = String.format("Wrong owner. Expected '%s', but found '%s' for %s.",
                        expectedOwner, actualOwner, path);
                fail(msg);
            }
        }
    }

    /**
     * Assert that a file is owned by a specific group. This is a noop if the
     * file does not exist.
     *
     * @param file The file to check.
     * @param expectedGroup The owner group of the file.
     */
    private static void assertFileOwnedByGroup(File file, String expectedGroup) {
        FileUtils.FileStatus status = new FileUtils.FileStatus();
        String path = file.getAbsolutePath();
        if (file.exists() && FileUtils.getFileStatus(path, status, true)) {
            String actualGroup = FileUtils.getGroupName(status.gid);
            if (!expectedGroup.equals(actualGroup)) {
                String msg = String.format("Wrong group. Expected '%s', but found '%s' for %s.",
                        expectedGroup, actualGroup, path);
                fail(msg);
            }
        }
    }

    @MediumTest
    public void testTtyO3Sane() throws Exception {
        File f = new File("/dev/ttyO3");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    @MediumTest
    public void testDataMediaSane() throws Exception {
        final File f = new File("/data/media");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    @MediumTest
    public void testMntShellSane() throws Exception {
        final File f = new File("/mnt/shell");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    @MediumTest
    public void testMntSecureSane() throws Exception {
        final File f = new File("/mnt/secure");
        assertFalse(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    private static boolean isDirectoryWritable(File directory) {
        File toCreate = new File(directory, "hello");
        try {
            toCreate.createNewFile();
            return true;
        } catch (IOException e) {
            // It's expected we'll get a "Permission denied" exception.
        } finally {
            toCreate.delete();
        }
        return false;
    }

    /**
     * Verify that any publicly readable directories reachable from
     * the root directory are not writable.  An application should only be
     * able to write to it's own home directory. World writable directories
     * are a security hole because they enable a number of different attacks.
     * <ul>
     *   <li><a href="http://en.wikipedia.org/wiki/Symlink_race">Symlink Races</a></li>
     *   <li>Data destruction by deleting or renaming files you don't own</li>
     *   <li>Data substitution by replacing trusted files with untrusted files</li>
     * </ul>
     *
     * Note: Because not all directories are readable, this is a best-effort
     * test only.  Writable directories within unreadable subdirectories
     * will NOT be detected by this code.
     */
    @LargeTest
    public void testAllOtherDirectoriesNotWritable() throws Exception {
        File start = new File("/");
        Set<File> writableDirs = getWritableDirectoryiesAndSubdirectoriesOf(start);

        assertTrue("Found writable directories: " + writableDirs.toString(),
                writableDirs.isEmpty());
    }

    private static final Set<String> OTHER_RANDOM_DIRECTORIES = new HashSet<String>(
            Arrays.asList(
                    "/app-cache",
                    "/app-cache/ciq/socket",
                    "/cache/fotapkg",
                    "/cache/fotapkg/tmp",
                    "/data/_SamsungBnR_",
                    "/data/_SamsungBnR_/BR",
                    "/data/2nd-init",
                    "/data/amit",
                    "/data/anr",
                    "/data/app",
                    "/data/app-private",
                    "/data/backup",
                    "/data/battd",
                    "/data/bootlogo",
                    "/data/btips",
                    "/data/btips/TI",
                    "/data/btips/TI/opp",
                    "/data/cache",
                    "/data/calibration",
                    "/data/clipboard",
                    "/data/clp",
                    "/data/dalvik-cache",
                    "/data/data",
                    "/data/data/.drm",
                    "/data/data/.drm/.wmdrm",
                    "/data/data/cw",
                    "/data/data/com.android.htcprofile",
                    "/data/data/com.android.providers.drm/rights",
                    "/data/data/com.htc.android.qxdm2sd",
                    "/data/data/com.htc.android.qxdm2sd/bin",
                    "/data/data/com.htc.android.qxdm2sd/data",
                    "/data/data/com.htc.android.qxdm2sd/tmp",
                    "/data/data/com.htc.android.netlogger/data",
                    "/data/data/com.htc.messagecs/att",
                    "/data/data/com.htc.messagecs/pdu",
                    "/data/data/com.htc.loggers/bin",
                    "/data/data/com.htc.loggers/data",
                    "/data/data/com.htc.loggers/htclog",
                    "/data/data/com.htc.loggers/tmp",
                    "/data/data/com.htc.loggers/htcghost",
                    "/data/data/com.lge.ers/android",
                    "/data/data/com.lge.ers/arm9",
                    "/data/data/com.lge.ers/kernel",
                    "/data/data/com.lge.wmc",
                    "/data/data/com.redbend.vdmc/lib",
                    "/data/data/recovery",
                    "/data/data/recovery/HTCFOTA",
                    "/data/data/recovery/OMADM",
                    "/data/data/shared",
                    "/data/dontpanic",
                    "/data/drm",
                    "/data/drm/fwdlock",
                    "/data/drm/IDM",
                    "/data/drm/IDM/HTTP",
                    "/data/drm/rights",
                    "/data/dump",
                    "/data/emt",
                    "/data/factory",
                    "/data/fics",
                    "/data/fics/dev",
                    "/data/fota",
                    "/data/gps",
                    "/data/gps/log",
                    "/data/gps/var",
                    "/data/gps/var/run",
                    "/data/gpscfg",
                    "/data/hwvefs",
                    "/data/htcfs",
                    "/data/img",
                    "/data/install",
                    "/data/internal-device",
                    "/data/internal-device/DCIM",
                    "/data/local",
                    "/data/local/logs",
                    "/data/local/logs/kernel",
                    "/data/local/logs/logcat",
                    "/data/local/logs/resetlog",
                    "/data/local/logs/smem",
                    "/data/local/mono",
                    "/data/local/mono/pulse",
                    "/data/local/purple",
                    "/data/local/purple/sound",
                    "/data/local/rights",
                    "/data/local/rwsystag",
                    "/data/local/skel",
                    "/data/local/skel/default",
                    "/data/local/skel/defualt", // Mispelled "defualt" is intentional
                    "/data/local/tmp",
                    "/data/local/tmp/com.nuance.android.vsuite.vsuiteapp",
                    "/data/log",
                    "/data/logger",
                    "/data/lost+found",
                    "/data/misc",
                    "/data/misc/bluetooth",
                    "/data/misc/dhcp",
                    "/data/misc/lockscreen",
                    "/data/misc/webwidgets",
                    "/data/misc/webwidgets/chess",
                    "/data/misc/widgets",
                    "/data/misc/wifi",
                    "/data/misc/wifi/sockets",
                    "/data/misc/wimax",
                    "/data/misc/wimax/sockets",
                    "/data/misc/wminput",
                    "/data/misc/wpa_supplicant",
                    "/data/nv",
                    "/data/nvcam",
                    "/data/panic",
                    "/data/panicreports",
                    "/data/preinstall_md5",
                    "/data/property",
                    "/data/radio",
                    "/data/secure",
                    "/data/sensors",
                    "/data/shared",
                    "/data/simcom",
                    "/data/simcom/btadd",
                    "/data/simcom/simlog",
                    "/data/system",
                    "/data/tmp",
                    "/data/tombstones",
                    "/data/tpapi",
                    "/data/tpapi/etc",
                    "/data/tpapi/etc/tpa",
                    "/data/tpapi/etc/tpa/persistent",
                    "/data/tpapi/user.bin",
                    "/data/vpnch",
                    "/data/wapi",
                    "/data/wifi",
                    "/data/wimax",
                    "/data/wimax/log",
                    "/data/wiper",
                    "/data/wpstiles",
                    "/data/xt9",
                    "/dbdata/databases",
                    "/efs/.android",
                    "/mnt/sdcard",
                    "/mnt/usbdrive",
                    "/mnt_ext",
                    "/mnt_ext/badablk2",
                    "/mnt_ext/badablk3",
                    "/mnt_ext/cache",
                    "/mnt_ext/data",
                    "/system/etc/dhcpcd/dhcpcd-run-hooks",
                    "/system/etc/security/drm",
                    "/synthesis/hades",
                    "/synthesis/chimaira",
                    "/synthesis/shdisp",
                    "/synthesis/hdmi",
                    "/tmp"
            )
    );

    /**
     * Verify that directories not discoverable by
     * testAllOtherDirectoriesNotWritable are not writable.  An application
     * should only be able to write to it's own home directory. World
     * writable directories are a security hole because they enable a
     * number of different attacks.
     * <ul>
     *   <li><a href="http://en.wikipedia.org/wiki/Symlink_race">Symlink Races</a></li>
     *   <li>Data destruction by deleting or renaming files you don't own</li>
     *   <li>Data substitution by replacing trusted files with untrusted files</li>
     * </ul>
     *
     * Because /data and /data/data are not readable, we blindly try to
     * poke around in there looking for bad directories.  There has to be
     * a better way...
     */
    @LargeTest
    public void testOtherRandomDirectoriesNotWritable() throws Exception {
        Set<File> writableDirs = new HashSet<File>();
        for (String dir : OTHER_RANDOM_DIRECTORIES) {
            File start = new File(dir);
            writableDirs.addAll(getWritableDirectoryiesAndSubdirectoriesOf(start));
        }

        assertTrue("Found writable directories: " + writableDirs.toString(),
                writableDirs.isEmpty());
    }

    private static final Set<File> SYS_EXCEPTIONS = new HashSet<File>(
            Arrays.asList(
                new File("/sys/kernel/debug/tracing/trace_marker")
            ));

    @LargeTest
    public void testAllFilesInSysAreNotWritable() throws Exception {
        Set<File> writable = getAllWritableFilesInDirAndSubDir(new File("/sys"));
        writable.removeAll(SYS_EXCEPTIONS);
        assertTrue("Found writable: " + writable.toString(),
                writable.isEmpty());
    }

    private static Set<File>
    getAllWritableFilesInDirAndSubDir(File dir) throws Exception {
        assertTrue(dir.isDirectory());
        Set<File> retval = new HashSet<File>();

        if (isSymbolicLink(dir)) {
            // don't examine symbolic links.
            return retval;
        }

        File[] subDirectories = dir.listFiles(new FileFilter() {
            @Override public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });


        /* recurse into subdirectories */
        if (subDirectories != null) {
            for (File f : subDirectories) {
                retval.addAll(getAllWritableFilesInDirAndSubDir(f));
            }
        }

        File[] filesInThisDirectory = dir.listFiles(new FileFilter() {
            @Override public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        if (filesInThisDirectory == null) {
            return retval;
        }

        for (File f: filesInThisDirectory) {
            if (f.canWrite()) {
                retval.add(f.getCanonicalFile());
            }
        }
        return retval;
    }

    public void testAllBlockDevicesAreSecure() throws Exception {
        Set<File> insecure = getAllInsecureBlockDevicesInDirAndSubdir(new File("/dev"));
        assertTrue("Found insecure: " + insecure.toString(),
                insecure.isEmpty());
    }

    private static Set<File>
    getAllInsecureBlockDevicesInDirAndSubdir(File dir) throws Exception {
        assertTrue(dir.isDirectory());
        Set<File> retval = new HashSet<File>();
        File[] subDirectories = dir.listFiles(new FileFilter() {
            @Override public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });


        /* recurse into subdirectories */
        if (subDirectories != null) {
            for (File f : subDirectories) {
                retval.addAll(getAllInsecureBlockDevicesInDirAndSubdir(f));
            }
        }

        File[] filesInThisDirectory = dir.listFiles();
        if (filesInThisDirectory == null) {
            return retval;
        }

        for (File f: filesInThisDirectory) {
            FileUtils.FileStatus status = new FileUtils.FileStatus();
            FileUtils.getFileStatus(f.getAbsolutePath(), status, false);
            if (status.hasModeFlag(FileUtils.S_IFBLK)) {
                if (f.canRead() || f.canWrite() || f.canExecute()) {
                    retval.add(f);
                }
            }
        }
        return retval;
    }

    private Set<File> getWritableDirectoryiesAndSubdirectoriesOf(File dir) throws Exception {
        Set<File> retval = new HashSet<File>();
        if (!dir.isDirectory()) {
            return retval;
        }

        if (isSymbolicLink(dir)) {
            // don't examine symbolic links.
            return retval;
        }

        String myHome = getContext().getApplicationInfo().dataDir;
        String thisDir = dir.getCanonicalPath();
        if (thisDir.startsWith(myHome)) {
            // Don't examine directories within our home directory.
            // We expect these directories to be writable.
            return retval;
        }

        if (isDirectoryWritable(dir)) {
            retval.add(dir);
        }

        File[] subFiles = dir.listFiles();
        if (subFiles == null) {
            return retval;
        }

        for (File f : subFiles) {
            retval.addAll(getWritableDirectoryiesAndSubdirectoriesOf(f));
        }

        return retval;
    }

    private static boolean isSymbolicLink(File f) throws IOException {
        return !f.getAbsolutePath().equals(f.getCanonicalPath());
    }
}

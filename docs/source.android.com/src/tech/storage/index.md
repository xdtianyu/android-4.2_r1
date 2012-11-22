<!--
   Copyright 2012 The Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# External Storage Technical Information

Android supports devices with external storage, which is defined to be a
case-insensitive and permissionless filesystem.  External storage can be
provided by physical media (such as an SD card), or by an emulation layer backed
by internal storage.  Devices may contain multiple instances of external
storage, but currently only the primary external storage is exposed to
developers through API.

## Device specific configuration

External storage is managed by a combination of the `vold` init service and
`MountService` system service.

Mounting of physical external storage volumes is handled by `vold`, which
performs staging operations to prepare the media before exposing it to apps.
The device-specific `vold.fstab` configuration file defines mappings from sysfs
devices to filesystem mount points, and each line follows this format:

    dev_mount <label> <mount_point> <partition> <sysfs_path> [flags]

* `label`: Label for the volume.
* `mount_point`: Filesystem path where the volume should be mounted.
* `partition`: Partition number (1 based), or 'auto' for first usable partition.
* `sysfs_path`: One or more sysfs paths to devices that can provide this mount
point.  Separated by spaces, and each must start with `/`.
* `flags`: Optional comma separated list of flags, must not contain `/`.
Possible values include `nonremovable` and `encryptable`.

External storage interactions at and above the framework level are handled
through `MountService`.  The device-specific `storage_list.xml` configuration
file, typically provided through a `frameworks/base` overlay, defines the
attributes and constraints of storage devices.  The `<StorageList>` element
contains one or more `<storage>` elements, exactly one of which should be marked
primary.  `<storage>` attributes include:

* `mountPoint`: filesystem path of this mount.
* `storageDescription`: string resource that describes this mount.
* `primary`: true if this mount is the primary external storage.
* `removable`: true if this mount has removable media, such as a physical SD
card.
* `emulated`: true if this mount is emulated and is backed by internal storage,
possibly using a FUSE daemon.
* `mtp-reserve`: number of MB of storage that MTP should reserve for free
storage.  Only used when mount is marked as emulated.
* `allowMassStorage`: true if this mount can be shared via USB mass storage.
* `maxFileSize`: maximum file size in MB.

Devices may provide external storage by emulating a case-insensitive,
permissionless filesystem backed by internal storage.  One possible
implementation is provided by the FUSE daemon in `system/core/sdcard`, which can
be added as a device-specific `init.rc` service:

    # virtual sdcard daemon running as media_rw (1023)
    service sdcard /system/bin/sdcard <source_path> <dest_path> 1023 1023
        class late_start

Where `source_path` is the backing internal storage and `dest_path` is the
target mount point.

When configuring a device-specific `init.rc` script, the `EXTERNAL_STORAGE`
environment variable must be defined as the path to the primary external
storage.  The `/sdcard` path must also resolve to the same location, possibly
through a symlink.  If a device adjusts the location of external storage between
platform updates, symlinks should be created so that old paths continue working.

As an example, here’s the storage configuration for Xoom, which uses a FUSE
daemon to provide primary external storage, and includes a physical SD card as
secondary external storage:

* [vold.fstab](https://android.googlesource.com/device/moto/wingray/+/master/vold.fstab)
* [storage_list.xml](https://android.googlesource.com/device/moto/wingray/+/master/overlay/frameworks/base/core/res/res/xml/storage_list.xml)

Access to external storage is protected by various Android permissions.
Starting in Android 1.0, write access is protected with the
`WRITE_EXTERNAL_STORAGE` permission, implemented using the `sdcard_rw` GID.
Starting in Android 4.1, read access is protected with the new
`READ_EXTERNAL_STORAGE` permission, implemented using the `sdcard_r` GID.  To
implement the read permission, a new top-level `/storage` directory was created
such that processes must hold the `sdcard_r` GID to traverse into it.

Since external storage offers no support for traditional POSIX filesystem
permissions, system code should not store sensitive data on external storage.
Specifically, configuration and log files should only be stored on internal
storage where they can be effectively protected.

## Multi-user external storage

Starting in Android 4.2, devices can support multiple users, and external
storage must meet the following constraints:

* Each user must have their own isolated primary external storage, and must not
have access to the primary external storage of other users.
* The `/sdcard` path must resolve to the correct user-specific primary external
storage based on the user a process is running as.
* Storage for large OBB files in the `Android/obb` directory may be shared
between multiple users as an optimization.
* Secondary external storage must not be writable by apps.

The default platform implementation of this feature leverages Linux kernel
namespaces to create isolated mount tables for each Zygote-forked process, and
then uses bind mounts to offer the correct user-specific primary external
storage into that private namespace.

At boot, the system mounts a single emulated external storage FUSE daemon at
`EMULATED_STORAGE_SOURCE`, which is hidden from apps.  After the Zygote forks,
it bind mounts the appropriate user-specific subdirectory from under the FUSE
daemon to `EMULATED_STORAGE_TARGET` so that external storage paths resolve
correctly for the app.  Because an app lacks accessible mount points for other
users’ storage, they can only access storage for the user it was started as.

This implementation also uses the shared subtree kernel feature to propagate
mount events from the default root namespace into app namespaces, which ensures
that features like ASEC containers and OBB mounting continue working correctly.
It does this by mounting the rootfs as shared, and then remounting it as slave
after each Zygote namespace is created.

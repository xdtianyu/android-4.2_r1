<!--
   Copyright 2011 The Android Open Source Project

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

# Validate Keymaps Tool #

The Android framework has a small tool called `validatekeymaps` to validate the
syntax of input device configuration files, key layout files, key character
maps files and virtual key definition files.

## Compilation ##

To compile `validatekeymaps`, set up the development environment, download
the Android source tree, compile it, then run:

    $ mmm frameworks/base/tools/validatekeymaps

This command should compile a host tool called validatekeymaps into the
`out/host/&lt;os&gt;/bin` directory.

## Usage ##

If you ran `envsetup.sh` to set up your development environment, then the
`validatekeymaps` tool should already be on your path.  You can verify
this by running `validatekeymaps`.

    $ validatekeymaps

    Keymap Validation Tool

    Usage:
     validatekeymaps [*.kl] [*.kcm] [*.idc] [virtualkeys.*] [...]
       Validates the specified key layouts, key character maps, 
       input device configurations, or virtual key definitions.

Then all you need to do is run `validatekeymaps` an give it the path of
one or more files to validate.

    $ validatekeymaps frameworks/base/data/keyboards/Generic.kl

    Validating file 'frameworks/base/data/keyboards/Generic.kl'...
    No errors.

    Success.

And if there is an error...

    $ validatekeymaps Bad.kl

    Validating file 'Bad.kl'...
    E/KeyLayoutMap(87688): Bad.kl:24: Expected keyword, got 'ke'.
    Error -22 parsing key layout file.

    Failed!

## Automation ##

It is a *very* good idea to run `validatekeymaps` on all configuration files
before installing them on a device.

The process can easily be automated as part of the build system by using a
script or a makefile.

The following sample makefile is based on the contents of
`frameworks/base/data/keyboards/Android.mk`.

    # This makefile performs build time validation of framework keymap files.

    LOCAL_PATH := $(call my-dir)

    # Validate all key maps.
    include $(CLEAR_VARS)

    validatekeymaps := $(HOST_OUT_EXECUTABLES)/validatekeymaps$(HOST_EXECUTABLE_SUFFIX)
    files := MyKeyboard.kl MyKeyboard.kcm MyTouchScreen.idc

    LOCAL_MODULE := validate_framework_keymaps
    LOCAL_MODULE_TAGS := optional
    LOCAL_REQUIRED_MODULES := validatekeymaps

    validate_framework_keymaps: $(files)
        $(hide) $(validatekeymaps) $(files)

    include $(BUILD_PHONY_PACKAGE)

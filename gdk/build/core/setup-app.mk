# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# this file is included repeatedly from build/core/main.mk
# and is used to prepare for app-specific build rules.
#

$(call assert-defined,_app)

_map := GDK_APP.$(_app)

# ok, let's parse all Android.mk source files in order to build
# the modules for this app.
#

# Restore the APP_XXX variables just for this pass as GDK_APP_XXX
#
GDK_APP_NAME           := $(_app)
GDK_APP_APPLICATION_MK := $(call get,$(_map),Application.mk)

$(foreach __name,$(GDK_APP_VARS),\
  $(eval GDK_$(__name) := $(call get,$(_map),$(__name)))\
)

# make the application depend on the modules it requires
.PHONY: gdk-app-$(_app)
gdk-app-$(_app): $(GDK_APP_MODULES)
all: gdk-app-$(_app)

TARGET_PLATFORM := android-portable

APP_ABI := $(strip $(GDK_APP_ABI))
APP_ABI := llvm


# Clear all installed binaries for this application
# This ensures that if the build fails, you're not going to mistakenly
# package an obsolete version of it. Or if you change the ABIs you're targetting,
# you're not going to leave a stale shared library for the old one.
#
clean-installed-binaries::
	$(hide) rm -f $(GDK_APP_PROJECT_PATH)/res/raw/lib*.bc

$(foreach _abi,$(APP_ABI),\
    $(eval TARGET_ARCH_ABI := $(_abi))\
    $(eval include $(BUILD_SYSTEM)/setup-abi.mk) \
)

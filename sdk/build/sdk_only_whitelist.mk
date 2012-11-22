# Whitelist of SDK projects that can be built for the SDK on Windows

# The Windows SDK cannot build all the projects from the SDK tree, typically
# due to obvious compiler/architectures differences. When building the Windows
# SDK, we only care about a subset of projects (e.g. generally the SDK tools
# and a few platform-specific binaries.)
#
# This file defines a whitelist of projects that can be built in the Windows
# SDK case. Note that whitelisting a project directory will NOT actually build
# it -- it will only allow one to reference it as a make dependency.
#
# This file is included by build/core/main.mk.

# Note that there are 2 flavors of this file:
#
# - This file: sdk/build/sdk_only_whitelist.mk
#   must list all projects that are that are NOT specific to a given platform.
#   These binaries are the ones typically found in the SDK/tools directory.
#
# - The other file: development/build/sdk_only_whitelist.mk
#   must list all projects that are specific to a given platform. These
#   projects generate files that are generally locates in SDK/platform-tools,
#   or SDK/platforms/, etc.

# -----
# Whitelist of SDK specific projects that do NOT need Java (e.g. C libraries)

subdirs += \
	external/openssl \
	external/qemu \
	prebuilts/tools \
	sdk/avdlauncher \
	sdk/emulator/mksdcard \
	sdk/emulator/opengl \
	sdk/find_java \
	sdk/find_lock \
	sdk/sdklauncher

# -----
# Whitelist of SDK specific projects that DO require Java

ifneq (,$(shell which javac 2>/dev/null))
subdirs += \
	external/eclipse-windowbuilder/propertysheet \
	external/ant-glob \
	sdk/archquery \
	sdk/annotations \
	sdk/apkbuilder \
	sdk/assetstudio \
	sdk/common \
	sdk/ddms \
	sdk/device_validator \
	sdk/eclipse/scripts/rcp \
	sdk/hierarchyviewer2 \
	sdk/sdk_common \
	sdk/jarutils \
	sdk/layoutlib_api \
	sdk/manifmerger \
	sdk/monitor \
	sdk/ninepatch \
	sdk/rule_api \
	sdk/lint \
	sdk/sdkstats \
	sdk/sdkmanager \
	sdk/swtmenubar \
	sdk/traceview \
	sdk/uiautomatorviewer

else
$(warning SDK_ONLY: javac not available.)
endif

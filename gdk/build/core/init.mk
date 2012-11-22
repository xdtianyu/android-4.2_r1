GDK_LOG := $(strip $(GDK_LOG))

# Check that we have at least GNU Make 3.81
# We do this by detecting whether 'lastword' is supported
#
MAKE_TEST := $(lastword a b c d e f)
ifneq ($(MAKE_TEST),f)
    $(error Android GDK: GNU Make version $(MAKE_VERSION) is too low (should be >= 3.81))
endif
ifdef GDK_LOG
    $(info Android GDK: GNU Make version $(MAKE_VERSION) detected)
endif

# GDK_ROOT *must* be defined and point to the root of the GDK installation
GDK_ROOT := $(strip $(GDK_ROOT))
ifndef GDK_ROOT
    $(error ERROR while including init.mk: GDK_ROOT must be defined !)
endif
ifneq ($(words $(GDK_ROOT)),1)
    $(info,The Android GDK installation path contains spaces: '$(GDK_ROOT)')
    $(error,Please fix the problem by reinstalling to a different location.)
endif

# ====================================================================
#
# Define a few useful variables and functions.
# More stuff will follow in definitions.mk.
#
# ====================================================================

# Used to output warnings and error from the library, it's possible to
# disable any warnings or errors by overriding these definitions
# manually or by setting GDK_NO_WARNINGS or GDK_NO_ERRORS

__gdk_name    := Android GDK
__gdk_info     = $(info $(__gdk_name): $1 $2 $3 $4 $5)
__gdk_warning  = $(warning $(__gdk_name): $1 $2 $3 $4 $5)
__gdk_error    = $(error $(__gdk_name): $1 $2 $3 $4 $5)

ifdef GDK_NO_WARNINGS
__gdk_warning :=
endif
ifdef GDK_NO_ERRORS
__gdk_error :=
endif

# -----------------------------------------------------------------------------
# Function : gdk_log
# Arguments: 1: text to print when GDK_LOG is defined
# Returns  : None
# Usage    : $(call gdk_log,<some text>)
# -----------------------------------------------------------------------------
ifdef GDK_LOG
gdk_log = $(info $(__gdk_name): $1)
else
gdk_log :=
endif

# ====================================================================
#
# Host system auto-detection.
#
# ====================================================================

#
# Determine host system and architecture from the environment
#
HOST_OS := $(strip $(HOST_OS))
ifndef HOST_OS
    # On all modern variants of Windows (including Cygwin and Wine)
    # the OS environment variable is defined to 'Windows_NT'
    #
    # The value of PROCESSOR_ARCHITECTURE will be x86 or AMD64
    #
    ifeq ($(OS),Windows_NT)
        HOST_OS := windows
    else
        # For other systems, use the `uname` output
        UNAME := $(shell uname -s)
        ifneq (,$(findstring Linux,$(UNAME)))
            HOST_OS := linux
        endif
        ifneq (,$(findstring Darwin,$(UNAME)))
            HOST_OS := darwin
        endif
        # We should not be there, but just in case !
        ifneq (,$(findstring CYGWIN,$(UNAME)))
            HOST_OS := windows
        endif
        ifeq ($(HOST_OS),)
            $(call __gdk_info,Unable to determine HOST_OS from uname -s: $(UNAME))
            $(call __gdk_info,Please define HOST_OS in your environment.)
            $(call __gdk_error,Aborting.)
        endif
    endif
    $(call gdk_log,Host OS was auto-detected: $(HOST_OS))
else
    $(call gdk_log,Host OS from environment: $(HOST_OS))
endif

HOST_ARCH := $(strip $(HOST_ARCH))
ifndef HOST_ARCH
    ifeq ($(HOST_OS),windows)
        HOST_ARCH := $(PROCESSOR_ARCHITECTURE)
        ifeq ($(HOST_ARCH),AMD64)
            HOST_ARCH := x86
        endif
    else # HOST_OS != windows
        UNAME := $(shell uname -m)
        ifneq (,$(findstring 86,$(UNAME)))
            HOST_ARCH := x86
        endif
        # We should probably should not care at all
        ifneq (,$(findstring Power,$(UNAME)))
            HOST_ARCH := ppc
        endif
        ifeq ($(HOST_ARCH),)
            $(call __gdk_info,Unsupported host architecture: $(UNAME))
            $(call __gdk_error,Aborting)
        endif
    endif # HOST_OS != windows
    $(call gdk_log,Host CPU was auto-detected: $(HOST_ARCH))
else
    $(call gdk_log,Host CPU from environment: $(HOST_ARCH))
endif

HOST_TAG := $(HOST_OS)-$(HOST_ARCH)

# The directory separator used on this host
HOST_DIRSEP := :

# For now always use ":" since Windows is Cygwin
#ifeq ($(HOST_OS),windows)
#  HOST_DIRSEP := ;
#endif

# If we are on Windows, we need to check that we are not running
# Cygwin 1.5, which is deprecated and won't run our toolchain
# binaries properly.
#
ifeq ($(HOST_TAG),windows-x86)
    # On cygwin, 'uname -r' returns something like 1.5.23(0.225/5/3)
    # We recognize 1.5. as the prefix to look for then.
    CYGWIN_VERSION := $(shell uname -r)
    ifneq ($(filter XX1.5.%,XX$(CYGWIN_VERSION)),)
        $(call __gdk_info,You seem to be running Cygwin 1.5, which is not supported.)
        $(call __gdk_info,Please upgrade to Cygwin 1.7 or higher.)
        $(call __gdk_error,Aborting.)
    endif
    # special-case the host-tag
    HOST_TAG := windows
endif
$(call gdk_log,HOST_TAG set to $(HOST_TAG))

#
# Verify that the 'awk' tool has the features we need.
# Both Nawk and Gawk do.
#
HOST_AWK := $(strip $(HOST_AWK))
ifndef HOST_AWK
    HOST_AWK := awk
    $(call gdk_log,Host awk tool was auto-detected: $(HOST_AWK))
else
    $(call gdk_log,Host awk tool from environment: $(HOST_AWK))
endif

# Location of all awk scripts we use
BUILD_AWK := $(GDK_ROOT)/build/awk

AWK_TEST := $(shell $(HOST_AWK) -f $(BUILD_AWK)/check-awk.awk)
$(call gdk_log,Host awk test returned: $(AWK_TEST))
ifneq ($(AWK_TEST),Pass)
    $(call __gdk_info,Host awk tool is outdated. Please define HOST_AWK to point to Gawk or Nawk !)
    $(call __gdk_error,Aborting.)
endif

#
# On windows, define the 'cygwin-to-host-path' function here depending on the
# environment. The rules are the following:
#
# 1/ If "cygpath' is not in your path, do not use it at all. It looks like
#    this allows to build with the GDK from MSys without problems.
#
# 2/ Since invoking 'cygpath -m' from GNU Make for each source file is
#    _very_ slow, try to generate a Make function that performs the mapping
#    from cygwin to host paths through simple substitutions.
#
# 3/ In case we fail horribly, allow the user to define GDK_USE_CYGPATH to '1'
#    in order to use 'cygpath -m' nonetheless. This is only a backup plan in
#    case our automatic substitution function doesn't work (only likely if you
#    have a very weird cygwin setup).
#
# The function for 2/ is generated by an awk script. It's really a series
# of nested patsubst calls, that look like:
#
#     cygwin-to-host-path = $(patsubst /cygdrive/c/%,c:/%,\
#                             $(patsusbt /cygdrive/d/%,d:/%, \
#                              $1)
#
# except that the actual definition is built from the list of mounted
# drives as reported by "mount" and deals with drive letter cases (i.e.
# '/cygdrive/c' and '/cygdrive/C')
#
ifeq ($(HOST_OS),windows)
    CYGPATH := $(strip $(HOST_CYGPATH))
    ifndef CYGPATH
        $(call gdk_log, Probing for 'cygpath' program)
        CYGPATH := $(strip $(shell which cygpath 2>/dev/null))
        ifndef CYGPATH
            $(call gdk_log, 'cygpath' was *not* found in your path)
        else
            $(call gdk_log, 'cygpath' found as: $(CYGPATH))
        endif
    endif
    ifndef CYGPATH
        cygwin-to-host-path = $1
    else
        ifeq ($(GDK_USE_CYGPATH),1)
            $(call gdk_log, Forced usage of 'cygpath -m' through GDK_USE_CYGPATH=1)
            cygwin-to-host-path = $(strip $(shell $(CYGPATH) -m $1))
        else
            # Call an awk script to generate a Makefile fragment used to define a function
            WINDOWS_HOST_PATH_FRAGMENT := $(shell mount | $(HOST_AWK) -f $(BUILD_AWK)/gen-windows-host-path.awk)
            ifeq ($(GDK_LOG),1)
                $(info Using cygwin substitution rules:)
                $(eval $(shell mount | $(HOST_AWK) -f $(BUILD_AWK)/gen-windows-host-path.awk -vVERBOSE=1))
            endif
            $(eval cygwin-to-host-path = $(WINDOWS_HOST_PATH_FRAGMENT))
        endif
    endif
endif # HOST_OS == windows

# The location of the build system files
BUILD_SYSTEM := $(GDK_ROOT)/build/core

# Include common definitions
include $(BUILD_SYSTEM)/definitions.mk

# ====================================================================
#
# Read all toolchain-specific configuration files.
#
# Each toolchain must have a corresponding config.mk file located
# in build/toolchains/<name>/ that will be included here.
#
# Each one of these files should define the following variables:
#   TOOLCHAIN_NAME   toolchain name (e.g. arm-eabi-4.2.1)
#   TOOLCHAIN_ABIS   list of target ABIs supported by the toolchain.
#
# Then, it should include $(ADD_TOOLCHAIN) which will perform
# book-keeping for the build system.
#
# ====================================================================

# the build script to include in each toolchain config.mk
ADD_TOOLCHAIN := $(BUILD_SYSTEM)/add-toolchain.mk

# the list of all toolchains in this GDK
GDK_ALL_TOOLCHAINS :=
GDK_ALL_ABIS       :=

TOOLCHAIN_CONFIGS := $(wildcard $(GDK_ROOT)/toolchains/*/config.mk)
$(foreach _config_mk,$(TOOLCHAIN_CONFIGS),\
  $(eval include $(BUILD_SYSTEM)/add-toolchain.mk)\
)

GDK_ALL_TOOLCHAINS   := $(call remove-duplicates,$(GDK_ALL_TOOLCHAINS))
GDK_ALL_ABIS         := $(call remove-duplicates,$(GDK_ALL_ABIS))

# The default toolchain is now arm-eabi-4.4.0, however its
# C++ compiler is a tad bit more pedantic with certain
# constructs (e.g. templates) so allow users to switch back
# to the old 4.2.1 instead if they really want to.
#
# NOTE: you won't get armeabi-v7a support though !
#
GDK_TOOLCHAIN := $(strip $(GDK_TOOLCHAIN))
ifdef GDK_TOOLCHAIN
    # check that the toolchain name is supported
    $(if $(filter-out $(GDK_ALL_TOOLCHAINS),$(GDK_TOOLCHAIN)),\
      $(call __gdk_info,GDK_TOOLCHAIN is defined to the unsupported value $(GDK_TOOLCHAIN)) \
      $(call __gdk_info,Please use one of the following values: $(GDK_ALL_TOOLCHAINS))\
      $(call __gdk_error,Aborting)\
    ,)
    $(call gdk_log, Using specific toolchain $(GDK_TOOLCHAIN))
endif

$(call gdk_log, This GDK supports the following toolchains and target ABIs:)
$(foreach tc,$(GDK_ALL_TOOLCHAINS),\
    $(call gdk_log, $(space)$(space)$(tc):  $(GDK_TOOLCHAIN.$(tc).abis))\
)

# ====================================================================
#
# Read all platform-specific configuration files.
#
# Each platform must be located in build/platforms/android-<apilevel>
# where <apilevel> corresponds to an API level number, with:
#   3 -> Android 1.5
#   4 -> next platform release
#
# ====================================================================

# The platform files were moved in the Android source tree from
# $TOP/gdk/build/platforms to $TOP/development/gdk/platforms. However,
# the official GDK release packages still place them under the old
# location for now, so deal with this here
#
GDK_PLATFORMS_ROOT := $(strip $(GDK_PLATFORMS_ROOT))
ifndef GDK_PLATFORMS_ROOT
    GDK_PLATFORMS_ROOT := $(strip $(wildcard $(GDK_ROOT)/platforms))
    ifndef GDK_PLATFORMS_ROOT
        GDK_PLATFORMS_ROOT := $(strip $(wildcard $(GDK_ROOT)/build/platforms))
    endif

    ifndef GDK_PLATFORMS_ROOT
        $(call __gdk_info,Could not find platform files (headers and libraries))
        $(if $(strip $(wildcard $(GDK_ROOT)/RELEASE.TXT)),\
            $(call __gdk_info,Please define GDK_PLATFORMS_ROOT to point to a valid directory.)\
        ,\
            $(call __gdk_info,Please run build/tools/build-platforms.sh to build the corresponding directory.)\
        )
        $(call __gdk_error,Aborting)
    endif

    $(call gdk_log,Found platform root directory: $(GDK_PLATFORMS_ROOT))
endif
ifeq ($(strip $(wildcard $(GDK_PLATFORMS_ROOT)/android-*)),)
    $(call __gdk_info,Your GDK_PLATFORMS_ROOT points to an invalid directory)
    $(call __gdk_info,Current value: $(GDK_PLATFORMS_ROOT))
    $(call __gdk_error,Aborting)
endif

GDK_ALL_PLATFORMS := $(strip $(notdir $(wildcard $(GDK_PLATFORMS_ROOT)/android-*)))
$(call gdk_log,Found supported platforms: $(GDK_ALL_PLATFORMS))

$(foreach _platform,$(GDK_ALL_PLATFORMS),\
  $(eval include $(BUILD_SYSTEM)/add-platform.mk)\
)

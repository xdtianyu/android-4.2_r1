# FIXME(Nowar): Use llvm-ndk-cc instead of clang.
#TARGET_CC := $(TOOLCHAIN_PREBUILT_ROOT)/llvm-ndk-cc
#TARGET_CFLAGS :=
#
#TARGET_CXX := $(TARGET_CC)
#TARGET_CXXFLAGS := $(TARGET_CFLAGS) -fno-exceptions -fno-rtti -D __cplusplus
#
#TARGET_LD := $(TOOLCHAIN_PREBUILT_ROOT)/llvm-ndk-link
#TARGET_LDFLAGS :=

TARGET_C_INCLUDES := $(GDK_PLATFORMS_ROOT)/android-portable/arch-llvm/usr/include

# Workaround before the required headers are in the above dir.
##TARGET_C_INCLUDES += $(NDK_ROOT)/platforms/android-9/arch-arm/usr/include
TARGET_C_INCLUDES += $(NDK_ROOT)/sources/cxx-stl/system/include
#TARGET_C_INCLUDES += $(NDK_ROOT)/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3/include
TARGET_C_INCLUDES += $(NDK_ROOT)/toolchains/mips-linux-android-4.4.3/prebuilt/linux-x86/lib/gcc/mips-linux-android/4.4.3/include
#TARGET_C_INCLUDES += $(NDK_ROOT)/toolchains/x86-4.4.3/prebuilt/linux-x86/lib/gcc/i686-android-linux/4.4.3/include

TARGET_CC       := $(OUT)/../../../host/linux-x86/bin/clang
#TARGET_CFLAGS   := -ccc-host-triple armv7-none-linux-gnueabi
TARGET_CFLAGS   := -ccc-host-triple mipsel-none-linux-gnueabi
#TARGET_CFLAGS   := -ccc-host-triple i686-unknown-linux
TARGET_CFLAGS   += -emit-llvm
TARGET_CFLAGS += -D_MIPS_SZLONG=32  ## Workaround for arch-mips

TARGET_CXX      := $(OUT)/../../../host/linux-x86/bin/clang++
TARGET_CXXFLAGS := $(TARGET_CFLAGS) -fno-exceptions -fno-rtti -D __cplusplus -ffunction-sections -fdata-sections

TARGET_LD       := $(OUT)/../../../host/linux-x86/bin/llvm-link
TARGET_LDFLAGS  :=

TARGET_STRIP    := $(OUT)/../../../host/linux-x86/bin/opt

define cmd-link-bitcodes
$(TARGET_LD) \
  $(call host-path, $(PRIVATE_OBJECTS)) | \
  $(TARGET_STRIP) -strip -o $(call host-path,$@)
endef

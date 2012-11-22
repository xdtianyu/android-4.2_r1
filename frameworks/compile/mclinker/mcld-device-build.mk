include $(LLVM_DEVICE_BUILD_MK)

# The three inline options together reduce libbcc.so almost 1MB.
# We move them from global build/core/combo/TARGET_linux-arm.mk
# to here.
LOCAL_CFLAGS := -DANDROID_TARGET_BUILD \
		-finline-limit=64 \
		-finline-functions \
		-fno-inline-functions-called-once \
		$(LOCAL_CFLAGS)

LOCAL_CPPFLAGS :=	\
	$(LOCAL_CPPFLAGS)	\
  -Wformat  \
  -Werror=format-security \
  -Werror=return-type \
  -Werror=non-virtual-dtor  \
  -Werror=address \
  -Werror=sequence-point  \
	-Woverloaded-virtual	\
	-Wno-sign-promo

ifeq ($(MCLD_ENABLE_ASSERTION),true)
  LOCAL_CPPFLAGS += \
    -D_DEBUG  \
    -UNDEBUG
endif

# Make sure bionic is first so we can include system headers.
LOCAL_C_INCLUDES :=	\
	bionic \
	external/stlport/stlport \
  $(MCLD_ROOT_PATH)/include \
	$(LLVM_ROOT_PATH)	\
	$(LLVM_ROOT_PATH)/include	\
	$(LLVM_ROOT_PATH)/device/include	\
	$(LOCAL_C_INCLUDES)

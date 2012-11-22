include $(LLVM_HOST_BUILD_MK)

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

LOCAL_C_INCLUDES :=	\
  $(MCLD_ROOT_PATH)/include \
	$(LLVM_ROOT_PATH)	\
	$(LLVM_ROOT_PATH)/include	\
	$(LLVM_ROOT_PATH)/host/include	\
	$(LOCAL_C_INCLUDES)

LOCAL_IS_HOST_MODULE := true

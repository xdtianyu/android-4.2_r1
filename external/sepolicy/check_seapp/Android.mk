##
# checkseapp
#

include $(CLEAR_VARS)

LOCAL_MODULE := checkseapp
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libsepol/include/ 
LOCAL_CFLAGS := -DLINK_SEPOL_STATIC 
LOCAL_SRC_FILES := check_seapp/check_seapp.c
LOCAL_STATIC_LIBRARIES := libsepol
LOCAL_MODULE_CLASS := EXECUTABLES

include $(BUILD_HOST_EXECUTABLE)

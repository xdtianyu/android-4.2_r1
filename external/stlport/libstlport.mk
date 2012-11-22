# Add a couple include paths to use stlport.

# Make sure bionic is first so we can include system headers.
LOCAL_C_INCLUDES := \
	bionic \
	external/stlport/stlport \
	$(LOCAL_C_INCLUDES)

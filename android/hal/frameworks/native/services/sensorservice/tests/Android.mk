LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	sensorservicetest.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils libutils libui libgui

LOCAL_MODULE:= test-sensorservice

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

# test-sensorcontrol
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= sensorcontrol.cpp

LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libui
LOCAL_SHARED_LIBRARIES += libgui
LOCAL_SHARED_LIBRARIES += libbinder

LOCAL_MODULE:= test-sensorcontrol
LOCAL_MODULE_OWNER := invensense
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)


LOCAL_PATH:= $(call my-dir)

# test-sensorload
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= sensorloadtest.cpp

LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_SHARED_LIBRARIES += libutils
LOCAL_SHARED_LIBRARIES += libui
LOCAL_SHARED_LIBRARIES += libgui
LOCAL_SHARED_LIBRARIES += libbinder

LOCAL_MODULE:= test-sensorload
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
LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)

intermediates := $(call local-intermediates-dir)

LOCAL_GENERATED_SOURCES += \
  $(intermediates)/LLDBWrapPython.cpp

LLDB_ROOT_PATH_ABS := $(abspath $(LLDB_ROOT_PATH))

# setup target-specific variables; otherwise, this evaluates to a
# device-specific path when we want the host-specific path.
$(intermediates)/LLDBWrapPython.cpp $(intermediates)/lldb.py: \
	intermediates_abs := $(abspath .)/$(intermediates)

$(intermediates)/LLDBWrapPython.cpp $(intermediates)/lldb.py: $(LLDB_ROOT_PATH)/scripts/Python/modify-python-lldb.py \
                            $(LLDB_ROOT_PATH)/scripts/Python/edit-swig-python-wrapper-file.py \
                            $(wildcard $(LLDB_ROOT_PATH)/scripts/Python/interface/*.i)
	@echo "Generating LLDBWrapPython.cpp"
	$(hide) mkdir -p $(intermediates)
	$(hide) "$(LLDB_ROOT_PATH_ABS)/scripts/build-swig-wrapper-classes.sh" "$(LLDB_ROOT_PATH_ABS)" "$(intermediates_abs)" "$(intermediates_abs)" "$(intermediates_abs)" -m
	$(hide) "$(LLDB_ROOT_PATH_ABS)/scripts/finish-swig-wrapper-classes.sh" "$(LLDB_ROOT_PATH_ABS)" "$(intermediates_abs)" "$(intermediates_abs)" "$(intermediates_abs)" -m

# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cts_security_apps_list := \
    CtsAppAccessData \
    CtsAppWithData \
    CtsDocumentProvider \
    CtsDocumentClient \
    CtsExternalStorageApp \
    CtsInstrumentationAppDiffCert \
    CtsUsePermissionApp \
    CtsUsePermissionAppCompat \
    CtsPermissionDeclareApp \
    CtsPermissionDeclareAppCompat \
    CtsReadExternalStorageApp \
    CtsSharedUidInstall \
    CtsSharedUidInstallDiffCert \
    CtsSimpleAppInstall \
    CtsSimpleAppInstallDiffCert \
    CtsSplitApp \
    CtsSplitApp_x86 \
    CtsSplitApp_x86_64 \
    CtsSplitApp_armeabi-v7a \
    CtsSplitApp_armeabi \
    CtsSplitApp_arm64-v8a \
    CtsSplitApp_mips64 \
    CtsSplitApp_mips \
    CtsSplitAppDiffRevision \
    CtsSplitAppDiffVersion \
    CtsSplitAppDiffCert \
    CtsSplitAppFeature \
    CtsTargetInstrumentationApp \
    CtsUsePermissionDiffCert \
    CtsWriteExternalStorageApp \
    CtsMultiUserStorageApp

cts_security_keysets_list := \
    CtsKeySetTestApp \
    CtsKeySetPermDefSigningA \
    CtsKeySetPermDefSigningB\
    CtsKeySetPermUseSigningA \
    CtsKeySetPermUseSigningB \
    CtsKeySetSigningAUpgradeA \
    CtsKeySetSigningBUpgradeA \
    CtsKeySetSigningAUpgradeAAndB \
    CtsKeySetSigningAUpgradeAOrB \
    CtsKeySetSigningAUpgradeB \
    CtsKeySetSigningBUpgradeB \
    CtsKeySetSigningAAndBUpgradeA \
    CtsKeySetSigningAAndCUpgradeB \
    CtsKeySetSigningAUpgradeNone \
    CtsKeySetSharedUserSigningAUpgradeB \
    CtsKeySetSharedUserSigningBUpgradeB \
    CtsKeySetSigningABadUpgradeB \
    CtsKeySetSigningCBadAUpgradeAB \
    CtsKeySetSigningANoDefUpgradeB \
    CtsKeySetSigningAUpgradeEcA \
    CtsKeySetSigningEcAUpgradeA

cts_account_support_packages := \
    CtsUnaffiliatedAccountAuthenticators

cts_support_packages := \
    CtsAccelerationTestStubs \
    CtsAlarmClockService \
    CtsAppTestStubs \
    CtsAssistService \
    CtsAssistApp \
    CtsAtraceTestApp \
    CtsCertInstallerApp \
    CtsDeviceAdmin \
    CtsDeviceOpenGl \
    CtsWifiConfigCreator \
    CtsDeviceAndProfileOwnerApp \
    CtsDeviceAppUsageTestApp \
    CtsDeviceInfo \
    CtsDeviceOsTestApp \
    CtsDeviceOwnerApp \
    CtsDeviceTaskswitchingAppA \
    CtsDeviceTaskswitchingAppB \
    CtsDeviceTaskswitchingControl \
    CtsDeviceUi \
    CtsHostsideNetworkTestsApp \
    CtsIntentReceiverApp \
    CtsIntentSenderApp \
    CtsLauncherAppsTests \
    CtsLauncherAppsTestsSupport \
    CtsLeanbackJank \
    CtsManagedProfileApp \
    CtsMonkeyApp \
    CtsMonkeyApp2 \
    CtsPackageInstallerApp \
    CtsPermissionApp \
    CtsSimpleApp \
    CtsSimplePreMApp \
    CtsSomeAccessibilityServices \
    CtsThemeDeviceApp \
    TestDeviceSetup \
    CtsUiAutomatorApp \
    CtsUsbSerialTestApp \
    CtsVoiceInteractionService \
    CtsVoiceInteractionApp \
    CtsVoiceSettingsService \
    CtsWidgetProviderApp \
    $(cts_account_support_packages) \
    $(cts_security_apps_list) \
    $(cts_security_keysets_list)

cts_external_packages := \
    com.replica.replicaisland \
    com.drawelements.deqp

# Any APKs that need to be copied to the CTS distribution's testcases
# directory but do not require an associated test package XML.
CTS_TEST_CASE_LIST := \
    $(cts_support_packages) \
    $(cts_external_packages)

# Test packages that require an associated test package XML.
cts_test_packages := \
    CtsDeviceFilePerf \
    CtsDeviceUi \
    CtsDeviceDram \
    CtsDeviceSimpleCpu \
    CtsDeviceBrowserBench \
    CtsDeviceVideoPerf \
    CtsDeviceOpenGl \
    CtsDeviceTvProviderPerf \
    CtsAccelerationTestCases \
    CtsAccountManagerTestCases \
    CtsAccessibilityServiceTestCases \
    CtsAccessibilityTestCases \
    CtsAdminTestCases \
    CtsAlarmClockTestCases \
    CtsAnimationTestCases \
    CtsAppTestCases \
    CtsAppWidgetTestCases \
    CtsAssistTestCases \
    CtsBluetoothTestCases \
    CtsCalendarcommon2TestCases \
    CtsCallLogTestCases \
    CtsContentTestCases \
    CtsDatabaseTestCases \
    CtsDisplayTestCases \
    CtsDpiTestCases \
    CtsDpiTestCases2 \
    CtsDreamsTestCases \
    CtsDrmTestCases \
    CtsEffectTestCases \
    CtsGestureTestCases \
    CtsGraphicsTestCases \
    CtsGraphics2TestCases \
    CtsHardwareTestCases \
    CtsJankTestCases \
    CtsLeanbackJankTestCases \
    CtsJobSchedulerDeviceTestCases \
    CtsJniTestCases \
    CtsKeystoreTestCases \
    CtsLibcoreLegacy22TestCases \
    CtsLocationTestCases \
    CtsLocation2TestCases \
    CtsMediaStressTestCases \
    CtsMediaTestCases \
    CtsMidiTestCases \
    CtsNativeOpenGLTestCases \
    CtsNdefTestCases \
    CtsNetTestCases \
    CtsNetTestCasesLegacyApi22 \
    CtsNetTestCasesLegacyPermission22 \
    CtsOpenGLTestCases \
    CtsOpenGlPerfTestCases \
    CtsOsTestCases \
    CtsPermissionTestCases \
    CtsPermission2TestCases \
    CtsPreferenceTestCases \
    CtsPreference2TestCases \
    CtsPrintTestCases \
    CtsProviderTestCases \
    CtsRenderscriptTestCases \
    CtsRenderscriptLegacyTestCases \
    CtsRsCppTestCases \
    CtsSaxTestCases \
    CtsSecurityTestCases \
    CtsSignatureTestCases \
    CtsSpeechTestCases \
    CtsTelecomTestCases \
    CtsTelecomTestCases2 \
    CtsTelephonyTestCases \
    CtsTextTestCases \
    CtsTextureViewTestCases \
    CtsThemeTestCases \
    CtsTransitionTestCases \
    CtsTvTestCases \
    CtsUiAutomationTestCases \
    CtsUiRenderingTestCases \
    CtsUsageStatsTestCases \
    CtsUtilTestCases \
    CtsViewTestCases \
    CtsVoiceInteractionTestCases \
    CtsVoiceSettingsTestCases \
    CtsWebkitTestCases \
    CtsWidgetTestCases

# All APKs that need to be scanned by the coverage utilities.
CTS_COVERAGE_TEST_CASE_LIST := \
    $(cts_support_packages) \
    $(cts_test_packages)

# Host side only tests
cts_host_libraries := \
    CtsAdbTests \
    CtsAppSecurityTests \
    CtsAtraceHostTestCases \
    CtsDevicePolicyManagerTestCases \
    CtsDumpsysHostTestCases \
    CtsHostJank \
    CtsHostsideNetworkTests \
    CtsHostUi \
    CtsJdwpSecurityHostTestCases \
    CtsMonkeyTestCases \
    CtsOsHostTestCases \
    CtsThemeHostTestCases \
    CtsUsageHostTestCases \
    CtsSecurityHostTestCases \
    CtsUsbTests

# List of native tests. For 32 bit targets, assumes that there will be
# one test executable, and it will end in 32. For 64 bit targets, assumes
# that there will be two executables, one that ends in 32 for the 32
# bit executable and one that ends in 64 for the 64 bit executable.
cts_native_tests := \
    NativeMediaTest_SL \
    NativeMediaTest_XA \

ifeq ($(HOST_OS)-$(HOST_ARCH),$(filter $(HOST_OS)-$(HOST_ARCH),linux-x86 linux-x86_64))
cts_native_tests += bionic-unit-tests-cts
endif

cts_ui_tests := \
    CtsUiAutomatorTests

cts_device_jars := \
    CtsDeviceJank \
    CtsJdwpApp

cts_target_junit_tests := \
    CtsJdwp

cts_deqp_test_apis := \
    egl \
    gles2 \
    gles3 \
    gles31

# All the files that will end up under the repository/testcases
# directory of the final CTS distribution.
CTS_TEST_CASES := $(call cts-get-lib-paths,$(cts_host_libraries)) \
    $(call cts-get-package-paths,$(cts_test_packages)) \
    $(call cts-get-ui-lib-paths,$(cts_ui_tests)) \
    $(call cts-get-ui-lib-paths,$(cts_device_jars)) \
    $(call cts-get-ui-lib-paths,$(cts_target_junit_tests)) \
    $(call cts-get-executable-paths,$(cts_device_executables)) \
    $(call cts-get-native-paths,$(cts_native_tests),32)

ifeq ($(TARGET_IS_64_BIT),true)
CTS_TEST_CASES += $(call cts-get-native-paths,$(cts_native_tests),64)
endif

# All the XMLs that will end up under the repository/testcases
# and that need to be created before making the final CTS distribution.
CTS_TEST_XMLS := $(call cts-get-test-xmls,$(cts_host_libraries)) \
    $(call cts-get-test-xmls,$(cts_test_packages)) \
    $(call cts-get-test-xmls,$(cts_native_tests)) \
    $(call cts-get-test-xmls,$(cts_target_junit_tests)) \
    $(call cts-get-test-xmls,$(cts_ui_tests)) \
    $(call cts-get-deqp-test-xmls,$(cts_deqp_test_apis))

# The following files will be placed in the tools directory of the CTS distribution
CTS_TOOLS_LIST :=

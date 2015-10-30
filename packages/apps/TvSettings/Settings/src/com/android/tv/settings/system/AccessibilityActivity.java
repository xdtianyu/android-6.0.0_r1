/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.settings.system;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.NonNull;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TtsEngines;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.ArrayMap;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.android.tv.settings.R;
import com.android.tv.settings.dialog.Layout;
import com.android.tv.settings.dialog.SettingsLayoutActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;
import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

public class AccessibilityActivity extends SettingsLayoutActivity {

    private static final String TAG = "AccessibilityActivity";
    private static final boolean DEBUG = false;

    private static final int GET_SAMPLE_TEXT = 1983;
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    private static final char ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':';

    private static final int TTS_RATE_VERY_SLOW = 60;
    private static final int TTS_RATE_SLOW = 80;
    private static final int TTS_RATE_NORMAL = 100;
    private static final int TTS_RATE_FAST = 150;
    private static final int TTS_RATE_VERY_FAST = 200;

    private static final int ACTION_BASE_MASK = -1 << 20;

    private static final int ACTION_SERVICE_ENABLE_BASE = 1 << 20;

    private static final int ACTION_SERVICE_DISABLE_BASE = 2 << 20;

    private static final int ACTION_SPEAK_PASSWORD_BASE = 3 << 20;
    private static final int ACTION_SPEAK_PASSWORD_ENABLE = ACTION_SPEAK_PASSWORD_BASE;
    private static final int ACTION_SPEAK_PASSWORD_DISABLE = ACTION_SPEAK_PASSWORD_BASE + 1;

    private static final int ACTION_TTS_BASE = 4 << 20;
    private static final int ACTION_PLAY_SAMPLE = ACTION_TTS_BASE;

    private static final int ACTION_TTS_ENGINE_BASE = 5 << 20;

    private static final int ACTION_TTS_LANGUAGE_BASE = 6 << 20;

    private static final int ACTION_TTS_RATE_BASE = 7 << 20;
    private static final int ACTION_TTS_RATE_VERY_SLOW = ACTION_TTS_RATE_BASE + TTS_RATE_VERY_SLOW;
    private static final int ACTION_TTS_RATE_SLOW = ACTION_TTS_RATE_BASE + TTS_RATE_SLOW;
    private static final int ACTION_TTS_RATE_NORMAL = ACTION_TTS_RATE_BASE + TTS_RATE_NORMAL;
    private static final int ACTION_TTS_RATE_FAST = ACTION_TTS_RATE_BASE + TTS_RATE_FAST;
    private static final int ACTION_TTS_RATE_VERY_FAST = ACTION_TTS_RATE_BASE + TTS_RATE_VERY_FAST;

    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;
    private String mCurrentEngine;
    private String mPreviousEngine;
    private Intent mVoiceCheckData = null;
    private String mVoiceCheckEngine;

    private final ArrayList<AccessibilityComponentHolder> mAccessibilityComponentHolders =
            new ArrayList<>();

    private final ArrayList<Locale> mEngineLocales = new ArrayList<>();

    private final ArrayList<EngineInfo> mEngineInfos = new ArrayList<>();

    private final Layout.LayoutGetter mTtsLanguageLayoutGetter = new TtsLanguageLayoutGetter();

    /**
     * The initialization listener used when we are initalizing the settings
     * screen for the first time (as opposed to when a user changes his choice
     * of engine).
     */
    private final TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
            @Override
        public void onInit(int status) {
            onInitEngine(status);
        }
    };

    /**
     * The initialization listener used when the user changes his choice of
     * engine (as opposed to when then screen is being initialized for the first
     * time).
     */
    private final TextToSpeech.OnInitListener mUpdateListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            onUpdateEngine(status);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mTts = new TextToSpeech(getApplicationContext(), mInitListener);
        mEnginesHelper = new TtsEngines(getApplicationContext());
        mCurrentEngine = mTts.getCurrentEngine();

        checkVoiceData(mCurrentEngine);

        super.onCreate(savedInstanceState);
    }

    @Override
    public Layout createLayout() {
        final Resources res = getResources();
        return new Layout()
                .breadcrumb(getString(R.string.header_category_preferences))
                .add(new Layout.Header.Builder(res)
                        .title(R.string.system_accessibility)
                        .icon(R.drawable.ic_settings_accessibility)
                        .build()
                        .add(getCaptionsAction())
                        .add(getServicesHeader())
                        // TODO b/18007521
                        // uncomment when Talkback is able to support not speaking passwords aloud
                        //.add(getSpeakPasswordsHeader())
                        .add(getTtsHeader()));
    }

    private Layout.Action getCaptionsAction() {
        final ComponentName comp = new ComponentName(this, CaptionSetupActivity.class);
        final Intent captionsIntent = new Intent(Intent.ACTION_MAIN).setComponent(comp);

        return new Layout.Action.Builder(getResources(), captionsIntent)
                .title(R.string.accessibility_captions)
                .build();
    }

    private Layout.Header getServicesHeader() {
        final Resources res = getResources();
        final Layout.Header header = new Layout.Header.Builder(res)
                .title(R.string.system_services)
                .build();

        final List<AccessibilityServiceInfo> installedServiceInfos = AccessibilityManager
                .getInstance(this).getInstalledAccessibilityServiceList();

        final Set<ComponentName> enabledServices = getEnabledServicesFromSettings();

        final boolean accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        final int serviceInfoCount = installedServiceInfos.size();
        mAccessibilityComponentHolders.clear();
        mAccessibilityComponentHolders.ensureCapacity(serviceInfoCount);
        for (int i = 0; i < serviceInfoCount; i++) {
            final AccessibilityServiceInfo accInfo = installedServiceInfos.get(i);
            final ServiceInfo serviceInfo = accInfo.getResolveInfo().serviceInfo;
            final ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            final boolean serviceEnabled = accessibilityEnabled
                    && enabledServices.contains(componentName);

            final String title =
                    accInfo.getResolveInfo().loadLabel(getPackageManager()).toString();

            final AccessibilityComponentHolder component =
                    new AccessibilityComponentHolder(componentName, title, serviceEnabled);

            mAccessibilityComponentHolders.add(component);

            header.add(getServiceHeader(component, title,
                    ACTION_SERVICE_ENABLE_BASE + i, ACTION_SERVICE_DISABLE_BASE + i));
        }

        return header;
    }

    private Layout.Header getServiceHeader(AccessibilityComponentHolder componentHolder,
            String title, int enableActionId, int disableActionId) {
        final Resources res = getResources();

        final Layout.Action enableAction = new Layout.Action.Builder(res, enableActionId)
                .title(R.string.settings_on)
                .checked(componentHolder.getEnabledGetter())
                .build();
        final Layout.Action disableAction = new Layout.Action.Builder(res, disableActionId)
                .title(R.string.settings_off)
                .checked(componentHolder.getDisabledGetter())
                .build();

        final ComponentName componentName = componentHolder.getComponentName();
        final ComponentName settingsIntentComponent = getSettingsForService(componentName);

        if (settingsIntentComponent != null) {
            final Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
                    .setComponent(settingsIntentComponent);

            return new Layout.Header.Builder(res)
                    .title(title)
                    .description(componentHolder.getStateStringGetter())
                    .build()
                    .add(new Layout.Header.Builder(res)
                            .title(R.string.system_accessibility_status)
                            .description(componentHolder.getStateStringGetter())
                            .build()
                            .add(enableAction)
                            .add(disableAction))
                    .add(new Layout.Action.Builder(res, settingsIntent)
                            .title(R.string.system_accessibility_config)
                            .build());
        } else {
            return new Layout.Header.Builder(res)
                    .title(title)
                    .description(componentHolder.getStateStringGetter())
                    .build()
                    .add(enableAction)
                    .add(disableAction);
        }
    }

    private Layout.Header getSpeakPasswordsHeader() {
        final boolean speakPasswordsEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0) != 0;
        return new Layout.Header.Builder(getResources())
                .title(R.string.system_speak_passwords)
                .build()
                .setSelectionGroup(new Layout.SelectionGroup.Builder()
                        .add(getString(R.string.settings_on), null, ACTION_SPEAK_PASSWORD_ENABLE)
                        .add(getString(R.string.settings_off), null, ACTION_SPEAK_PASSWORD_DISABLE)
                        .select(speakPasswordsEnabled ?
                                ACTION_SPEAK_PASSWORD_ENABLE : ACTION_SPEAK_PASSWORD_DISABLE)
                        .build());
    }

    private Layout.Header getTtsHeader() {
        final Resources res = getResources();
        final Layout.Header header = new Layout.Header.Builder(res)
                .title(R.string.system_accessibility_tts_output)
                .build();
        header.add(getTtsPreferredEngineHeader());
        header.add(mTtsLanguageLayoutGetter);
        if (mCurrentEngine != null) {
            final Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(mCurrentEngine);

            header.add(new Layout.Action.Builder(res, intent)
                    .title(R.string.system_install_voice_data)
                    .build());
        }
        header.add(getTtsRateHeader());
        header.add(new Layout.Action.Builder(res, ACTION_PLAY_SAMPLE)
                .title(R.string.system_play_sample)
                .build());
        return header;
    }

    private Layout.Header getTtsPreferredEngineHeader() {
        mEngineInfos.clear();
        mEngineInfos.addAll(mEnginesHelper.getEngines());
        final Layout.SelectionGroup.Builder engineBuilder =
                new Layout.SelectionGroup.Builder(mEngineInfos.size());
        int index = 0;
        for (final EngineInfo engineInfo : mEngineInfos) {
            final int action = ACTION_TTS_ENGINE_BASE + index++;
            engineBuilder.add(engineInfo.label, null, action);
            if (TextUtils.equals(mCurrentEngine, engineInfo.name)) {
                engineBuilder.select(action);
            }
        }

        return new Layout.Header.Builder(getResources())
            .title(R.string.system_preferred_engine)
            .build()
            .setSelectionGroup(engineBuilder.build());
    }

    private class TtsLanguageLayoutGetter extends Layout.LayoutGetter {

        @Override
        public Layout get() {
            if (mVoiceCheckData == null) {
                return new Layout();
            }

            final Layout.SelectionGroup.Builder languageBuilder =
                    new Layout.SelectionGroup.Builder();

            final ArrayList<String> available = mVoiceCheckData.getStringArrayListExtra(
                    TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
            if (available != null) {
                final Map<String, Locale> langMap = new ArrayMap<>(available.size());
                for (final String lang : available) {
                    final Locale locale = mEnginesHelper.parseLocaleString(lang);
                    if (locale != null) {
                        langMap.put(locale.getDisplayName(), locale);
                    }
                }

                final List<String> languages = new ArrayList<>(langMap.keySet());

                Collections.sort(languages, new Comparator<String>() {
                    @Override
                    public int compare(String lhs, String rhs) {
                        return lhs.compareToIgnoreCase(rhs);
                    }
                });

                final Locale currentLocale = mEnginesHelper.getLocalePrefForEngine(mCurrentEngine);
                mEngineLocales.clear();
                mEngineLocales.ensureCapacity(languages.size());
                int index = 0;
                for (final String langName : languages) {
                    final int action = ACTION_TTS_LANGUAGE_BASE + index++;
                    final Locale locale = langMap.get(langName);
                    mEngineLocales.add(locale);
                    languageBuilder.add(langName, null, action);
                    if (Objects.equals(currentLocale, locale)) {
                        languageBuilder.select(action);
                    }
                }
            }
            final Resources res = getResources();
            final Locale locale = mTts.getLanguage();
            if (locale != null) {
                return new Layout().add(new Layout.Header.Builder(res)
                        .title(R.string.system_language)
                        .description(locale.getDisplayName())
                        .build()
                        .setSelectionGroup(languageBuilder.build()));
            } else {
                return new Layout().add(new Layout.Header.Builder(res)
                        .title(R.string.system_language)
                        .build()
                        .setSelectionGroup(languageBuilder.build()));
            }
        }
    }

    private Layout.Header getTtsRateHeader() {
        final int selectedRateAction = Settings.Secure.getInt(getContentResolver(),
                TTS_DEFAULT_RATE, TTS_RATE_NORMAL) + ACTION_TTS_RATE_BASE;
        return new Layout.Header.Builder(getResources())
                .title(R.string.system_speech_rate)
                .build()
                .setSelectionGroup(new Layout.SelectionGroup.Builder()
                        .add(getString(R.string.tts_rate_very_slow), null,
                                ACTION_TTS_RATE_VERY_SLOW)
                        .add(getString(R.string.tts_rate_slow), null,
                                ACTION_TTS_RATE_SLOW)
                        .add(getString(R.string.tts_rate_normal), null,
                                ACTION_TTS_RATE_NORMAL)
                        .add(getString(R.string.tts_rate_fast), null,
                                ACTION_TTS_RATE_FAST)
                        .add(getString(R.string.tts_rate_very_fast), null,
                                ACTION_TTS_RATE_VERY_FAST)
                        .select(selectedRateAction)
                        .build());
    }

    private ComponentName getSettingsForService(@NonNull ComponentName comp) {
        final List<AccessibilityServiceInfo> installedServiceInfos = AccessibilityManager
                .getInstance(this).getInstalledAccessibilityServiceList();

        for (AccessibilityServiceInfo accInfo : installedServiceInfos) {
            final ServiceInfo serviceInfo = accInfo.getResolveInfo().serviceInfo;
            if (serviceInfo.packageName.equals(comp.getPackageName()) &&
                    serviceInfo.name.equals(comp.getClassName())) {
                final String settingsClassName = accInfo.getSettingsActivityName();
                if (!TextUtils.isEmpty(settingsClassName)) {
                    return new ComponentName(comp.getPackageName(), settingsClassName);
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private Set<ComponentName> getEnabledServicesFromSettings() {
        String enabledServicesSetting = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) {
            enabledServicesSetting = "";
        }
        Set<ComponentName> enabledServices = new HashSet<>();
        SimpleStringSplitter colonSplitter = new SimpleStringSplitter(
                ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(
                    componentNameString);
            if (enabledService != null) {
                enabledServices.add(enabledService);
            }
        }
        return enabledServices;
    }

    private void updateDefaultEngine(String engine) {
        if (DEBUG) {
            Log.d(TAG, "Updating default synth to : " + engine);
        }

        // TODO Disable the "play sample text" preference and the speech
        // rate preference while the engine is being swapped.

        // Keep track of the previous engine that was being used. So that
        // we can reuse the previous engine.
        //
        // Note that if TextToSpeech#getCurrentEngine is not null, it means at
        // the very least that we successfully bound to the engine service.
        mPreviousEngine = mTts.getCurrentEngine();

        // Shut down the existing TTS engine.
        try {
            mTts.shutdown();
            mTts = null;
        } catch (Exception e) {
            Log.e(TAG, "Error shutting down TTS engine" + e);
        }

        // Connect to the new TTS engine.
        // #onUpdateEngine (below) is called when the app binds successfully to the engine.
        if (DEBUG) {
            Log.d(TAG, "Updating engine : Attempting to connect to engine: " + engine);
        }
        mTts = new TextToSpeech(getApplicationContext(), mUpdateListener, engine);
        setTtsUtteranceProgressListener();
    }

    @Override
    public void onActionClicked(Layout.Action action) {
        if (action.getIntent() != null) {
            startActivity(action.getIntent());
            return;
        }
        final int actionId = action.getId();
        final int category = actionId & ACTION_BASE_MASK;
        switch (category) {
            case ACTION_SERVICE_ENABLE_BASE:
                handleServiceClick(actionId & ~ACTION_BASE_MASK, true);
                break;
            case ACTION_SERVICE_DISABLE_BASE:
                handleServiceClick(actionId & ~ACTION_BASE_MASK, false);
                break;
            case ACTION_SPEAK_PASSWORD_BASE:
                handleSpeakPasswordClick(actionId);
                break;
            case ACTION_TTS_BASE:
                handleTtsClick(actionId);
                break;
            case ACTION_TTS_ENGINE_BASE:
                handleTtsEngineClick(actionId & ~ACTION_BASE_MASK);
                break;
            case ACTION_TTS_LANGUAGE_BASE:
                handleTtsLanguageClick(actionId & ~ACTION_BASE_MASK);
                break;
            case ACTION_TTS_RATE_BASE:
                handleTtsRateClick(actionId & ~ACTION_BASE_MASK);
                break;
        }
    }

    private void handleServiceClick(int serviceIndex, boolean enable) {
        AccessibilityComponentHolder holder = mAccessibilityComponentHolders.get(serviceIndex);
        final ComponentName componentName = holder.getComponentName();
        final String label = holder.getLabel();
        final boolean currentlyEnabled = holder.isEnabled();

        if (enable == currentlyEnabled) {
            onBackPressed();
            return;
        }

        if (enable) {
            EnableServiceDialogFragment.getInstance(componentName, label)
                    .show(getFragmentManager(), null);
        } else {
            DisableServiceDialogFragment.getInstance(componentName, label)
                    .show(getFragmentManager(), null);
        }
    }

    public static class EnableServiceDialogFragment extends DialogFragment {

        private static final String ARG_COMPONENT_NAME = "componentName";
        private static final String ARG_LABEL = "label";

        public static EnableServiceDialogFragment getInstance(ComponentName componentName,
                String label) {
            final EnableServiceDialogFragment fragment = new EnableServiceDialogFragment();
            final Bundle args = new Bundle(2);
            args.putParcelable(ARG_COMPONENT_NAME, componentName);
            args.putString(ARG_LABEL, label);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String label = getArguments().getString(ARG_LABEL);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.system_accessibility_service_on_confirm_title,
                            label))
                    .setMessage(getString(R.string.system_accessibility_service_on_confirm_desc,
                            label))
                    .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final AccessibilityActivity activity =
                                    (AccessibilityActivity) getActivity();
                            final ComponentName componentName =
                                    getArguments().getParcelable(ARG_COMPONENT_NAME);
                            activity.setAccessibilityServiceState(componentName, true);
                            dismiss();
                            activity.onBackPressed();
                        }
                    })
                    .setNegativeButton(R.string.disagree, null)
                    .create();
        }
    }

    public static class DisableServiceDialogFragment extends DialogFragment {

        private static final String ARG_COMPONENT_NAME = "componentName";
        private static final String ARG_LABEL = "label";

        public static DisableServiceDialogFragment getInstance(ComponentName componentName,
                String label) {
            final DisableServiceDialogFragment fragment = new DisableServiceDialogFragment();
            final Bundle args = new Bundle(2);
            args.putParcelable(ARG_COMPONENT_NAME, componentName);
            args.putString(ARG_LABEL, label);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String label = getArguments().getString(ARG_LABEL);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.system_accessibility_service_off_confirm_title,
                            label))
                    .setMessage(getString(R.string.system_accessibility_service_off_confirm_desc,
                            label))
                    .setPositiveButton(R.string.settings_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final AccessibilityActivity activity =
                                    (AccessibilityActivity) getActivity();
                            final ComponentName componentName =
                                    getArguments().getParcelable(ARG_COMPONENT_NAME);
                            activity.setAccessibilityServiceState(componentName, false);
                            activity.onBackPressed();
                        }
                    })
                    .setNegativeButton(R.string.settings_cancel, null)
                    .create();
        }
    }

    private void handleSpeakPasswordClick(int actionId) {
        switch (actionId) {
            case ACTION_SPEAK_PASSWORD_ENABLE:
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 1);
                break;
            case ACTION_SPEAK_PASSWORD_DISABLE:
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0);
                break;
        }
    }

    private void handleTtsClick(int actionId) {
        switch (actionId) {
            case ACTION_PLAY_SAMPLE:
                getSampleText();
                break;
        }
    }

    private void handleTtsEngineClick(int engineIndex) {
        final EngineInfo info = mEngineInfos.get(engineIndex);
        mCurrentEngine = info.name;
        updateDefaultEngine(info.name);
    }

    private void handleTtsLanguageClick(int languageIndex) {
        updateLanguageTo(mEngineLocales.get(languageIndex));
    }

    private void handleTtsRateClick(int rate) {
        Settings.Secure.putInt(getContentResolver(), TTS_DEFAULT_RATE, rate);
    }

    private Set<ComponentName> getInstalledServices() {
        final Set<ComponentName> installedServices = new HashSet<>();
        installedServices.clear();

        final List<AccessibilityServiceInfo> installedServiceInfos =
                AccessibilityManager.getInstance(this)
                        .getInstalledAccessibilityServiceList();
        if (installedServiceInfos == null) {
            return installedServices;
        }

        for (final AccessibilityServiceInfo info : installedServiceInfos) {
            final ResolveInfo resolveInfo = info.getResolveInfo();
            final ComponentName installedService = new ComponentName(
                    resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            installedServices.add(installedService);
        }
        return installedServices;
    }

    private void setAccessibilityServiceState(ComponentName toggledService, boolean enabled) {
        // Parse the enabled services.
        final Set<ComponentName> enabledServices = getEnabledServicesFromSettings();

        // Determine enabled services and accessibility state.
        boolean accessibilityEnabled = false;
        if (enabled) {
            enabledServices.add(toggledService);
            // Enabling at least one service enables accessibility.
            accessibilityEnabled = true;
        } else {
            enabledServices.remove(toggledService);
            // Check how many enabled and installed services are present.
            final Set<ComponentName> installedServices = getInstalledServices();
            for (ComponentName enabledService : enabledServices) {
                if (installedServices.contains(enabledService)) {
                    // Disabling the last service disables accessibility.
                    accessibilityEnabled = true;
                    break;
                }
            }
        }

        // Update the enabled services setting.
        final StringBuilder enabledServicesBuilder = new StringBuilder();
        // Keep the enabled services even if they are not installed since we
        // have no way to know whether the application restore process has
        // completed. In general the system should be responsible for the
        // clean up not settings.
        for (ComponentName enabledService : enabledServices) {
            enabledServicesBuilder.append(enabledService.flattenToString());
            enabledServicesBuilder.append(ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR);
        }
        final int enabledServicesBuilderLength = enabledServicesBuilder.length();
        if (enabledServicesBuilderLength > 0) {
            enabledServicesBuilder.deleteCharAt(enabledServicesBuilderLength - 1);
        }
        Settings.Secure.putString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabledServicesBuilder.toString());

        // Update accessibility enabled.
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, accessibilityEnabled ? 1 : 0);

        for (final AccessibilityComponentHolder holder : mAccessibilityComponentHolders) {
            if (holder.getComponentName().equals(toggledService)) {
                holder.updateComponentState(enabled);
            }
        }
    }

    /*
     * Check whether the voice data for the engine is ok.
     */
    private void checkVoiceData(String engine) {
        if (TextUtils.equals(mVoiceCheckEngine, engine)) {
            return;
        }
        mVoiceCheckEngine = engine;
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        intent.setPackage(engine);
        try {
            if (DEBUG) {
                Log.d(TAG, "Updating engine: Checking voice data: " + intent.toUri(0));
            }
            startActivityForResult(intent, VOICE_DATA_INTEGRITY_CHECK);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to check TTS data, no activity found for " + intent + ")");
        }
    }

    /**
     * Called when the TTS engine is initialized.
     */
    private void onInitEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DEBUG) {
                Log.d(TAG, "TTS engine for settings screen initialized.");
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "TTS engine for settings screen failed to initialize successfully.");
            }
        }
    }

    /*
     * We have now bound to the TTS engine the user requested. We will
     * attempt to check voice data for the engine if we successfully bound to it,
     * or revert to the previous engine if we didn't.
     */
    private void onUpdateEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DEBUG) {
                Log.d(TAG, "Updating engine: Successfully bound to the engine: " +
                        mTts.getCurrentEngine());
            }
            checkVoiceData(mTts.getCurrentEngine());
        } else {
            if (DEBUG) {
                Log.d(TAG, "Updating engine: Failed to bind to engine, reverting.");
            }
            if (mPreviousEngine != null) {
                // This is guaranteed to at least bind, since mPreviousEngine
                // would be
                // null if the previous bind to this engine failed.
                mTts = new TextToSpeech(getApplicationContext(), mInitListener,
                        mPreviousEngine);
                setTtsUtteranceProgressListener();
            }
            mPreviousEngine = null;
        }
        onBackPressed();
    }

    private void setTtsUtteranceProgressListener() {
        if (mTts == null) {
            return;
        }
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
            public void onStart(String utteranceId) {
            }

                @Override
            public void onDone(String utteranceId) {
            }

                @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "Error while trying to synthesize sample text");
            }
        });
    }

    private void updateLanguageTo(Locale locale) {
        mEnginesHelper.updateLocalePrefForEngine(mCurrentEngine, locale);
        if (mCurrentEngine.equals(mTts.getCurrentEngine())) {
            // Null locale means "use system default"
            mTts.setLanguage((locale != null) ? locale : Locale.getDefault());
        }
    }

    /**
     * Called when voice data integrity check returns
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SAMPLE_TEXT) {
            onSampleTextReceived(resultCode, data);
        } else if (requestCode == VOICE_DATA_INTEGRITY_CHECK) {
            onVoiceDataIntegrityCheckDone(data);
        }
    }

    /**
     * Ask the current default engine to return a string of sample text to be
     * spoken to the user.
     */
    private void getSampleText() {
        String currentEngine = mTts.getCurrentEngine();

        if (TextUtils.isEmpty(currentEngine))
            currentEngine = mTts.getDefaultEngine();

        Locale defaultLocale = mTts.getDefaultLanguage();
        if (defaultLocale == null) {
            Log.e(TAG, "Failed to get default language from engine " + currentEngine);
            return;
        }
        mTts.setLanguage(defaultLocale);

        // TODO: This is currently a hidden private API. The intent extras
        // and the intent action should be made public if we intend to make this
        // a public API. We fall back to using a canned set of strings if this
        // doesn't work.
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT);

        intent.putExtra("language", defaultLocale.getLanguage());
        intent.putExtra("country", defaultLocale.getCountry());
        intent.putExtra("variant", defaultLocale.getVariant());
        intent.setPackage(currentEngine);

        try {
            if (DEBUG) {
                Log.d(TAG, "Getting sample text: " + intent.toUri(0));
            }
            startActivityForResult(intent, GET_SAMPLE_TEXT);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to get sample text, no activity found for " + intent + ")");
        }
    }

    private String getDefaultSampleString() {
        if (mTts != null && mTts.getLanguage() != null) {
            final String currentLang = mTts.getLanguage().getISO3Language();
            final Resources res = getResources();
            final String[] strings = res.getStringArray(R.array.tts_demo_strings);
            final String[] langs = res.getStringArray(R.array.tts_demo_string_langs);

            for (int i = 0; i < strings.length; ++i) {
                if (langs[i].equals(currentLang)) {
                    return strings[i];
                }
            }
        }
        return null;
    }

    private void onSampleTextReceived(int resultCode, Intent data) {
        String sample = getDefaultSampleString();

        if (resultCode == TextToSpeech.LANG_AVAILABLE && data != null) {
            if (data.getStringExtra("sampleText") != null) {
                sample = data.getStringExtra("sampleText");
            }
            if (DEBUG) {
                Log.d(TAG, "Got sample text: " + sample);
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, "Using default sample text :" + sample);
            }
        }

        if (sample != null && mTts != null) {
            // The engine is guaranteed to have been initialized here
            // because this preference is not enabled otherwise.

            final boolean networkRequired = isNetworkRequiredForSynthesis();
            if (!networkRequired ||
                    (mTts.isLanguageAvailable(mTts.getLanguage())
                            >= TextToSpeech.LANG_AVAILABLE)) {
                mTts.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "Sample");
            } else {
                Log.w(TAG, "Network required for sample synthesis for requested language");
                // TODO displayNetworkAlert();
            }
        } else {
            // TODO: Display an error here to the user.
            Log.e(TAG, "Did not have a sample string for the requested language");
        }
    }

    private boolean isNetworkRequiredForSynthesis() {
        Set<String> features = mTts.getFeatures(mTts.getLanguage());
        return features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) &&
                !features.contains(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
    }

    /*
     * Called when the voice data check is complete.
     */
    private void onVoiceDataIntegrityCheckDone(Intent data) {
        final String engine = mTts.getCurrentEngine();

        if (engine == null) {
            Log.e(TAG, "Voice data check complete, but no engine bound");
            return;
        }

        if (data == null) {
            Log.e(TAG, "Engine failed voice data integrity check (null return)" +
                    mTts.getCurrentEngine());
            return;
        }

        Settings.Secure.putString(getContentResolver(), TTS_DEFAULT_SYNTH, engine);

        mVoiceCheckData = data;

        mTtsLanguageLayoutGetter.refreshView();
    }

    private class AccessibilityComponentHolder {
        final ComponentName mComponentName;
        final Layout.MutableBooleanGetter mEnabledGetter;
        final Layout.MutableBooleanGetter mDisabledGetter;
        final Layout.StringGetter mStateStringGetter;
        final String mLabel;
        boolean mEnabled;

        public AccessibilityComponentHolder(ComponentName componentName, String label,
                boolean enabled) {
            mComponentName = componentName;
            mEnabledGetter = new Layout.MutableBooleanGetter(enabled);
            mDisabledGetter = new Layout.MutableBooleanGetter(!enabled);
            mStateStringGetter = new Layout.StringGetter() {
                @Override
                public String get() {
                    return getString(mEnabled ? R.string.settings_on : R.string.settings_off);
                }
            };
            mLabel = label;
            mEnabled = enabled;
        }

        public ComponentName getComponentName() {
            return mComponentName;
        }

        public Layout.MutableBooleanGetter getEnabledGetter() {
            return mEnabledGetter;
        }

        public Layout.MutableBooleanGetter getDisabledGetter() {
            return mDisabledGetter;
        }

        public Layout.StringGetter getStateStringGetter() {
            return mStateStringGetter;
        }

        public String getLabel() {
            return mLabel;
        }

        public boolean isEnabled() {
            return mEnabled;
        }

        public void updateComponentState(boolean enabled) {
            mEnabled = enabled;
            mStateStringGetter.refreshView();
            mEnabledGetter.set(enabled);
            mDisabledGetter.set(!enabled);
        }
    }
}

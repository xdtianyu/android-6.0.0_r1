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

package com.android.tv.settings.device.privacy;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.backup.IBackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;

import com.android.tv.settings.ActionBehavior;
import com.android.tv.settings.ActionKey;
import com.android.tv.settings.R;
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.dialog.old.ActionAdapter;
import com.android.tv.settings.dialog.old.ActionFragment;
import com.android.tv.settings.dialog.old.ContentFragment;
import com.android.tv.settings.dialog.old.DialogActivity;
import com.android.tv.settings.util.IntentUtils;

import java.util.ArrayList;

/**
 * Activity that allows enabling and disabling of backup and restore functions.
 */
public class PrivacyActivity extends DialogActivity
        implements ActionAdapter.Listener, DialogInterface.OnClickListener {

    private static final int DIALOG_ERASE_BACKUP = 2;

    /**
     * Support for shutdown-after-reset. If our launch intent has a true value for
     * the boolean extra under the following key, then include it in the intent we
     * use to trigger a factory reset. This will cause us to shut down instead of
     * restart after the reset.
     */
    private static final String SHUTDOWN_INTENT_EXTRA = "shutdown";

    private IBackupManager mBackupManager;
    private ContentFragment mContentFragment;
    private ActionFragment mActionFragment;
    private Dialog mConfirmDialog;
    private int mDialogType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        // TODO implement backup and restore (b/10414565), then:
        // NOTE1: Don't disable backups in SetupCompletedFragment under
        // SetupWraith.
        // NOTE2: Do no call finish() in onFactoryReset() upon CANCEL.
        // NOTE3: Set breadcrumb to R.string.device_backup_restore for
        // createFactoryResetContentFragment().
        // NOTE4: Below call
        // createMainMenuContentFragment() and getMainActions()
        // instead of
        // createFactoryResetContentFragment() and getFactoryResetActions().
        // NOTE5: Change the backup_restore preference under res/xml/device to
        // device_backup_restore.
        mContentFragment = createFactoryResetContentFragment();
        mActionFragment = ActionFragment.newInstance(getFactoryResetActions());
        setContentAndActionFragments(mContentFragment, mActionFragment);
    }

    @Override
    public void onStop() {
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
        }
        mConfirmDialog = null;
        mDialogType = 0;
        super.onStop();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mDialogType == DIALOG_ERASE_BACKUP) {
                try {
                    mBackupManager.setBackupEnabled(false);
                } catch (RemoteException e) {
                    // ignore.
                }
                goToMainScreen();
            }
        }
        mDialogType = 0;
    }

    @Override
    public void onActionClicked(Action action) {
        ActionKey<ActionType, ActionBehavior> actionKey = new ActionKey<ActionType, ActionBehavior>(
                ActionType.class, ActionBehavior.class, action.getKey());
        ActionType type = actionKey.getType();
        ActionBehavior behaviour = actionKey.getBehavior();
        switch (actionKey.getType()) {
            case BACKUP_DATA:
                onBackupData(behaviour);
                break;
            case BACKUP_ACCOUNT:
                onBackupAccount(behaviour);
                break;
            case AUTOMATIC_RESTORE:
                onAutomaticRestore(behaviour);
                break;
            case FACTORY_RESET:
                onFactoryReset(behaviour);
                break;
            case FACTORY_RESET_CONFIRM:
                onFactoryResetConfirm(behaviour);
                break;
            default:
                break;
        }
    }

    private void onFactoryReset(ActionBehavior behaviour) {
        switch (behaviour) {
            case INIT:
                setContentAndActionFragments(createFactoryResetContentFragment(),
                        ActionFragment.newInstance(getFactoryResetActions()));
                break;
            case CANCEL:
                // TODO pop from backstack to go back to main menu after backups
                // are implemented (b/10414565).
                finish();
                break;
            default:
                break;
        }
    }

    private void onFactoryResetConfirm(ActionBehavior behaviour) {
        switch (behaviour) {
            case INIT:
                setContentAndActionFragments(ContentFragment.newInstance(
                        ActionType.FACTORY_RESET_CONFIRM.getTitle(getResources()),
                        getString(R.string.device_reset),
                        getString(R.string.confirm_factory_reset_description),
                        R.drawable.ic_settings_backuprestore,
                        getResources().getColor(R.color.icon_background)),
                        ActionFragment.newInstance(getFactoryResetConfirmActions()));
                break;
            case OK:
                if (!ActivityManager.isUserAMonkey()) {
                    Intent resetIntent = new Intent("android.intent.action.MASTER_CLEAR");
                    if (getIntent().getBooleanExtra(SHUTDOWN_INTENT_EXTRA, false)) {
                        resetIntent.putExtra(SHUTDOWN_INTENT_EXTRA, true);
                    }
                    sendBroadcast(resetIntent);
                }
                break;
            case CANCEL:
                getFragmentManager().popBackStack(null, 0);
                getFragmentManager().popBackStack(null, 0);
                break;
            default:
                break;
        }
    }

    private ArrayList<Action> getFactoryResetActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder()
                .key(new ActionKey<ActionType, ActionBehavior>(ActionType.FACTORY_RESET_CONFIRM,
                        ActionBehavior.INIT).getKey())
                .title(getString(R.string.factory_reset_device))
                .build());
        actions.add(new Action.Builder()
                .key(new ActionKey<ActionType, ActionBehavior>(ActionType.FACTORY_RESET,
                        ActionBehavior.CANCEL).getKey())
                .title(getString(R.string.settings_cancel))
                .build());
        return actions;
    }

    private ArrayList<Action> getFactoryResetConfirmActions() {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new Action.Builder()
                .key(new ActionKey<ActionType, ActionBehavior>(ActionType.FACTORY_RESET_CONFIRM,
                        ActionBehavior.OK).getKey())
                .title(getString(R.string.confirm_factory_reset_device))
                .build());
        actions.add(new Action.Builder()
                .key(new ActionKey<ActionType, ActionBehavior>(ActionType.FACTORY_RESET_CONFIRM,
                        ActionBehavior.CANCEL).getKey())
                .title(getString(R.string.settings_cancel))
                .build());
        return actions;
    }

    private void onBackupData(ActionBehavior behaviour) {
        try {
            boolean isOn = mBackupManager.isBackupEnabled();
            switch (behaviour) {
                case INIT:
                    ArrayList<Action> actions = new ArrayList<Action>();
                    actions.add(ActionBehavior.ON.toAction(new ActionKey<
                            ActionType, ActionBehavior>(
                            ActionType.BACKUP_DATA, ActionBehavior.ON).getKey(),
                            getResources(), isOn));
                    actions.add(ActionBehavior.OFF.toAction(new ActionKey<
                            ActionType, ActionBehavior>(
                            ActionType.BACKUP_DATA, ActionBehavior.OFF).getKey(),
                            getResources(), !isOn));
                    setContentAndActionFragments(
                            createSubMenuContentFragment(ActionType.BACKUP_DATA),
                            ActionFragment.newInstance(actions));
                    break;
                case ON:
                    if (!isOn) {
                        try {
                            mBackupManager.setBackupEnabled(true);
                        } catch (RemoteException e) {
                            // ignore.
                        }
                    }
                    goToMainScreen();
                    break;
                case OFF:
                    if (isOn) {
                        mDialogType = DIALOG_ERASE_BACKUP;
                        CharSequence msg = getResources()
                                .getText(R.string.backup_erase_dialog_message);
                        // TODO: DialogFragment?
                        mConfirmDialog = new AlertDialog.Builder(this).setMessage(msg)
                                .setTitle(R.string.backup_erase_dialog_title)
                                .setIconAttribute(android.R.attr.alertDialogIcon)
                                .setPositiveButton(android.R.string.ok, this)
                                .setNegativeButton(android.R.string.cancel, this).show();
                    }
                    goToMainScreen();
                    break;
                default:
                    break;
            }
        } catch (RemoteException e) {
            // ignore.
        }
    }

    private void onBackupAccount(ActionBehavior behaviour) {
        try {
            String transport = mBackupManager.getCurrentTransport();
            Intent configIntent = mBackupManager.getConfigurationIntent(transport);
            if (configIntent != null) {
                IntentUtils.startActivity(this, configIntent);
            }
        } catch (RemoteException e) {
            // ignore.
        }
    }

    private void onAutomaticRestore(ActionBehavior behaviour) {
        boolean isOn = isAutoRestoreEnabled();
        switch (behaviour) {
            case INIT:
                ArrayList<Action> actions = new ArrayList<Action>();
                actions.add(ActionBehavior.ON.toAction(new ActionKey<ActionType, ActionBehavior>(
                        ActionType.AUTOMATIC_RESTORE, ActionBehavior.ON).getKey(), getResources(),
                        isOn));
                actions.add(ActionBehavior.OFF.toAction(new ActionKey<ActionType, ActionBehavior>(
                        ActionType.AUTOMATIC_RESTORE, ActionBehavior.OFF).getKey(), getResources(),
                        !isOn));
                setContentAndActionFragments(
                        createSubMenuContentFragment(ActionType.AUTOMATIC_RESTORE),
                        ActionFragment.newInstance(actions));
                break;
            case ON:
                if (!isOn) {
                    try {
                        mBackupManager.setAutoRestore(true);
                    } catch (RemoteException e) {
                        // ignore.
                    }
                }
                goToMainScreen();
                break;
            case OFF:
                if (isOn) {
                    try {
                        mBackupManager.setAutoRestore(false);
                    } catch (RemoteException e) {
                        // ignore.
                    }
                }
                goToMainScreen();
                break;
            default:
                break;
        }
    }

    private boolean isAutoRestoreEnabled() {
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.BACKUP_AUTO_RESTORE, 1)
                == 1;
    }

    private void goToMainScreen() {
        updateMainScreen();
        getFragmentManager().popBackStack(null, 0);
    }

    private ArrayList<Action> getMainActions() {
        ArrayList<Action> actions = new ArrayList<Action>();

        try {
            boolean isBackupEnabled = mBackupManager.isBackupEnabled();
            String transport = mBackupManager.getCurrentTransport();
            Intent configIntent = mBackupManager.getConfigurationIntent(transport);
            String configSummary = mBackupManager.getDestinationString(transport);

            actions.add(ActionType.BACKUP_DATA.toAction(getResources(),
                    mBackupManager.isBackupEnabled() ? getString(R.string.settings_on)
                            : getString(R.string.settings_off)));
            if (isBackupEnabled && configIntent != null) {
                actions.add(ActionType.BACKUP_ACCOUNT.toAction(
                        getResources(), configSummary == null ? getString(
                                R.string.backup_configure_account_default_summary)
                                : configSummary));
            }
            if (isBackupEnabled) {
                actions.add(ActionType.AUTOMATIC_RESTORE.toAction(
                        getResources(), isAutoRestoreEnabled() ? getString(R.string.settings_on)
                                : getString(R.string.settings_off)));
            }

        } catch (RemoteException re) {
            // no backup manager means no actions.
        }

        actions.add(ActionType.FACTORY_RESET.toAction(getResources(), null));

        return actions;
    }

    private Fragment createSubMenuContentFragment(ActionType type) {
        return ContentFragment.newInstance(type.getTitle(getResources()),
                getString(R.string.device_backup_restore), null,
                R.drawable.ic_settings_backuprestore,
                getResources().getColor(R.color.icon_background));
    }

    private ContentFragment createMainMenuContentFragment() {
        return ContentFragment.newInstance(getString(R.string.device_backup_restore),
                getString(R.string.header_category_device),
                null, R.drawable.ic_settings_backuprestore,
                getResources().getColor(R.color.icon_background));
    }

    private ContentFragment createFactoryResetContentFragment() {
        // TODO Set breadcrumb to R.string.device_backup_restore b/10414565
        return ContentFragment.newInstance(
                ActionType.FACTORY_RESET.getTitle(getResources()),
                getString(R.string.header_category_device),
                getString(R.string.factory_reset_description),
                R.drawable.ic_settings_backuprestore,
                getResources().getColor(R.color.icon_background));
    }

    private void updateMainScreen() {
        ((ActionAdapter) mActionFragment.getAdapter()).setActions(getMainActions());
    }
}

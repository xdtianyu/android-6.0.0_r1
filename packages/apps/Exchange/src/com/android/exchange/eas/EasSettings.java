/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.exchange.eas;

import com.android.exchange.EasResponse;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.SettingsParser;
import com.android.exchange.adapter.Tags;

import org.apache.http.HttpEntity;

import java.io.IOException;

/**
 * Performs an Exchange Settings request to the server to communicate our device information.
 * While the settings command can be used for all sorts of things, we currently only use it to
 * notify the server of our device information after a Provision command, and only for certain
 * versions of the protocol (12.1 and 14.0; versions after 14.0 instead specify the device info
 * in the provision command).
 *
 * See http://msdn.microsoft.com/en-us/library/ee202944(v=exchg.80).aspx for details on the Settings
 * command in general.
 * See http://msdn.microsoft.com/en-us/library/gg675476(v=exchg.80).aspx for details on the
 * requirement for communicating device info for some versions of Exchange.
 */
public class EasSettings extends EasOperation {


    /** Result code indicating the Settings command succeeded. */
    private static final int RESULT_OK = 1;

    public EasSettings(final EasOperation parentOperation) {
        super(parentOperation);
    }

    public boolean sendDeviceInformation() {
        return performOperation() == RESULT_OK;
    }

    @Override
    protected String getCommand() {
        return "Settings";
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        final Serializer s = new Serializer();
        s.start(Tags.SETTINGS_SETTINGS);
        addDeviceInformationToSerializer(s);
        s.end().done();
        return makeEntity(s);
    }

    @Override
    protected int handleResponse(final EasResponse response) throws IOException {
        return new SettingsParser(response.getInputStream()).parse()
                ? RESULT_OK : RESULT_OTHER_FAILURE;
    }

}

/*
 * Copyright 2014, The Android Open Source Project
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

package com.android.managedprovisioning;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * This activity shows a web view, which loads the {@link #EXTRA_URL} indicated in the starting
 * intent. By default the user can click on links and load other urls. However, if the
 * {@link EXTRA_ALLOWED_URL_BASE} is set in the starting intent, then only url starting with the
 * allowed url base will be loaded.
 *
 * <p>
 * This activity is currently used by the {@link UserConsentDialog} to display the google support
 * web page about the Device Owner concept.
 * </p>
 */
public class WebActivity extends Activity {
    public static final String EXTRA_URL = "extra_url";

    // Users can only browse urls starting with the base specified by the following extra.
    // If this extra is not used, there are no restrictions on browsable urls.
    public static final String EXTRA_ALLOWED_URL_BASE = "extra_allowed_url_base";

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWebView = new WebView(this);
        final String extraUrl = getIntent().getStringExtra(EXTRA_URL);
        final String extraAllowedUrlBase = getIntent().getStringExtra(EXTRA_ALLOWED_URL_BASE);
        if (extraUrl == null) {
            ProvisionLogger.loge("No url provided to WebActivity.");
            finish();
            return;
        }
        mWebView.loadUrl(extraUrl);
        mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (extraAllowedUrlBase != null && url.startsWith(extraAllowedUrlBase)) {
                        view.loadUrl(url);
                    }
                    return true;
                }
            });

        this.setContentView(mWebView);
    }
}
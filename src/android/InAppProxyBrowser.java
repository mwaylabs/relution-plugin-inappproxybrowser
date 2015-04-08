/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.mwaysolutions.relution.inappproxybrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.StringTokenizer;


@SuppressLint("SetJavaScriptEnabled")
public class InAppProxyBrowser extends CordovaPlugin {

    private static final String     NULL                 = "null";
    protected static final String   LOG_TAG              = "InAppProxyBrowser";
    private static final String     SELF                 = "_self";
    private static final String     SYSTEM               = "_system";
    // private static final String BLANK = "_blank";
    private static final String     EXIT_EVENT           = "exit";
    private static final String     LOCATION             = "location";
    private static final String     ZOOM                 = "zoom";
    private static final String     HIDDEN               = "hidden";
    private static final String     LOAD_START_EVENT     = "loadstart";
    private static final String     LOAD_STOP_EVENT      = "loadstop";
    private static final String     LOAD_ERROR_EVENT     = "loaderror";
    private static final String     CLEAR_ALL_CACHE      = "clearcache";
    private static final String     CLEAR_SESSION_CACHE  = "clearsessioncache";
    private static final String     HARDWARE_BACK_BUTTON = "hardwareback";

    private InAppProxyBrowserDialog dialog;
    private WebView                 inAppWebView;
    private EditText                edittext;
    private CallbackContext         callbackContext;
    private boolean                 showLocationBar      = true;
    private boolean                 showZoomControls     = true;
    private boolean                 openWindowHidden     = false;
    private boolean                 clearAllCache        = false;
    private boolean                 clearSessionCache    = false;
    private boolean                 hadwareBackButton    = true;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action        The action to execute.
     * @param args          JSONArry of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              A PluginResult object with a status and message.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("open")) {
            this.callbackContext = callbackContext;
            final String url = args.getString(0);
            String t = args.optString(1);
            if (t == null || t.equals("") || t.equals(NULL)) {
                t = SELF;
            }
            final String target = t;
            final HashMap<String, Boolean> features = this.parseFeature(args.optString(2));

            Log.d(LOG_TAG, "target = " + target);

            this.cordova.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    String result = "";
                    // SELF
                    if (SELF.equals(target)) {
                        Log.d(LOG_TAG, "in self");
                        /* This code exists for compatibility between 3.x and 4.x versions of Cordova.
                         * Previously the Config class had a static method, isUrlWhitelisted(). That
                         * responsibility has been moved to the plugins, with an aggregating method in
                         * PluginManager.
                         */
                        Boolean shouldAllowNavigation = null;
                        if (url.startsWith("javascript:")) {
                            shouldAllowNavigation = true;
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                final Method iuw = Config.class.getMethod("isUrlWhiteListed", String.class);
                                shouldAllowNavigation = (Boolean) iuw.invoke(null, url);
                            } catch (final NoSuchMethodException e) {
                            } catch (final IllegalAccessException e) {
                            } catch (final InvocationTargetException e) {
                            }
                        }
                        if (shouldAllowNavigation == null) {
                            try {
                                final Method gpm = InAppProxyBrowser.this.webView.getClass().getMethod("getPluginManager");
                                final PluginManager pm = (PluginManager) gpm.invoke(InAppProxyBrowser.this.webView);
                                final Method san = pm.getClass().getMethod("shouldAllowNavigation", String.class);
                                shouldAllowNavigation = (Boolean) san.invoke(pm, url);
                            } catch (final NoSuchMethodException e) {
                            } catch (final IllegalAccessException e) {
                            } catch (final InvocationTargetException e) {
                            }
                        }
                        // load in webview
                        if (Boolean.TRUE.equals(shouldAllowNavigation)) {
                            Log.d(LOG_TAG, "loading in webview");
                            InAppProxyBrowser.this.webView.loadUrl(url);
                        }
                        //Load the dialer
                        else if (url.startsWith(WebView.SCHEME_TEL))
                        {
                            try {
                                Log.d(LOG_TAG, "loading in dialer");
                                final Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse(url));
                                InAppProxyBrowser.this.cordova.getActivity().startActivity(intent);
                            } catch (final android.content.ActivityNotFoundException e) {
                                LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                            }
                        }
                        // load in InAppProxyBrowser
                        else {
                            Log.d(LOG_TAG, "loading in InAppProxyBrowser");
                            result = InAppProxyBrowser.this.showWebPage(url, features);
                        }
                    }
                    // SYSTEM
                    else if (SYSTEM.equals(target)) {
                        Log.d(LOG_TAG, "in system");
                        result = InAppProxyBrowser.this.openExternal(url);
                    }
                    // BLANK - or anything else
                    else {
                        Log.d(LOG_TAG, "in blank");
                        result = InAppProxyBrowser.this.showWebPage(url, features);
                    }

                    final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        }
        else if (action.equals("close")) {
            this.closeDialog();
        }
        else if (action.equals("injectScriptCode")) {
            String jsWrapper = null;
            if (args.getBoolean(1)) {
                jsWrapper = String.format("prompt(JSON.stringify([eval(%%s)]), 'gap-iab://%s')", callbackContext.getCallbackId());
            }
            this.injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectScriptFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format(
                        "(function(d) { var c = d.createElement('script'); c.src = %%s; c.onload = function() { prompt('', 'gap-iab://%s'); }; d.body.appendChild(c); })(document)",
                        callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('script'); c.src = %s; d.body.appendChild(c); })(document)";
            }
            this.injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleCode")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format(
                        "(function(d) { var c = d.createElement('style'); c.innerHTML = %%s; d.body.appendChild(c); prompt('', 'gap-iab://%s');})(document)",
                        callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
            }
            this.injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("injectStyleFile")) {
            String jsWrapper;
            if (args.getBoolean(1)) {
                jsWrapper = String.format(
                        "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %%s; d.head.appendChild(c); prompt('', 'gap-iab://%s');})(document)",
                        callbackContext.getCallbackId());
            } else {
                jsWrapper = "(function(d) { var c = d.createElement('link'); c.rel='stylesheet'; c.type='text/css'; c.href = %s; d.head.appendChild(c); })(document)";
            }
            this.injectDeferredObject(args.getString(0), jsWrapper);
        }
        else if (action.equals("show")) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    InAppProxyBrowser.this.dialog.show();
                }
            });
            final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
        }
        else {
            return false;
        }
        return true;
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        this.closeDialog();
    }

    /**
     * Called by AccelBroker when listener is to be shut down.
     * Stop listener.
     */
    @Override
    public void onDestroy() {
        this.closeDialog();
    }

    /**
     * Inject an object (script or style) into the InAppProxyBrowser WebView.
     *
     * This is a helper method for the inject{Script|Style}{Code|File} API calls, which
     * provides a consistent method for injecting JavaScript code into the document.
     *
     * If a wrapper string is supplied, then the source string will be JSON-encoded (adding
     * quotes) and wrapped using string formatting. (The wrapper string should have a single
     * '%s' marker)
     *
     * @param source      The source object (filename or script/style text) to inject into
     *                    the document.
     * @param jsWrapper   A JavaScript string to wrap the source string in, so that the object
     *                    is properly injected, or null if the source string is JavaScript text
     *                    which should be executed directly.
     */
    private void injectDeferredObject(final String source, final String jsWrapper) {
        String scriptToInject;
        if (jsWrapper != null) {
            final org.json.JSONArray jsonEsc = new org.json.JSONArray();
            jsonEsc.put(source);
            final String jsonRepr = jsonEsc.toString();
            final String jsonSourceString = jsonRepr.substring(1, jsonRepr.length() - 1);
            scriptToInject = String.format(jsWrapper, jsonSourceString);
        } else {
            scriptToInject = source;
        }
        final String finalScriptToInject = scriptToInject;
        this.cordova.getActivity().runOnUiThread(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    // This action will have the side-effect of blurring the currently focused element
                    InAppProxyBrowser.this.inAppWebView.loadUrl("javascript:" + finalScriptToInject);
                } else {
                    InAppProxyBrowser.this.inAppWebView.evaluateJavascript(finalScriptToInject, null);
                }
            }
        });
    }

    /**
     * Put the list of features into a hash map
     *
     * @param optString
     * @return
     */
    private HashMap<String, Boolean> parseFeature(final String optString) {
        if (optString.equals(NULL)) {
            return null;
        } else {
            final HashMap<String, Boolean> map = new HashMap<String, Boolean>();
            final StringTokenizer features = new StringTokenizer(optString, ",");
            StringTokenizer option;
            while (features.hasMoreElements()) {
                option = new StringTokenizer(features.nextToken(), "=");
                if (option.hasMoreElements()) {
                    final String key = option.nextToken();
                    final Boolean value = option.nextToken().equals("no") ? Boolean.FALSE : Boolean.TRUE;
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url           The url to load.
     * @param usePhoneGap   Load url in PhoneGap webview
     * @return              "" if ok, or error message.
     */
    public String openExternal(final String url) {
        try {
            Intent intent = null;
            intent = new Intent(Intent.ACTION_VIEW);
            // Omitting the MIME type for file: URLs causes "No Activity found to handle Intent".
            // Adding the MIME type to http: URLs causes them to not be handled by the downloader.
            final Uri uri = Uri.parse(url);
            if ("file".equals(uri.getScheme())) {
                intent.setDataAndType(uri, this.webView.getResourceApi().getMimeType(uri));
            } else {
                intent.setData(uri);
            }
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, this.cordova.getActivity().getPackageName());
            this.cordova.getActivity().startActivity(intent);
            return "";
        } catch (final android.content.ActivityNotFoundException e) {
            Log.d(LOG_TAG, "InAppProxyBrowser: Error loading url " + url + ":" + e.toString());
            return e.toString();
        }
    }

    /**
     * Closes the dialog
     */
    public void closeDialog() {
        final WebView childView = this.inAppWebView;
        // The JS protects against multiple calls, so this should happen only when
        // closeDialog() is called by other native code.
        if (childView == null) {
            return;
        }
        this.cordova.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                childView.setWebViewClient(new WebViewClient() {

                    // NB: wait for about:blank before dismissing
                    @Override
                    public void onPageFinished(final WebView view, final String url) {
                        if (InAppProxyBrowser.this.dialog != null) {
                            InAppProxyBrowser.this.dialog.dismiss();
                        }
                    }
                });
                // NB: From SDK 19: "If you call methods on WebView from any thread
                // other than your app's UI thread, it can cause unexpected results."
                // http://developer.android.com/guide/webapps/migrating.html#Threads
                childView.loadUrl("about:blank");
            }
        });

        try {
            final JSONObject obj = new JSONObject();
            obj.put("type", EXIT_EVENT);
            this.sendUpdate(obj, false);
        } catch (final JSONException ex) {
            Log.d(LOG_TAG, "Should never happen");
        }
    }

    /**
     * Checks to see if it is possible to go back one page in history, then does so.
     */
    public void goBack() {
        if (this.inAppWebView.canGoBack()) {
            this.inAppWebView.goBack();
        }
    }

    /**
     * Can the web browser go back?
     * @return boolean
     */
    public boolean canGoBack() {
        return this.inAppWebView.canGoBack();
    }

    /**
     * Has the user set the hardware back button to go back
     * @return boolean
     */
    public boolean hardwareBack() {
        return this.hadwareBackButton;
    }

    /**
     * Checks to see if it is possible to go forward one page in history, then does so.
     */
    private void goForward() {
        if (this.inAppWebView.canGoForward()) {
            this.inAppWebView.goForward();
        }
    }

    /**
     * Navigate to the new page
     *
     * @param url to load
     */
    private void navigate(final String url) {
        final InputMethodManager imm = (InputMethodManager) this.cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.edittext.getWindowToken(), 0);

        if (!url.startsWith("http") && !url.startsWith("file:")) {
            this.inAppWebView.loadUrl("http://" + url);
        } else {
            this.inAppWebView.loadUrl(url);
        }
        this.inAppWebView.requestFocus();
    }

    /**
     * Should we show the location bar?
     *
     * @return boolean
     */
    private boolean getShowLocationBar() {
        return this.showLocationBar;
    }

    /**
     * Should we show the zoom controls?
     *
     * @return boolean
     */
    private boolean getShowZoomControls() {
        return this.showZoomControls;
    }

    private InAppProxyBrowser getInAppProxyBrowser() {
        return this;
    }

    /**
     * Display a new browser with the specified URL.
     *
     * @param url           The url to load.
     * @param jsonObject
     */
    public String showWebPage(final String url, final HashMap<String, Boolean> features) {
        // Determine if we should hide the location bar.
        this.showLocationBar = true;
        this.showZoomControls = true;
        this.openWindowHidden = false;
        if (features != null) {
            final Boolean show = features.get(LOCATION);
            if (show != null) {
                this.showLocationBar = show.booleanValue();
            }
            final Boolean zoom = features.get(ZOOM);
            if (zoom != null) {
                this.showZoomControls = zoom.booleanValue();
            }
            final Boolean hidden = features.get(HIDDEN);
            if (hidden != null) {
                this.openWindowHidden = hidden.booleanValue();
            }
            final Boolean hardwareBack = features.get(HARDWARE_BACK_BUTTON);
            if (hardwareBack != null) {
                this.hadwareBackButton = hardwareBack.booleanValue();
            }
            Boolean cache = features.get(CLEAR_ALL_CACHE);
            if (cache != null) {
                this.clearAllCache = cache.booleanValue();
            } else {
                cache = features.get(CLEAR_SESSION_CACHE);
                if (cache != null) {
                    this.clearSessionCache = cache.booleanValue();
                }
            }
        }

        final CordovaWebView thatWebView = this.webView;

        // Create dialog in new thread
        final Runnable runnable = new Runnable() {

            /**
             * Convert our DIP units to Pixels
             *
             * @return int
             */
            private int dpToPixels(final int dipValue) {
                final int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        dipValue,
                        InAppProxyBrowser.this.cordova.getActivity().getResources().getDisplayMetrics()
                        );

                return value;
            }

            @Override
            @SuppressLint("NewApi")
            public void run() {
                // Let's create the main dialog
                InAppProxyBrowser.this.dialog = new InAppProxyBrowserDialog(InAppProxyBrowser.this.cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                InAppProxyBrowser.this.dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                InAppProxyBrowser.this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                InAppProxyBrowser.this.dialog.setCancelable(true);
                InAppProxyBrowser.this.dialog.setInAppProxyBroswer(InAppProxyBrowser.this.getInAppProxyBrowser());

                // Main container layout
                final LinearLayout main = new LinearLayout(InAppProxyBrowser.this.cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                // Toolbar layout
                final RelativeLayout toolbar = new RelativeLayout(InAppProxyBrowser.this.cordova.getActivity());
                //Please, no more black!
                toolbar.setBackgroundColor(android.graphics.Color.LTGRAY);
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, this.dpToPixels(44)));
                toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
                toolbar.setHorizontalGravity(Gravity.LEFT);
                toolbar.setVerticalGravity(Gravity.TOP);

                // Action Button Container layout
                final RelativeLayout actionButtonContainer = new RelativeLayout(InAppProxyBrowser.this.cordova.getActivity());
                actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
                actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
                actionButtonContainer.setId(1);

                // Back button
                final Button back = new Button(InAppProxyBrowser.this.cordova.getActivity());
                final RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
                back.setLayoutParams(backLayoutParams);
                back.setContentDescription("Back Button");
                back.setId(2);
                final Resources activityRes = InAppProxyBrowser.this.cordova.getActivity().getResources();
                final int backResId = activityRes.getIdentifier("ic_action_previous_item", "drawable",
                        InAppProxyBrowser.this.cordova.getActivity().getPackageName());
                final Drawable backIcon = activityRes.getDrawable(backResId);
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
                {
                    back.setBackgroundDrawable(backIcon);
                }
                else
                {
                    back.setBackground(backIcon);
                }
                back.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        InAppProxyBrowser.this.goBack();
                    }
                });

                // Forward button
                final Button forward = new Button(InAppProxyBrowser.this.cordova.getActivity());
                final RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                forwardLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
                forward.setLayoutParams(forwardLayoutParams);
                forward.setContentDescription("Forward Button");
                forward.setId(3);
                final int fwdResId = activityRes.getIdentifier("ic_action_next_item", "drawable", InAppProxyBrowser.this.cordova.getActivity().getPackageName());
                final Drawable fwdIcon = activityRes.getDrawable(fwdResId);
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
                {
                    forward.setBackgroundDrawable(fwdIcon);
                }
                else
                {
                    forward.setBackground(fwdIcon);
                }
                forward.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        InAppProxyBrowser.this.goForward();
                    }
                });

                // Edit Text Box
                InAppProxyBrowser.this.edittext = new EditText(InAppProxyBrowser.this.cordova.getActivity());
                final RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
                textLayoutParams.addRule(RelativeLayout.LEFT_OF, 5);
                InAppProxyBrowser.this.edittext.setLayoutParams(textLayoutParams);
                InAppProxyBrowser.this.edittext.setId(4);
                InAppProxyBrowser.this.edittext.setSingleLine(true);
                InAppProxyBrowser.this.edittext.setText(url);
                InAppProxyBrowser.this.edittext.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                InAppProxyBrowser.this.edittext.setImeOptions(EditorInfo.IME_ACTION_GO);
                InAppProxyBrowser.this.edittext.setInputType(InputType.TYPE_NULL); // Will not except input... Makes the text NON-EDITABLE
                InAppProxyBrowser.this.edittext.setOnKeyListener(new View.OnKeyListener() {

                    @Override
                    public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            InAppProxyBrowser.this.navigate(InAppProxyBrowser.this.edittext.getText().toString());
                            return true;
                        }
                        return false;
                    }
                });

                // Close/Done button
                final Button close = new Button(InAppProxyBrowser.this.cordova.getActivity());
                final RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                close.setLayoutParams(closeLayoutParams);
                forward.setContentDescription("Close Button");
                close.setId(5);
                final int closeResId = activityRes.getIdentifier("ic_action_remove", "drawable", InAppProxyBrowser.this.cordova.getActivity().getPackageName());
                final Drawable closeIcon = activityRes.getDrawable(closeResId);
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
                {
                    close.setBackgroundDrawable(closeIcon);
                }
                else
                {
                    close.setBackground(closeIcon);
                }
                close.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View v) {
                        InAppProxyBrowser.this.closeDialog();
                    }
                });

                // WebView
                InAppProxyBrowser.this.inAppWebView = new WebView(InAppProxyBrowser.this.cordova.getActivity());
                InAppProxyBrowser.this.inAppWebView.setLayoutParams(new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                InAppProxyBrowser.this.inAppWebView.setWebChromeClient(new InAppProxyChromeClient(thatWebView));
                final WebViewClient client = new InAppProxyBrowserClient(thatWebView, InAppProxyBrowser.this.edittext);
                InAppProxyBrowser.this.inAppWebView.setWebViewClient(client);
                final WebSettings settings = InAppProxyBrowser.this.inAppWebView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setBuiltInZoomControls(InAppProxyBrowser.this.getShowZoomControls());
                settings.setPluginState(android.webkit.WebSettings.PluginState.ON);

                //Toggle whether this is enabled or not!
                final Bundle appSettings = InAppProxyBrowser.this.cordova.getActivity().getIntent().getExtras();
                final boolean enableDatabase = appSettings == null ? true : appSettings.getBoolean("InAppBrowserStorageEnabled", true);
                if (enableDatabase) {
                    final String databasePath = InAppProxyBrowser.this.cordova.getActivity().getApplicationContext().getDir("inAppBrowserDB",
                            Context.MODE_PRIVATE).getPath();
                    settings.setDatabasePath(databasePath);
                    settings.setDatabaseEnabled(true);
                }
                settings.setDomStorageEnabled(true);

                if (InAppProxyBrowser.this.clearAllCache) {
                    CookieManager.getInstance().removeAllCookie();
                } else if (InAppProxyBrowser.this.clearSessionCache) {
                    CookieManager.getInstance().removeSessionCookie();
                }

                InAppProxyBrowser.this.inAppWebView.loadUrl(url);
                InAppProxyBrowser.this.inAppWebView.setId(6);
                InAppProxyBrowser.this.inAppWebView.getSettings().setLoadWithOverviewMode(true);
                InAppProxyBrowser.this.inAppWebView.getSettings().setUseWideViewPort(true);
                InAppProxyBrowser.this.inAppWebView.requestFocus();
                InAppProxyBrowser.this.inAppWebView.requestFocusFromTouch();

                // Add the back and forward buttons to our action button container layout
                actionButtonContainer.addView(back);
                actionButtonContainer.addView(forward);

                // Add the views to our toolbar
                toolbar.addView(actionButtonContainer);
                toolbar.addView(InAppProxyBrowser.this.edittext);
                toolbar.addView(close);

                // Don't add the toolbar if its been disabled
                if (InAppProxyBrowser.this.getShowLocationBar()) {
                    // Add our toolbar to our main view/layout
                    main.addView(toolbar);
                }

                // Add our webview to our main view/layout
                main.addView(InAppProxyBrowser.this.inAppWebView);

                final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(InAppProxyBrowser.this.dialog.getWindow().getAttributes());
                lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;

                InAppProxyBrowser.this.dialog.setContentView(main);
                InAppProxyBrowser.this.dialog.show();
                InAppProxyBrowser.this.dialog.getWindow().setAttributes(lp);
                // the goal of openhidden is to load the url and not display it
                // Show() needs to be called to cause the URL to be loaded
                if (InAppProxyBrowser.this.openWindowHidden) {
                    InAppProxyBrowser.this.dialog.hide();
                }
            }
        };
        this.cordova.getActivity().runOnUiThread(runnable);
        return "";
    }

    /**
     * Create a new plugin success result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     */
    private void sendUpdate(final JSONObject obj, final boolean keepCallback) {
        this.sendUpdate(obj, keepCallback, PluginResult.Status.OK);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param obj a JSONObject contain event payload information
     * @param status the status code to return to the JavaScript environment
     */
    private void sendUpdate(final JSONObject obj, final boolean keepCallback, final PluginResult.Status status) {
        if (this.callbackContext != null) {
            final PluginResult result = new PluginResult(status, obj);
            result.setKeepCallback(keepCallback);
            this.callbackContext.sendPluginResult(result);
            if (!keepCallback) {
                this.callbackContext = null;
            }
        }
    }

    /**
     * The webview client receives notifications about appView
     */
    public class InAppProxyBrowserClient extends WebViewClient {

        EditText       edittext;
        CordovaWebView webView;

        /**
         * Constructor.
         *
         * @param mContext
         * @param edittext
         */
        public InAppProxyBrowserClient(final CordovaWebView webView, final EditText mEditText) {
            this.webView = webView;
            this.edittext = mEditText;
        }

        /**
         * Notify the host application that a page has started loading.
         *
         * @param view          The webview initiating the callback.
         * @param url           The url of the page.
         */
        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            String newloc = "";
            if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                newloc = url;
            }
            // If dialing phone (tel:5551212)
            else if (url.startsWith(WebView.SCHEME_TEL)) {
                try {
                    final Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    InAppProxyBrowser.this.cordova.getActivity().startActivity(intent);
                } catch (final android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
                }
            }

            else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:")) {
                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    InAppProxyBrowser.this.cordova.getActivity().startActivity(intent);
                } catch (final android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
                }
            }
            // If sms:5551212?body=This is the message
            else if (url.startsWith("sms:")) {
                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW);

                    // Get address
                    String address = null;
                    final int parmIndex = url.indexOf('?');
                    if (parmIndex == -1) {
                        address = url.substring(4);
                    }
                    else {
                        address = url.substring(4, parmIndex);

                        // If body, then set sms body
                        final Uri uri = Uri.parse(url);
                        final String query = uri.getQuery();
                        if (query != null) {
                            if (query.startsWith("body=")) {
                                intent.putExtra("sms_body", query.substring(5));
                            }
                        }
                    }
                    intent.setData(Uri.parse("sms:" + address));
                    intent.putExtra("address", address);
                    intent.setType("vnd.android-dir/mms-sms");
                    InAppProxyBrowser.this.cordova.getActivity().startActivity(intent);
                } catch (final android.content.ActivityNotFoundException e) {
                    LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
                }
            }
            else {
                newloc = "http://" + url;
            }

            if (!newloc.equals(this.edittext.getText().toString())) {
                this.edittext.setText(newloc);
            }

            try {
                final JSONObject obj = new JSONObject();
                obj.put("type", LOAD_START_EVENT);
                obj.put("url", newloc);

                InAppProxyBrowser.this.sendUpdate(obj, true);
            } catch (final JSONException ex) {
                Log.d(LOG_TAG, "Should never happen");
            }
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            super.onPageFinished(view, url);

            try {
                final JSONObject obj = new JSONObject();
                obj.put("type", LOAD_STOP_EVENT);
                obj.put("url", url);

                InAppProxyBrowser.this.sendUpdate(obj, true);
            } catch (final JSONException ex) {
                Log.d(LOG_TAG, "Should never happen");
            }
        }

        @Override
        public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            try {
                final JSONObject obj = new JSONObject();
                obj.put("type", LOAD_ERROR_EVENT);
                obj.put("url", failingUrl);
                obj.put("code", errorCode);
                obj.put("message", description);

                InAppProxyBrowser.this.sendUpdate(obj, true, PluginResult.Status.ERROR);
            } catch (final JSONException ex) {
                Log.d(LOG_TAG, "Should never happen");
            }
        }
    }
}

package com.burnweb.rnwebview;

import android.annotation.SuppressLint;

import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;

import javax.annotation.Nullable;

class RNWebView extends WebView implements LifecycleEventListener {

    private final EventDispatcher mEventDispatcher;
    private final RNWebViewManager mViewManager;
    ThemedReactContext mContext;
    private String charset = "UTF-8";
    private String baseUrl = "file:///";
    private String injectedJavaScript = null;
    private boolean allowUrlRedirect = false;

    protected class EventWebClient extends WebViewClient {
         @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

            handler.proceed();
        }
        public boolean shouldOverrideUrlLoading(WebView view, String url){

            if (url.contains("hnbcode")){
                WritableMap writableMapP = new WritableNativeMap();
                writableMapP.putString("HiNongbo_URL", url.toString());
                sendTransMisson(mContext, "HinongBao_Android_url", writableMapP);
                return true;
            }
                try {
                if(url.startsWith("weixin://") || url.startsWith("alipays://") ||
                        url.startsWith("mailto://") || url.startsWith("tel://")
                    //其他自定义的scheme
                        ) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    mContext.startActivity(intent);
                    return true;
                }
            } catch (Exception e) { //防止crash (如果手机上没有安装处理某个scheme开头的url的APP, 会导致crash)
                return false;
            }
            view.loadUrl(url);
            Log.e("URL",url.toString());
            if(RNWebView.this.getAllowUrlRedirect()) {
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:

                return false; // then it is not handled by default action
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        public void onPageFinished(WebView view, String url) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward()));
            Log.e("URL====【2】",url.toString());
            if(RNWebView.this.getInjectedJavaScript() != null) {
                view.loadUrl("javascript:(function() {\n" + RNWebView.this.getInjectedJavaScript() + ";\n})();");
            }
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), true, url, view.canGoBack(), view.canGoForward()));
        }
    }
    /**
     * @param reactContext
     * @param eventName    事件名
     * @param params       传惨
     */
    public void sendTransMisson(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }


    protected class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            getModule().showAlert(url, message, result);
            return true;
        }

        // For Android 4.1+
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            getModule().startFileChooserIntent(uploadMsg, acceptType);
        }

        // For Android 5.0+
        @SuppressLint("NewApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            return getModule().startFileChooserIntent(filePathCallback, fileChooserParams.createIntent());
        }
    }

    protected class GeoWebChromeClient extends CustomWebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    public RNWebView(RNWebViewManager viewManager, ThemedReactContext reactContext) {
        super(reactContext);

        mViewManager = viewManager;
        mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
        mContext=reactContext;
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setBuiltInZoomControls(false);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setGeolocationEnabled(false);
        this.getSettings().setPluginState(WebSettings.PluginState.ON);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.getSettings().setLoadsImagesAutomatically(true);
        this.getSettings().setBlockNetworkImage(false);
        this.getSettings().setBlockNetworkLoads(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        this.setWebViewClient(new EventWebClient());
        this.setWebChromeClient(getCustomClient());
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setAllowUrlRedirect(boolean a) {
        this.allowUrlRedirect = a;
    }

    public boolean getAllowUrlRedirect() {
        return this.allowUrlRedirect;
    }

    public void setInjectedJavaScript(String injectedJavaScript) {
        this.injectedJavaScript = injectedJavaScript;
    }

    public String getInjectedJavaScript() {
        return this.injectedJavaScript;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public CustomWebChromeClient getCustomClient() {
        return new CustomWebChromeClient();
    }

    public GeoWebChromeClient getGeoClient() {
        return new GeoWebChromeClient();
    }

    public RNWebViewModule getModule() {
        return mViewManager.getPackage().getModule();
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        destroy();
    }

    @Override
    public void onDetachedFromWindow() {
        this.loadDataWithBaseURL(this.getBaseUrl(), "<html></html>", "text/html", this.getCharset(), null);
        super.onDetachedFromWindow();
    }

}

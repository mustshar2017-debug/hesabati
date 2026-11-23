package com.wadkenana.hisabati;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4101;
    private static final int MEDIA_PERMISSION_REQUEST = 4102;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingWebPermission;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(18, 59, 59));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(18, 59, 59));
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new NativeBridge(), "HisabatiNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("mustshar2017-debug.github.io".equals(uri.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (ActivityNotFoundException ignored) {}
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                    "(()=>{if(!navigator.share){navigator.share=(d={})=>HisabatiNative.share(d.title||'حساباتي',d.text||d.url||'');}" +
                    "window.HisabatiApp=true;})();", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST); }
                catch (ActivityNotFoundException e) { fileCallback = null; return false; }
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                boolean needsCamera = false, needsAudio = false;
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) needsCamera = true;
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) needsAudio = true;
                }
                boolean cameraOk = !needsCamera || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                boolean audioOk = !needsAudio || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                if (cameraOk && audioOk) request.grant(request.getResources());
                else {
                    pendingWebPermission = request;
                    java.util.ArrayList<String> ask = new java.util.ArrayList<>();
                    if (needsCamera && !cameraOk) ask.add(Manifest.permission.CAMERA);
                    if (needsAudio && !audioOk) ask.add(Manifest.permission.RECORD_AUDIO);
                    requestPermissions(ask.toArray(new String[0]), MEDIA_PERMISSION_REQUEST);
                }
            }
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView popup = new WebView(MainActivity.this);
                popup.getSettings().setJavaScriptEnabled(true);
                popup.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                        webView.loadUrl(r.getUrl().toString());
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Hisabati-download");
                ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "بدأ التنزيل", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { Toast.makeText(this, "تعذر تنزيل الملف", Toast.LENGTH_SHORT).show(); }
        });
        if (state == null) webView.loadUrl("https://mustshar2017-debug.github.io/hesabati/?app=android");
        else webView.restoreState(state);
    }

    private class NativeBridge {
        @JavascriptInterface public void share(String title, String text) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, title);
                send.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(send, "مشاركة"));
            });
        }
        @JavascriptInterface public void print() {
            runOnUiThread(() -> {
                try {
                    PrintManager manager = (PrintManager)getSystemService(PRINT_SERVICE);
                    manager.print("كشف حساباتي", webView.createPrintDocumentAdapter("حساباتي"), new PrintAttributes.Builder().build());
                } catch (Exception e) { Toast.makeText(MainActivity.this, "تعذرت الطباعة", Toast.LENGTH_SHORT).show(); }
            });
        }
        @JavascriptInterface public boolean isInternetAvailable() {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        @JavascriptInterface public void openNetworkSettings() {
            runOnUiThread(() -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                try { startActivity(intent); }
                catch (ActivityNotFoundException e) {
                    try { startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)); }
                    catch (ActivityNotFoundException ignored) {
                        Toast.makeText(MainActivity.this, "افتح إعدادات الهاتف ثم فعّل Wi-Fi أو بيانات الهاتف", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == MEDIA_PERMISSION_REQUEST && pendingWebPermission != null) {
            boolean ok = true;
            for (int result : results) if (result != PackageManager.PERMISSION_GRANTED) ok = false;
            if (ok) pendingWebPermission.grant(pendingWebPermission.getResources());
            else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}

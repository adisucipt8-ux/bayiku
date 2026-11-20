package id.adi.bayiku;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_SAVE = 1;
    private static final int REQ_OPEN = 2;
    private static final int REQ_NOTIF = 3;

    private WebView web;
    private boolean pageReady = false;
    private String pendingAct, pendingArg;
    private String pendingJson;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notif.ensureChannels(this);
        applyBars(false);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Aplikasi ini sepenuhnya offline: link luar tidak dibuka di dalam aplikasi.
                return !request.getUrl().toString().startsWith("file:///android_asset/");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                runPendingAction();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                String[] types = params.getAcceptTypes();
                boolean image = types != null && types.length > 0 && types[0] != null && types[0].startsWith("image");
                Intent i;
                if (image) {
                    i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setType("image/*");
                } else {
                    i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.setType("*/*");
                }
                i.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(i, image ? "Pilih foto" : "Pilih file cadangan"), REQ_OPEN);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "Android");
        readAction(getIntent());
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readAction(intent);
        runPendingAction();
    }

    private void readAction(Intent intent) {
        if (intent == null) return;
        String a = intent.getStringExtra("act");
        if (a != null) {
            pendingAct = a;
            pendingArg = intent.getStringExtra("arg");
            intent.removeExtra("act");
        }
    }

    private void runPendingAction() {
        if (!pageReady || pendingAct == null) return;
        String js = "window.nativeAction&&nativeAction(" + JSONObject.quote(pendingAct) + ","
                + JSONObject.quote(pendingArg == null ? "" : pendingArg) + ")";
        pendingAct = null;
        pendingArg = null;
        web.evaluateJavascript(js, null);
    }

    private void applyBars(boolean dark) {
        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor(dark ? "#111917" : "#F4F7F5"));
        w.setNavigationBarColor(Color.parseColor(dark ? "#1A2422" : "#FFFFFF"));
        int flags = 0;
        if (!dark) {
            flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    class Bridge {
        @JavascriptInterface
        public void saveBackup(final String json, final String fileName) {
            runOnUiThread(() -> {
                pendingJson = json;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, fileName);
                try {
                    startActivityForResult(i, REQ_SAVE);
                } catch (Exception e) {
                    web.evaluateJavascript("window.onBackupSaved&&onBackupSaved(false)", null);
                }
            });
        }

        @JavascriptInterface
        public void setTimers(String json) {
            Notif.showTimers(MainActivity.this, json);
        }

        @JavascriptInterface
        public void setReminders(String json) {
            Notif.schedule(MainActivity.this, json);
        }

        @JavascriptInterface
        public void setWidget(String json) {
            Notif.prefs(MainActivity.this).edit().putString("widget", json).apply();
            BayiWidget.refresh(MainActivity.this);
        }

        @JavascriptInterface
        public void setDark(final boolean dark) {
            runOnUiThread(() -> applyBars(dark));
        }

        @JavascriptInterface
        public void requestNotif() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SAVE) {
            boolean ok = false;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingJson != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out != null) {
                        out.write(pendingJson.getBytes(StandardCharsets.UTF_8));
                        ok = true;
                    }
                } catch (Exception ignored) { }
            }
            pendingJson = null;
            web.evaluateJavascript("window.onBackupSaved&&onBackupSaved(" + ok + ")", null);
        } else if (requestCode == REQ_OPEN && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onBack?String(window.onBack()):'false'", value -> {
            if (value == null || !value.contains("true")) MainActivity.super.onBackPressed();
        });
    }
}

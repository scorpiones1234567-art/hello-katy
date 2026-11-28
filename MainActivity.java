package com.iam.musicstudio;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPerm;
    private static final int FILE_REQUEST = 4102;
    private static final int SAVE_REQUEST = 4103;
    private static final int MIC_REQUEST = 4104;
    private OutputStream exportStream;
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger audioImportIds = new AtomicInteger(1);
    private int pendingAudioTarget = -1;

    private class IAMBridge {
        @JavascriptInterface public void openAudioPicker(int target) {
            runOnUiThread(() -> launchAudioPicker(target));
        }
        @JavascriptInterface public void createWav(String name) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/wav");
                intent.putExtra(Intent.EXTRA_TITLE, name);
                try { startActivityForResult(intent, SAVE_REQUEST); }
                catch (Exception e) { notifySave(false, "No se pudo abrir Guardar como"); }
            });
        }
        @JavascriptInterface public void writeWavChunk(String base64) {
            try {
                if (exportStream == null) throw new IllegalStateException("No hay archivo de destino");
                exportStream.write(Base64.decode(base64, Base64.DEFAULT));
            } catch (Exception e) {
                closeExport();
                notifySave(false, "Error escribiendo WAV: " + e.getMessage());
            }
        }
        @JavascriptInterface public void finishWav() {
            try {
                if (exportStream == null) throw new IllegalStateException("No se abrió el archivo");
                exportStream.flush();
                closeExport();
                notifySave(true, "WAV guardado correctamente");
            } catch (Exception e) {
                closeExport();
                notifySave(false, "No se pudo terminar el WAV: " + e.getMessage());
            }
        }
        @JavascriptInterface public void cancelWav() {
            closeExport();
            notifySave(false, "Exportación cancelada");
        }
    }

    private void closeExport() {
        try { if (exportStream != null) exportStream.close(); } catch (Exception ignored) {}
        exportStream = null;
    }

    private void notifySave(boolean ok, String message) {
        if (web != null) {
            String safe = message.replace("\\", "\\\\").replace("'", "\\'");
            web.post(() -> web.evaluateJavascript("window.IAMNativeSaveDone && window.IAMNativeSaveDone(" + ok + ",'" + safe + "')", null));
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setKeepScreenOn(true);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        web.addJavascriptInterface(new IAMBridge(), "IAMAndroid");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Enlace nativo de respaldo: el botón abre Android aunque falle
                // algún controlador JavaScript de la consola.
                String js = "(function(){"
                    + "var b=document.getElementById('op');"
                    + "if(b && !b.dataset.iamNativeBound){"
                    + "b.dataset.iamNativeBound='1';"
                    + "b.addEventListener('click',function(e){"
                    + "e.preventDefault();e.stopImmediatePropagation();"
                    + "if(window.IAMAndroid){window.IAMAndroid.openAudioPicker(-1);}"
                    + "else if(window.IAMNativePickerStatus){window.IAMNativePickerStatus('No se encontró el puente Android');}"
                    + "},true);"
                    + "}"
                    + "document.querySelectorAll('.nm').forEach(function(n){"
                    + "if(n.dataset.iamNativeBound)return;n.dataset.iamNativeBound='1';"
                    + "n.addEventListener('click',function(e){"
                    + "e.preventDefault();e.stopImmediatePropagation();"
                    + "if(window.IAMAndroid){window.IAMAndroid.openAudioPicker(Number(n.dataset.i));}"
                    + "},true);"
                    + "});"
                    + "})();";
                view.evaluateJavascript(js, null);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        pendingPerm = request;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                    } else request.grant(request.getResources());
                });
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                launchAudioPicker(-1);
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void launchAudioPicker(int target) {
        pendingAudioTarget = target;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "audio/*", "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
            "audio/mp4", "audio/aac", "audio/flac", "audio/ogg", "audio/3gpp"
        });
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, FILE_REQUEST); }
        catch (Exception e) {
            sendAudioJs("window.IAMReceiveAudioBatchDone && window.IAMReceiveAudioBatchDone()");
            notifySave(false, "No se pudo abrir la biblioteca de música: " + e.getMessage());
        }
    }

    private void sendAudioJs(String script) {
        if (web != null) web.post(() -> {
            if (web != null) web.evaluateJavascript(script, null);
        });
    }

    private String displayName(Uri uri) {
        String name = "Audio";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0) name = cursor.getString(col);
            }
        } catch (Exception ignored) {
        } finally { if (cursor != null) cursor.close(); }
        return name == null ? "Audio" : name;
    }

    private void importAudioUris(ArrayList<Uri> uris, int target) {
        if (uris == null || uris.isEmpty()) {
            sendAudioJs("window.IAMReceiveAudioBatchStart && window.IAMReceiveAudioBatchStart(0,-1);window.IAMReceiveAudioBatchDone && window.IAMReceiveAudioBatchDone()");
            return;
        }
        final int chosenTarget = target;
        sendAudioJs("window.IAMReceiveAudioBatchStart && window.IAMReceiveAudioBatchStart(" + uris.size() + "," + chosenTarget + ")");
        audioExecutor.execute(() -> {
            for (Uri uri : uris) {
                int id = audioImportIds.getAndIncrement();
                String name = displayName(uri);
                sendAudioJs("window.IAMReceiveAudioStart && window.IAMReceiveAudioStart(" + id + "," + JSONObject.quote(name) + ")");
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IllegalStateException("Android no pudo abrir el archivo");
                    byte[] buffer = new byte[48 * 1024];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        String chunk = Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP);
                        sendAudioJs("window.IAMReceiveAudioChunk && window.IAMReceiveAudioChunk(" + id + ",'" + chunk + "')");
                    }
                    sendAudioJs("window.IAMReceiveAudioDone && window.IAMReceiveAudioDone(" + id + ")");
                } catch (Exception e) {
                    sendAudioJs("window.IAMReceiveAudioError && window.IAMReceiveAudioError(" + id + "," + JSONObject.quote(name + ": " + e.getMessage()) + ")");
                }
            }
            sendAudioJs("window.IAMReceiveAudioBatchDone && window.IAMReceiveAudioBatchDone()");
        });
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == MIC_REQUEST && pendingPerm != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) pendingPerm.grant(pendingPerm.getResources());
            else pendingPerm.deny();
            pendingPerm = null;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST) {
            ArrayList<Uri> selected = new ArrayList<>();
            if (resultCode == RESULT_OK && data != null) {
                ClipData clip = data.getClipData();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        if (uri != null) selected.add(uri);
                    }
                } else if (data.getData() != null) selected.add(data.getData());
                if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                    for (Uri uri : selected) {
                        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                        catch (Exception ignored) { }
                    }
                }
            }
            if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
            importAudioUris(selected, pendingAudioTarget);
            pendingAudioTarget = -1;
        } else if (requestCode == SAVE_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try {
                    exportStream = getContentResolver().openOutputStream(data.getData(), "wt");
                    if (exportStream == null) throw new IllegalStateException("No se pudo abrir el archivo");
                    if (web != null) web.post(() -> web.evaluateJavascript("window.IAMNativeSaveReady && window.IAMNativeSaveReady(true)", null));
                } catch (Exception e) { closeExport(); notifySave(false, "No se pudo crear el WAV: " + e.getMessage()); }
            } else notifySave(false, "Guardado cancelado");
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        closeExport();
        audioExecutor.shutdownNow();
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}

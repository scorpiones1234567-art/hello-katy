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

    private class IAMBridge {
        @JavascriptInterface public void openAudioPicker() {
            runOnUiThread(() -> {
                if (web != null) web.evaluateJavascript(
                    "(function(){var f=document.getElementById('fi');if(f){f.value='';f.click();}})();", null);
            });
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
        web.setWebViewClient(new WebViewClient());
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
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // El selector debe mostrar la biblioteca de audio del teléfono.
                // Usar */* + EXTRA_MIME_TYPES evita que algunos selectores Samsung
                // oculten canciones cuando reciben audio/* directamente.
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "audio/*", "audio/mpeg", "audio/mp3", "audio/wav",
                    "audio/x-wav", "audio/mp4", "audio/aac", "audio/flac",
                    "audio/ogg", "audio/3gpp"
                });
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                    params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try {
                    // Lanzar el selector directamente evita que algunos WebView Samsung
                    // pierdan el resultado al volver desde el chooser envolvente.
                    startActivityForResult(intent, FILE_REQUEST);
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
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
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            if (result == null && resultCode == RESULT_OK && data != null) {
                ArrayList<Uri> selected = new ArrayList<>();
                ClipData clip = data.getClipData();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        if (uri != null) selected.add(uri);
                    }
                } else if (data.getData() != null) {
                    selected.add(data.getData());
                }
                if (!selected.isEmpty()) result = selected.toArray(new Uri[0]);
            }
            if (result != null && data != null &&
                (data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                for (Uri uri : result) {
                    try { getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                    catch (Exception ignored) { /* El permiso temporal sigue siendo válido. */ }
                }
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
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
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}

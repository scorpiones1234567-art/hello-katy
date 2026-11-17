package com.hellokaty;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView web;
    private TextToSpeech tts;
    private SpeechRecognizer recognizer;

    private static final int REQ_AUDIO = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ==========================
        // WEBVIEW
        // ==========================
        web = new WebView(this);

        WebSettings settings = web.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Permitir que el HTML local funcione correctamente
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        settings.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());

        // Conectar Java con JavaScript
        web.addJavascriptInterface(new KatyBridge(), "KatyAndroid");

        setContentView(web);

        // ==========================
        // CARGAR HELLO KATTY
        // ==========================
        web.loadUrl("file:///android_asset/index.html");

        // ==========================
        // VOZ DE KATTY
        // ==========================
        tts = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int result =
                        tts.setLanguage(new Locale("es", "MX"));

                // Voz tipo ardillita
                tts.setPitch(1.75f);
                tts.setSpeechRate(1.12f);

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    tts.setLanguage(new Locale("es", "ES"));
                }
            }
        });
    }

    // ==========================
    // PUENTE JAVA ↔ JAVASCRIPT
    // ==========================
    private class KatyBridge {

        @JavascriptInterface
        public void speak(String text) {

            runOnUiThread(() -> {

                if (tts == null) {
                    return;
                }

                if (text == null) {
                    return;
                }

                String frase = text.trim();

                if (frase.isEmpty()) {
                    return;
                }

                frase = limpiarTexto(frase);

                tts.speak(
                        frase,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "KATTY"
                );
            });
        }

        @JavascriptInterface
        public void listen() {

            runOnUiThread(() -> {
                beginListening();
            });
        }
    }

    // ==========================
    // LIMPIAR TEXTO
    // ==========================
    private String limpiarTexto(String texto) {

        String t = texto;

        t = t.replaceAll("\\s+", " ");

        t = t.replace("🎀", "");
        t = t.replace("💙", "");
        t = t.replace("💗", "");
        t = t.replace("🐱", "");
        t = t.replace("⭐", "");
        t = t.replace("🍰", "");
        t = t.replace("🎮", "");
        t = t.replace("😴", "");
        t = t.replace("🍓", "");
        t = t.replace("🌸", "");
        t = t.replace("🧁", "");
        t = t.replace("✨", "");

        return t.trim();
    }

    // ==========================
    // ESCUCHAR AL USUARIO
    // ==========================
    private void beginListening() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQ_AUDIO
            );

            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            jsError(
                    "El reconocimiento de voz no está disponible."
            );

            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

        recognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(
                new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {

                jsError(
                        "No pude escucharte. Inténtalo otra vez."
                );
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> lista =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (lista != null &&
                    !lista.isEmpty()) {

                    String texto = lista.get(0);

                    web.post(() -> {

                        String js =
                                "onAndroidSpeech(" +
                                org.json.JSONObject.quote(texto) +
                                ")";

                        web.evaluateJavascript(
                                js,
                                null
                        );
                    });
                }
            }

            @Override
            public void onPartialResults(Bundle results) {
            }

            @Override
            public void onEvent(
                    int eventType,
                    Bundle params) {
            }
        });

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "es-MX"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "es-MX"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Habla con Katty"
        );

        recognizer.startListening(intent);
    }

    // ==========================
    // ERROR
    // ==========================
    private void jsError(String mensaje) {

        if (web == null) {
            return;
        }

        web.post(() -> {

            String js =
                    "onAndroidSpeechError(" +
                    org.json.JSONObject.quote(mensaje) +
                    ")";

            web.evaluateJavascript(
                    js,
                    null
            );
        });
    }

    // ==========================
    // PERMISO DEL MICRÓFONO
    // ==========================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQ_AUDIO) {

            if (grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

                beginListening();

            } else {

                jsError(
                        "Necesito permiso de micrófono."
                );
            }
        }
    }

    // ==========================
    // CERRAR
    // ==========================
    @Override
    protected void onDestroy() {

        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (web != null) {
            web.destroy();
            web = null;
        }

        super.onDestroy();
    }

    // ==========================
    // BOTÓN ATRÁS
    // ==========================
    @Override
    public void onBackPressed() {

        if (web != null && web.canGoBack()) {

            web.goBack();

        } else {

            super.onBackPressed();
        }
    }
}

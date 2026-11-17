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
    public void onCreate(Bundle b) {
        super.onCreate(b);

        // ==========================
        // VOZ DE KATTY
        // ==========================
        tts = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                Locale mexico = new Locale("es", "MX");

                int result = tts.setLanguage(mexico);

                // Voz aguda y rápida tipo ardillita
                tts.setPitch(1.75f);
                tts.setSpeechRate(1.12f);

                // Si no existe español de México,
                // usamos español de España.
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    tts.setLanguage(new Locale("es", "ES"));
                }
            }
        });

        // ==========================
        // WEBVIEW
        // ==========================
        web = new WebView(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new KatyBridge(), "KatyAndroid");

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());

        setContentView(web);

        web.loadUrl("file:///android_asset/index.html");
    }

    // ==========================
    // PUENTE ENTRE HTML Y ANDROID
    // ==========================
    private class KatyBridge {

        @JavascriptInterface
        public void speak(String text) {

            runOnUiThread(() -> {

                if (tts == null ||
                    text == null ||
                    text.trim().isEmpty()) {
                    return;
                }

                String frase = limpiarTexto(text);

                // Katty dice la frase completa
                // con voz aguda tipo ardillita.
                tts.speak(
                        frase,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "KATTY_VOICE"
                );
            });
        }

        @JavascriptInterface
        public void listen() {
            runOnUiThread(() -> beginListening());
        }
    }

    // ==========================
    // LIMPIAR TEXTO
    // ==========================
    private String limpiarTexto(String texto) {

        String t = texto.trim();

        // Quitar espacios repetidos
        t = t.replaceAll("\\s+", " ");

        // Cambiar algunos símbolos que pueden
        // confundir al motor de voz.
        t = t.replace("…", ".");
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
    // RECONOCIMIENTO DE VOZ
    // ==========================
    private void beginListening() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_AUDIO
            );

            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            jsError(
                    "El reconocimiento de voz no está disponible en este teléfono."
            );

            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {

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

                if (lista != null && !lista.isEmpty()) {

                    String texto = lista.get(0);

                    // Mandamos la frase completa
                    // al JavaScript de Katty.
                    web.post(() -> {

                        web.evaluateJavascript(
                                "onAndroidSpeech(" +
                                org.json.JSONObject.quote(texto) +
                                ")",
                                null
                        );
                    });
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(
                    int eventType,
                    Bundle params) {
            }
        });

        // ==========================
        // CONFIGURACIÓN DEL ESCUCHAR
        // ==========================
        Intent intent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

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
    // ERROR DE VOZ
    // ==========================
    private void jsError(String mensaje) {

        if (web != null) {

            web.post(() -> {

                web.evaluateJavascript(
                        "onAndroidSpeechError(" +
                        org.json.JSONObject.quote(mensaje) +
                        ")",
                        null
                );
            });
        }
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
                        "Necesito permiso de micrófono para escucharte."
                );
            }
        }
    }

    // ==========================
    // CERRAR LA APP
    // ==========================
    @Override
    public void onDestroy() {

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

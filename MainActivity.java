
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
    private boolean ttsReady = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("es", "MX"));

                if (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED) {

                    tts.setPitch(1.75f);
                    tts.setSpeechRate(1.08f);
                    ttsReady = true;
                }
            }
        });

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

    private class KatyBridge {

        @JavascriptInterface
        public void speak(String text) {
            runOnUiThread(() -> {
                if (tts != null && ttsReady && text != null) {
                    tts.stop();

                    tts.setPitch(1.75f);
                    tts.setSpeechRate(1.08f);

                    tts.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "KATY"
                    );
                }
            });
        }

        @JavascriptInterface
        public void listen() {
            runOnUiThread(() -> beginListening());
        }
    }

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
            jsError("El reconocimiento de voz no está disponible en este teléfono.");
            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {

            public void onReadyForSpeech(Bundle p) {}

            public void onBeginningOfSpeech() {}

            public void onRmsChanged(float r) {}

            public void onBufferReceived(byte[] b) {}

            public void onEndOfSpeech() {}

            public void onError(int e) {
                jsError("No pude escucharte. Inténtalo otra vez.");
            }

            public void onResults(Bundle b) {

                ArrayList<String> a =
                    b.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (a != null && !a.isEmpty()) {

                    String words = a.get(0);

                    web.post(() -> {
                        web.evaluateJavascript(
                            "onAndroidSpeech(" +
                            org.json.JSONObject.quote(words) +
                            ")",
                            null
                        );
                    });
                }
            }

            public void onPartialResults(Bundle b) {}

            public void onEvent(int t, Bundle p) {}
        });

        Intent i = new Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        i.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con Katty");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        recognizer.startListening(i);
    }

    private void jsError(String m) {
        if (web != null) {
            web.post(() -> web.evaluateJavascript(
                "onAndroidSpeechError(" +
                org.json.JSONObject.quote(m) +
                ")",
                null
            ));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int r, String[] p, int[] g) {

        super.onRequestPermissionsResult(r, p, g);

        if (r == REQ_AUDIO) {
            if (g.length > 0 &&
                g[0] == PackageManager.PERMISSION_GRANTED) {

                beginListening();

            } else {
                jsError("Necesito permiso de micrófono para escucharte.");
            }
        }
    }

    @Override
    public void onDestroy() {

        if (recognizer != null) {
            recognizer.destroy();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (web != null) {
            web.destroy();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

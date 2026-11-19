
package com.voicecontrol.ai;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status, result;
    private Button micButton;
    private boolean flashlightOn = false;

    private int bg = Color.rgb(12, 12, 20);
    private int purple = Color.rgb(135, 85, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        tts = new TextToSpeech(this, value -> {
            if (value == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        if (Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            setupRecognizer();
            status.setText("Offline voice recognition ready");
        } else {
            status.setText("Offline speech recognition unavailable. Check your device speech services.");
        }
    }

    private void createInterface() {
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER);
        main.setPadding(25, 35, 25, 25);
        main.setBackgroundColor(bg);

        TextView heading = new TextView(this);
        heading.setText("VOICE CONTROL AI");
        heading.setTextSize(25);
        heading.setTextColor(Color.WHITE);
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(null, 1);

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR PHONE. YOUR VOICE.");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("Initializing offline assistant...");
        status.setTextColor(Color.GREEN);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(10, 35, 10, 20);

        micButton = new Button(this);
        micButton.setText("🎙 TAP TO SPEAK");
        micButton.setTextSize(18);
        micButton.setTextColor(Color.WHITE);
        micButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(purple)
        );
        micButton.setOnClickListener(v -> startListening());

        result = new TextView(this);
        result.setText("Your commands will appear here.");
        result.setTextColor(Color.WHITE);
        result.setTextSize(16);
        result.setGravity(Gravity.CENTER);
        result.setPadding(15, 30, 15, 30);

        TextView examples = new TextView(this);
        examples.setText(
            "TRY THESE COMMANDS\n\n" +
            "• Open WhatsApp\n" +
            "• Open YouTube\n" +
            "• Open TikTok\n" +
            "• Turn on flashlight\n" +
            "• Turn off flashlight\n" +
            "• Volume up\n" +
            "• Volume down\n" +
            "• Open settings"
        );
        examples.setTextColor(Color.LTGRAY);
        examples.setTextSize(15);
        examples.setGravity(Gravity.CENTER);

        main.addView(heading);
        main.addView(subtitle);
        main.addView(status);
        main.addView(micButton);
        main.addView(result);
        main.addView(examples);

        setContentView(main);
    }

    private void setupRecognizer() {
        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
                status.setText("Hearing your voice...");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase(Locale.ROOT);
                    result.setText("You said: " + command);
                    executeCommand(command);
                }

                status.setText("Ready for your next command");
                micButton.setEnabled(true);
            }

            @Override
            public void onError(int error) {
                status.setText("Recognition stopped. Tap to try again.");
                micButton.setEnabled(true);
            }

            @Override public void onRmsChanged(float rms) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (Build.VERSION.SDK_INT < 31 ||
                !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            status.setText("Offline speech service is not available.");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                101
            );
            return;
        }

        if (recognizer == null) {
            recognizer =
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            setupRecognizer();
        }

        Intent intent = new Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_PREFER_OFFLINE, true
        );

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault().toLanguageTag()
        );

        micButton.setEnabled(false);

        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            status.setText("Could not start voice recognition.");
            micButton.setEnabled(true);
        }
    }

    private void executeCommand(String command) {

        if (command.startsWith("hey assistant")) {
            command = command.replace("hey assistant", "").trim();
        }

        if (command.contains("open whatsapp")) {
            openApp("com.whatsapp", "WhatsApp");
        }
        else if (command.contains("open youtube")) {
            openApp("com.google.android.youtube", "YouTube");
        }
        else if (command.contains("open tiktok")) {
            openApp("com.zhiliaoapp.musically", "TikTok");
        }
        else if (command.contains("open settings")) {
            speak("Opening settings");
            startActivity(new Intent(
                android.provider.Settings.ACTION_SETTINGS
            ));
        }
        else if (command.contains("turn on flashlight")) {
            setFlashlight(true);
        }
        else if (command.contains("turn off flashlight")) {
            setFlashlight(false);
        }
        else if (command.contains("volume up")) {
            AudioManager audio =
                (AudioManager) getSystemService(AUDIO_SERVICE);

            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            );

            speak("Volume increased");
        }
        else if (command.contains("volume down")) {
            AudioManager audio =
                (AudioManager) getSystemService(AUDIO_SERVICE);

            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            );

            speak("Volume decreased");
        }
        else {
            speak("Sorry, I do not recognize that command yet.");
        }
    }

    private void openApp(String packageName, String appName) {
        Intent launch = getPackageManager()
            .getLaunchIntentForPackage(packageName);

        if (launch != null) {
            speak("Opening " + appName);
            startActivity(launch);
        } else {
            speak(appName + " is not installed.");
        }
    }

    private void setFlashlight(boolean enabled) {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                102
            );
            return;
        }

        try {
            CameraManager camera =
                (CameraManager) getSystemService(CAMERA_SERVICE);

            String[] cameras = camera.getCameraIdList();

            for (String id : cameras) {
                if (camera.getCameraCharacteristics(id)
                    .get(CameraManager
                    .FLASH_INFO_AVAILABLE) == Boolean.TRUE) {

                    camera.setTorchMode(id, enabled);
                    flashlightOn = enabled;

                    speak(enabled
                        ? "Flashlight turned on"
                        : "Flashlight turned off");
                    return;
                }
            }

            speak("No flashlight found.");

        } catch (Exception e) {
            speak("Flashlight control failed.");
        }
    }

    private void speak(String message) {
        if (tts != null) {
            tts.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "voice_response"
            );
        }

        status.setText(message);
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (requestCode == 101) {
                startListening();
            } else if (requestCode == 102) {
                speak("Permission granted. Please repeat your flashlight command.");
            }

        } else {
            status.setText("Permission denied.");
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
        }

        if (tts != null) {
            tts.shutdown();
        }

        super.onDestroy();
    }
}

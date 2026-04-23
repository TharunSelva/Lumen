package com.energyapp.service;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class TextToSpeechService {

    private Voice voice;
    private VoiceManager voiceManager;

    public TextToSpeechService() {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        voiceManager = VoiceManager.getInstance();
        voice = voiceManager.getVoice("kevin16");

        if (voice != null) {
            voice.allocate();
        } else {
            System.err.println("Warning: Kevin16 voice not found. Screen reader will not be enabled.");
        }

    }

    public void speak(String text) {
        Thread thread = new Thread(() -> {
            if (voice != null) {
                voice.speak(text);
            }
        });
        thread.start();
    }
}

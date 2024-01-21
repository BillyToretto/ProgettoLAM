package com.lam.progetto_lam.Utility.Misurazioni;

import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;

/**
 * Recorder per accedere al microfono e calcolare i valori dei decibel.
 */
public class AudioRecorder {
    private static final String TAG = "DecibelRecorder";
    private MediaRecorder mediaRecorder = null;
    private String file;
    

    public AudioRecorder(String file){
        this.file=file;
        Log.d(TAG, "AudioRecorder: " + file);
    }
    public void start() throws IOException {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(file);
            mediaRecorder.prepare();
            mediaRecorder.start();
            mediaRecorder.getMaxAmplitude();
            Log.d(TAG, "start: metodo start ok!");
        }
    }

    public void stop() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    public double getAmplitude() {
        if (mediaRecorder != null)
            return  mediaRecorder.getMaxAmplitude();
        else
            return 0;

    }
}

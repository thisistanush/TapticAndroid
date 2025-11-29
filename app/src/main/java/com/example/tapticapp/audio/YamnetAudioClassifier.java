package com.example.tapticapp.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * YamNet audio classifier for Android (Java).
 * Records microphone audio and feeds it into the YamNet TensorFlow Lite model.
 */
public class YamnetAudioClassifier {

    private static final String TAG = "YamnetClassifier";
    private static final int SAMPLE_RATE = 16000;
    private static final int WINDOW_SAMPLES = 15600;
    private static final int HOP_SAMPLES = 7800;
    private static final int NUM_CLASSES = 521;

    private final Context context;
    private Interpreter interpreter;
    private String[] labels;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private final float[] audioBuffer = new float[WINDOW_SAMPLES];
    private int bufferFill = 0;
    private Thread recordingThread;

    public interface AudioClassificationCallback {
        void onResult(float[] scores, String[] labels, double level);
    }

    public YamnetAudioClassifier(Context context) {
        this.context = context;
        try {
            MappedByteBuffer modelBuffer = loadModelFile("models/yamnet.tflite");
            interpreter = new Interpreter(modelBuffer);
            labels = loadLabels("models/yamnet_class_map.csv");
            Log.d(TAG, "YamNet model loaded with " + labels.length + " classes");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load YamNet model", e);
        }
    }

    public String[] getLabels() {
        return labels;
    }

    public void startListening(AudioClassificationCallback callback) {
        if (isRecording)
            return;

        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            audioRecord.startRecording();
            isRecording = true;
            bufferFill = 0;

            recordingThread = new Thread(() -> processAudio(callback));
            recordingThread.start();
            Log.d(TAG, "Audio recording started");

        } catch (SecurityException e) {
            Log.e(TAG, "Microphone permission not granted", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio recording", e);
        }
    }

    public void stopListening() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;
        }
        Log.d(TAG, "Audio recording stopped");
    }

    private void processAudio(AudioClassificationCallback callback) {
        byte[] hopBytes = new byte[HOP_SAMPLES * 2]; // 16-bit PCM

        while (isRecording && audioRecord != null) {
            int bytesRead = audioRecord.read(hopBytes, 0, hopBytes.length);
            if (bytesRead != hopBytes.length)
                continue;

            // Convert bytes to floats
            ByteBuffer byteBuffer = ByteBuffer.wrap(hopBytes).order(ByteOrder.LITTLE_ENDIAN);
            float[] hopFloats = new float[HOP_SAMPLES];
            for (int i = 0; i < HOP_SAMPLES; i++) {
                hopFloats[i] = byteBuffer.getShort() / 32768f;
            }

            // Slide window
            if (bufferFill < WINDOW_SAMPLES) {
                int copyCount = Math.min(HOP_SAMPLES, WINDOW_SAMPLES - bufferFill);
                System.arraycopy(hopFloats, 0, audioBuffer, bufferFill, copyCount);
                bufferFill += copyCount;
            } else {
                System.arraycopy(audioBuffer, HOP_SAMPLES, audioBuffer, 0, WINDOW_SAMPLES - HOP_SAMPLES);
                System.arraycopy(hopFloats, 0, audioBuffer, WINDOW_SAMPLES - HOP_SAMPLES, HOP_SAMPLES);
            }

            if (bufferFill < WINDOW_SAMPLES)
                continue;

            // Calculate RMS
            double sumSquares = 0;
            for (float sample : audioBuffer) {
                sumSquares += sample * sample;
            }
            double rms = Math.sqrt(sumSquares / WINDOW_SAMPLES);
            double boostedLevel = Math.min(1.0, Math.max(0.02, Math.pow(rms * 16.0, 0.65)));

            // Run inference
            float[] scores = runInference(audioBuffer);
            callback.onResult(scores, labels, boostedLevel);
        }
    }

    private float[] runInference(float[] audioWindow) {
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(WINDOW_SAMPLES * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (float sample : audioWindow) {
            inputBuffer.putFloat(sample);
        }

        float[][] outputScores = new float[1][NUM_CLASSES];
        if (interpreter != null) {
            interpreter.run(inputBuffer, outputScores);
        }
        return outputScores[0];
    }

    private MappedByteBuffer loadModelFile(String path) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(path);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String[] loadLabels(String path) throws IOException {
        List<String> labelList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(path)))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    labelList.add(parts[2].trim());
                }
            }
        }
        return labelList.toArray(new String[0]);
    }

    public void close() {
        stopListening();
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}

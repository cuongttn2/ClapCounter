package com.sandipbhattacharya.clapcounter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;

public class ClapSequenceDetector {
    private static final int SAMPLE_RATE = 8000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    );

    private final double thresholdRms;     // ngưỡng phát hiện clap
    private final long pauseThresholdMs;   // nếu khoảng giữa 2 clap > giá trị này thì tách đoạn
    private final SequenceListener listener;

    private AudioRecord recorder;
    private boolean running = false;
    private boolean prevBelow = true;
    private List<Long> clapTimestamps = new ArrayList<>();

    public ClapSequenceDetector(double thresholdRms, long pauseThresholdMs, SequenceListener listener) {
        this.thresholdRms      = thresholdRms;
        this.pauseThresholdMs  = pauseThresholdMs;
        this.listener          = listener;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        recorder = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE
        );
        running = true;
        recorder.startRecording();

        new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE];
            try {
                while (running) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    double rms = computeRms(buffer, read);
                    // detect rising edge
                    if (rms > thresholdRms && prevBelow) {
                        clapTimestamps.add(System.currentTimeMillis());
                    }
                    prevBelow = rms <= thresholdRms;
                }
            } catch (Exception e) {
                listener.onError(e);
            } finally {
                recorder.stop();
                recorder.release();
            }
        }).start();
    }

    public void stop() {
        running = false;
        // khi thread dừng, chuyển sang xử lý timestamps
        List<Integer> segments = new ArrayList<>();
        if (!clapTimestamps.isEmpty()) {
            int count = 1;
            for (int i = 1; i < clapTimestamps.size(); i++) {
                long delta = clapTimestamps.get(i) - clapTimestamps.get(i - 1);
                if (delta > pauseThresholdMs) {
                    segments.add(count);
                    count = 1;
                } else {
                    count++;
                }
            }
            segments.add(count);
        }
        listener.onSequence(segments);
    }

    private double computeRms(short[] buffer, int read) {
        double sumSq = 0;
        for (int i = 0; i < read; i++) {
            sumSq += buffer[i] * buffer[i];
        }
        return Math.sqrt(sumSq / read);
    }
}
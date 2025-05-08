package com.sandipbhattacharya.clapcounter;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorseDetector {
    private static final int SAMPLE_RATE = 8000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    );

    private final AudioRecord recorder;
    private final double threshold;      // ngưỡng RMS để coi là “âm thanh”
    private final long dotTimeMs;        // thời gian chuẩn cho dot (ví dụ 200ms)
    private final long dashTimeMs;       // dash = 3 * dotTimeMs
    private volatile boolean running = false;
    private Thread thread;

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public MorseDetector(double threshold, long dotTimeMs) {
        this.threshold = threshold;
        this.dotTimeMs = dotTimeMs;
        this.dashTimeMs = dotTimeMs * 3;
        recorder = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            BUFFER_SIZE
        );
    }

    public interface Listener {
        void onSymbol(char morseChar);
        void onError(Exception e);
    }

    public void start(Listener listener) {
        running = true;
        recorder.startRecording();
        thread = new Thread(() -> {
            short[] buffer = new short[BUFFER_SIZE];
            boolean prevOn = false;
            long stateStart = System.currentTimeMillis();
            List<Long> onDurations = new ArrayList<>();
            List<Long> offDurations = new ArrayList<>();

            try {
                while (running) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    double rms = 0;
                    for (int i = 0; i < read; i++) {
                        rms += buffer[i] * buffer[i];
                    }
                    rms = Math.sqrt(rms / read);

                    boolean isOn = rms > threshold;
                    long now = System.currentTimeMillis();

                    if (isOn != prevOn) {
                        long duration = now - stateStart;
                        if (prevOn) onDurations.add(duration);
                        else offDurations.add(duration);

                        // nếu vừa vừa kết thúc một ON và có OFF lâu > dot*3 (khoảng cách chữ)
                        if (!isOn && duration >= dotTimeMs * 7) {
                            // decode last batch
                            decode(onDurations, listener);
                            onDurations.clear();
                        }
                        prevOn = isOn;
                        stateStart = now;
                    }
                }
            } catch (Exception e) {
                listener.onError(e);
            } finally {
                recorder.stop();
                recorder.release();
            }
        });
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            try { thread.join(); } catch (InterruptedException ignored) {}
        }
    }

    private void decode(List<Long> onDurations, Listener listener) {
        // xây dựng ký tự Morse: dot = duration < (dotTimeMs*2), dash ngược lại
        StringBuilder morse = new StringBuilder();
        for (long d : onDurations) {
            if (d < dotTimeMs * 2) morse.append('.');
            else morse.append('-');
        }
        // mapping Morse -> ký tự Latin
        Character decoded = MORSE_MAP.get(morse.toString());
        if (decoded != null) listener.onSymbol(decoded);
    }

    // Bảng map Morse code cơ bản
    private static final Map<String,Character> MORSE_MAP = new HashMap<>();
    static {
        MORSE_MAP.put(".-", 'A');
        MORSE_MAP.put("-...", 'B');
        // ... tiếp tục cho toàn bộ A–Z, 0–9
    }
}
package com.sandipbhattacharya.clapcounter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity2 extends AppCompatActivity {

    private static final String TAG = "MorseDetector";
    private static final double RMS_THRESHOLD = 3000.0;   // điều chỉnh tuỳ thiết bị
    private static final long DOT_DURATION_MS = 200;      // dot = 200ms, dash = 600ms

    final int RECORD_AUDIO = 0;
    private ImageButton btnStart;
    private Spinner spinDuration;
    private MorseDetector morseDetector;
    private StringBuilder decodedMessage = new StringBuilder();
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.start);
        spinDuration = findViewById(R.id.duration);

        // Thiết lập Spinner với các giá trị 5, 10, 15, 20 giây
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"5", "10", "15", "20"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDuration.setAdapter(adapter);

        btnStart.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
            } else {
                int seconds = Integer.parseInt(
                        spinDuration.getSelectedItem().toString()
                );
                startMorseDetection(seconds);
            }
        });
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startMorseDetection(int durationSeconds) {
        decodedMessage.setLength(0);
        btnStart.setEnabled(false);
        btnStart.setBackgroundResource(R.drawable.mic_on); // đổi icon khi lắng nghe

        morseDetector = new MorseDetector(RMS_THRESHOLD, DOT_DURATION_MS);
        morseDetector.start(new MorseDetector.Listener() {
            @Override
            public void onSymbol(char morseChar) {
                decodedMessage.append(morseChar);
                Log.d(TAG, "Detected symbol: " + morseChar);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error in MorseDetector", e);
                uiHandler.post(() ->
                        Toast.makeText(MainActivity2.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        });

        // Tự động dừng sau N giây và hiển thị kết quả
        uiHandler.postDelayed(() -> {
            morseDetector.stop();
            btnStart.setEnabled(true);
            btnStart.setBackgroundResource(R.drawable.mic_off); // trả icon ban đầu
            Log.d(TAG, "Final decoded message: " + decodedMessage);
            Toast.makeText(MainActivity2.this,
                    "Decoded: " + decodedMessage,
                    Toast.LENGTH_LONG).show();
        }, durationSeconds * 1000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (morseDetector != null) {
            morseDetector.stop();
        }
    }
}

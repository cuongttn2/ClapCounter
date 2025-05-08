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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "ClapSeq";
    private static final double RMS_THRESHOLD = 3000.0; // điều chỉnh
    private static final long PAUSE_THRESHOLD = 500;    // tĩnh lặng >500ms => tách đoạn
    final int RECORD_AUDIO = 0;
    private ImageButton btnStart;
    private Spinner spinDuration;
    private Handler handler = new Handler(Looper.getMainLooper());
    String[] durationItems = {"5", "10", "15", "20"};
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        btnStart = findViewById(R.id.start);
        spinDuration = findViewById(R.id.duration);
        ArrayAdapter<String> spinDurationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, durationItems);
        spinDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDuration.setAdapter(spinDurationAdapter);

        btnStart.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
            } else {
                int secs = Integer.parseInt(spinDuration.getSelectedItem().toString());
                startDetect(secs);
            }
        });
    }

    private void startDetect(int secs) {
        btnStart.setEnabled(false);
        ClapSequenceDetector detector = new ClapSequenceDetector(
                RMS_THRESHOLD,
                PAUSE_THRESHOLD,
                new SequenceListener() {
                    @Override
                    public void onSequence(List<Integer> segments) {
                        Log.d(TAG, "Segments: " + segments);
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity2.this,
                                    "Pattern: " + segments,
                                    Toast.LENGTH_LONG
                            ).show();
                            btnStart.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error", e);
                    }
                }
        );
        detector.start();
        handler.postDelayed(detector::stop, secs * 1000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }
}

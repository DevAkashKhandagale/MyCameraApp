
package com.devakashk.mycameraapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, SensorEventListener {

    private SurfaceView surfaceView;
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private SurfaceHolder surfaceHolder;
    private Button startButton, stopButton;
    private ImageButton showListButton;
    private TextView timerTextView;
    private boolean isRecording = false;
    private File outputFile;
    private int timerSeconds = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    // Sensors for motion detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] accelerationValues = new float[3];
    private boolean isMovingFast = false;

    // Recording limit
    private static final int MAX_RECORD_TIME = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        surfaceView = findViewById(R.id.surfaceView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        showListButton = findViewById(R.id.showListButton);
        timerTextView = findViewById(R.id.timerTextView);
        stopButton.setEnabled(false);  // Disable stop button initially

        // Set up surface holder for camera preview
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        // Set up sensor manager for device movement detection
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Button click listeners
        startButton.setOnClickListener(v -> startRecording());
        stopButton.setOnClickListener(v -> stopRecording());

        showListButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecordedVideoListActivity.class);
            startActivity(intent);
        });

        // Timer Runnable
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timerSeconds++;
                timerTextView.setText("0:" + String.valueOf(timerSeconds));
                if (timerSeconds >= MAX_RECORD_TIME) {
                    stopRecording();  // Stop recording after 30 seconds
                } else {
                    timerHandler.postDelayed(this, 1000);  // Run every second
                }
            }
        };

        // Request camera and storage permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
            }, 100);
        }
    }

    // Starts video recording
    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Set output file
            File outputDir = getExternalFilesDir(null);
            outputFile = new File(outputDir, "video_" + System.currentTimeMillis() + ".mp4");
            mediaRecorder.setOutputFile(outputFile.getPath());

            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            mediaRecorder.prepare();
            mediaRecorder.start();

            // Update UI for recording state
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            isRecording = true;
            timerSeconds = 0;
            timerHandler.postDelayed(timerRunnable, 1000);  // Start timer

            // Register sensor listener to monitor movement
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Stops video recording
    private void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;

            // Reset UI
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            isRecording = false;
            timerHandler.removeCallbacks(timerRunnable);  // Stop timer
            timerTextView.setText("0:00");

            // Unregister sensor listener
            sensorManager.unregisterListener(this);

            // Show message for saving the video
            Toast.makeText(this, "Video saved: " + outputFile.getPath(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // Surface created
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // Surface changed
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Surface destroyed
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerationValues, 0, event.values.length);

            float x = accelerationValues[0];  // Horizontal movement
            float y = accelerationValues[1];  // Vertical movement

            // Threshold values to detect significant movement (tune these values as needed)
            float reverseThreshold = -3.0f;  // Threshold for reverse movement (left to right)
            float forwardThreshold = 3.0f;   // Threshold for correct movement (right to left)
            float speedThreshold = 12.0f;    // Threshold for detecting fast movement

            // Check if the user is moving the device in the reverse direction (left to right)
            if (x > reverseThreshold) {
                showAlert("Please don’t move device in reverse direction while recording.");
            }

            // Check if the user is moving the device too fast
            if (Math.abs(x) > speedThreshold || Math.abs(y) > speedThreshold) {
                if (!isMovingFast) {
                    isMovingFast = true;
                    showAlert("Please move device with slow speed while recording.");
                }
            } else {
                isMovingFast = false;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No implementation needed
    }

    private void showAlert(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Permissions are required to use the camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

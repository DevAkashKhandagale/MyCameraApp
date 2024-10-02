package com.devakashk.mycameraapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecordedVideoListActivity extends AppCompatActivity {

    private List<String> videoPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recorded_video_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            videoPaths = getRecordedVideos();
        }
        VideoListAdapter videoListAdapter = new VideoListAdapter(this, videoPaths);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(videoListAdapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private List<String> getRecordedVideos() {

        ArrayList<String> videoPlayList = new ArrayList<>();

        File directory = getExternalFilesDir(null);  // The directory where videos are saved
        if (directory != null) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.getPath().endsWith(".mp4")) {
                        videoPlayList.add(file.getPath());
                    }
                }

            }
        }
        return videoPlayList;
    }

}
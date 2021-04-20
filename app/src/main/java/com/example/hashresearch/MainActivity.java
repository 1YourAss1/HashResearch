package com.example.hashresearch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private TextView fileNameTextView;

    private static final int RESULT_OPEN_FILE = 1;
    private CurrentFile currentFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileNameTextView = findViewById(R.id.textFileName);

        FloatingActionButton button = findViewById(R.id.chooseFileButton);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, RESULT_OPEN_FILE);
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_OPEN_FILE:
                    currentFile = new CurrentFile(data.getData(), getApplicationContext());
//                    fileImageView.setImageBitmap(currentFile.getImageBitmap());
                    fileNameTextView.setText(String.format("%s (%s)", currentFile.getFileName(), currentFile.getFileSizeFormatted() ));
                    break;
            }
        }
    }



}
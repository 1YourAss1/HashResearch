package com.example.hashresearch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button openFileButton;
    private Button calculateHashSumButton;
    private Button checkIntegrityButton;

    private TextView fileNameTextView;

    private static final int RESULT_OPEN_FILE = 1;
    private static final int RESULT_CHOOSE_DIRECTORY = 2;

    private CurrentFile currentFile;
    private byte[] currentHash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton floatingActionButton = findViewById(R.id.chooseFileButton);
        openFileButton = findViewById(R.id.openFileButton);
        calculateHashSumButton = findViewById(R.id.calculateHashSumButton);
        checkIntegrityButton = findViewById(R.id.checkIntegrityButton);

        floatingActionButton.setOnClickListener(this);
        openFileButton.setOnClickListener(this);
        calculateHashSumButton.setOnClickListener(this);
        checkIntegrityButton.setOnClickListener(this);

        fileNameTextView = findViewById(R.id.textFileName);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.chooseFileButton) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, RESULT_OPEN_FILE);
        } else if (id == R.id.openFileButton) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(currentFile.getUri());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (id == R.id.calculateHashSumButton) {
            HashAsyncTask calcHashAsyncTask = new HashAsyncTask(this, currentFile.getUri(), "GOST3411-2012-256") {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                AlertDialog loadDialog;
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.layout_loading_dialog, null);
                ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

                @Override
                protected void onPreExecute() {
                    progressBar.setMax((int) currentFile.getFileSize());
                    builder.setTitle("Вычисление...");
                    builder.setView(view);
                    builder.setCancelable(false);
                    loadDialog = builder.create();
                    loadDialog.show();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    progressBar.setProgress(values[0]);
                }

                @Override
                protected void onPostExecute(byte[] hash) {
                    loadDialog.dismiss();
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Хеш-сумма успешно посчитана");
                    builder.setMessage(bytesToHex(hash));
                    builder.setPositiveButton("Сохранить", (dialog, which) -> {
                        currentHash = hash;
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        startActivityForResult(intent, RESULT_CHOOSE_DIRECTORY);
                    });
                    builder.setNeutralButton("Поделиться", (dialog, which) -> {
                        try {
                            File tmpFile = new File(getCacheDir(), currentFile.getFileName() + ".hash");
                            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
                            fileOutputStream.write(hash);
                            fileOutputStream.close();
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Uri tmpFileUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID, tmpFile);
                            intent.putExtra(Intent.EXTRA_TEXT, bytesToHex(hash));
                            intent.putExtra(Intent.EXTRA_STREAM, tmpFileUri);
                            intent.setType("*/*");
                            startActivity(Intent.createChooser(intent, "Поделиться хеш-суммой"));
                            tmpFile.deleteOnExit();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    builder.setNegativeButton("Отмена", (dialog, which) -> {});
                    builder.show();
                }
            };
            calcHashAsyncTask.execute();
        } else if (id == R.id.checkIntegrityButton) {

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_OPEN_FILE:
                    currentFile = new CurrentFile(data.getData(), getApplicationContext());
                    fileNameTextView.setText(String.format("%s (%s)", currentFile.getFileName(), currentFile.getFileSizeFormatted() ));
//            fileImageView.setImageBitmap(currentFile.getImageBitmap());

                    openFileButton.setEnabled(true);
                    calculateHashSumButton.setEnabled(true);
                    checkIntegrityButton.setEnabled(true);
                    break;
                case RESULT_CHOOSE_DIRECTORY:
                    Uri uriTree = data.getData();
                    DocumentFile documentTree = DocumentFile.fromTreeUri(this, uriTree);
                    DocumentFile documentFile = documentTree.createFile("*/*", currentFile.getFileName() + ".hash");
                    try {
                        OutputStream outputStream = getContentResolver().openOutputStream(documentFile.getUri());
                        outputStream.write(currentHash);
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("File_tree", e.getMessage());
                    }
                    break;
            }

        }
    }


}
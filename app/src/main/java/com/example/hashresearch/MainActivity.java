package com.example.hashresearch;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String APP_PREFERENCES = "settings";
    SharedPreferences mSettings;

    private String ALGORITHM;

    private Button openFileButton;
    private Button calculateHashSumButton;
    private Button checkIntegrityButton;

    private TextView fileNameTextView;

    private static final int RESULT_OPEN_FILE = 1;
    private static final int RESULT_DIRECTORY_TO_SAVE_HASH = 2;
    private static final int RESULT_DIRECTORY_TO_EXPORT_DB = 3;
    private static final int RESULT_OPEN_HASH = 4;

    private CurrentFile currentFile;
    private byte[] currentHash;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        ALGORITHM = mSettings.getString("ALGORITHM", "GOST3411");

        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

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
    protected void onResume() {
        super.onResume();
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        ALGORITHM = mSettings.getString("ALGORITHM", "GOST3411");
    }

    // Добавить меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    // Обработка меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_export) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, RESULT_DIRECTORY_TO_EXPORT_DB);
        } else if (itemId == R.id.item_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        return true;
    }

    // Обработка нажатий на кнопки
    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Выбор файла
        if (id == R.id.chooseFileButton) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, RESULT_OPEN_FILE);
        // Открытие файла
        } else if (id == R.id.openFileButton) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(currentFile.getUri());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        // Рассчет хеша
        } else if (id == R.id.calculateHashSumButton) {
            new CalculateHashTask().execute();
        // Проверка целостности
        } else if (id == R.id.checkIntegrityButton) {
            // Выбор хеша
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            AlertDialog chooseHashDialog;
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_choose_hash, null);
            final EditText editTextHash = view.findViewById(R.id.editTextHash);
            builder.setTitle("Хеш-сумма");
            builder.setView(view);
            builder.setPositiveButton("OK", (dialog, which) -> {
                currentHash = hex2bytes(editTextHash.getText().toString());
                // Рассчет хеша для сравнения с выбранным
                new CheckIntegrityTask().execute();
            });
            builder.setNeutralButton("Из файла", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, RESULT_OPEN_HASH);
            });
            builder.setNegativeButton("Отмена", (dialog, which) -> {});
            chooseHashDialog = builder.create();
            chooseHashDialog.show();
        }
    }

    // Обработка полученных URI при работе с файловой системой
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                // Открытия файла
                case RESULT_OPEN_FILE:
                    currentFile = new CurrentFile(data.getData(), getApplicationContext());
                    fileNameTextView.setText(String.format("%s (%s)", currentFile.getFileName(), currentFile.getFileSizeFormatted() ));
//            fileImageView.setImageBitmap(currentFile.getImageBitmap());

                    openFileButton.setEnabled(true);
                    calculateHashSumButton.setEnabled(true);
                    checkIntegrityButton.setEnabled(true);
                    break;

                // Сохранение хеша в файл
                case RESULT_DIRECTORY_TO_SAVE_HASH:
                    // Получение пути для сохранения хеша
                    DocumentFile documentHash = DocumentFile.fromTreeUri(this, data.getData()).createFile("*/*", currentFile.getFileName() + ".hash");
                    // Сохранниея файла с хешем по выбранному пути
                    try {
                        OutputStream outputStream = getContentResolver().openOutputStream(documentHash.getUri());
                        outputStream.write(currentHash);
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                // Экспорт базы данных
                case RESULT_DIRECTORY_TO_EXPORT_DB:
                    DocumentFile documentDB = DocumentFile.fromTreeUri(this, data.getData()).createFile("*/*", "myDB.db");
                    try {
                        FileInputStream fileInputStream = new FileInputStream(new File(String.valueOf(this.getDatabasePath("myDB"))));
                        OutputStream outputStream = getContentResolver().openOutputStream(documentDB.getUri());

                        byte[] buffer = new byte[1024];
                        while ((fileInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer);
                        }

                        fileInputStream.close();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                // Проверка хеша из файла с рассчитанным
                case RESULT_OPEN_HASH:
                    // Загрузка хеша из файла
                    try {
                        ByteArrayOutputStream bufferHash = new ByteArrayOutputStream();
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        byte[] buffer = new byte[32];
                        while ((inputStream.read(buffer)) != -1) {
                            bufferHash.write(buffer);
                        }
                        currentHash = bufferHash.toByteArray();
                        // Рассчет хеша для сравнения с выбранным
                        new CheckIntegrityTask().execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
            }
        }

    }

    // Перевод хеша из массива байт в HEX
    public static String bytes2hex(final byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }

    // Перевод хеша из HEX в массив байт
    public static byte[] hex2bytes(final String s) {
        if (s == null) {
            return (new byte[]{});
        }

        if (s.length() % 2 != 0 || s.length() == 0) {
            return (new byte[]{});
        }

        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            try {
                data[i / 2] = (Integer.decode("0x" + s.charAt(i) + s.charAt(i + 1))).byteValue();
            } catch (NumberFormatException e) {
                return (new byte[]{});
            }
        }
        return data;
    }

    // Рассчет хеша в отдельном потоке
    class CalculateHashTask extends AsyncTask<Void, Integer, byte[]> {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        long time;

        @Override
        protected void onPreExecute() {
            progressBar.setMax((int) currentFile.getFileSize());
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            loadDialog = builder.create();
            loadDialog.show();

            time = System.currentTimeMillis();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected byte[] doInBackground(Void... voids) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM, Security.getProvider("SC"));
                InputStream inputStream = getContentResolver().openInputStream(currentFile.getUri());
                int bufferSize = 32;
                byte[] buffer = new byte[bufferSize];
                int counter = 0;
                while ((inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer);
                    counter += bufferSize;
                    publishProgress(counter);
                }
                return messageDigest.digest();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(byte[] hash) {
            time = System.currentTimeMillis() - time;

            new DBHelper(getApplicationContext()).addDataToBD(currentFile, ALGORITHM, (int) time);

            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Хеш-сумма успешно посчитана");
            builder.setMessage(bytes2hex(hash));
            builder.setPositiveButton("Сохранить", (dialog, which) -> {
                currentHash = hash;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, RESULT_DIRECTORY_TO_SAVE_HASH);
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
                    intent.putExtra(Intent.EXTRA_TEXT, bytes2hex(hash));
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
    }

    // Проверка целостности в отдельном потоке
    class CheckIntegrityTask extends AsyncTask<Void, Integer, byte[]> {

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
        protected byte[] doInBackground(Void... voids) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM, Security.getProvider("SC"));
                InputStream inputStream = getContentResolver().openInputStream(currentFile.getUri());
                int bufferSize = 32;
                byte[] buffer = new byte[bufferSize];
                int counter = 0;
                while ((inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer);
                    counter += bufferSize;
                    publishProgress(counter);
                }
                return messageDigest.digest();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(byte[] hash) {
            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Результат проверки");
            if (Arrays.equals(currentHash, hash)) {
                builder.setMessage("Целостность не нарушена");
            } else {
                builder.setMessage("Целостность нарушена");
            }
            builder.setPositiveButton("OK", (dialog, which) -> {});
            builder.show();
        }

    }

    // Работа с БД
    static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table time ("
                    + "_id integer primary key autoincrement,"
                    + "file_name text,"
                    + "size_mb float,"
                    + "algorithm text,"
                    + "time_sec float"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        public void addDataToBD(CurrentFile file, String algorithm, int time) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            Cursor cursor = db.query("time",
                    null,
                    "file_name = ? AND size_mb = ? AND algorithm = ?",
                    new String[] {file.getFileName(), Double.toString((double) file.getFileSize() / 1000000), algorithm},
                    null,
                    null,
                    null,
                    null);
            if (cursor.moveToFirst()) {
                String where = "_id = " + cursor.getInt(cursor.getColumnIndex("_id"));
                double time_from_db = cursor.getDouble(cursor.getColumnIndex("time_sec"));
                cv.put("time_sec", (( (time/1000.0) + time_from_db) / 2.0));
                db.update("time", cv, where, null);
                cursor.close();
            } else {
                cv.put("file_name", file.getFileName());
                cv.put("size_mb", file.getFileSize() / 1000000.0);
                cv.put("algorithm", algorithm);
                cv.put("time_sec", time / 1000.0);
                db.insert("time", null, cv);
            }
        }
    }
}
package com.example.hashresearch;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String APP_PREFERENCES = "settings";
    private SharedPreferences mSettings;

    private static final String GOST3411 = "GOST3411";
    private static final String GOST3411_2012_256 = "GOST3411-2012-256";
    private static final String GOST3411_2012_512 = "GOST3411-2012-512";

    private static final String GOST28147 = "GOST28147";

    private String ALGORITHM_DIGEST, ALGORITHM_CIPHER;

    private Button calculateHashSumButton;
    private Button checkIntegrityButton;
    private Button encryptButton;
    private Button decryptButton;

    private TextView fileNameTextView;

    private static final int RESULT_OPEN_FILE = 1;
    private static final int RESULT_DIRECTORY_TO_EXPORT_DB = 2;
    private static final int RESULT_OPEN_HASH = 3;

    private CurrentFile currentFile;
    private byte[] currentHash;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Security.removeProvider("BC");
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);

        for (Provider provider: Security.getProviders()) {
            Log.d("providers_info", provider.getInfo());
        }

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        ALGORITHM_DIGEST = mSettings.getString("ALGORITHM_DIGEST", GOST3411);
        ALGORITHM_CIPHER = mSettings.getString("ALGORITHM_CIPHER", GOST28147);

        FloatingActionButton floatingActionButton = findViewById(R.id.chooseFileButton);
        calculateHashSumButton = findViewById(R.id.calculateHashSumButton);
        checkIntegrityButton = findViewById(R.id.checkIntegrityButton);
        encryptButton = findViewById(R.id.encryptButton);
        decryptButton = findViewById(R.id.decryptButton);

        floatingActionButton.setOnClickListener(this);
        calculateHashSumButton.setOnClickListener(this);
        checkIntegrityButton.setOnClickListener(this);
        encryptButton.setOnClickListener(this);
        decryptButton.setOnClickListener(this);

        fileNameTextView = findViewById(R.id.textFileName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ALGORITHM_DIGEST = mSettings.getString("ALGORITHM_DIGEST", GOST3411);
        ALGORITHM_CIPHER = mSettings.getString("ALGORITHM_CIPHER", GOST28147);
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
        } else if (itemId == R.id.item_salt) {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[256];
            random.nextBytes(salt);

            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString("SALT", bytes2hex(salt)).apply();
        }
        return true;
    }



    // Обработка нажатий на кнопки
    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Выбор файла
        if (id == R.id.chooseFileButton) {
            Intent intent = new Intent(MainActivity.this, Browser.class);
            startActivityForResult(intent, RESULT_OPEN_FILE);
        // Рассчет имитовставки
        } else if (id == R.id.calculateHashSumButton) {
            if (ALGORITHM_DIGEST.equals(GOST3411) || ALGORITHM_DIGEST.equals(GOST3411_2012_256) || ALGORITHM_DIGEST.equals(GOST3411_2012_512)) {
                new CalculateHashTask().execute();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                AlertDialog enterPasswordDialog;
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.layout_enter_password, null);
                final EditText editTextPassword = view.findViewById(R.id.editTextPassword);
                builder.setTitle("Введите пароль");
                builder.setView(view);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    if (!editTextPassword.getText().toString().equals("")) new CalculateMacTask().execute(editTextPassword.getText().toString());
                });
                builder.setNegativeButton("Отмена", (dialog, which) -> {});
                enterPasswordDialog = builder.create();
                enterPasswordDialog.show();
            }
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
                if (ALGORITHM_DIGEST.equals(GOST3411) || ALGORITHM_DIGEST.equals(GOST3411_2012_256) || ALGORITHM_DIGEST.equals(GOST3411_2012_512)) {
                    new CheckHashTask().execute();
                } else {
                    AlertDialog enterPasswordDialog;
                    View view1 = inflater.inflate(R.layout.layout_enter_password, null);
                    final EditText editTextPassword = view1.findViewById(R.id.editTextPassword);
                    builder.setTitle("Введите пароль");
                    builder.setView(view1);
                    builder.setPositiveButton("OK", (dialog1, which1) -> {
                        if (!editTextPassword.getText().toString().equals("")) {
                            if (!editTextPassword.getText().toString().equals("")) new CheckMACTask().execute(editTextPassword.getText().toString());
                        }
                    });
                    builder.setNegativeButton("Отмена", (dialog1, which1) -> {});
                    enterPasswordDialog = builder.create();
                    enterPasswordDialog.show();
                }
            });
            builder.setNeutralButton("Из файла", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, Browser.class);
                startActivityForResult(intent, RESULT_OPEN_HASH);
            });
            builder.setNegativeButton("Отмена", (dialog, which) -> {});
            chooseHashDialog = builder.create();
            chooseHashDialog.show();

        // Зашифровать файл
        } else if (id == R.id.encryptButton) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            AlertDialog enterPasswordDialog;
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_enter_password, null);
            final EditText editTextPassword = view.findViewById(R.id.editTextPassword);
            builder.setTitle("Введите пароль");
            builder.setView(view);
            builder.setPositiveButton("OK", (dialog, which) -> {
                if (!editTextPassword.getText().toString().equals("")) new EncryptTask().execute(editTextPassword.getText().toString());
            });
            builder.setNegativeButton("Отмена", (dialog, which) -> {});
            enterPasswordDialog = builder.create();
            enterPasswordDialog.show();
        } else if (id == R.id.decryptButton) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            AlertDialog enterPasswordDialog;
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.layout_enter_password, null);
            final EditText editTextPassword = view.findViewById(R.id.editTextPassword);
            builder.setTitle("Введите пароль");
            builder.setView(view);
            builder.setPositiveButton("OK", (dialog, which) -> {
                if (!editTextPassword.getText().toString().equals("")) new DecryptTask().execute(editTextPassword.getText().toString());
            });
            builder.setNegativeButton("Отмена", (dialog, which) -> {});
            enterPasswordDialog = builder.create();
            enterPasswordDialog.show();
        }
    }

    // Обработка полученных URI при работе с файловой системой
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                // Открытия файла
                case RESULT_OPEN_FILE:
                    currentFile = new CurrentFile(data.getStringExtra("file_path"));
                    fileNameTextView.setText(String.format("%s (%s)", currentFile.getName(), currentFile.getFileSizeFormatted()));

                    calculateHashSumButton.setEnabled(true);
                    checkIntegrityButton.setEnabled(true);
                    encryptButton.setEnabled(true);
                    decryptButton.setEnabled(true);
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
                        InputStream inputStream = new FileInputStream(new CurrentFile(data.getStringExtra("file_path")));
                        byte[] buffer = new byte[32];
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            bufferHash.write(buffer, 0, read);
                        }
                        currentHash = bufferHash.toByteArray();
                        // Рассчет хеша для сравнения с выбранным
                        if (ALGORITHM_DIGEST.equals(GOST3411) || ALGORITHM_DIGEST.equals(GOST3411_2012_256) || ALGORITHM_DIGEST.equals(GOST3411_2012_512)) {
                            new CheckHashTask().execute();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            AlertDialog enterPasswordDialog;
                            LayoutInflater inflater = getLayoutInflater();
                            View view = inflater.inflate(R.layout.layout_enter_password, null);
                            final EditText editTextPassword = view.findViewById(R.id.editTextPassword);
                            builder.setTitle("Введите пароль");
                            builder.setView(view);
                            builder.setPositiveButton("OK", (dialog, which) -> {
                                if (!editTextPassword.getText().toString().equals("")) new CheckMACTask().execute(editTextPassword.getText().toString());
                            });
                            builder.setNegativeButton("Отмена", (dialog, which) -> {});
                            enterPasswordDialog = builder.create();
                            enterPasswordDialog.show();
                        }
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

    // Генератор ключа на основе пароля
    public SecretKey generateKey(String password, int len) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = hex2bytes(mSettings.getString("SALT", ""));
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, 100, len);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacGOST3411");
        return secretKeyFactory.generateSecret(pbeKeySpec);
    }



    // Рассчет MDC в отдельном потоке
    class CalculateHashTask extends AsyncTask<Void, Integer, byte[]> {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        @Override
        protected void onPreExecute() {
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            builder.setNegativeButton("Отмена", (dialog, which) -> cancel(true));
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
                MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM_DIGEST, Security.getProvider("SC"));
                InputStream inputStream = new FileInputStream(currentFile);

                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                long counter = 0;
                long max = currentFile.length();
                int read;

                long time = System.currentTimeMillis();
                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) return null;

                    messageDigest.update(buffer);
                    counter += read;
                    float percent = ((counter * 100) / (float) max);
                    publishProgress((int) percent);
                }

                time = System.currentTimeMillis() - time;
                new DBHelper(getApplicationContext()).addDataToBD(currentFile, "MDC", ALGORITHM_DIGEST, (int) time);

                return messageDigest.digest();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadDialog.dismiss();
        }

        @Override
        protected void onPostExecute(byte[] hash) {
            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Имитовставка успешно посчитана");
            builder.setMessage(bytes2hex(hash));
            builder.setPositiveButton("Сохранить", (dialog, which) -> {
//                currentHash = hash;
                try {
                    OutputStream outputStream = new FileOutputStream(currentFile.getAbsolutePath() + ".hash");
                    outputStream.write(hash);
                    outputStream.close();
                    Toast.makeText(getApplicationContext(), "Успешно сохранено", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
//                startActivityForResult(intent, RESULT_DIRECTORY_TO_SAVE_HASH);
            });
            builder.setNeutralButton("Поделиться", (dialog, which) -> {
                try {
//                    File tmpFile = new File(getCacheDir(), currentFile.getFileName() + ".hash");
                    File tmpFile = new File(getCacheDir(), currentFile.getName() + ".hash");
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

    // Проверка MDC в отдельном потоке
    class CheckHashTask extends AsyncTask<Void, Integer, byte[]> {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        @Override
        protected void onPreExecute() {
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            builder.setNegativeButton("Отмена", (dialog, which) -> cancel(true));
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
                MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM_DIGEST, Security.getProvider("SC"));
                InputStream inputStream = new FileInputStream(currentFile);

                byte[] buffer = new byte[1024];
                long counter = 0;
                long max = currentFile.length();
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) return null;

                    messageDigest.update(buffer);
                    counter += read;
                    float percent = ((counter * 100) / (float) max);
                    publishProgress((int) percent);
                }
                return messageDigest.digest();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadDialog.dismiss();
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

    // Рассчет MAC в отдельном потоке
    class CalculateMacTask extends AsyncTask<String, Integer, byte[]> {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        @Override
        protected void onPreExecute() {
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            builder.setNegativeButton("Отмена", (dialog, which) -> cancel(true));
            loadDialog = builder.create();
            loadDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected byte[] doInBackground(String... strings) {
            String password = strings[0];
            try {
                Mac mac = Mac.getInstance(ALGORITHM_DIGEST, Security.getProvider("SC"));
                SecretKey key = generateKey(password, 256);
                mac.init(key);

                InputStream inputStream = new FileInputStream(currentFile);

                byte[] buffer = new byte[1024];
                long counter = 0;
                long max = currentFile.length();
                int read;

                long time = System.currentTimeMillis();
                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) return null;

                    mac.update(buffer);
                    counter += read;
                    float percent = ((counter * 100) / (float) max);
                    publishProgress((int) percent);
                }

                time = System.currentTimeMillis() - time;
                new DBHelper(getApplicationContext()).addDataToBD(currentFile, "MAC", ALGORITHM_DIGEST, (int) time);

                return mac.doFinal();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadDialog.dismiss();
        }

        @Override
        protected void onPostExecute(byte[] mac) {
            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Имитовставка успешно посчитана");
            builder.setMessage(bytes2hex(mac));
            builder.setPositiveButton("Сохранить", (dialog, which) -> {
                try {
                    OutputStream outputStream = new FileOutputStream(currentFile.getAbsolutePath() + ".mac");
                    outputStream.write(mac);
                    outputStream.close();
                    Toast.makeText(getApplicationContext(), "Успешно сохранено", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            builder.setNeutralButton("Поделиться", (dialog, which) -> {
                try {
//                    File tmpFile = new File(getCacheDir(), currentFile.getFileName() + ".hash");
                    File tmpFile = new File(getCacheDir(), currentFile.getName() + ".hash");
                    FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
                    fileOutputStream.write(mac);
                    fileOutputStream.close();
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri tmpFileUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID, tmpFile);
                    intent.putExtra(Intent.EXTRA_TEXT, bytes2hex(mac));
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

    // Проверка MAC в отдельном потоке (не реализован)
    class CheckMACTask extends AsyncTask<String, Integer, byte[]> {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        @Override
        protected void onPreExecute() {
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            builder.setNegativeButton("Отмена", (dialog, which) -> cancel(true));
            loadDialog = builder.create();
            loadDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected byte[] doInBackground(String... strings) {
            String password = strings[0];
            try {
                Mac mac = Mac.getInstance(ALGORITHM_DIGEST, Security.getProvider("SC"));
                SecretKey key = generateKey(password, 256);
                mac.init(key);

                InputStream inputStream = new FileInputStream(currentFile);

                byte[] buffer = new byte[1024];
                long counter = 0;
                long max = currentFile.length();
                int read;

                long time = System.currentTimeMillis();
                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) return null;

                    mac.update(buffer);
                    counter += read;
                    float percent = ((counter * 100) / (float) max);
                    publishProgress((int) percent);
                }

                time = System.currentTimeMillis() - time;
                new DBHelper(getApplicationContext()).addDataToBD(currentFile, "MAC", ALGORITHM_DIGEST, (int) time);

                return mac.doFinal();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadDialog.dismiss();
        }

        @Override
        protected void onPostExecute(byte[] mac) {
            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Результат проверки");
            if (Arrays.equals(currentHash, mac)) {
                builder.setMessage("Целостность не нарушена");
            } else {
                builder.setMessage("Целостность нарушена");
            }
            builder.setPositiveButton("OK", (dialog, which) -> {});
            builder.show();
        }

    }

    // Шифрование
    class EncryptTask extends AsyncTask<String, Integer, Void> {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        @Override
        protected void onPreExecute() {
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            builder.setNegativeButton("Отмена", (dialog, which) -> cancel(true));
            loadDialog = builder.create();
            loadDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(String... strings) {
            String password = strings[0];
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(currentFile.getAbsolutePath() + ".encrypted");

                SecretKey key = generateKey(password, 256);

                Cipher cipher = Cipher.getInstance(ALGORITHM_CIPHER, Security.getProvider("BC"));
                cipher.init(Cipher.ENCRYPT_MODE, key);

                InputStream inputStream = new FileInputStream(currentFile);
                CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);

                long counter = 0;
                long max = currentFile.length();

                int read;
                byte[] buffer = new byte[1024];

                long time = System.currentTimeMillis();
                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) return null;

                    cipherOutputStream.write(buffer, 0, read);

                    counter += read;
                    float percent = ((counter * 100) / (float) max);
                    publishProgress((int) percent);
                }
                time = System.currentTimeMillis() - time;
                new DBHelper(getApplicationContext()).addDataToBD(currentFile, "Cipher", ALGORITHM_CIPHER, (int) time);

                cipherOutputStream.flush();
                cipherOutputStream.close();
                inputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Void result) {
            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Файл успешно зашифрован");
            builder.setPositiveButton("Ok", (dialog, which) -> {});
            builder.show();
        }
    }

    // Расшифрование
    class DecryptTask extends AsyncTask<String, Integer, Void> {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        AlertDialog loadDialog;
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_loading_dialog, null);
        ProgressBar progressBar = view.findViewById(R.id.progressBarLoading);

        @Override
        protected void onPreExecute() {
            builder.setTitle("Вычисление...");
            builder.setView(view);
            builder.setCancelable(false);
            builder.setNegativeButton("Отмена", (dialog, which) -> cancel(true));
            loadDialog = builder.create();
            loadDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(String... strings) {
            String password = strings[0];

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(currentFile.getAbsolutePath() + ".decrypted");

                SecretKey key = generateKey(password, 256);

                Cipher cipher = Cipher.getInstance(ALGORITHM_CIPHER, Security.getProvider("BC"));
                cipher.init(Cipher.DECRYPT_MODE, key);

                InputStream inputStream = new FileInputStream(currentFile);
                CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);

                long counter = 0;
                long max = currentFile.length();

                int read;
                byte[] buffer = new byte[1024];

                long time = System.currentTimeMillis();
                while ((read = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) return null;

                    cipherOutputStream.write(buffer, 0, read);

                    counter += read;
                    float percent = ((counter * 100) / (float) max);
                    publishProgress((int) percent);
                }
                time = System.currentTimeMillis() - time;
                new DBHelper(getApplicationContext()).addDataToBD(currentFile, "Cipher", ALGORITHM_CIPHER, (int) time);

                cipherOutputStream.flush();
                cipherOutputStream.close();
                inputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            loadDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Void result) {
            loadDialog.dismiss();
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Файл успешно зашифрован");
            builder.setPositiveButton("Ok", (dialog, which) -> {});
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
                    + "file_type text,"
                    + "size_mb float,"
                    + "mode text,"
                    + "algorithm text,"
                    + "time_sec float"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        public void addDataToBD(CurrentFile file, String mode, String algorithm, int time) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            Cursor cursor = db.query("time",
                    null,
                    "file_name = ? AND file_type = ? AND size_mb = ? AND mode = ? AND algorithm = ?",
                    new String[] {file.getName(), file.getFileType(), Double.toString((double) file.length() / 1000000), mode, algorithm},
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
                cv.put("file_name", file.getName());
                cv.put("file_type", file.getFileType());
                cv.put("size_mb", file.length() / 1000000.0);
                cv.put("mode", mode);
                cv.put("algorithm", algorithm);
                cv.put("time_sec", time / 1000.0);
                db.insert("time", null, cv);
            }
        }
    }
}
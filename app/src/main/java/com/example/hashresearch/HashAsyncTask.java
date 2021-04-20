package com.example.hashresearch;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;

public abstract class HashAsyncTask extends AsyncTask<Void, Integer, byte[]> {

    private final Context context;
    private final Uri uriFile;
    private final String algorithm;

    HashAsyncTask(Context context, Uri uri, String algorithm) {
        this.context = context;
        this.uriFile = uri;
        this.algorithm = algorithm;
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @Override
    protected abstract void onPreExecute();

    @Override
    protected abstract void onProgressUpdate(Integer... values);

    @Override
    protected abstract void onPostExecute(byte[] hash);

    @Override
    protected byte[] doInBackground(Void... voids) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm, Security.getProvider("SC"));
            InputStream inputStream = context.getContentResolver().openInputStream(uriFile);
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

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}

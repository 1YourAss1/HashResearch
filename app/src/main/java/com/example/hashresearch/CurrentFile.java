package com.example.hashresearch;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.util.Locale;

public class CurrentFile {
    private final Uri uri;
    private final Cursor cursor;

    public CurrentFile(Uri uri, Context context) {
        this.uri = uri;
        this.cursor  = context.getContentResolver().query(uri, null, null, null, null);
        this.cursor.moveToFirst();
    }

    public String getFileName() {
        return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
    }

    public long getFileSize() {
        return cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
    }

    public String getFileSizeFormatted() {
        long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
        String s = "";
        double kb = size / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        double tb = gb / 1024.0;
        if (size < 1024L) {
            s = size + " Bytes";
        } else if(size >= 1024 && size < (1024L * 1024)) {
            s =  String.format(Locale.getDefault(), "%.2f KB", kb);
        } else if(size >= (1024L * 1024) && size < (1024L * 1024 * 1024)) {
            s = String.format(Locale.getDefault(), "%.2f MB", mb);
        } else if(size >= (1024L * 1024 * 1024) && size < (1024L * 1024 * 1024 * 1024)) {
            s = String.format(Locale.getDefault(), "%.2f + GB", gb);
        } else if(size >= (1024L * 1024 * 1024 * 1024)) {
            s = String.format(Locale.getDefault(), "%.2f TB", tb);
        }
        return s;
    }

    public Uri getUri() {
        return uri;
    }
}

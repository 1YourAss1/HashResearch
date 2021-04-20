package com.example.hashresearch;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;

public class CurrentFile {
    private final Cursor cursor;

    public CurrentFile(Uri uri, Context context) {
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
        if(size < 1024L) {
            s = size + " Bytes";
        } else if(size >= 1024 && size < (1024L * 1024)) {
            s =  String.format("%.2f", kb) + " KB";
        } else if(size >= (1024L * 1024) && size < (1024L * 1024 * 1024)) {
            s = String.format("%.2f", mb) + " MB";
        } else if(size >= (1024L * 1024 * 1024) && size < (1024L * 1024 * 1024 * 1024)) {
            s = String.format("%.2f", gb) + " GB";
        } else if(size >= (1024L * 1024 * 1024 * 1024)) {
            s = String.format("%.2f", tb) + " TB";
        }
        return s;
    }
    }

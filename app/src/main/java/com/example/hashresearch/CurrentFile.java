package com.example.hashresearch;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;

public class CurrentFile extends File {

    public CurrentFile(String pathname) {
        super(pathname);
    }

    public String getFileType() {
        String[] s = getName().split("\\.");
        return s[s.length - 1];
    }


    public String getFileSizeFormatted() {
        long size = length();
        String s = "";
        double kb = size / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        double tb = gb / 1024.0;
        if (size <= 1024L) {
            s = size + " B";
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
}

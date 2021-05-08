package com.example.hashresearch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Browser extends AppCompatActivity {
    private static final String APP_PREFERENCES = "settings";
    private SharedPreferences mSettings;
    private RecyclerView recyclerView;
    private TextView textDir;
    private List<String> directoryEntries = new ArrayList<>();
    private File currentDirectory = new File("/storage");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        recyclerView = findViewById(R.id.recyclerView);
        textDir = findViewById(R.id.textDir);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        currentDirectory = new File(mSettings.getString("LAST_PATH", "/storage"));
        textDir.setText(currentDirectory.getPath());
        browseTo(currentDirectory);
    }


    public void browseTo(final File aDirectory) {
        if (aDirectory.isDirectory()) {
            currentDirectory = aDirectory;
            if (aDirectory.getAbsolutePath().equals("/storage") || aDirectory.getAbsolutePath().equals("/storage/emulated")) {
                firstFill();
                currentDirectory = new File("/storage");
            } else {
                File[] files = aDirectory.listFiles();
                if (files != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Arrays.sort(files, Comparator.comparingLong(File::length));
                    }
                    fill(files);
                } else {
                    fill(new File[0]);
                }
            }
            BrowseRecyclerViewAdapter recyclerAdapter = new BrowseRecyclerViewAdapter();
            recyclerView.setAdapter(recyclerAdapter);
            textDir.setText(currentDirectory.getPath());
        }
    }

    private void firstFill() {
        this.directoryEntries.clear();
        File[] externalDirs = getExternalFilesDirs(null);
        for (File file: externalDirs) {
            int ind = file.getAbsolutePath().lastIndexOf("/Android");
            String path = file.getAbsolutePath().substring(0, ind);
            directoryEntries.add(path);
        }
    }

    private void fill(File[] files) {
        this.directoryEntries.clear();
        this.directoryEntries.add("..");

        for (File file : files) {
            this.directoryEntries.add(file.getAbsolutePath());
        }
    }

    public void upOneLevel() {
        if (currentDirectory.getParentFile() != null) {
            browseTo(currentDirectory.getParentFile());
        }
    }


    class BrowseRecyclerViewAdapter extends RecyclerView.Adapter<BrowseRecyclerViewAdapter.ListViewHolder>{

        @Override
        public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            View view = inflater.inflate(R.layout.rv_item, parent, false);
            return new ListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ListViewHolder holder, int position) {
            holder.fileNameViewTextView.setText(new File(directoryEntries.get(position)).getName());

            Date lastModifiedDate = new Date(new CurrentFile(directoryEntries.get(position)).lastModified());
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat ("HH:mm dd.MM.yyyy", Locale.getDefault());
            if (new CurrentFile(directoryEntries.get(position)).isFile() && !directoryEntries.get(position).equals("..")) {
                holder.fileSizeTextView.setText(new CurrentFile(directoryEntries.get(position)).getFileSizeFormatted());
                holder.fileLastModifiedTextView.setText(simpleDateFormat.format(lastModifiedDate));
            } else if (new CurrentFile(directoryEntries.get(position)).isDirectory()  && !directoryEntries.get(position).equals("..")) {
                holder.fileLastModifiedTextView.setText(simpleDateFormat.format(lastModifiedDate));
            }

            if (currentDirectory.getAbsolutePath().equals("/storage")) {
                if (directoryEntries.get(position).equals("/storage/emulated/0")) {
                    holder.fileNameViewTextView.setText("Память устройства");
                    holder.imageView.setImageResource(R.drawable.ic_phone_memory_24dp);
                } else {
                    holder.fileNameViewTextView.setText("Карта памяти");
                    holder.imageView.setImageResource(R.drawable.ic_sd_storage_24dp);
                }
            } else if (directoryEntries.get(position).equals("..")) {
                holder.imageView.setImageResource(R.drawable.ic_arrow_back_24dp);
            } else if (new File(directoryEntries.get(position)).isFile()){
                holder.imageView.setImageResource(R.drawable.ic_insert_drive_file_24dp);
            } else if (new File(directoryEntries.get(position)).isDirectory()){
                holder.imageView.setImageResource(R.drawable.ic_folder_24dp);
            }
        }

        @Override
        public int getItemCount() {
            return directoryEntries.size();
        }

        public class ListViewHolder extends RecyclerView.ViewHolder {
            TextView fileNameViewTextView, fileSizeTextView, fileLastModifiedTextView;
            ImageView imageView;

            public ListViewHolder(View itemView) {
                super(itemView);
                fileNameViewTextView = itemView.findViewById(R.id.fileName);
                fileSizeTextView = itemView.findViewById(R.id.fileSize);
                fileLastModifiedTextView = itemView.findViewById(R.id.fileLastModified);
                imageView = itemView.findViewById(R.id.image);

                itemView.setOnClickListener(v -> {
                    if (new File(directoryEntries.get(getAdapterPosition())).getName().equals("..")){
                        upOneLevel();
                    } else if (new File(directoryEntries.get(getAdapterPosition())).isDirectory()) {
                        browseTo(new File(directoryEntries.get(getAdapterPosition())).getAbsoluteFile());
                    } else {
                        SharedPreferences.Editor editor = mSettings.edit();
                        editor.putString("LAST_PATH", currentDirectory.getAbsolutePath()).apply();

                        Intent intent = new Intent();
                        intent.putExtra("file_path", new File(directoryEntries.get(getAdapterPosition())).getAbsolutePath());
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    if (new File(directoryEntries.get(getAdapterPosition())).getName().equals("..")) browseTo(new File("/storage"));
                    return true;
                });
            }
        }
    }
}
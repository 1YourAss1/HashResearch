package com.example.hashresearch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String APP_PREFERENCES = "settings";
    SharedPreferences mSettings;

    RadioButton radioButtonGost3411;
    RadioButton radioButtonGost3411_2012_256;
    RadioButton radioButtonGost3411_2012_512;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        String algorithm = mSettings.getString("ALGORITHM", "GOST3411");

        radioButtonGost3411 = findViewById(R.id.radioButtonGost3411);
        radioButtonGost3411_2012_256 = findViewById(R.id.radioButtonGost3411_2012_256);
        radioButtonGost3411_2012_512 = findViewById(R.id.radioButtonGost3411_2012_512);

        switch (algorithm) {
            case "GOST3411":
                radioButtonGost3411.setChecked(true);
                break;
            case "GOST3411-2012-256":
                radioButtonGost3411_2012_256.setChecked(true);
                break;
            case "GOST3411-2012-512":
                radioButtonGost3411_2012_512.setChecked(true);
                break;
        }

        radioButtonGost3411.setOnClickListener(this);
        radioButtonGost3411_2012_256.setOnClickListener(this);
        radioButtonGost3411_2012_512.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        SharedPreferences.Editor editor = mSettings.edit();
        int id = v.getId();

        if (id == R.id.radioButtonGost3411) {
            editor.putString("ALGORITHM", "GOST3411");
        } else if (id == R.id.radioButtonGost3411_2012_256) {
            editor.putString("ALGORITHM", "GOST3411-2012-256");
        } else if (id == R.id.radioButtonGost3411_2012_512) {
            editor.putString("ALGORITHM", "GOST3411-2012-512");
        }

        editor.apply();
    }
}
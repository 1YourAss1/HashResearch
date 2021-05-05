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

    private static final String GOST3411 = "GOST3411";
    private static final String GOST3411_2012_256 = "GOST3411-2012-256";
    private static final String GOST3411_2012_512 = "GOST3411-2012-512";
    private static final String GOST28147 = "GOST28147";
    private static final String HMAC_GOST3411 = "HMAC-GOST3411";
    private static final String HMAC_GOST3411_2012_256 = "HMAC-GOST3411-2012-256";
    private static final String HMAC_GOST3411_2012_512 = "HMAC-GOST3411-2012-512";

    // MDC
    RadioButton radioButtonGost3411;
    RadioButton radioButtonGost3411_2012_256;
    RadioButton radioButtonGost3411_2012_512;

    // MAC
    RadioButton radioButtonMacGost28147_89;
    RadioButton radioButtonMacGost3411;
    RadioButton radioButtonMacGost3411_2012_256;
    RadioButton radioButtonMacGost3411_2012_512;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        String algorithm = mSettings.getString("ALGORITHM", GOST3411);

        radioButtonGost3411 = findViewById(R.id.radioButtonGost3411);
        radioButtonGost3411_2012_256 = findViewById(R.id.radioButtonGost3411_2012_256);
        radioButtonGost3411_2012_512 = findViewById(R.id.radioButtonGost3411_2012_512);

        radioButtonMacGost28147_89 = findViewById(R.id.radioButtonMacGost28147_89);
        radioButtonMacGost3411 = findViewById(R.id.radioButtonMacGost3411);
        radioButtonMacGost3411_2012_256 = findViewById(R.id.radioButtonMacGost3411_2012_256);
        radioButtonMacGost3411_2012_512 = findViewById(R.id.radioButtonMacGost3411_2012_512);

        switch (algorithm) {
            case GOST3411:
                radioButtonGost3411.setChecked(true);
                break;
            case GOST3411_2012_256:
                radioButtonGost3411_2012_256.setChecked(true);
                break;
            case GOST3411_2012_512:
                radioButtonGost3411_2012_512.setChecked(true);
                break;
            case GOST28147:
                radioButtonMacGost28147_89.setChecked(true);
                break;
            case "HMAC-GOST3411":
                radioButtonMacGost3411.setChecked(true);
                break;
            case "HMAC-GOST3411-2012-256":
                radioButtonMacGost3411_2012_256.setChecked(true);
                break;
            case "HMAC-GOST3411-2012-512":
                radioButtonMacGost3411_2012_512.setChecked(true);
                break;
        }

        radioButtonGost3411.setOnClickListener(this);
        radioButtonGost3411_2012_256.setOnClickListener(this);
        radioButtonGost3411_2012_512.setOnClickListener(this);

        radioButtonMacGost28147_89.setOnClickListener(this);
        radioButtonMacGost3411.setOnClickListener(this);
        radioButtonMacGost3411_2012_256.setOnClickListener(this);
        radioButtonMacGost3411_2012_512.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        SharedPreferences.Editor editor = mSettings.edit();
        int id = v.getId();

        if (id == R.id.radioButtonGost3411) {
            editor.putString("ALGORITHM", GOST3411);
        } else if (id == R.id.radioButtonGost3411_2012_256) {
            editor.putString("ALGORITHM", GOST3411_2012_256);
        } else if (id == R.id.radioButtonGost3411_2012_512) {
            editor.putString("ALGORITHM", GOST3411_2012_512);
        } else if (id == R.id.radioButtonMacGost28147_89) {
            editor.putString("ALGORITHM", GOST28147);
        } else if (id == R.id.radioButtonMacGost3411) {
            editor.putString("ALGORITHM", HMAC_GOST3411);
        } else if (id == R.id.radioButtonMacGost3411_2012_256) {
            editor.putString("ALGORITHM", HMAC_GOST3411_2012_256);
        } else if (id == R.id.radioButtonMacGost3411_2012_512) {
            editor.putString("ALGORITHM", HMAC_GOST3411_2012_512);
        }

        editor.apply();
    }
}
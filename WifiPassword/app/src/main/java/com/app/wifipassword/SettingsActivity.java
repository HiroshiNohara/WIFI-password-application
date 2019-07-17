package com.app.wifipassword;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.function_settings));
        final SwitchCompat clearDisplaySwitch = (SwitchCompat) findViewById(R.id.clear_display_switch);
        boolean isClearDisplay = pref.getBoolean("is_clear_display", false);
        clearDisplaySwitch.setChecked(isClearDisplay);
        final RelativeLayout clearDisplaySetting = (RelativeLayout) findViewById(R.id.clear_display_setting);
        clearDisplaySetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDisplaySwitch.setChecked(!clearDisplaySwitch.isChecked());
                editor.putBoolean("is_clear_display", clearDisplaySwitch.isChecked());
                editor.apply();
            }
        });
        clearDisplaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("is_clear_display", isChecked);
                editor.apply();
            }
        });
        final SwitchCompat hideWifiSwitch = (SwitchCompat) findViewById(R.id.hide_wifi_switch);
        boolean isHideWifi = pref.getBoolean("is_hide_wifi", true);
        hideWifiSwitch.setChecked(isHideWifi);
        final RelativeLayout hideWifiSetting = (RelativeLayout) findViewById(R.id.hide_wifi_setting);
        hideWifiSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideWifiSwitch.setChecked(!hideWifiSwitch.isChecked());
                editor.putBoolean("is_hide_wifi", hideWifiSwitch.isChecked());
                editor.apply();
            }
        });
        hideWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("is_hide_wifi", isChecked);
                editor.apply();
            }
        });
        if (pref.getInt("language", 0) == 0) {
            TextView textView = (TextView) findViewById(R.id.current_language);
            textView.setText(this.getString(R.string.settings_followSystemLanguage));
        }
        RelativeLayout switchLanguage = (RelativeLayout) findViewById(R.id.switch_language);
        switchLanguage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setSingleChoiceItems(new String[]{SettingsActivity.this.getString(R.string.settings_followSystemLanguage), "简体中文", "繁體中文", "English"},
                        pref.getInt("language", 0),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (pref.getInt("language", 0) != i) {
                                    editor = pref.edit();
                                    editor.putInt("language", i);
                                    editor.apply();
                                    dialog.dismiss();
                                    Intent intent = new Intent(SettingsActivity.this, WifiActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            }
                        });
                dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(SettingsActivity.this, WifiActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);
    }

}

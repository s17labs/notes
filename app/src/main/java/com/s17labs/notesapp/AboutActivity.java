package com.s17labs.notesapp;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        TextView versionText = findViewById(R.id.versionText);
        TextView copyrightText = findViewById(R.id.copyrightText);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionText.setText("Version " + packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("");
        }

        copyrightText.setText("© " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " s17 Labs.");

        findViewById(R.id.githubButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/s17labs/notes"));
            startActivity(intent);
        });

        findViewById(R.id.licenseButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/s17labs/notes/blob/main/LICENSE"));
            startActivity(intent);
        });
    }
}

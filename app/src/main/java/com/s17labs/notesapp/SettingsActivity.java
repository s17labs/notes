package com.s17labs.notesapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout autoSaveSwitch;
    private ImageView switchThumb;
    private boolean isAutoSaveOn = true;
    private JSONArray pendingExportData;
    private String pendingExportFileName;

    private final ActivityResultLauncher<String[]> importFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    importNotes(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> exportDirPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    saveExportToDirectory(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton backButton = findViewById(R.id.backButton);
        autoSaveSwitch = findViewById(R.id.autoSaveSwitch);
        switchThumb = findViewById(R.id.switchThumb);
        View exportButton = findViewById(R.id.exportButton);
        View importButton = findViewById(R.id.importButton);

        isAutoSaveOn = NoteDbHelper.isAutoSaveEnabled(this);
        updateSwitchUI();

        autoSaveSwitch.setOnClickListener(v -> {
            isAutoSaveOn = !isAutoSaveOn;
            NoteDbHelper.setAutoSaveEnabled(this, isAutoSaveOn);
            updateSwitchUI();
            Toast.makeText(this, isAutoSaveOn ? "Auto-save enabled" : "Auto-save disabled", Toast.LENGTH_SHORT).show();
        });

        backButton.setOnClickListener(v -> finish());

        exportButton.setOnClickListener(v -> exportNotes());
        importButton.setOnClickListener(v -> showImportDialog());
    }

    private void updateSwitchUI() {
        if (isAutoSaveOn) {
            autoSaveSwitch.setBackgroundResource(R.drawable.switch_track_on);
            switchThumb.setImageResource(R.drawable.switch_circle_on);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) switchThumb.getLayoutParams();
            params.leftMargin = (int) (20 * getResources().getDisplayMetrics().density);
            switchThumb.setLayoutParams(params);
        } else {
            autoSaveSwitch.setBackgroundResource(R.drawable.switch_track_off);
            switchThumb.setImageResource(R.drawable.switch_circle_off);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) switchThumb.getLayoutParams();
            params.leftMargin = (int) (8 * getResources().getDisplayMetrics().density);
            switchThumb.setLayoutParams(params);
        }
    }

    private void showImportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_warning, null);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            dialog.dismiss();
            openImportFilePicker();
        });

        dialog.show();
        dialog.getWindow().setLayout((int) (340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void openImportFilePicker() {
        importFilePickerLauncher.launch(new String[]{"application/json", "text/plain", "*/*"});
    }

    private void importNotes(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Cannot read file", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String content = new String(buffer, StandardCharsets.UTF_8);
            JSONArray notesArray = new JSONArray(content);

            NoteDbHelper dbHelper = new NoteDbHelper(this);
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

            int imported = 0;
            for (int i = 0; i < notesArray.length(); i++) {
                JSONObject note = notesArray.getJSONObject(i);
                android.content.ContentValues values = new android.content.ContentValues();
                values.put("title", note.optString("title", ""));
                values.put("note_text", note.optString("note_text", ""));
                values.put("date_text", note.optString("date_text", ""));
                values.put("created_at", note.optLong("created_at", System.currentTimeMillis()));
                values.put("modified_at", note.optLong("modified_at", System.currentTimeMillis()));
                values.put("pinned", note.optInt("pinned", 0));
                values.put("color", note.optInt("color", 0));
                db.insert("notes", null, values);
                imported++;
            }

            Toast.makeText(this, "Imported " + imported + " notes", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Import failed: Invalid file format", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportNotes() {
        try {
            NoteDbHelper dbHelper = new NoteDbHelper(this);
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT _id, title, note_text, date_text, created_at, modified_at, pinned, color FROM notes WHERE deleted = 0", null);

            JSONArray notesArray = new JSONArray();

            if (cursor.moveToFirst()) {
                do {
                    JSONObject note = new JSONObject();
                    note.put("id", cursor.getLong(0));
                    note.put("title", cursor.getString(1));
                    note.put("note_text", cursor.getString(2));
                    note.put("date_text", cursor.getString(3));
                    note.put("created_at", cursor.getLong(4));
                    note.put("modified_at", cursor.getLong(5));
                    note.put("pinned", cursor.getInt(6));
                    note.put("color", cursor.getInt(7));
                    notesArray.put(note);
                } while (cursor.moveToNext());
            }
            cursor.close();

            pendingExportData = notesArray;

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            pendingExportFileName = "notes_backup_" + timestamp + ".json";

            exportDirPickerLauncher.launch(null);

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExportToDirectory(Uri directoryUri) {
        try {
            DocumentFile directory = DocumentFile.fromTreeUri(this, directoryUri);
            if (directory == null || !directory.canWrite()) {
                Toast.makeText(this, "Cannot write to selected folder", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentFile exportFile = directory.createFile("application/json", pendingExportFileName);
            if (exportFile == null) {
                Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(exportFile.getUri());
            if (outputStream == null) {
                Toast.makeText(this, "Cannot save file", Toast.LENGTH_SHORT).show();
                return;
            }

            outputStream.write(pendingExportData.toString(2).getBytes(StandardCharsets.UTF_8));
            outputStream.close();

            Toast.makeText(this, "Notes exported successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

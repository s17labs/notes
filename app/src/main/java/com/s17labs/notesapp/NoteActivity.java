package com.s17labs.notesapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteActivity extends AppCompatActivity {

    private NoteDbHelper dbHelper;
    private long noteId = -1;
    private EditText titleEditText;
    private EditText noteEditText;
    private TextView charCountText;
    private TextView statusText;
    private TextView toolbarTitle;
    private ImageButton deleteButton;
    private boolean hasChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        dbHelper = new NoteDbHelper(this);
        titleEditText = findViewById(R.id.titleEditText);
        noteEditText = findViewById(R.id.noteEditText);
        charCountText = findViewById(R.id.charCountText);
        statusText = findViewById(R.id.statusText);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);

        noteId = getIntent().getLongExtra("NOTE_ID", -1);

        if (noteId != -1) {
            toolbarTitle.setText("Editing Note");
            loadNote();
        } else {
            toolbarTitle.setText("New Note");
            deleteButton.setVisibility(android.view.View.GONE);
            noteEditText.setHint("Start typing your note...");
            charCountText.setText("0 characters");
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCountText.setText(noteEditText.getText().length() + " characters");
                hasChanges = true;
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        };

        titleEditText.addTextChangedListener(textWatcher);
        noteEditText.addTextChangedListener(textWatcher);

        backButton.setOnClickListener(v -> onBackPressed());
        
        saveButton.setOnClickListener(v -> saveNote());
        
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadNote() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        if (cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String noteText = cursor.getString(cursor.getColumnIndex("note_text"));
            String dateText = cursor.getString(cursor.getColumnIndex("date_text"));
            
            titleEditText.setText(title);
            noteEditText.setText(noteText);
            statusText.setText("Last edited: " + dateText);
            charCountText.setText(noteText.length() + " characters");
        }
        cursor.close();
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String text = noteEditText.getText().toString().trim();
        
        if (title.isEmpty() && text.isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        long now = System.currentTimeMillis();
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(now));
        
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("note_text", text);
        values.put("date_text", date);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (noteId == -1) {
            values.put("created_at", now);
            values.put("modified_at", now);
            db.insert("notes", null, values);
        } else {
            values.put("modified_at", now);
            db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        }
        
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void confirmDelete() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            dbHelper.getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unsaved, null);
            
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();
            
            dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
                dialog.dismiss();
                saveNote();
            });
            
            dialogView.findViewById(R.id.btnDiscard).setOnClickListener(v -> {
                dialog.dismiss();
                super.onBackPressed();
            });
            
            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
            dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            super.onBackPressed();
        }
    }
}

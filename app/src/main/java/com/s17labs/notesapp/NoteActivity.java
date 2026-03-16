package com.s17labs.notesapp;

import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private TextView dateText;
    private ImageButton saveButton;
    private boolean hasChanges = false;
    private int activeColor;
    private int inactiveColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        dbHelper = new NoteDbHelper(this);
        titleEditText = findViewById(R.id.titleEditText);
        noteEditText = findViewById(R.id.noteEditText);
        charCountText = findViewById(R.id.charCountText);
        dateText = findViewById(R.id.dateText);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        ImageButton backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        ImageButton deleteButton = findViewById(R.id.deleteButton);
        LinearLayout noteContentLayout = findViewById(R.id.noteContentLayout);

        activeColor = getResources().getColor(R.color.colorAccent, null);
        inactiveColor = getResources().getColor(R.color.textSecondary, null);

        noteContentLayout.setOnClickListener(v -> {
            noteEditText.requestFocus();
            int cursorPosition = noteEditText.getText() != null ? noteEditText.getText().length() : 0;
            noteEditText.setSelection(cursorPosition);
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(noteEditText, InputMethodManager.SHOW_IMPLICIT);
        });

        noteEditText.setOnTouchListener((v, event) -> {
            noteEditText.onTouchEvent(event);
            noteEditText.requestFocus();
            int cursorPosition = noteEditText.getOffsetForPosition(event.getX(), event.getY());
            noteEditText.setSelection(cursorPosition);
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(noteEditText, InputMethodManager.SHOW_IMPLICIT);
            return true;
        });

        noteId = getIntent().getLongExtra("NOTE_ID", -1);

        if (noteId != -1) {
            toolbarTitle.setText("Edit Note");
            loadNote();
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            toolbarTitle.setText("New Note");
            deleteButton.setVisibility(View.GONE);
            charCountText.setText("0 characters");
            dateText.setText("");
        }

        updateSaveButtonColor();

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCountText.setText(noteEditText.getText().length() + " characters");
                if (!hasChanges) {
                    hasChanges = true;
                    updateSaveButtonColor();
                }
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

    private void updateSaveButtonColor() {
        saveButton.setImageTintList(ColorStateList.valueOf(hasChanges ? activeColor : inactiveColor));
    }

    private void loadNote() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, note_text, date_text FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        if (cursor.moveToFirst()) {
            titleEditText.setText(cursor.getString(0));
            noteEditText.setText(cursor.getString(1));
            dateText.setText("Last edited: " + cursor.getString(2));
            charCountText.setText(noteEditText.getText().length() + " characters");
        }
        cursor.close();
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
                finish();
            });
            
            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
            dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            finish();
        }
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String text = noteEditText.getText().toString();
        
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
            noteId = db.insert("notes", null, values);
        } else {
            values.put("modified_at", now);
            db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        }
        
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        hasChanges = false;
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
}

package com.s17labs.notesapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

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
    private ImageButton pinButton;
    private boolean hasChanges = false;
    private int activeColor;
    private int inactiveColor;
    private boolean isPinned = false;
    private boolean autoSaveEnabled = true;
    private Handler autoSaveHandler;
    private Runnable autoSaveRunnable;
    private static final long AUTO_SAVE_DELAY = 2000;
    private PopupWindow tooltipPopup;
    private Timer tooltipTimer;

    private void showTooltip(View anchor, String text) {
        if (tooltipPopup != null && tooltipPopup.isShowing()) {
            tooltipPopup.dismiss();
        }
        if (tooltipTimer != null) {
            tooltipTimer.cancel();
        }

        TextView tooltipView = new TextView(this);
        tooltipView.setText(text);
        tooltipView.setTextColor(getResources().getColor(R.color.textPrimary, getTheme()));
        tooltipView.setBackgroundColor(getResources().getColor(R.color.surface, getTheme()));
        tooltipView.setPadding(24, 16, 24, 16);
        tooltipView.setTextSize(14);
        tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int tooltipWidth = tooltipView.getMeasuredWidth();
        int tooltipHeight = tooltipView.getMeasuredHeight();

        tooltipPopup = new PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        tooltipPopup.setBackgroundDrawable(null);
        tooltipPopup.setOutsideTouchable(true);

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorCenterX = location[0] + anchor.getWidth() / 2;
        int anchorTop = location[1];
        int anchorBottom = location[1] + anchor.getHeight();

        int displayHeight = getResources().getDisplayMetrics().heightPixels;
        boolean showBelow = (anchorBottom + tooltipHeight + 20) <= displayHeight;

        int x = anchorCenterX - tooltipWidth / 2;
        int y = showBelow ? anchorBottom + 10 : anchorTop - tooltipHeight - 10;

        tooltipPopup.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, x, y);

        tooltipTimer = new Timer();
        tooltipTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (tooltipPopup != null && tooltipPopup.isShowing()) {
                        tooltipPopup.dismiss();
                    }
                });
            }
        }, 2000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        dbHelper = new NoteDbHelper(this);
        autoSaveEnabled = NoteDbHelper.isAutoSaveEnabled(this);
        autoSaveHandler = new Handler(Looper.getMainLooper());
        
        titleEditText = findViewById(R.id.titleEditText);
        noteEditText = findViewById(R.id.noteEditText);
        
        int accentColor = getResources().getColor(R.color.colorAccent, null);
        
        Drawable cursorDrawable = getResources().getDrawable(R.drawable.cursor_drawable, getTheme());
        titleEditText.setTextCursorDrawable(cursorDrawable);
        noteEditText.setTextCursorDrawable(cursorDrawable);
        
        applyHandleTint(titleEditText, accentColor);
        applyHandleTint(noteEditText, accentColor);
        
        charCountText = findViewById(R.id.charCountText);
        dateText = findViewById(R.id.dateText);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        ImageButton backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        pinButton = findViewById(R.id.pinButton);
        ImageButton moreButton = findViewById(R.id.moreButton);
        LinearLayout noteContentLayout = findViewById(R.id.noteContentLayout);

        activeColor = getResources().getColor(R.color.colorAccent, null);
        inactiveColor = getResources().getColor(R.color.textSecondary, null);

        noteEditText.setOnClickListener(v -> {
            noteEditText.requestFocus();
        });

        noteId = getIntent().getLongExtra("NOTE_ID", -1);

        if (noteId != -1) {
            toolbarTitle.setText("Edit Note");
            loadNote();
            noteEditText.requestFocus();
            noteEditText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(noteEditText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        } else {
            toolbarTitle.setText("New Note");
            charCountText.setText("0 characters");
            dateText.setText("");
            noteEditText.requestFocus();
            noteEditText.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(noteEditText, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        }

        updateSaveButtonColor();
        updatePinButton();
        updateSaveButtonVisibility();

        autoSaveRunnable = () -> {
            if (hasChanges && autoSaveEnabled) {
                autoSaveNote();
            }
        };

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCountText.setText(noteEditText.getText().length() + " characters");
                if (!hasChanges) {
                    hasChanges = true;
                    updateSaveButtonColor();
                    updateSaveButtonVisibility();
                }
                if (autoSaveEnabled) {
                    autoSaveHandler.removeCallbacks(autoSaveRunnable);
                    autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY);
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        };

        titleEditText.addTextChangedListener(textWatcher);
        noteEditText.addTextChangedListener(textWatcher);

        backButton.setOnClickListener(v -> onBackPressed());
        backButton.setOnLongClickListener(v -> {
            showTooltip(backButton, "Go back");
            return true;
        });
        saveButton.setOnClickListener(v -> saveNote());
        saveButton.setOnLongClickListener(v -> {
            showTooltip(saveButton, "Save note");
            return true;
        });
        pinButton.setOnClickListener(v -> togglePin());
        pinButton.setOnLongClickListener(v -> {
            showTooltip(pinButton, isPinned ? "Unpin note" : "Pin note");
            return true;
        });
        moreButton.setOnClickListener(v -> showMoreMenu(v));
        moreButton.setOnLongClickListener(v -> {
            showTooltip(moreButton, "More options");
            return true;
        });
    }

    private void updateSaveButtonColor() {
        saveButton.setImageTintList(ColorStateList.valueOf(hasChanges ? activeColor : inactiveColor));
    }

    private void updateSaveButtonVisibility() {
        if (autoSaveEnabled) {
            saveButton.setVisibility(View.GONE);
        } else {
            saveButton.setVisibility(View.VISIBLE);
        }
    }

    private void updatePinButton() {
        pinButton.setImageResource(R.drawable.ic_pin_filled);
        if (isPinned) {
            pinButton.setColorFilter(activeColor);
        } else {
            pinButton.setColorFilter(inactiveColor);
        }
    }

    private void loadNote() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, note_text, date_text, pinned FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        if (cursor.moveToFirst()) {
            titleEditText.setText(cursor.getString(0));
            noteEditText.setText(cursor.getString(1));
            dateText.setText("Last edited: " + cursor.getString(2));
            charCountText.setText(noteEditText.getText().length() + " characters");
            isPinned = cursor.getInt(3) == 1;
        }
        cursor.close();
        updatePinButton();
    }

    private void togglePin() {
        isPinned = !isPinned;
        updatePinButton();
        if (noteId != -1) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("pinned", isPinned ? 1 : 0);
            db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        }
        Toast.makeText(this, isPinned ? "Note pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.note_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_formatting) {
                showFormattingDialog();
                return true;
            } else if (id == R.id.action_share) {
                shareNote();
                return true;
            } else if (id == R.id.action_delete) {
                if (noteId != -1) {
                    ContentValues values = new ContentValues();
                    values.put("deleted", 1);
                    dbHelper.getWritableDatabase().update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
                }
                Toast.makeText(this, "Note moved to trash", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NoteActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void showFormattingDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_formatting, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void shareNote() {
        String title = titleEditText.getText().toString();
        String text = noteEditText.getText().toString();
        String shareContent = (title.isEmpty() ? "" : title + "\n\n") + text;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title.isEmpty() ? "Shared Note" : title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(shareIntent, "Share note"));
    }

    private void autoSaveNote() {
        String title = titleEditText.getText().toString().trim();
        String text = noteEditText.getText().toString();
        
        if (title.isEmpty() && text.isEmpty()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(now));
        
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("note_text", text);
        values.put("date_text", date);
        values.put("pinned", isPinned ? 1 : 0);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (noteId == -1) {
            values.put("created_at", now);
            values.put("modified_at", now);
            noteId = db.insert("notes", null, values);
            runOnUiThread(() -> {
                TextView toolbarTitle = findViewById(R.id.toolbarTitle);
                if (toolbarTitle != null) toolbarTitle.setText("Edit Note");
                updateSaveButtonVisibility();
            });
        } else {
            values.put("modified_at", now);
            db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        }
        
        hasChanges = false;
        runOnUiThread(() -> {
            dateText.setText("Last edited: " + date);
            updateSaveButtonColor();
        });
    }

    @Override
    public void onBackPressed() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        
        if (hasChanges && autoSaveEnabled) {
            autoSaveNote();
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
            finish();
        } else if (hasChanges) {
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
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveNote();
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applyHandleTint(EditText editText, int color) {
        Drawable handleLeft = editText.getTextSelectHandleLeft();
        if (handleLeft != null) {
            handleLeft.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            editText.setTextSelectHandleLeft(handleLeft);
        }
        Drawable handleRight = editText.getTextSelectHandleRight();
        if (handleRight != null) {
            handleRight.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            editText.setTextSelectHandleRight(handleRight);
        }
        Drawable handle = editText.getTextSelectHandle();
        if (handle != null) {
            handle.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            editText.setTextSelectHandle(handle);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoSaveHandler != null && autoSaveRunnable != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
        }
    }
}

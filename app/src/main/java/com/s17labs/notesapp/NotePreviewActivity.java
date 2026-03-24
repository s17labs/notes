package com.s17labs.notesapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotePreviewActivity extends AppCompatActivity {

    private long noteId = -1;
    private TextView titleText;
    private TextView noteText;
    private TextView dateText;
    private ImageButton pinButton;
    private ImageButton editButton;
    private ImageButton moreButton;
    private ImageButton restoreButton;
    private ImageButton deleteForeverButton;
    private boolean isPinned = false;
    private boolean isTrashView = false;
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

        tooltipPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);

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
        setContentView(R.layout.activity_note_preview);

        titleText = findViewById(R.id.titleText);
        noteText = findViewById(R.id.noteText);
        dateText = findViewById(R.id.dateText);
        ImageButton backButton = findViewById(R.id.backButton);
        editButton = findViewById(R.id.editButton);
        pinButton = findViewById(R.id.pinButton);
        moreButton = findViewById(R.id.moreButton);
        restoreButton = findViewById(R.id.restoreButton);
        deleteForeverButton = findViewById(R.id.deleteForeverButton);

        noteId = getIntent().getLongExtra("NOTE_ID", -1);
        isTrashView = getIntent().getBooleanExtra("IS_TRASH_VIEW", false);

        if (noteId != -1) {
            loadNote();
        }

        updateUIForTrashView();

        backButton.setOnClickListener(v -> finish());
        backButton.setOnLongClickListener(v -> {
            showTooltip(backButton, "Go back");
            return true;
        });

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("NOTE_ID", noteId);
            startActivity(intent);
        });
        editButton.setOnLongClickListener(v -> {
            showTooltip(editButton, "Edit note");
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

        restoreButton.setOnClickListener(v -> restoreNote());
        restoreButton.setOnLongClickListener(v -> {
            showTooltip(restoreButton, "Restore note");
            return true;
        });

        deleteForeverButton.setOnClickListener(v -> confirmPermanentDelete());
        deleteForeverButton.setOnLongClickListener(v -> {
            showTooltip(deleteForeverButton, "Delete forever");
            return true;
        });

        noteText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                int offset = noteText.getOffsetForPosition(x, y);
                if (checkForLinkClickAt(offset)) {
                    noteText.cancelLongPress();
                    return true;
                }
            }
            return false;
        });
    }

    private void updateUIForTrashView() {
        if (isTrashView) {
            editButton.setVisibility(View.GONE);
            pinButton.setVisibility(View.GONE);
            moreButton.setVisibility(View.GONE);
            restoreButton.setVisibility(View.VISIBLE);
            deleteForeverButton.setVisibility(View.VISIBLE);
        } else {
            editButton.setVisibility(View.VISIBLE);
            pinButton.setVisibility(View.VISIBLE);
            moreButton.setVisibility(View.VISIBLE);
            restoreButton.setVisibility(View.GONE);
            deleteForeverButton.setVisibility(View.GONE);
        }
    }

    private void restoreNote() {
        NoteDbHelper dbHelper = new NoteDbHelper(this);
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 0);
        db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        Toast.makeText(this, "Note restored", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmPermanentDelete() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        TextView titleText = dialogView.findViewById(R.id.deleteTitle);
        titleText.setText("Delete Forever?");
        TextView messageText = dialogView.findViewById(R.id.deleteMessage);
        messageText.setText("This note will be permanently deleted and cannot be recovered.");
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            NoteDbHelper dbHelper = new NoteDbHelper(this);
            dbHelper.getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
            Toast.makeText(this, "Note deleted permanently", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (noteId != -1) {
            loadNote();
        }
    }

    private void loadNote() {
        NoteDbHelper dbHelper = new NoteDbHelper(this);
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, note_text, date_text, pinned FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        if (cursor.moveToFirst()) {
            String title = cursor.getString(0);
            String noteTextContent = cursor.getString(1);
            String date = cursor.getString(2);
            isPinned = cursor.getInt(3) == 1;

            titleText.setText(title);
            dateText.setText("Last edited: " + date);
            noteText.setText(parseMarkdown(noteTextContent));

            updatePinButton();
        }
        cursor.close();
    }

    private void updatePinButton() {
        pinButton.setImageResource(R.drawable.ic_pin_filled);
        if (isPinned) {
            pinButton.setColorFilter(getResources().getColor(R.color.colorAccent, getTheme()));
        } else {
            pinButton.setColorFilter(getResources().getColor(R.color.textSecondary, getTheme()));
        }
    }

    private void togglePin() {
        NoteDbHelper dbHelper = new NoteDbHelper(this);
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pinned", isPinned ? 0 : 1);
        db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        isPinned = !isPinned;
        updatePinButton();
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
                NoteDbHelper dbHelper = new NoteDbHelper(this);
                ContentValues values = new ContentValues();
                values.put("deleted", 1);
                dbHelper.getWritableDatabase().update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
                Toast.makeText(this, "Note moved to trash", Toast.LENGTH_SHORT).show();
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
        String title = titleText.getText().toString();
        String text = noteText.getText().toString();
        String shareContent = (title.isEmpty() ? "" : title + "\n\n") + text;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title.isEmpty() ? "Shared Note" : title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(shareIntent, "Share note"));
    }

    private boolean checkForLinkClickAt(int offset) {
        if (offset >= noteText.getText().length()) return false;
        
        Spannable spannable = (Spannable) noteText.getText();
        URLSpan[] spans = spannable.getSpans(offset, offset + 1, URLSpan.class);
        if (spans.length > 0) {
            String url = spans[0].getURL();
            showLinkDialog(url);
            return true;
        }
        return false;
    }

    private void showLinkDialog(String url) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_link, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        TextView linkTitle = dialogView.findViewById(R.id.linkTitle);
        TextView btnOpenLink = dialogView.findViewById(R.id.btnOpenLink);
        TextView btnEditLink = dialogView.findViewById(R.id.btnEditLink);
        
        linkTitle.setText(url);
        
        btnOpenLink.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        
        btnEditLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("NOTE_ID", noteId);
            startActivity(intent);
            dialog.dismiss();
        });
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private Spannable parseMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) return new SpannableStringBuilder("");

        String result = markdown;
        
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1");
        result = result.replaceAll("_(.+?)_", "$1");
        result = result.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "$1");
        
        SpannableStringBuilder spannable = new SpannableStringBuilder(result);

        int totalOffset = 0;
        int lastMatchEnd = -1;

        Pattern allPattern = Pattern.compile("\\*\\*\\*(.+?)\\*\\*\\*|\\*\\*(.+?)\\*\\*|_(.+?)_|\\[(.+?)\\]\\((.+?)\\)|(?<!\\*)\\*(?!\\*)([^*]+)\\*(?!\\*)");
        Matcher matcher = allPattern.matcher(markdown);
        while (matcher.find()) {
            int matchStart = matcher.start();
            if (matchStart < lastMatchEnd) continue;
            
            String fullMatch = matcher.group(0);
            int style = -1;
            String content = null;
            int charsRemoved = 0;
            
            if (fullMatch.startsWith("***") && fullMatch.endsWith("***") && fullMatch.length() > 6) {
                content = matcher.group(1);
                if (content != null && !content.contains("*")) {
                    style = Typeface.BOLD_ITALIC;
                    charsRemoved = fullMatch.length() - content.length();
                }
            } else if (fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length() > 4) {
                content = matcher.group(2);
                if (content != null && !content.contains("*")) {
                    style = Typeface.BOLD;
                    charsRemoved = fullMatch.length() - content.length();
                }
            } else if (fullMatch.startsWith("_") && fullMatch.endsWith("_") && fullMatch.length() > 2) {
                content = matcher.group(3);
                if (content != null) {
                    style = -2;
                    charsRemoved = fullMatch.length() - content.length();
                }
            } else if (fullMatch.startsWith("[") && fullMatch.contains("](")) {
                content = matcher.group(4);
                String url = matcher.group(5);
                if (content != null && url != null) {
                    style = -3;
                    charsRemoved = fullMatch.length() - content.length();
                }
            } else if (fullMatch.startsWith("*") && fullMatch.endsWith("*") && !fullMatch.startsWith("**") && !fullMatch.startsWith("***")) {
                content = matcher.group(6);
                if (content != null && !content.contains("*")) {
                    style = Typeface.ITALIC;
                    charsRemoved = fullMatch.length() - content.length();
                }
            }
            
            if (style != -1 && content != null) {
                int start = matchStart - totalOffset;
                totalOffset += charsRemoved;
                lastMatchEnd = matchStart + fullMatch.length();
                int end = start + content.length();
                
                if (start >= 0 && end <= result.length()) {
                    if (style == Typeface.BOLD_ITALIC) {
                        spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.BOLD) {
                        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Typeface.ITALIC) {
                        spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == -2) {
                        spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == -3) {
                        String url = matcher.group(5);
                        spannable.setSpan(new URLSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        return spannable;
    }
}

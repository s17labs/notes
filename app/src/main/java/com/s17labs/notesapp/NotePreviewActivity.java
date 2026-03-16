package com.s17labs.notesapp;

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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotePreviewActivity extends AppCompatActivity {

    private long noteId = -1;
    private TextView titleText;
    private TextView noteText;
    private TextView dateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_preview);

        titleText = findViewById(R.id.titleText);
        noteText = findViewById(R.id.noteText);
        dateText = findViewById(R.id.dateText);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton editButton = findViewById(R.id.editButton);
        ImageButton deleteButton = findViewById(R.id.deleteButton);

        noteId = getIntent().getLongExtra("NOTE_ID", -1);

        if (noteId != -1) {
            loadNote();
        }

        backButton.setOnClickListener(v -> finish());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("NOTE_ID", noteId);
            startActivity(intent);
        });

        deleteButton.setOnClickListener(v -> confirmDelete());

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
        Cursor cursor = db.rawQuery("SELECT title, note_text, date_text FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        if (cursor.moveToFirst()) {
            String title = cursor.getString(0);
            String noteTextContent = cursor.getString(1);
            String date = cursor.getString(2);
            
            titleText.setText(title);
            dateText.setText("Last edited: " + date);
            noteText.setText(parseMarkdown(noteTextContent));
        }
        cursor.close();
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

    private void confirmDelete() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            NoteDbHelper dbHelper = new NoteDbHelper(this);
            dbHelper.getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
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

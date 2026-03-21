package com.s17labs.notesapp;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.s17labs.notesapp.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    
    private final Context context;
    private Cursor cursor;
    private final OnNoteClickListener listener;
    private boolean isTrashView = false;

    public interface OnNoteClickListener {
        void onNoteClick(long id);
        void onNoteLongClick(long id, View view);
    }

    public NoteAdapter(Context context, Cursor cursor, OnNoteClickListener listener) {
        this(context, cursor, listener, false);
    }

    public NoteAdapter(Context context, Cursor cursor, OnNoteClickListener listener, boolean isTrashView) {
        this.context = context;
        this.cursor = cursor;
        this.listener = listener;
        this.isTrashView = isTrashView;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) return;
        
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        String noteText = cursor.getString(cursor.getColumnIndexOrThrow("note_text"));
        String dateText = cursor.getString(cursor.getColumnIndexOrThrow("date_text"));
        int pinned = cursor.getInt(cursor.getColumnIndexOrThrow("pinned"));

        if (title != null && !title.isEmpty()) {
            holder.noteTitle.setText(title);
            holder.noteTitle.setVisibility(View.VISIBLE);
        } else {
            holder.noteTitle.setVisibility(View.GONE);
        }
        
        holder.noteText.setText(parseMarkdown(noteText));
        holder.dateText.setText(dateText);
        
        holder.pinIcon.setVisibility(pinned == 1 ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(id));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onNoteLongClick(id, v);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle;
        TextView noteText;
        TextView dateText;
        ImageView pinIcon;
        com.google.android.material.card.MaterialCardView cardView;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteText = itemView.findViewById(R.id.noteText);
            dateText = itemView.findViewById(R.id.dateText);
            pinIcon = itemView.findViewById(R.id.pinIcon);
            cardView = (com.google.android.material.card.MaterialCardView) itemView;
        }
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

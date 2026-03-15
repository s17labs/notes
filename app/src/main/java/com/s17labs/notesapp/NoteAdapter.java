package com.s17labs.notesapp;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    
    private final Context context;
    private Cursor cursor;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(long id);
        void onNoteLongClick(long id);
    }

    public NoteAdapter(Context context, Cursor cursor, OnNoteClickListener listener) {
        this.context = context;
        this.cursor = cursor;
        this.listener = listener;
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

        if (title != null && !title.isEmpty()) {
            holder.noteTitle.setText(title);
            holder.noteTitle.setVisibility(View.VISIBLE);
        } else {
            holder.noteTitle.setVisibility(View.GONE);
        }
        
        holder.noteText.setText(noteText);
        holder.dateText.setText(dateText);
        
        holder.itemView.setOnClickListener(v -> listener.onNoteClick(id));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onNoteLongClick(id);
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

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteText = itemView.findViewById(R.id.noteText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}

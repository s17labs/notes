package com.s17labs.notesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardItemAdapter extends RecyclerView.Adapter<BoardItemAdapter.BoardItemViewHolder> {

    private final Context context;
    private List<BoardItem> items;
    private final OnBoardItemListener listener;

    public interface OnBoardItemListener {
        void onItemClick(long itemId, String currentText);
        void onItemCompletedChanged(long itemId, boolean completed);
        void onItemDeleteRequested(long itemId);
    }

    public BoardItemAdapter(Context context, OnBoardItemListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<BoardItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void addItem(BoardItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(items, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(items, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<BoardItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public BoardItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.board_item_card, parent, false);
        return new BoardItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BoardItemViewHolder holder, int position) {
        BoardItem item = items.get(position);
        
        holder.itemText.setText(item.text);

        holder.itemCheckbox.setOnCheckedChangeListener(null);
        holder.itemCheckbox.setChecked(item.completed == 1);

        if (item.completed == 1) {
            holder.itemText.setAlpha(0.5f);
            holder.itemText.setPaintFlags(holder.itemText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.itemText.setAlpha(1f);
            holder.itemText.setPaintFlags(holder.itemText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.itemCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onItemCompletedChanged(item.id, isChecked);
            }
        });

        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item.id, item.text);
            }
        });

        holder.menuButton.setOnClickListener(null);
        holder.menuButton.setOnClickListener(v -> {
            if (listener != null) {
                showPopupMenu(v, item.id);
            }
        });
    }

    private void showPopupMenu(View anchor, long itemId) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "Delete");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                listener.onItemDeleteRequested(itemId);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BoardItemViewHolder extends RecyclerView.ViewHolder {
        CheckBox itemCheckbox;
        TextView itemText;
        ImageButton menuButton;

        BoardItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemCheckbox = itemView.findViewById(R.id.itemCheckbox);
            itemText = itemView.findViewById(R.id.itemText);
            menuButton = itemView.findViewById(R.id.menuButton);
        }
    }

    public static class BoardItem {
        public long id;
        public String text;
        public int completed;
        public int columnId;

        public BoardItem(long id, String text, int completed, int columnId) {
            this.id = id;
            this.text = text;
            this.completed = completed;
            this.columnId = columnId;
        }
    }
}

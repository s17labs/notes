package com.s17labs.notesapp;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BoardColumnAdapter extends RecyclerView.Adapter<BoardColumnAdapter.BoardColumnViewHolder> {

    private final Context context;
    private List<BoardColumn> columns;
    private final OnColumnHeaderListener headerListener;
    private final OnBoardItemListener itemListener;

    public interface OnColumnHeaderListener {
        void onColumnIndicatorClick(int columnId, int color);
        void onColumnIndicatorLongClick(int columnId);
        void onColumnTitleClick(int columnId, String currentTitle);
        void onColumnMenuClick(int columnId, View anchorView);
    }

    public interface OnBoardItemListener {
        void onItemClick(long itemId, String currentText);
        void onItemCompletedChanged(long itemId, boolean completed);
        void onItemDeleteRequested(long itemId);
        void onAddItemClick(int columnId);
        void onItemMoved(long itemId, int fromColumnId, int toColumnId, int newPosition);
        void onItemPositionChanged(long itemId, int newPosition);
    }

    public BoardColumnAdapter(Context context, OnColumnHeaderListener headerListener, OnBoardItemListener itemListener) {
        this.context = context;
        this.columns = new ArrayList<>();
        this.headerListener = headerListener;
        this.itemListener = itemListener;
    }

    public void setColumns(List<BoardColumn> columns) {
        this.columns = columns;
        notifyDataSetChanged();
    }

    public List<BoardColumn> getColumns() {
        return columns;
    }

    public void updateColumnItems(int columnId, List<BoardItemAdapter.BoardItem> items) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).id == columnId) {
                columns.get(i).items = items;
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void addItemToColumn(int columnId, BoardItemAdapter.BoardItem item) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).id == columnId) {
                columns.get(i).items.add(item);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void removeItemFromColumn(long itemId) {
        for (int i = 0; i < columns.size(); i++) {
            for (int j = 0; j < columns.get(i).items.size(); j++) {
                if (columns.get(i).items.get(j).id == itemId) {
                    columns.get(i).items.remove(j);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    public void updateItemInColumn(long itemId, boolean completed) {
        for (int i = 0; i < columns.size(); i++) {
            for (int j = 0; j < columns.get(i).items.size(); j++) {
                if (columns.get(i).items.get(j).id == itemId) {
                    columns.get(i).items.get(j).completed = completed ? 1 : 0;
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    @NonNull
    @Override
    public BoardColumnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.board_column_item, parent, false);
        return new BoardColumnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BoardColumnViewHolder holder, int position) {
        BoardColumn column = columns.get(position);
        
        holder.columnTitle.setText(column.title);
        
        if (column.items.size() > 0) {
            holder.columnCount.setVisibility(View.VISIBLE);
            holder.columnCount.setText(String.valueOf(column.items.size()));
        } else {
            holder.columnCount.setVisibility(View.GONE);
        }

        boolean hasItems = column.items.size() > 0;
        boolean allCompleted = false;
        if (hasItems) {
            allCompleted = true;
            for (BoardItemAdapter.BoardItem item : column.items) {
                if (item.completed != 1) {
                    allCompleted = false;
                    break;
                }
            }
        }

        updateColumnIndicator(holder.columnIndicator, column.color, hasItems, allCompleted);

        holder.columnIndicator.setOnClickListener(v -> headerListener.onColumnIndicatorClick(column.id, column.color));
        holder.columnIndicator.setOnLongClickListener(v -> {
            headerListener.onColumnIndicatorLongClick(column.id);
            return true;
        });

        holder.columnTitle.setOnClickListener(v -> headerListener.onColumnTitleClick(column.id, column.title));

        holder.columnMenuButton.setOnClickListener(v -> headerListener.onColumnMenuClick(column.id, v));

        BoardItemAdapter itemAdapter = new BoardItemAdapter(context, new BoardItemAdapter.OnBoardItemListener() {
            @Override
            public void onItemClick(long itemId, String currentText) {
                itemListener.onItemClick(itemId, currentText);
            }

            @Override
            public void onItemCompletedChanged(long itemId, boolean completed) {
                itemListener.onItemCompletedChanged(itemId, completed);
            }

            @Override
            public void onItemDeleteRequested(long itemId) {
                itemListener.onItemDeleteRequested(itemId);
            }
        });
        
        holder.columnCardsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.columnCardsRecyclerView.setAdapter(itemAdapter);
        itemAdapter.setItems(column.items);

        BoardItemTouchHelper touchHelper = new BoardItemTouchHelper(new BoardItemTouchHelper.OnItemMoveListener() {
            @Override
            public void onItemMoved(int fromPosition, int toPosition) {
                itemAdapter.moveItem(fromPosition, toPosition);
            }

            @Override
            public void onMoveCompleted(int fromPosition, int toPosition, int fromColumnId, int toColumnId) {
                if (fromColumnId != toColumnId && fromPosition < column.items.size()) {
                    long itemId = column.items.get(fromPosition).id;
                    itemListener.onItemMoved(itemId, fromColumnId, toColumnId, toPosition);
                } else if (fromColumnId == -1 && toColumnId == -1) {
                    for (int i = 0; i < column.items.size(); i++) {
                        long itemId = column.items.get(i).id;
                        itemListener.onItemPositionChanged(itemId, i);
                    }
                }
            }
        });
        
        new ItemTouchHelper(touchHelper).attachToRecyclerView(holder.columnCardsRecyclerView);

        holder.addItemButton.setOnClickListener(v -> itemListener.onAddItemClick(column.id));
    }

    private void updateColumnIndicator(ImageView indicator, int color, boolean hasItems, boolean allCompleted) {
        indicator.setImageDrawable(null);
        
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setSize(16, 16);
        
        if (allCompleted) {
            background.setColor(color);
            indicator.setImageResource(R.drawable.ic_checkmark_white);
        } else if (hasItems) {
            background.setColor(color);
            indicator.setImageResource(0);
        } else {
            background.setColor(android.graphics.Color.TRANSPARENT);
            background.setStroke(8, color);
            indicator.setImageDrawable(null);
        }
        
        indicator.setBackground(background);
    }

    @Override
    public int getItemCount() {
        return columns.size();
    }

    static class BoardColumnViewHolder extends RecyclerView.ViewHolder {
        TextView columnTitle;
        TextView columnCount;
        ImageView columnIndicator;
        ImageButton columnMenuButton;
        RecyclerView columnCardsRecyclerView;
        Button addItemButton;

        BoardColumnViewHolder(@NonNull View itemView) {
            super(itemView);
            columnTitle = itemView.findViewById(R.id.columnTitle);
            columnCount = itemView.findViewById(R.id.columnCount);
            columnIndicator = itemView.findViewById(R.id.columnIndicator);
            columnMenuButton = itemView.findViewById(R.id.columnMenuButton);
            columnCardsRecyclerView = itemView.findViewById(R.id.columnCardsRecyclerView);
            addItemButton = itemView.findViewById(R.id.addItemButton);
        }
    }

    public static class BoardColumn {
        public int id;
        public String title;
        public int color;
        public boolean completed;
        public List<BoardItemAdapter.BoardItem> items;

        public BoardColumn(int id, String title, int color, boolean completed) {
            this.id = id;
            this.title = title;
            this.color = color;
            this.completed = completed;
            this.items = new ArrayList<>();
        }
    }
}

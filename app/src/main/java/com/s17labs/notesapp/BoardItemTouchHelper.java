package com.s17labs.notesapp;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class BoardItemTouchHelper extends ItemTouchHelper.Callback {

    private final OnItemMoveListener listener;
    private boolean isDragging = false;
    private int dragFromPosition = -1;
    private int dragToPosition = -1;

    public interface OnItemMoveListener {
        void onItemMoved(int fromPosition, int toPosition);
        void onMoveCompleted(int fromPosition, int toPosition, int fromColumnId, int toColumnId);
    }

    public BoardItemTouchHelper(OnItemMoveListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        
        if (dragFromPosition == -1) {
            dragFromPosition = fromPosition;
        }
        dragToPosition = toPosition;
        
        listener.onItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            isDragging = true;
            if (dragFromPosition == -1) {
                dragFromPosition = viewHolder.getAdapterPosition();
            }
            View itemView = viewHolder.itemView;
            itemView.setAlpha(0.8f);
            itemView.setScaleX(1.02f);
            itemView.setScaleY(1.02f);
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (isDragging && dragFromPosition != -1 && dragToPosition != -1) {
                listener.onMoveCompleted(dragFromPosition, dragToPosition, -1, -1);
            }
            isDragging = false;
            dragFromPosition = -1;
            dragToPosition = -1;
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        
        View itemView = viewHolder.itemView;
        itemView.setAlpha(1f);
        itemView.setScaleX(1f);
        itemView.setScaleY(1f);
        
        if (isDragging && dragFromPosition != -1 && dragToPosition != -1) {
            listener.onMoveCompleted(dragFromPosition, dragToPosition, -1, -1);
        }
        
        isDragging = false;
        dragFromPosition = -1;
        dragToPosition = -1;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        
        if (isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            itemView.setElevation(4f);
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }
}

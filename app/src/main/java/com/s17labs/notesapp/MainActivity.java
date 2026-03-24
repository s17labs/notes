package com.s17labs.notesapp;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {
    
    private NoteDbHelper dbHelper;
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private FloatingActionButton fab;
    private TextView emptyView;
    private EditText searchEditText;
    private ImageButton clearButton;
    private ImageButton sortButton;
    private ImageButton menuButton;
    private ImageButton moreButton;
    private ImageButton viewModeButton;
    private boolean isGridView;
    private TextView toolbarTitle;
    private FrameLayout searchContainer;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private int sortBy = 0;
    private int sortOrder = 0;
    private PopupWindow sortPopup;
    private boolean isSortMenuOpen = false;
    private static final int SORT_BY_CREATED = 0;
    private static final int SORT_BY_MODIFIED = 1;
    private static final int SORT_NEWEST = 0;
    private static final int SORT_OLDEST = 1;
    
    private int currentView = VIEW_NOTES;
    private static final int VIEW_NOTES = 0;
    private static final int VIEW_PINNED = 1;
    private static final int VIEW_TRASH = 2;
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
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawable(null);

        dbHelper = new NoteDbHelper(this);
        
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        menuButton = findViewById(R.id.menuButton);
        toolbarTitle = findViewById(R.id.toolbarTitle);
        recyclerView = findViewById(R.id.notesRecyclerView);
        isGridView = NoteDbHelper.isGridView(this);
        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        searchContainer = findViewById(R.id.searchContainer);
        
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NoteActivity.class));
        });
        
        emptyView = findViewById(R.id.emptyView);
        
        searchEditText = findViewById(R.id.searchEditText);
        
        int accentColor = getResources().getColor(R.color.colorAccent, getTheme());
        
        Drawable cursorDrawable = getResources().getDrawable(R.drawable.cursor_drawable, getTheme());
        searchEditText.setTextCursorDrawable(cursorDrawable);
        
        Drawable handleLeft = searchEditText.getTextSelectHandleLeft();
        if (handleLeft != null) {
            handleLeft.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            searchEditText.setTextSelectHandleLeft(handleLeft);
        }
        Drawable handleRight = searchEditText.getTextSelectHandleRight();
        if (handleRight != null) {
            handleRight.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            searchEditText.setTextSelectHandleRight(handleRight);
        }
        Drawable handle = searchEditText.getTextSelectHandle();
        if (handle != null) {
            handle.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            searchEditText.setTextSelectHandle(handle);
        }
        
        clearButton = findViewById(R.id.clearButton);
        sortButton = findViewById(R.id.sortButton);
        moreButton = findViewById(R.id.moreButton);
        viewModeButton = findViewById(R.id.viewModeButton);
        viewModeButton.setImageResource(isGridView ? R.drawable.ic_list_view : R.drawable.ic_grid_view);
        
        sortButton.setOnLongClickListener(v -> {
            showTooltip(sortButton, "Sort");
            return true;
        });
        viewModeButton.setOnLongClickListener(v -> {
            showTooltip(viewModeButton, isGridView ? "Single column view" : "Multi-column view");
            return true;
        });
        moreButton.setOnLongClickListener(v -> {
            showTooltip(moreButton, "More options");
            return true;
        });
        fab.setOnLongClickListener(v -> {
            showTooltip(fab, "Create new note");
            return true;
        });
        
        menuButton.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
        menuButton.setOnLongClickListener(v -> {
            showTooltip(menuButton, "Menu");
            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_notes) {
                currentView = VIEW_NOTES;
                updateViewForCurrentSelection();
                loadNotes(searchEditText.getText().toString());
            } else if (id == R.id.nav_pinned) {
                currentView = VIEW_PINNED;
                updateViewForCurrentSelection();
                loadNotes("");
            } else if (id == R.id.nav_trash) {
                currentView = VIEW_TRASH;
                updateViewForCurrentSelection();
                loadNotes("");
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes(s.toString());
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
        
        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
            hideKeyboard();
            loadNotes("");
        });
        clearButton.setOnLongClickListener(v -> {
            showTooltip(clearButton, "Clear search");
            return true;
        });

        sortButton.setOnClickListener(this::showSortMenu);
        
        viewModeButton.setOnClickListener(v -> toggleViewMode());
        
        moreButton.setOnClickListener(this::showMoreMenu);
    }
    
    private void updateViewForCurrentSelection() {
        switch (currentView) {
            case VIEW_NOTES:
                toolbarTitle.setText("Notes");
                searchContainer.setVisibility(View.VISIBLE);
                sortButton.setVisibility(View.VISIBLE);
                viewModeButton.setVisibility(View.VISIBLE);
                moreButton.setVisibility(View.GONE);
                fab.show();
                break;
            case VIEW_PINNED:
                toolbarTitle.setText("Pinned Notes");
                searchContainer.setVisibility(View.VISIBLE);
                sortButton.setVisibility(View.VISIBLE);
                viewModeButton.setVisibility(View.VISIBLE);
                moreButton.setVisibility(View.GONE);
                fab.hide();
                break;
            case VIEW_TRASH:
                toolbarTitle.setText("Trash");
                searchContainer.setVisibility(View.GONE);
                sortButton.setVisibility(View.GONE);
                viewModeButton.setVisibility(View.GONE);
                moreButton.setVisibility(View.VISIBLE);
                fab.hide();
                break;
        }
    }

    private void toggleViewMode() {
        isGridView = !isGridView;
        NoteDbHelper.setGridView(this, isGridView);
        viewModeButton.setImageResource(isGridView ? R.drawable.ic_list_view : R.drawable.ic_grid_view);
        updateLayoutManager();
    }

    private void updateLayoutManager() {
        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        loadNotes(searchEditText.getText().toString());
    }
    
    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.trash_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_empty_trash) {
                showEmptyTrashDialog();
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void showEmptyTrashDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        TextView titleText = dialogView.findViewById(R.id.deleteTitle);
        titleText.setText("Empty Trash?");
        TextView messageText = dialogView.findViewById(R.id.deleteMessage);
        messageText.setText("All notes in trash will be permanently deleted and cannot be recovered.");
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            dbHelper.getWritableDatabase().delete("notes", "deleted=?", new String[]{"1"});
            Toast.makeText(this, "Trash emptied", Toast.LENGTH_SHORT).show();
            loadNotes("");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showSortMenu(View anchor) {
        if (sortPopup != null && sortPopup.isShowing()) {
            closeSortMenu();
            return;
        }

        isSortMenuOpen = true;
        animateSortToX();

        View popupView = LayoutInflater.from(this).inflate(R.layout.sort_popup, null);
        
        RadioGroup sortByGroup = popupView.findViewById(R.id.sortByGroup);
        RadioButton sortByCreated = popupView.findViewById(R.id.sortByCreated);
        RadioButton sortByModified = popupView.findViewById(R.id.sortByModified);
        
        ImageButton btnDescending = popupView.findViewById(R.id.btnDescending);
        ImageButton btnAscending = popupView.findViewById(R.id.btnAscending);

        sortByCreated.setChecked(sortBy == SORT_BY_CREATED);
        sortByModified.setChecked(sortBy == SORT_BY_MODIFIED);

        updateOrderButtons(btnDescending, btnAscending, sortOrder);

        sortByGroup.setOnCheckedChangeListener((group, checkedId) -> {
            sortBy = checkedId == R.id.sortByCreated ? SORT_BY_CREATED : SORT_BY_MODIFIED;
            loadNotes(searchEditText.getText().toString());
        });

        btnAscending.setOnClickListener(v -> {
            if (sortOrder != SORT_NEWEST) {
                sortOrder = SORT_NEWEST;
                updateOrderButtons(btnDescending, btnAscending, sortOrder);
                loadNotes(searchEditText.getText().toString());
            }
        });

        btnDescending.setOnClickListener(v -> {
            if (sortOrder != SORT_OLDEST) {
                sortOrder = SORT_OLDEST;
                updateOrderButtons(btnDescending, btnAscending, sortOrder);
                loadNotes(searchEditText.getText().toString());
            }
        });

        sortPopup = new PopupWindow(popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true);
        sortPopup.setBackgroundDrawable(null);
        sortPopup.setOutsideTouchable(true);
        sortPopup.setAnimationStyle(android.R.style.Animation_Dialog);
        
        sortPopup.setOnDismissListener(() -> {
            if (isSortMenuOpen) {
                closeSortMenu();
            }
        });
        
        sortPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, 
            anchor.getRight() - 220, 
            anchor.getBottom() + 114);
    }

    private void closeSortMenu() {
        isSortMenuOpen = false;
        animateXToSort();
    }

    private void updateOrderButtons(ImageButton btnDescending, ImageButton btnAscending, int currentOrder) {
        int accentColor = getResources().getColor(R.color.colorAccent, getTheme());
        int secondaryColor = getResources().getColor(R.color.textSecondary, getTheme());
        
        if (currentOrder == SORT_OLDEST) {
            btnDescending.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            btnAscending.setColorFilter(new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN));
        } else {
            btnDescending.setColorFilter(new PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN));
            btnAscending.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
        }
    }

    private void animateSortToX() {
        sortButton.setRotation(0f);
        sortButton.animate()
            .rotation(180f)
            .setDuration(150)
            .setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    sortButton.setImageResource(R.drawable.ic_clear);
                    sortButton.setRotation(0f);
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            })
            .start();
    }

    private void animateXToSort() {
        sortButton.setRotation(0f);
        sortButton.animate()
            .rotation(-180f)
            .setDuration(150)
            .setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    sortButton.setImageResource(R.drawable.ic_sort);
                    sortButton.setRotation(0f);
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            })
            .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(searchEditText.getText().toString());
    }

    private void loadNotes(String searchQuery) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        
        String sortColumn = sortBy == SORT_BY_CREATED ? "created_at" : "modified_at";
        String sortDirection = sortOrder == SORT_NEWEST ? "DESC" : "ASC";
        String sortOrderSql;
        String baseQuery;
        String[] queryArgs;
        
        switch (currentView) {
            case VIEW_PINNED:
                sortOrderSql = sortColumn + " " + sortDirection;
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    baseQuery = "SELECT * FROM notes WHERE pinned = 1 AND deleted = 0 AND (title LIKE ? OR note_text LIKE ?) ORDER BY " + sortOrderSql;
                    queryArgs = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%"};
                } else {
                    baseQuery = "SELECT * FROM notes WHERE pinned = 1 AND deleted = 0 ORDER BY " + sortOrderSql;
                    queryArgs = null;
                }
                break;
            case VIEW_TRASH:
                sortOrderSql = sortColumn + " " + sortDirection;
                baseQuery = "SELECT * FROM notes WHERE deleted = 1 ORDER BY " + sortOrderSql;
                queryArgs = null;
                break;
            default:
                sortOrderSql = "pinned DESC, " + sortColumn + " " + sortDirection;
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    baseQuery = "SELECT * FROM notes WHERE deleted = 0 AND (title LIKE ? OR note_text LIKE ?) ORDER BY " + sortOrderSql;
                    queryArgs = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%"};
                } else {
                    baseQuery = "SELECT * FROM notes WHERE deleted = 0 ORDER BY " + sortOrderSql;
                    queryArgs = null;
                }
                break;
        }
        
        cursor = db.rawQuery(baseQuery, queryArgs);
        
        adapter = new NoteAdapter(this, cursor, this, currentView == VIEW_TRASH);
        recyclerView.setAdapter(adapter);
        
        if (cursor.getCount() == 0) {
            switch (currentView) {
                case VIEW_PINNED:
                    emptyView.setText("No pinned notes");
                    break;
                case VIEW_TRASH:
                    emptyView.setText("No notes in Trash");
                    moreButton.setVisibility(View.GONE);
                    break;
                default:
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        emptyView.setText("No matching notes");
                    } else {
                        emptyView.setText("No notes yet.\nTap + to create one!");
                    }
                    break;
            }
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            if (currentView == VIEW_TRASH) {
                moreButton.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    @Override
    public void onNoteClick(long id) {
        Intent intent = new Intent(MainActivity.this, NotePreviewActivity.class);
        intent.putExtra("NOTE_ID", id);
        intent.putExtra("IS_TRASH_VIEW", currentView == VIEW_TRASH);
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(long id, View view) {
        showNoteMenu(view, id);
    }

    private void showDeleteForeverDialog(long noteId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        TextView titleText = dialogView.findViewById(R.id.deleteTitle);
        titleText.setText("Delete Forever?");
        TextView messageText = dialogView.findViewById(R.id.deleteMessage);
        messageText.setText("This note will be permanently deleted and cannot be recovered.");
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            dbHelper.getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
            Toast.makeText(this, "Note deleted permanently", Toast.LENGTH_SHORT).show();
            loadNotes("");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        dialog.getWindow().setLayout((int)(340 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showNoteMenu(View anchor, long noteId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note_options, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pinned FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        boolean isPinned = false;
        if (cursor.moveToFirst()) {
            isPinned = cursor.getInt(0) == 1;
        }
        cursor.close();
        final boolean pinned = isPinned;
        
        TextView txtPin = dialogView.findViewById(R.id.txtPin);
        View btnEdit = dialogView.findViewById(R.id.btnEdit);
        View btnPin = dialogView.findViewById(R.id.btnPin);
        View btnShare = dialogView.findViewById(R.id.btnShare);
        View btnRestore = dialogView.findViewById(R.id.btnRestore);
        TextView txtDelete = dialogView.findViewById(R.id.txtDelete);
        
        if (currentView == VIEW_TRASH) {
            btnEdit.setVisibility(View.GONE);
            btnPin.setVisibility(View.GONE);
            btnShare.setVisibility(View.GONE);
            btnRestore.setVisibility(View.VISIBLE);
            txtDelete.setText("Delete Forever");
        } else {
            btnEdit.setVisibility(View.VISIBLE);
            btnPin.setVisibility(View.VISIBLE);
            btnShare.setVisibility(View.VISIBLE);
            btnRestore.setVisibility(View.GONE);
            txtDelete.setText("Delete");
            txtPin.setText(isPinned ? "Unpin" : "Pin");
        }
        
        dialogView.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, NoteActivity.class);
            intent.putExtra("NOTE_ID", noteId);
            startActivity(intent);
        });
        dialogView.findViewById(R.id.btnEdit).setOnLongClickListener(v -> {
            showTooltip(btnEdit, "Edit note");
            return true;
        });
        dialogView.findViewById(R.id.btnPin).setOnLongClickListener(v -> {
            showTooltip(btnPin, pinned ? "Unpin note" : "Pin note");
            return true;
        });
        
        dialogView.findViewById(R.id.btnShare).setOnLongClickListener(v -> {
            showTooltip(btnShare, "Share note");
            return true;
        });

        dialogView.findViewById(R.id.btnRestore).setOnLongClickListener(v -> {
            showTooltip(btnRestore, "Restore note");
            return true;
        });
        
        dialogView.findViewById(R.id.btnDelete).setOnLongClickListener(v -> {
            showTooltip(dialogView.findViewById(R.id.btnDelete), currentView == VIEW_TRASH ? "Delete forever" : "Move to trash");
            return true;
        });
        
        dialogView.findViewById(R.id.btnPin).setOnClickListener(v -> {
            togglePin(noteId);
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnPin).setOnLongClickListener(v -> {
            Toast.makeText(this, pinned ? "Unpin note" : "Pin note", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        dialogView.findViewById(R.id.btnShare).setOnClickListener(v -> {
            shareNote(noteId);
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnShare).setOnLongClickListener(v -> {
            Toast.makeText(this, "Share note", Toast.LENGTH_SHORT).show();
            return true;
        });

        dialogView.findViewById(R.id.btnRestore).setOnClickListener(v -> {
            restoreNote(noteId);
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnRestore).setOnLongClickListener(v -> {
            Toast.makeText(this, "Restore note", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        dialogView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            dialog.dismiss();
            if (currentView == VIEW_TRASH) {
                showDeleteForeverDialog(noteId);
            } else {
                ContentValues values = new ContentValues();
                values.put("deleted", 1);
                dbHelper.getWritableDatabase().update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
                Toast.makeText(this, "Note moved to trash", Toast.LENGTH_SHORT).show();
                loadNotes(searchEditText.getText().toString());
            }
        });
        dialogView.findViewById(R.id.btnDelete).setOnLongClickListener(v -> {
            Toast.makeText(this, currentView == VIEW_TRASH ? "Delete forever" : "Move to trash", Toast.LENGTH_SHORT).show();
            return true;
        });

        dialog.show();
        dialog.getWindow().setLayout((int)(280 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void togglePin(long noteId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pinned FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        boolean isPinned = false;
        if (cursor.moveToFirst()) {
            isPinned = cursor.getInt(0) == 1;
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put("pinned", isPinned ? 0 : 1);
        db.update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        Toast.makeText(this, isPinned ? "Note unpinned" : "Note pinned", Toast.LENGTH_SHORT).show();
        loadNotes(searchEditText.getText().toString());
    }

    private void shareNote(long noteId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, note_text FROM notes WHERE _id=?", new String[]{String.valueOf(noteId)});
        if (cursor.moveToFirst()) {
            String title = cursor.getString(0);
            String noteText = cursor.getString(1);
            cursor.close();
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title != null ? title : "Note");
            shareIntent.putExtra(Intent.EXTRA_TEXT, noteText);
            startActivity(Intent.createChooser(shareIntent, "Share Note"));
        } else {
            cursor.close();
        }
    }

    public void restoreNote(long noteId) {
        ContentValues values = new ContentValues();
        values.put("deleted", 0);
        dbHelper.getWritableDatabase().update("notes", values, "_id=?", new String[]{String.valueOf(noteId)});
        Toast.makeText(this, "Note restored", Toast.LENGTH_SHORT).show();
        loadNotes(searchEditText.getText().toString());
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

package com.example.mob_dev_portfolio.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.widget.Toast;

import com.example.mob_dev_portfolio.AddFavouriteActivity.FavouriteAddressAdapter;
import com.example.mob_dev_portfolio.Helper.FavouriteAddressDatabaseHelper;

public class SwipeDeleteHandler extends ItemTouchHelper.SimpleCallback {
    private FavouriteAddressAdapter adapter;

    public SwipeDeleteHandler(FavouriteAddressAdapter adapter) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        String address = adapter.getAddress(position);

        FavouriteAddressDatabaseHelper dbHelper = new FavouriteAddressDatabaseHelper(adapter.context);
        boolean success = dbHelper.deleteAddress(address);

        if (success) {
            Toast.makeText(adapter.context, "Address deleted", Toast.LENGTH_SHORT).show();
            adapter.deleteAddress(address);
        } else {
            Toast.makeText(adapter.context, "Error deleting address", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        float itemHeight = itemView.getBottom() - itemView.getTop();

        // Set up the paint for the red background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.RED);

        // Create the red background rectangle
        RectF backgroundRect = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        c.drawRect(backgroundRect, backgroundPaint);

        // Set up the paint for the "delete" label
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        // Calculate the position for the "delete" label
        float textMargin = (itemHeight - textPaint.getTextSize()) / 2;
        float textPosX = itemView.getRight() - textMargin;
        float textPosY = itemView.getTop() + itemHeight / 2 + textPaint.getTextSize() / 2 - textPaint.descent() / 2;

        // Draw the "delete" label only if there's enough space
        if (-dX > textMargin + textPaint.getTextSize()) {
            c.drawText("Delete", textPosX, textPosY, textPaint);
        }
    }
}
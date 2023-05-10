package com.example.mob_dev_portfolio;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mob_dev_portfolio.Handler.SwipeDeleteHandler;
import com.example.mob_dev_portfolio.Helper.DatabaseHelper;
import com.example.mob_dev_portfolio.Helper.FavouriteAddressDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class AddFavouriteActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_MAIN_ACTIVITY = 1;
    private EditText favoriteAddress;
    private FavouriteAddressAdapter adapter;
    private List<String> addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favourite);

        adapter = new FavouriteAddressAdapter(this, addresses, this::onAddressClicked);
        favoriteAddress = findViewById(R.id.favoriteAddress);
        ImageButton saveButton = findViewById(R.id.check_button);
        saveButton.setOnClickListener(view -> saveFavoriteAddress());

        addresses = new ArrayList<>();

        adapter = new FavouriteAddressAdapter(this, addresses, this::onAddressClicked);
        RecyclerView recyclerView = findViewById(R.id.favorite_address_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeDeleteHandler(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        FavouriteAddressDatabaseHelper dbHelper = new FavouriteAddressDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {DatabaseHelper.FavoriteAddressEntry.COLUMN_NAME_ADDRESS};
        Cursor cursor = db.query(
                DatabaseHelper.FavoriteAddressEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        addresses.clear();

        while (cursor.moveToNext()) {
            String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.FavoriteAddressEntry.COLUMN_NAME_ADDRESS));
            addresses.add(address);
        }
        cursor.close();

        adapter.notifyDataSetChanged();
    }

    private void saveFavoriteAddress() {
        String rawAddress = favoriteAddress.getText().toString();
        String address = rawAddress.trim().replaceAll("\\s+", " ");

        if (rawAddress.length() == 0 || address.length() == 0) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            return;
        }

        FavouriteAddressDatabaseHelper dbHelper = new FavouriteAddressDatabaseHelper(this);
        boolean success = dbHelper.addAddress(address);

        if (success) {
            Toast.makeText(this, "Address added", Toast.LENGTH_SHORT).show();
            addresses.add(address);
            adapter.notifyDataSetChanged();
            favoriteAddress.setText("");
        } else {
            Toast.makeText(this, "Error adding address", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAddressClicked(String address) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selectedAddress", address);
        startActivityForResult(intent, REQUEST_CODE_MAIN_ACTIVITY);
    }

    private void deleteFavoriteAddress() {
        String address = favoriteAddress.getText().toString().trim().replaceAll("[\\s]+", " ");
        if (address.isEmpty()) {
            Toast.makeText(this, "Please select an address to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        FavouriteAddressDatabaseHelper dbHelper = new FavouriteAddressDatabaseHelper(this);
        boolean success = dbHelper.deleteAddress(address);

        if (success) {
            Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show();
            addresses.remove(address);
            adapter.notifyDataSetChanged();
            favoriteAddress.setText("");
        } else {
            Toast.makeText(this, "Error deleting address", Toast.LENGTH_SHORT).show();
        }
    }

    public static class FavouriteAddressAdapter extends RecyclerView.Adapter<FavouriteAddressAdapter.ViewHolder> {

        public Context context;
        private final List<String> addresses;
        private final OnAddressClickListener onAddressClickListener;

        public FavouriteAddressAdapter(Context context, List<String> addresses, OnAddressClickListener onAddressClickListener) {
            this.context = context;
            this.addresses = addresses;
            this.onAddressClickListener = onAddressClickListener;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView addressTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                addressTextView = itemView.findViewById(R.id.addressTextView);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_favorite_address, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String address = addresses.get(position);
            holder.addressTextView.setText(address);

            holder.itemView.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedAddress", address);
                ((Activity) context).setResult(Activity.RESULT_OK, resultIntent);
                ((Activity) context).finish();
            });
        }

        @Override
        public int getItemCount() {
            return addresses.size();
        }

        public String getAddress(int position) {
            return addresses.get(position);
        }

        public void deleteAddress(String address) {
            addresses.remove(address);
            notifyDataSetChanged();
        }
    }

    public interface OnAddressClickListener {
        void onAddressClick(String address);
    }
}

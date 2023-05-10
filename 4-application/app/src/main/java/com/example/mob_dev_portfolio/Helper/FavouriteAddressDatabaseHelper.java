package com.example.mob_dev_portfolio.Helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FavouriteAddressDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorite_address.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "favorite_address";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME_ADDRESS = "address";

    public FavouriteAddressDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_FAVORITE_ADDRESS_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME_ADDRESS + " TEXT NOT NULL);";
        db.execSQL(SQL_CREATE_FAVORITE_ADDRESS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Recreate the table
        onCreate(db);
    }

    public boolean addAddress(String address) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ADDRESS, address);
        long newRowId = db.insert(TABLE_NAME, null, values);
        db.close();
        return newRowId != -1;
    }

    public List<String> getAllAddresses() {
        List<String> addresses = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {COLUMN_NAME_ADDRESS};
        Cursor cursor = db.query(
                TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        while (cursor.moveToNext()) {
            String address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_ADDRESS));
            addresses.add(address);
        }
        cursor.close();
        db.close();
        return addresses;
    }

    public boolean deleteAddress(String address) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = DatabaseHelper.FavoriteAddressEntry.COLUMN_NAME_ADDRESS + " = ?";
        String[] whereArgs = {address};
        int rowsDeleted = db.delete(DatabaseHelper.FavoriteAddressEntry.TABLE_NAME, whereClause, whereArgs);
        return rowsDeleted > 0;
    }

}

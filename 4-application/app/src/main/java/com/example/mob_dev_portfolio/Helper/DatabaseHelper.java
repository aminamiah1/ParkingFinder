package com.example.mob_dev_portfolio.Helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "favorite_address.db";
    private static final int DATABASE_VERSION = 1;

    public static class FavoriteAddressEntry implements BaseColumns {
        public static final String TABLE_NAME = "favorite_address";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FavoriteAddressEntry.TABLE_NAME + " (" +
                    FavoriteAddressEntry._ID + " INTEGER PRIMARY KEY," +
                    FavoriteAddressEntry.COLUMN_NAME_ADDRESS + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FavoriteAddressEntry.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_FAVORITE_ADDRESS_TABLE = "CREATE TABLE " +
                FavoriteAddressEntry.TABLE_NAME + " (" +
                FavoriteAddressEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FavoriteAddressEntry.COLUMN_NAME_ADDRESS + " TEXT NOT NULL" +
                "); ";

        db.execSQL(SQL_CREATE_FAVORITE_ADDRESS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public boolean addAddress(String name, String address, double latitude, double longitude) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoriteAddressEntry.COLUMN_NAME_NAME, name);
        values.put(FavoriteAddressEntry.COLUMN_NAME_ADDRESS, address);
        values.put(FavoriteAddressEntry.COLUMN_NAME_LATITUDE, latitude);
        values.put(FavoriteAddressEntry.COLUMN_NAME_LONGITUDE, longitude);
        long newRowId = db.insert(FavoriteAddressEntry.TABLE_NAME, null, values);
        return newRowId != -1;
    }
}

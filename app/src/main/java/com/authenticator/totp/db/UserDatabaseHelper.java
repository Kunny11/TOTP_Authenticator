package com.authenticator.totp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "user";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PASSWORD = "password";

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PASSWORD + " TEXT" + ")";
        db.execSQL(CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertPassword(String password) {
        if (isPasswordRegistered()) {
            return false; // Password already registered
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, password);
        db.insert(TABLE_NAME, null, values);
        db.close();
        return true;
    }

    public boolean isPasswordRegistered() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_PASSWORD},
                null, null, null, null, null);
        boolean hasPassword = (cursor.getCount() > 0);
        cursor.close();
        return hasPassword;
    }

    public String getPassword() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_PASSWORD},
                null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String password = cursor.getString(0);
            cursor.close();
            return password;
        } else {
            return null;
        }
    }

    public void logDatabaseContent() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
                String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
                Log.d("DatabaseHelper", "ID: " + id + ", Password: " + password);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}

package com.authenticator.totp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.mindrot.jbcrypt.BCrypt;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "user.db";
    private static final int DATABASE_VERSION = 2;
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
        Log.d("UserDatabaseHelper", "Database upgraded. Old passwords removed.");
    }

    public boolean insertPassword(String password) {
        if (isPasswordRegistered()) {
            Log.d("UserDatabaseHelper", "Password already registered, cannot insert new password.");
            return false;
        }

        String hashedPassword = hashPassword(password);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, hashedPassword);
        long result = db.insert(TABLE_NAME, null, values);
        db.close();

        if (result == -1) {
            Log.d("UserDatabaseHelper", "Failed to insert password into database.");
        } else {
            Log.d("UserDatabaseHelper", "Password successfully inserted with ID: " + result);
        }

        return result != -1;
    }

    public boolean updatePassword(String newPassword) {
        String hashedPassword = hashPassword(newPassword);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, hashedPassword);

        int rowsAffected = db.update(TABLE_NAME, values, null, null);
        db.close();
        return rowsAffected > 0;
    }

    public boolean verifyPassword(String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_PASSWORD},
                null, null, null, null, null);
        boolean isValid = false;
        if (cursor.moveToFirst()) {
            String storedHashedPassword = cursor.getString(0);
            isValid = verifyPasswordHash(password, storedHashedPassword);
        }
        cursor.close();
        return isValid;
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    private boolean verifyPasswordHash(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    public boolean isPasswordRegistered() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_PASSWORD}, null, null, null, null, null);
        boolean hasPassword = (cursor.getCount() > 0);
        cursor.close();
        Log.d("UserDatabaseHelper", "isPasswordRegistered: " + hasPassword);
        return hasPassword;
    }

    public void logDatabaseContent() {
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null)) {

            if (cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndex(COLUMN_ID);
                    int passwordIndex = cursor.getColumnIndex(COLUMN_PASSWORD);

                    if (idIndex != -1 && passwordIndex != -1) {
                        int id = cursor.getInt(idIndex);
                        String password = cursor.getString(passwordIndex);
                        Log.d("DatabaseHelper", "ID: " + id + ", Hashed Password: " + password);
                    } else {
                        Log.e("DatabaseHelper", "Column not found: " +
                                (idIndex == -1 ? COLUMN_ID : COLUMN_PASSWORD));
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error logging database content", e);
        }
    }
}

package com.authenticator.totp.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import com.authenticator.totp.OtpInfo;

import java.util.ArrayList;
import java.util.List;

public class OtpDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "otp.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_NAME = "otp_accounts";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ACCOUNT_NAME = "account_name";
    private static final String COLUMN_ISSUER = "issuer";
    private static final String COLUMN_SECRET = "secret";

    //private static final String COLUMN_IS_MANUAL = "is_manual";
    private static final String COLUMN_OTP_LENGTH = "otp_length";
    private static final String COLUMN_USER_TIME_STEP = "user_time_step";
    private static final String COLUMN_ALGORITHM = "algorithm";

    public OtpDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ACCOUNT_NAME + " TEXT,"
                + COLUMN_ISSUER + " TEXT,"
                + COLUMN_SECRET + " TEXT,"
                + COLUMN_OTP_LENGTH + " INTEGER,"
                + COLUMN_USER_TIME_STEP + " INTEGER,"
                + COLUMN_ALGORITHM + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Drop older table if existed
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            // Create tables again
            onCreate(db);
        }
    }


    public void addOtpInfo(OtpInfo otpInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCOUNT_NAME, otpInfo.accountName);
        values.put(COLUMN_ISSUER, otpInfo.issuer);
        values.put(COLUMN_SECRET, otpInfo.secret);
        //values.put(COLUMN_IS_MANUAL, otpInfo.isManual ? 1 : 0);
        values.put(COLUMN_OTP_LENGTH, otpInfo.otpLength);
        values.put(COLUMN_USER_TIME_STEP, otpInfo.userTimeStep);
        values.put(COLUMN_ALGORITHM, otpInfo.algorithm);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public List<OtpInfo> getAllOtpInfo() {
        List<OtpInfo> otpInfoList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                OtpInfo otpInfo = new OtpInfo();
                otpInfo.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                otpInfo.setAccountName(cursor.getString(cursor.getColumnIndex(COLUMN_ACCOUNT_NAME)));
                otpInfo.setIssuer(cursor.getString(cursor.getColumnIndex(COLUMN_ISSUER)));
                otpInfo.setSecret(cursor.getString(cursor.getColumnIndex(COLUMN_SECRET)));
                //otpInfo.setIsManual(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_MANUAL)) == 1);
                otpInfo.setOtpLength(cursor.getInt(cursor.getColumnIndex(COLUMN_OTP_LENGTH)));
                otpInfo.setUserTimeStep(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_TIME_STEP)));
                otpInfo.setAlgorithm(cursor.getString(cursor.getColumnIndex(COLUMN_ALGORITHM)));

                otpInfoList.add(otpInfo);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return otpInfoList;
    }

    public void updateOtpInfo(OtpInfo otpInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCOUNT_NAME, otpInfo.accountName);
        values.put(COLUMN_ISSUER, otpInfo.issuer);
        values.put(COLUMN_SECRET, otpInfo.secret);
        //values.put(COLUMN_IS_MANUAL, otpInfo.isManual ? 1 : 0);
        values.put(COLUMN_OTP_LENGTH, otpInfo.otpLength);
        values.put(COLUMN_USER_TIME_STEP, otpInfo.userTimeStep);
        values.put(COLUMN_ALGORITHM, otpInfo.algorithm);

        db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(otpInfo.getId())});
        db.close();
    }

    public void deleteOtpInfo(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

}

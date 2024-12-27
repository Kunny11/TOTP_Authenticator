package com.authenticator.totp.db;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.util.Log;

import com.authenticator.totp.OtpInfo;

import java.util.ArrayList;
import java.util.List;

public class OtpDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "otp.db";
    private static final int DATABASE_VERSION = 6;

    private static final String TABLE_NAME = "otp_accounts";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ACCOUNT_NAME = "account_name";
    private static final String COLUMN_ISSUER = "issuer";
    private static final String COLUMN_SECRET = "secret";

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
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }


    public void addOtpInfo(OtpInfo otpInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            //Encrypting sensitive information
            String encryptedAccountName = Encryption.encrypt(otpInfo.accountName);
            String encryptedIssuer = Encryption.encrypt(otpInfo.issuer);
            String encryptedSecret = Encryption.encrypt(otpInfo.secret);

            values.put(COLUMN_ACCOUNT_NAME, encryptedAccountName);
            values.put(COLUMN_ISSUER, encryptedIssuer);
            values.put(COLUMN_SECRET, encryptedSecret);
            values.put(COLUMN_OTP_LENGTH, otpInfo.otpLength);
            values.put(COLUMN_USER_TIME_STEP, otpInfo.userTimeStep);
            values.put(COLUMN_ALGORITHM, otpInfo.algorithm);

            long result = db.insert(TABLE_NAME, null, values);
            if (result == -1) {
                Log.e("failed", "Failed to insert OTP info");
            } else {
                Log.d("inserted", "Inserted OTP info with id: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting OTP info: ", e);
        } finally {
            db.close();
        }
    }

    // Batch insert method
    public void addOtpInfoBatch(List<OtpInfo> otpInfoList) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (OtpInfo otpInfo : otpInfoList) {
                ContentValues values = new ContentValues();
                // Encrypting sensitive information
                String encryptedAccountName = Encryption.encrypt(otpInfo.accountName);
                String encryptedIssuer = Encryption.encrypt(otpInfo.issuer);
                String encryptedSecret = Encryption.encrypt(otpInfo.secret);

                values.put(COLUMN_ACCOUNT_NAME, encryptedAccountName);
                values.put(COLUMN_ISSUER, encryptedIssuer);
                values.put(COLUMN_SECRET, encryptedSecret);
                values.put(COLUMN_OTP_LENGTH, otpInfo.otpLength);
                values.put(COLUMN_USER_TIME_STEP, otpInfo.userTimeStep);
                values.put(COLUMN_ALGORITHM, otpInfo.algorithm);

                // Insert OTP info in the batch
                db.insert(TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
            Log.d("batch_insert", "Batch insert successful");
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting OTP info in batch: ", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    public List<OtpInfo> getAllOtpInfo() {
        List<OtpInfo> otpInfoList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery(selectQuery, null)) {

            if (cursor.moveToFirst()) {
                do {
                    OtpInfo otpInfo = new OtpInfo();

                    int idIndex = cursor.getColumnIndex(COLUMN_ID);
                    int accountNameIndex = cursor.getColumnIndex(COLUMN_ACCOUNT_NAME);
                    int issuerIndex = cursor.getColumnIndex(COLUMN_ISSUER);
                    int secretIndex = cursor.getColumnIndex(COLUMN_SECRET);
                    int otpLengthIndex = cursor.getColumnIndex(COLUMN_OTP_LENGTH);
                    int userTimeStepIndex = cursor.getColumnIndex(COLUMN_USER_TIME_STEP);
                    int algorithmIndex = cursor.getColumnIndex(COLUMN_ALGORITHM);

                    if (idIndex != -1) otpInfo.setId(cursor.getInt(idIndex));
                    if (accountNameIndex != -1)
                        otpInfo.setAccountName(Encryption.decrypt(cursor.getString(accountNameIndex)));
                    if (issuerIndex != -1)
                        otpInfo.setIssuer(Encryption.decrypt(cursor.getString(issuerIndex)));
                    if (secretIndex != -1)
                        otpInfo.setSecret(Encryption.decrypt(cursor.getString(secretIndex)));
                    if (otpLengthIndex != -1) otpInfo.setOtpLength(cursor.getInt(otpLengthIndex));
                    if (userTimeStepIndex != -1)
                        otpInfo.setUserTimeStep(cursor.getInt(userTimeStepIndex));
                    if (algorithmIndex != -1)
                        otpInfo.setAlgorithm(cursor.getString(algorithmIndex));

                    otpInfoList.add(otpInfo);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching OTP info: ", e);
        }

        return otpInfoList;
    }

    public void updateOtpInfo(OtpInfo otpInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCOUNT_NAME, otpInfo.accountName);
        values.put(COLUMN_ISSUER, otpInfo.issuer);
        values.put(COLUMN_SECRET, otpInfo.secret);
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
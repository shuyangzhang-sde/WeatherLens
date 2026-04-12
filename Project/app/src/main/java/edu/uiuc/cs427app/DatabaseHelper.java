package edu.uiuc.cs427app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cs427app.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";

    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_THEME_DESC = "theme_description";
    private static final String TABLE_CITIES = "cities";
    private static final String COL_CITY_ID = "id";
    private static final String COL_CITY_USERNAME = "username";
    private static final String COL_CITY_NAME = "city";

    private static final String COL_THEME_JSON = "theme_json";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COL_PASSWORD + " TEXT NOT NULL, "
                + COL_THEME_DESC + " TEXT, "
                + COL_THEME_JSON + " TEXT)";
        db.execSQL(createUsersTable);

        String createCitiesTable = "CREATE TABLE " + TABLE_CITIES + " ("
                + COL_CITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CITY_USERNAME + " TEXT NOT NULL, "
                + COL_CITY_NAME + " TEXT NOT NULL)";
        db.execSQL(createCitiesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CITIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public boolean addUser(String username, String password, String themeDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, hashPassword(password));
        values.put(COL_THEME_DESC, themeDescription);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS
                        + " WHERE " + COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, hashedPassword});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + " = ?",
                new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String getUserThemeDescription(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_THEME_DESC + " FROM " + TABLE_USERS
                        + " WHERE " + COL_USERNAME + " = ?",
                new String[]{username});
        String theme = null;
        if (cursor.moveToFirst()) {
            theme = cursor.getString(0);
        }
        cursor.close();
        return theme != null ? theme : "";
    }

    public String getThemeJson(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_THEME_JSON + " FROM " + TABLE_USERS
                        + " WHERE " + COL_USERNAME + " = ?",
                new String[]{username});
        String json = null;
        if (cursor.moveToFirst()) {
            json = cursor.getString(0);
        }
        cursor.close();
        return json != null ? json : "";
    }

    public void updateThemeJson(String username, String themeJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_THEME_JSON, themeJson);
        db.update(TABLE_USERS, values, COL_USERNAME + " = ?", new String[]{username});
    }

    public boolean addCityForUser(String username, String city) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CITY_USERNAME, username);
        values.put(COL_CITY_NAME, city);

        long result = db.insert(TABLE_CITIES, null, values);
        return result != -1;
    }

    public boolean removeCityForUser(String username, String city) {
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_CITIES,
                COL_CITY_USERNAME + " = ? AND " + COL_CITY_NAME + " = ?",
                new String[]{username, city}
        );

        return rowsDeleted > 0;
    }

    public boolean cityExistsForUser(String username, String city) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_CITIES
                        + " WHERE " + COL_CITY_USERNAME + " = ? AND " + COL_CITY_NAME + " = ?",
                new String[]{username, city});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public ArrayList<String> getCitiesForUser(String username) {
        ArrayList<String> cities = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_CITY_NAME + " FROM " + TABLE_CITIES
                        + " WHERE " + COL_CITY_USERNAME + " = ?",
                new String[]{username});

        if (cursor.moveToFirst()) {
            do {
                cities.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return cities;
    }
}
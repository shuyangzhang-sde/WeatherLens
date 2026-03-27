package edu.uiuc.cs427app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database helper that manages user accounts and per-user city lists.
 * Provides methods for user registration, authentication, and city CRUD operations.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cs427app.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_THEME_DESC = "theme_description";

    /**
     * Constructs a new DatabaseHelper bound to the given application context.
     *
     * @param context the application context used to create or open the database
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * Creates the users table (with unique usernames) and the user_cities table
     * (with a foreign key reference to the users table).
     *
     * @param db the newly created database instance
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COL_PASSWORD + " TEXT NOT NULL, "
                + COL_THEME_DESC + " TEXT)";
        db.execSQL(createUsersTable);
    }

    /**
     * Called when the database needs to be upgraded. Drops existing tables
     * and recreates them. All existing data is lost.
     *
     * @param db         the database being upgraded
     * @param oldVersion the old database version number
     * @param newVersion the new database version number
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CITIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /**
     * Hashes a plaintext password using SHA-256.
     *
     * @param password the plaintext password to hash
     * @return the hex-encoded SHA-256 hash of the password
     */
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

    /**
     * Registers a new user with the given credentials and theme description.
     *
     * @param username         the desired username (must be unique)
     * @param password         the plaintext password (will be hashed before storage)
     * @param themeDescription a natural-language description of the user's preferred UI theme
     * @return true if the user was created successfully, false if the username already exists
     */
    public boolean addUser(String username, String password, String themeDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, hashPassword(password));
        values.put(COL_THEME_DESC, themeDescription);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    /**
     * Validates user credentials for login.
     *
     * @param username the username to check
     * @param password the plaintext password to verify
     * @return true if the username exists and the password matches
     */
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

    /**
     * Checks whether a username is already registered.
     *
     * @param username the username to look up
     * @return true if the username exists in the database
     */
    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + " = ?",
                new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * Retrieves the theme description for the given user.
     *
     * @param username the username whose theme to retrieve
     * @return the stored theme description, or null if the user is not found
     */
    public String getUserTheme(String username) {
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
        return theme;
    }
}
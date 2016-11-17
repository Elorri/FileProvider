package com.example.android.fileprovider;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

public class CustomFileProvider extends FileProvider {

    // will match public apps dir files and public specific app dir files like
    // exple content://storage/emulated/0/Documents/_nebo/welcome.nebo
    // exple content://storage/emulated/0/Images/_nebo/welcome.nebo
    // exple content://sdcard/Zoho Mail/attachments/Welcome.nebo
    // exple content://storage/emulated/0/Android/data/com.my.mail/cache/attachments/myscripthwr%40gmail.com/myscripthwr%40gmail.com/14782617120000000658/1971668901.nebo
    // or uri with schemes like 
    // exple file:///storage/emulated/0/Documents/_nebo/welcome.nebo
    // exple file:///storage/emulated/0/Images/_nebo/welcome.nebo
    // exple file:///sdcard/Zoho Mail/attachments/Welcome.nebo
    // exple file:///storage/emulated/0/Android/data/com.my.mail/cache/attachments/myscripthwr%40gmail.com/myscripthwr%40gmail.com/14782617120000000658/1971668901.nebo
    private static final int EXTERNAL_URI = 100;

    private static final int NOTEBOOK = 200;
    private static final int COLLECTION = 300;

    Context mContext;


    private static final UriMatcher sUriMatcher = buildUriMatcher();

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(CustomContract.CONTENT_AUTHORITY, CustomContract.PATH_NOTEBOOK, NOTEBOOK);
        matcher.addURI(CustomContract.CONTENT_AUTHORITY, CustomContract.PATH_COLLECTION, COLLECTION);
        // matcher.addURI(TieUsContract.CONTENT_AUTHORITY, TieUsContract.AddActionData.PATH_ADD_ACTION + "/#/#/#", DATA_ADD_ACTION_VALIDATE);
        return matcher;
    }

    private int buildUriMatcher(Uri uri) {
        String localPath = mContext.getCacheDir().getPath();
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "local path" + localPath);

        String path = uri.getPath();
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "path" + path);

        if (path.startsWith(localPath)) {
            return EXTERNAL_URI;
        }
        return sUriMatcher.match(uri);
    }

    @Override
    public boolean onCreate() {
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "");
        mContext = getContext();
        return super.onCreate();
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "");
        switch (buildUriMatcher(uri)) {
            case EXTERNAL_URI:
                return null;
            case NOTEBOOK:
                return null;
            case COLLECTION:
                return null;
            default:
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "");
        return getType(uri);
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return insert(uri, contentValues);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return delete(uri, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return update(uri, values, selection, selectionArgs);
    }
}

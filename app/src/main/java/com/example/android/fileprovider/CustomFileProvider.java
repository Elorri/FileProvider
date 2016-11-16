package com.example.android.fileprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

public class CustomFileProvider extends FileProvider {


    @Override
    public boolean onCreate() {
        return super.onCreate();
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.e("TAG", Thread.currentThread().getStackTrace()[2]+"");
        return super.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        Log.e("TAG", Thread.currentThread().getStackTrace()[2]+"");
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

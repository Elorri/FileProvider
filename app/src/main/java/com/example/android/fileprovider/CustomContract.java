package com.example.android.fileprovider;


import android.net.Uri;

public class CustomContract {
    public static final String CONTENT_AUTHORITY = "com.example.android.fileprovider";
    public static String PATH_NOTEBOOK="notebook";
    public static String PATH_COLLECTION="collection";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static Uri buildNotebookUri() {
        return BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_NOTEBOOK)
                .build();
    }
}

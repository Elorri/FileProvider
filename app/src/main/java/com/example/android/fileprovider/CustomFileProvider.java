package com.example.android.fileprovider;

import android.content.ContentProvider;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

//A content provider for data streams provides access to its data with a file descriptor object such as AssetFileDescriptor instead of a Cursor object.
//To "transform" the cursor into an AssetFileDescriptor we needs to implements PipeDataWriter<Cursor>
public class CustomFileProvider extends FileProvider implements ContentProvider.PipeDataWriter<Cursor> {

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


    private static final int NEBO_FILE_URI = 400; //Will match content://com.example.android.fileprovider/appDir/.tmp/hello(1).txt

    Context mContext;


    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final String TAG=CustomFileProvider.class.getName();

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(CustomContract.CONTENT_AUTHORITY, CustomContract.PATH_NOTEBOOK, NOTEBOOK);
        matcher.addURI(CustomContract.CONTENT_AUTHORITY, CustomContract.PATH_COLLECTION, COLLECTION);
        matcher.addURI(CustomContract.CONTENT_AUTHORITY, "appDir/" + CustomContract.PATH_TMP_USER + "/*", NEBO_FILE_URI);

        return matcher;
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
        switch (sUriMatcher.match(uri)) {
            case EXTERNAL_URI:
                return null;
            case NOTEBOOK:
                return null;
            case COLLECTION:
                return null;
            case NEBO_FILE_URI:
                Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "NEBO_FILE_URI");
                return null;
            default:
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        super.getType(uri);

        Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "uri " + uri);
        switch (sUriMatcher.match(uri)) {
            case NEBO_FILE_URI:
                Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "NEBO_FILE_URI " + CustomContract.CONTENT_ITEM_TYPE);
                return CustomContract.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    /**
     * Returns a stream of data for each supported stream type. This method does a query on the
     * incoming URI, then uses
     * {@link android.content.ContentProvider#openPipeHelper(Uri, String, Bundle, Object,
     * PipeDataWriter)} to start another thread in which to convert the data into a stream.
     *
     * @param uri            The URI pattern that points to the data stream
     * @param mimeTypeFilter A String containing a MIME type. This method tries to get a stream of
     *                       data with this MIME type.
     * @param opts           Additional options supplied by the caller.  Can be interpreted as
     *                       desired by the content provider.
     * @return AssetFileDescriptor A handle to the file.
     * @throws FileNotFoundException if there is no file associated with the incoming URI.
     */
    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {
        // Checks to see if the MIME type filter matches a supported MIME type.
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);
        // If the MIME type is supported
        if (mimeTypes != null) {
            //Query if file is existing
            // Retrieves the note for this URI. Uses the query method defined for this provider,
            // rather than using the database query method.
            Cursor c = query(
                    uri,                    // The URI of a note
                    null,   // Gets a projection containing the note's ID, title,
                    // and contents
                    null,                   // No WHERE clause, get all matching records
                    null,                   // Since there is no WHERE clause, no selection criteria
                    null                    // Use the default sort order (modification date,
                    // descending
            );
            // If the query fails or the cursor is empty, stop
            if (c == null || !c.moveToFirst()) {
                // If the cursor is empty, simply close the cursor and return
                if (c != null) {
                    c.close();
                }
                // If the cursor is null, throw an exception
                throw new FileNotFoundException("Unable to query " + uri);
            }
            // Start a new thread that pipes the stream data back to the caller.
            return new AssetFileDescriptor(openPipeHelper(uri, mimeTypes[0], opts, c, this), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        }
        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    /**
     * Implementation of {@link android.content.ContentProvider.PipeDataWriter}
     * to perform the actual work of converting the data in one of cursors to a
     * stream of data for the client to read.
     */
    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, Cursor cursor) {
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "See if we do different stream import depending on the data type given");

        FileOutputStream out = new FileOutputStream(output.getFileDescriptor());
        PrintWriter printWriter = null;
        String charset="UTF-8";
        try {
            printWriter = new PrintWriter(new OutputStreamWriter(out, charset));
//            printWriter.println(cursor.getString(READ_NOTE_TITLE_INDEX));
//            printWriter.println("");
//            printWriter.println(cursor.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "There was a problem encoding text using "+charset, e);
        } finally {
            cursor.close();
            if (printWriter != null) {
                printWriter.flush();
            }
            try {
                out.close();
            } catch (IOException e) {
            }
        }

    }
}

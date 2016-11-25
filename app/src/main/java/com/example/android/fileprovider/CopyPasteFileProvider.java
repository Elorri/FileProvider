package com.example.android.fileprovider;


import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;


/**
 * This a copyPlainText of FileProvider {@link android.support.v4.content.FileProvider} with some custom modifications.
 */
//We chose to make a copyPlainText to get easy access to private field mStrategy
public class CopyPasteFileProvider extends ContentProvider implements ContentProvider.PipeDataWriter<Cursor> {

    private static final String META_DATA_FILE_PROVIDER_PATHS = "android.support.FILE_PROVIDER_PATHS";
    private static final String TAG_ROOT_PATH = "root-path";
    private static final String TAG_FILES_PATH = "files-path";
    private static final String TAG_CACHE_PATH = "cache-path";
    private static final String TAG_EXTERNAL = "external-path";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PATH = "path";
    private static final File DEVICE_ROOT = new File("/");
    private static HashMap<String, PathStrategy> sCache = new HashMap<String, PathStrategy>();
    private PathStrategy mStrategy;


    private static final int CUSTOM_FILE_URI = 400; //Will match content://com.example.android.fileprovider/appDir/.tmp/hello(1).txt
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final String TAG = CopyPasteFileProvider.class.getName();
    private Context mContext;

    private static final String[] FILE_COLUMNS = {
            CustomContract.FileEntry.DISPLAY_NAME,
            CustomContract.FileEntry.SIZE,
            CustomContract.FileEntry.TEXT,
    };
    // These indices are tied to MOVIE_COLUMNS.  If FILE_COLUMNS changes, these must change.
    static final int COL_DISPLAY_NAME = 0;
    static final int COL_SIZE = 1;
    static final int COL_TEXT = 2;

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(CustomContract.CONTENT_AUTHORITY, "appDir/" + CustomContract.PATH_TMP_USER + "/*", CUSTOM_FILE_URI);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "");
        mContext = getContext();
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

        // Sanity check our security
        if (info.exported) {
            throw new SecurityException("Provider must not be exported");
        }
        if (!info.grantUriPermissions) {
            throw new SecurityException("Provider must grant uri permissions");
        }

        mStrategy = getPathStrategy(context, info.authority);
    }

    public static Uri getUriForFile(Context context, String authority, File file) {
        final PathStrategy strategy = getPathStrategy(context, authority);
        return strategy.getUriForFile(file);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "");
        switch (sUriMatcher.match(uri)) {
            case CUSTOM_FILE_URI:
                Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "CUSTOM_FILE_URI");
                // ContentProvider has already checked granted permissions
                final File file = mStrategy.getFileForUri(uri);

                if (projection == null) {
                    projection = FILE_COLUMNS;
                }

                String[] cols = new String[projection.length];
                Object[] values = new Object[projection.length];
                int i = 0;
                for (String col : projection) {
                    if (CustomContract.FileEntry.DISPLAY_NAME.equals(col)) {
                        cols[i] = CustomContract.FileEntry.DISPLAY_NAME;
                        values[i++] = file.getName();
                    } else if (CustomContract.FileEntry.SIZE.equals(col)) {
                        cols[i] = CustomContract.FileEntry.SIZE;
                        values[i++] = file.length();
                    } else if (CustomContract.FileEntry.TEXT.equals(col)) {
                        cols[i] = CustomContract.FileEntry.TEXT;
                        values[i++] = FileUtils.readFile(file);
                    }
                }

                cols = copyOf(cols, i);
                values = copyOf(values, i);

                final MatrixCursor cursor = new MatrixCursor(cols, 1);
                cursor.addRow(values);
                return cursor;
            default:
                return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        Log.e("TAG", Thread.currentThread().getStackTrace()[2] + "uri " + uri);
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);

        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = file.getName().substring(lastDot+1);

            if (extension.equals(mContext.getResources().getString(R.string.custom_extension))) {
                Log.e("FP", Thread.currentThread().getStackTrace()[2] + "mimetype registered " + CustomContract.FileEntry.CONTENT_ITEM_TYPE);
                return CustomContract.FileEntry.CONTENT_ITEM_TYPE;
            }
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                Log.e("FP", Thread.currentThread().getStackTrace()[2] + "mimetype registered " + mime);
                return mime;
            }
        }
        Log.e("FP", Thread.currentThread().getStackTrace()[2] + "mimetype registered application/octet-stream");
        return "application/octet-stream";

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // ContentProvider has already checked granted permissions
        final File file = mStrategy.getFileForUri(uri);
        return file.delete() ? 1 : 0;
    }

    @Override //Copied from {@link android.support.v4.content.FileProvider}
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        Log.e("FP", Thread.currentThread().getStackTrace()[2] + "");
        // ContentProvider has already checked granted permissions
        return getParcelFileDescriptor(uri, mode);

        //Should I use instead ?
        //return openPipeHelper(uri, mimeTypes[0], opts, cursor, this);
    }

    private ParcelFileDescriptor getParcelFileDescriptor(Uri uri, String mode) {
        final File file = mStrategy.getFileForUri(uri);
        try {
            return ParcelFileDescriptor.open(file, modeToMode(mode));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File " + file.getName() + " not found" + e);
            return null;
        }
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        Log.e("FP", Thread.currentThread().getStackTrace()[2] + "");
        String mimetype = "text/plain"; //Default mimetype we decide to use in case no mimetype are specified by the calling app.
        return openTypedAssetFile(uri, mimetype, null);
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        Log.e("FP", Thread.currentThread().getStackTrace()[2] + "");
        return openAssetFile(uri, mode);
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
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        Log.e("FP", Thread.currentThread().getStackTrace()[2] + "");

        // Checks to see if the MIME type filter matches a supported MIME type.
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);
        // If the MIME type is supported
        if (mimeTypes != null) {
            Log.e("FP", Thread.currentThread().getStackTrace()[2] + "Renvoyer un AssetFileDescriptor different pour chaque mimetype here");
            //Query if file is existing
            // Retrieves the note for this URI. Uses the query method defined for this provider,
            // rather than using the database query method.
            Cursor cursor = query(
                    uri,                    // The URI of a note
                    FILE_COLUMNS,   // Gets a projection containing the note's file name, size and text
                    null,                   // No WHERE clause, get all matching records
                    null,                   // Since there is no WHERE clause, no selection criteria
                    null                    // Use the default sort order (modification date,
                    // descending
            );
            // If the query fails or the cursor is empty, stop
            if (cursor == null || !cursor.moveToFirst()) {
                // If the cursor is empty, simply close the cursor and return
                if (cursor != null) {
                    cursor.close();
                }
                // If the cursor is null, throw an exception
                throw new FileNotFoundException("Unable to query " + uri);
            }
            // Start a new thread that pipes the stream data back to the caller.
            ParcelFileDescriptor parcelFileDescritor = openPipeHelper(uri, mimeTypes[0], opts, cursor, this);
            //ParcelFileDescriptor parcelFileDescritor = getParcelFileDescriptor(uri, mode);             //Should I use this instead ?
            return new AssetFileDescriptor(parcelFileDescritor, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        }
        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    /**
     * Returns the types of available data streams.  URIs to specific notes are supported.
     * The application can convert such a note to a plain text stream.
     *
     * @param uri            the URI to analyze
     * @param mimeTypeFilter The MIME type to check for. This method only returns a data stream
     *                       type for MIME types that match the filter. Currently, only text/plain MIME types match.
     * @return a data stream MIME type. Currently, only text/plan is returned.
     * @throws IllegalArgumentException if the URI pattern doesn't match any supported patterns.
     */
    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        Log.e("FP", Thread.currentThread().getStackTrace()[2] + "getStreamTypes " + getType(uri));
        ClipDescription clipItemMimeType = new ClipDescription(null, new String[]{getType(uri)});
        return clipItemMimeType.filterMimeTypes(mimeTypeFilter);

//        switch (sUriMatcher.match(uri)) {
//            case CUSTOM_FILE_URI:
//                return CustomContract.CLIP_DESC_MIMETYPES_HTML.filterMimeTypes(mimeTypeFilter);
//            default:
//                throw new IllegalArgumentException("Unknown URI " + uri);
//        }
    }


    /**
     * Implementation of {@link android.content.ContentProvider.PipeDataWriter}
     * to perform the actual work of converting the data in one of cursors to a
     * stream of data for the client to read.
     */
    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, Cursor cursor) {
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "See if we do different stream import depending on the data type given");

        Log.e("FP", "output " + output);

        FileOutputStream out = new FileOutputStream(output.getFileDescriptor());
        PrintWriter printWriter = null;
        String charset = "UTF-8";
        try {
            printWriter = new PrintWriter(new OutputStreamWriter(out, charset));
            String text = cursor.getString(COL_TEXT);
            Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "text" + text);
            printWriter.println(text);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "There was a problem encoding text using " + charset, e);
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

    //Copied from {@link android.support.v4.content.FileProvider}
    private static PathStrategy getPathStrategy(Context context, String authority) {
        PathStrategy strat;
        synchronized (sCache) {
            strat = sCache.get(authority);
            if (strat == null) {
                try {
                    strat = parsePathStrategy(context, authority);
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Failed to parse " + META_DATA_FILE_PROVIDER_PATHS + " meta-data", e);
                } catch (XmlPullParserException e) {
                    throw new IllegalArgumentException(
                            "Failed to parse " + META_DATA_FILE_PROVIDER_PATHS + " meta-data", e);
                }
                sCache.put(authority, strat);
            }
        }
        return strat;
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    private static PathStrategy parsePathStrategy(Context context, String authority)
            throws IOException, XmlPullParserException {
        final SimplePathStrategy strat = new SimplePathStrategy(authority);

        final ProviderInfo info = context.getPackageManager()
                .resolveContentProvider(authority, PackageManager.GET_META_DATA);
        final XmlResourceParser in = info.loadXmlMetaData(
                context.getPackageManager(), META_DATA_FILE_PROVIDER_PATHS);
        if (in == null) {
            throw new IllegalArgumentException(
                    "Missing " + META_DATA_FILE_PROVIDER_PATHS + " meta-data");
        }

        int type;
        while ((type = in.next()) != END_DOCUMENT) {
            if (type == START_TAG) {
                final String tag = in.getName();

                final String name = in.getAttributeValue(null, ATTR_NAME);
                String path = in.getAttributeValue(null, ATTR_PATH);

                File target = null;
                if (TAG_ROOT_PATH.equals(tag)) {
                    target = buildPath(DEVICE_ROOT, path);
                } else if (TAG_FILES_PATH.equals(tag)) {
                    target = buildPath(context.getFilesDir(), path);
                } else if (TAG_CACHE_PATH.equals(tag)) {
                    target = buildPath(context.getCacheDir(), path);
                } else if (TAG_EXTERNAL.equals(tag)) {
                    target = buildPath(Environment.getExternalStorageDirectory(), path);
                }

                if (target != null) {
                    strat.addRoot(name, target);
                }
            }
        }

        return strat;
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    interface PathStrategy {
        /**
         * Return a {@link Uri} that represents the given {@link File}.
         */
        public Uri getUriForFile(File file);

        /**
         * Return a {@link File} that represents the given {@link Uri}.
         */
        public File getFileForUri(Uri uri);
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    static class SimplePathStrategy implements PathStrategy {
        private final String mAuthority;
        private final HashMap<String, File> mRoots = new HashMap<String, File>();

        public SimplePathStrategy(String authority) {
            mAuthority = authority;
        }

        /**
         * Add a mapping from a name to a filesystem root. The provider only offers
         * access to files that live under configured roots.
         */
        public void addRoot(String name, File root) {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name must not be empty");
            }

            try {
                // Resolve to canonical path to keep path checking fast
                root = root.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Failed to resolve canonical path for " + root, e);
            }

            mRoots.put(name, root);
        }

        @Override
        public Uri getUriForFile(File file) {
            String path;
            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }

            // Find the most-specific root path
            Map.Entry<String, File> mostSpecific = null;
            for (Map.Entry<String, File> root : mRoots.entrySet()) {
                final String rootPath = root.getValue().getPath();
                if (path.startsWith(rootPath) && (mostSpecific == null
                        || rootPath.length() > mostSpecific.getValue().getPath().length())) {
                    mostSpecific = root;
                }
            }

            if (mostSpecific == null) {
                throw new IllegalArgumentException(
                        "Failed to find configured root that contains " + path);
            }

            // Start at first char of path under root
            final String rootPath = mostSpecific.getValue().getPath();
            if (rootPath.endsWith("/")) {
                path = path.substring(rootPath.length());
            } else {
                path = path.substring(rootPath.length() + 1);
            }

            // Encode the tag and path separately
            path = Uri.encode(mostSpecific.getKey()) + '/' + Uri.encode(path, "/");
            return new Uri.Builder().scheme("content")
                    .authority(mAuthority).encodedPath(path).build();
        }

        @Override
        public File getFileForUri(Uri uri) {
            String path = uri.getEncodedPath();

            final int splitIndex = path.indexOf('/', 1);
            final String tag = Uri.decode(path.substring(1, splitIndex));
            path = Uri.decode(path.substring(splitIndex + 1));

            final File root = mRoots.get(tag);
            if (root == null) {
                throw new IllegalArgumentException("Unable to find configured root for " + uri);
            }

            File file = new File(root, path);
            try {
                file = file.getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }

            if (!file.getPath().startsWith(root.getPath())) {
                throw new SecurityException("Resolved path jumped beyond configured root");
            }

            return file;
        }
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    private static int modeToMode(String mode) {
        int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        return modeBits;
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    private static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (segment != null) {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    private static String[] copyOf(String[] original, int newLength) {
        final String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    //Copied from {@link android.support.v4.content.FileProvider}
    private static Object[] copyOf(Object[] original, int newLength) {
        final Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }
}
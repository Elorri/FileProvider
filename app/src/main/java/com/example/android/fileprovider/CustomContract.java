package com.example.android.fileprovider;


import android.content.ClipDescription;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.OpenableColumns;

public class CustomContract {
    public static final String CONTENT_AUTHORITY = "com.example.android.fileprovider";
    public static String CUSTOM_MIMETYPE = "custom_nebo";
    public static String PATH_TMP_USER = ".tmp";

    public static String PATH_COLLECTION = "collection";
    public static String PATH_NOTEBOOK = "notebook";


    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);


    /**
     * This describes the MIME types supported for the data copied in the clipboard. Means mimetype pasting app will be able to use.
     */
    static ClipDescription CLIP_DESC_MIMETYPES = new ClipDescription("text", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
    static ClipDescription CLIP_DESC_MIMETYPES_HTML = new ClipDescription("html", new String[]{ClipDescription.MIMETYPE_TEXT_HTML});
    static ClipDescription CLIP_DESC_MIMETYPES_IMAGES = new ClipDescription("image", new String[]{"image/png"});
    static ClipDescription CLIP_DESC_MIMETYPES_ALL = new ClipDescription("all", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML, "image/png"});


    public static final class FileEntry implements OpenableColumns {

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + CUSTOM_MIMETYPE;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + CUSTOM_MIMETYPE;

        public static final String TEXT = "text";
    }
}

package com.example.android.fileprovider;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {

    private static String TAG = FileUtils.class.getName();

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final String CAMERA_DIR = "/dcim/";


    public static File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir(context);
        File file = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        //Need java 7
//        UserDefinedFileAttributeView view =
//                MediaStore.Files.getFileAttributeView(file.getAbsolutePath(), UserDefinedFileAttributeView.class);
//        view.write("user.mimetype", Charset.defaultCharset().encode("text/html"));
        return file;
    }

    private static File getAlbumDir(Context context) {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + context.getResources().getString(R.string.app_name));

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(context.getResources().getString(R.string.app_name),
                    "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }


    private static File createExternalDir(File dir, String folder) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //External storage is not mounted. It's not always available, because the user can mount the external storage as USB storage and in some cases remove it from the device.
            Log.e(TAG, "Unable to create directory. External storage is not mounted.");
            return null;
        }
        return createDir(dir, folder);
    }

    private static File createDir(File dir, String folder) {
        File storageDir = new File(dir, folder);

        if (storageDir != null) {
            if (!storageDir.isDirectory() && !storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory.");
                return null;
            }
        }
        return storageDir;
    }

    //Will replace current DMSUtils.getRootDir()
    //Used when we need to store a file for intermediate result. Once our calculs are done it's best to remove the files,
    //but if we don't the system will remove them when running low.
    //see: https://developer.android.com/training/basics/data-storage/files.html
    //Exple to /data/data/com.myscript.nebo.debug/cache/_nebo/
    public static File getAppTempDir(Context context, String folder) {
        return createDir(context.getCacheDir(), folder);
    }

    //Used for data we want to keep forever but want to hide from the user.
    //Exple to /data/data/com.myscript.nebo.debug/file/My folder/_nebo/
    //see: https://developer.android.com/training/basics/data-storage/files.html
    public static File getAppDir(Context context, String folder) {
        return createDir(context.getFilesDir(), folder);
    }

    //Used for data we want visible to the user and other apps. And data we want in a general directory.
    //Exple /storage/emulated/0/Documents/_nebo/
    //Exple /storage/emulated/0/Images/_nebo/
    //see: https://developer.android.com/training/basics/data-storage/files.html
    public static File getPublicDir(Activity activity, String publicDir, String folder) {
        PlatformUtils.checkAndAskForPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return createExternalDir(Environment.getExternalStoragePublicDirectory(publicDir), folder);
    }

    //Used for data we want visible to the user and other apps. And data we want located in the app directory.
    //Exple to /storage/emulated/0/Android/data/com.myscript.nebo.debug/files/_nebo/
    //see: https://developer.android.com/training/basics/data-storage/files.html
    public static File getPublicAppDir(Activity activity, String folder) {
        PlatformUtils.checkAndAskForPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return createExternalDir(activity.getApplicationContext().getExternalFilesDir(null), folder);
    }

    //Used for data we want visible to the user and other apps.
    //And data we want located in the app directory
    //And data we want to allow system to delete in case of low memory
    //We use this method for example to temporary store the .nebo that will be send as a mail attachment.
    //We also choose to remove directory files every time we use it, to avoid a file renaming (when we try to add a file that already exist) layer.
    //Exple /storage/emulated/0/Android/data/com.myscript.nebo.debug/cache/_nebo/
    //see: https://developer.android.com/training/basics/data-storage/files.html
    public static File getPublicAppTempDir(Activity activity, String folder) {
        PlatformUtils.checkAndAskForPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        File directory = activity.getApplicationContext().getExternalCacheDir();
        deleteFiles(directory);
        return createExternalDir(directory, folder);
    }

    private static void deleteFiles(File directory) {
        for (File file : directory.listFiles()) {
            file.delete();
        }
    }

    public static File createFile(File directory, File file) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            Log.e(TAG, "Unable to create directory for " + file.getName() + " export " + directory.getAbsolutePath());
            return null;
        }
        if (!file.isFile()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                Log.e(TAG, "Something went wrong while creating " + file.getName() + " file", e);
                return null;
            }
        }
        return file;
    }

    /**
     * Check if the file name we want to give exist and suggest another one if so.
     *
     * @param directory where we want our file
     * @param fullname  with extension
     * @return
     */
    public static File getUniqueFile(File directory, String fullname) {
        File file = new File(directory, fullname);
        return getUniqueFile(file, 0);
    }

    private static File getUniqueFile(File file, int number) {
        if (file == null) {
            Log.e(TAG, "Method shouldn't be invoked with a null file.");
            return null;
        }
        if (!file.exists()) {
            return file;
        }
        number++;

        String fileName = file.getName();
        int patternStart = fileName.lastIndexOf("(");
        int extensionStart = fileName.lastIndexOf(".");

        String extension = "";

        //Remove pattern and extension from file name
        if (extensionStart != -1) {
            extension = fileName.substring(extensionStart, fileName.length());
            fileName = fileName.substring(0, extensionStart);
        }

        if (patternStart != -1) {
            fileName = fileName.substring(0, patternStart);
        }

        //Add pattern and extension to file name
        StringBuilder stringBuilder = new StringBuilder(fileName);
        fileName = stringBuilder.append("(").append(number).append(")").append(extension).toString();
        File uniqueFile = new File(file.getParentFile(), fileName);
        if (uniqueFile.exists()) {
            //keep going until we get something unique
            return getUniqueFile(file, number);
        }
        return uniqueFile;
    }

    public static File streamToFile(File directory, File fileName, InputStream in) {
        File file = createFile(directory, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            // IOUtils.copyPlainText(in, out);
            copyStream(in, out);
        } catch (IOException e) {
            Log.e(TAG, "An error occured while copying file stream", e);
        }
        return file;
    }

    public static FileInputStream getStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "An error occured while getting file stream", e);
            return null;
        }
    }

    public static InputStream getStream(Context context, Uri uri) {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "An error occured while getting file stream", e);
            return null;
        }
    }

    public static void copyStream(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }

        FileInputStream input = null;
        StringBuilder output = new StringBuilder();
        try {
            input = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.append(new String(buffer, 0, read));
            }
            input.close();
            return output.toString();
        } catch (IOException e) {
            // if any reading error occurs, we can't do anything here
            Log.e(TAG, "Error while reading file content " + file.getAbsolutePath(), e);
            return null;
        }
    }

    public static String readStream(InputStream input) {

        StringBuilder output = new StringBuilder();
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.append(new String(buffer, 0, read));
            }
            input.close();
            return output.toString();
        } catch (IOException e) {
            // if any reading error occurs, we can't do anything here
            Log.e(TAG, "Error while reading stream content ", e);
            return null;
        }
    }

    public static Object coerceToMimetypeUsingOpenTypeAssetFile(Context context, Uri uri, String mimetype, String charset) {
        if (uri == null) {
            return "";
        }
        try {
            AssetFileDescriptor assetFileDescriptor = context.getContentResolver().openTypedAssetFileDescriptor(uri, mimetype, null);

            if (mimetype.equals("text/*")) { //If app requested a text. May crash because there is no actual test on mimetype
                return getAssetFileDescriptorText(assetFileDescriptor, charset);
            }

            if (mimetype.equals("image/*")) {//If app requested an Image.  May crash because there is no actual test on mimetype
                return BitmapFactory.decodeFileDescriptor(assetFileDescriptor.getFileDescriptor());
            }
            assetFileDescriptor.close();
        } catch (IOException e) {
            Log.e("FP", Thread.currentThread().getStackTrace()[2] + "");
            //Log.e(TAG, "There was a problem reading file descriptor " + e);
            e.printStackTrace();
        }
        return uri.toString();
    }

    public static Object coerceToMimetypeUsingOpenAssetFile(Context context, Uri uri, String mode, String mimetype, String charset) {
        if (uri == null) {
            return "";
        }

        try {
            AssetFileDescriptor assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, mode);

            if (mimetype.equals("text/*")) { //If app requested a text. May crash because there is no actual test on mimetype
                return getAssetFileDescriptorText(assetFileDescriptor, charset);
            }

            if (mimetype.equals("image/*")) {//If app requested an Image.  May crash because there is no actual test on mimetype
                return BitmapFactory.decodeFileDescriptor(assetFileDescriptor.getFileDescriptor());
            }
            assetFileDescriptor.close();
        } catch (IOException e) {
            Log.e(TAG, "There was a problem reading file descriptor " + e);
            e.printStackTrace();
        }

        return uri.toString();
    }

    public static Object coerceToMimetypeUsingOpenFile(Context context, Uri uri, String mode, String mimetype, String charset) {
        if (uri == null) {
            return "";
        }
        try {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, mode);

            if (mimetype.equals("text/*")) { //If app requested a text. May crash because there is no actual test on mimetype
                return getParcelFileDescriptorText(parcelFileDescriptor, charset);
            }

            if (mimetype.equals("image/*")) {//If app requested an Image.  May crash because there is no actual test on mimetype
                return getParcelFileDescriptorImage(parcelFileDescriptor);
            }
            parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.e(TAG, "There was a problem reading file descriptor " + e);
            e.printStackTrace();
        }

        return uri.toString();// If we couldn't open the URI as a stream, then the URI will be used as a textual representation.
    }


    private static String getParcelFileDescriptorText(ParcelFileDescriptor parcelFileDescriptor, String charset) {
        AssetFileDescriptor assetFileDescriptor = new AssetFileDescriptor(parcelFileDescriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        return getAssetFileDescriptorText(assetFileDescriptor, charset);
    }

    private static String getAssetFileDescriptorText(AssetFileDescriptor assetFileDescriptor, String charset) {
        FileInputStream stream = null;
        try {
            stream = assetFileDescriptor.createInputStream();
            return FileUtils.readStream(stream, charset);
        } catch (IOException e) {
            Log.e(TAG, "There was a problem reading file descriptor " + e);
            e.printStackTrace();
            return null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static String readStream(FileInputStream stream, String charset) {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(stream, charset);
            StringBuilder builder = new StringBuilder(128);
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, len);
            }
            return builder.toString();
        } catch (IOException e) {
            Log.e(TAG, "There was a problem reading file descriptor " + e);
            return null;
        }

    }

    private static Object getParcelFileDescriptorImage(ParcelFileDescriptor parcelFileDescriptor) {
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        return image;
    }


    public static Object coerceToMimetypeUsingOpenInputStream(Context context, Uri uri, String mimetype) {
        if (uri == null) {
            return "";
        }
        FileInputStream stream = null;
        try {
            stream = (FileInputStream) context.getContentResolver().openInputStream(uri);

            if (mimetype.equals("text/*")) { //If app requested a text. May crash because there is no actual test on mimetype
                readStream(stream);
            }

            if (mimetype.equals("image/*")) {//If app requested an Image.  May crash because there is no actual test on mimetype
                //streamToBitmap ? or we decide not to manage it
            }

        } catch (IOException e) {
            // Something bad has happened.
            Log.w("ClippedData", "Failure loading text", e);
            return e.toString();

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
        return uri.toString();
    }
}

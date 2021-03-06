package com.example.android.fileprovider;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static File getUniqueFile(File directory, String fileName, String extension) {
        File file = new File(directory, fileName + "" + extension);
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
            IOUtils.copy(in, out);
        } catch (IOException e) {
            Log.e(TAG, "An error occured while copying file stream", e);
        }
        return file;
    }

    public static FileInputStream getStream(File file)
    {
        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "An error occured while getting file stream", e);
            return null;
        }
    }

    public static InputStream getStream(Context context, Uri uri)
    {
        try
        {
            return context.getContentResolver().openInputStream(uri);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "An error occured while getting file stream", e);
            return null;
        }
    }




}

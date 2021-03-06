package com.example.android.fileprovider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    private ImageView mImageView;
    private TextView mTextView;

    private Uri mUri;
    private Bitmap mBitmap;

    private boolean isGalleryPicture = false;
    private int REQUEST_CODE = 2;
    private File mLocal;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = (TextView) findViewById(R.id.image_uri);
        mImageView = (ImageView) findViewById(R.id.image);

        String path = importUri(getIntent().getData());
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "local path " + path);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_email_opt1) {
            sedEmailOpt1();
            return true;
        }
        if (id == R.id.action_send_email_opt2) {
            sedEmailOpt2();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void pickImage(View view) {
        Intent intent;
        Log.e(LOG_TAG, "While is set and the ifs are worked through.");

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // Show only images, no videos or anything else
        Log.e(LOG_TAG, "Check write to external permissions");

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void takePicture(View view) {

        try {
            File file = FileUtils.createImageFile(this);

            Log.d(LOG_TAG, "File: " + file.getAbsolutePath());

            //Will look for a provider in the manifest with authority equals to
            // FILE_PROVIDER_AUTHORITY and then will look for the file given in parameter in all
            // its resource paths. When the file is found it create a corresponding uri.
            //Same process is used by FileManager when browsing file, it looks if an app has
            // registered the path where the file is. Then it query the content resolver getType
            // method to get the Mimetype.
            mUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(LOG_TAG, "Received an \"Activity Result\"");
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mUri.toString());

                mTextView.setText(mUri.toString());
                mBitmap = getBitmapFromUri(mUri);
                mImageView.setImageBitmap(mBitmap);

                isGalleryPicture = true;
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Log.i(LOG_TAG, "Uri: " + mUri.toString());

            mTextView.setText(mUri.toString());
            mBitmap = getBitmapFromUri(mUri);
            mImageView.setImageBitmap(mBitmap);

            isGalleryPicture = false;
        } else if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "");
            Uri uri = resultData.getData();
            importUri(uri);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    // Recommended option found here: https://github.com/crlsndrsjmnz/MyShareImageExample
    private void sedEmailOpt1() {
        if (mUri != null) {
            Uri imageUri = getShareableImageUri();

            String subject = "URI Example";
            String stream = "Hello! \n"
                    + "Uri example" + ".\n"
                    + "Uri: " + imageUri.toString() + "\n";

            Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                    .setStream(imageUri)
                    .setSubject(subject)
                    .setText(stream)
                    .getIntent();

            // Provide read access
            shareIntent.setData(imageUri);
            shareIntent.setType("message/rfc822");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share with"));

        } else {
            Snackbar.make(mTextView, "Image not selected", Snackbar.LENGTH_LONG)
                    .setAction("Select", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            pickImage(view);
                        }
                    }).show();
        }
    }

    // Recommended option found here: https://github.com/crlsndrsjmnz/MyShareImageExample
    private void sedEmailOpt2() {
        if (mUri != null) {
            Uri imageUri = getShareableImageUri();

            String subject = "URI Example";
            String stream = "Hello! \n"
                    + "Uri example" + ".\n"
                    + "Uri: " + imageUri.toString() + "\n";

            Intent orderIntent = new Intent(Intent.ACTION_SENDTO);
            orderIntent.setData(Uri.parse("mailto:"));
            orderIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            orderIntent.putExtra(Intent.EXTRA_TEXT, stream);
            orderIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            orderIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Solution taken from http://stackoverflow.com/a/18332000/3346625
            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(orderIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivity(Intent.createChooser(orderIntent, "Share with"));

        } else {
            Snackbar.make(mTextView, "Image not selected", Snackbar.LENGTH_LONG)
                    .setAction("Select", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            pickImage(view);
                        }
                    }).show();
        }
    }

    public Uri getShareableImageUri() {
        Uri imageUri;

        if (isGalleryPicture) {
            String filename = getFilePath();
            saveBitmapToFile(getCacheDir(), filename, mBitmap, Bitmap.CompressFormat.JPEG, 100);
            File imageFile = new File(getCacheDir(), filename);

            imageUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, imageFile);

        } else {
            imageUri = mUri;
        }

        return imageUri;
    }

    public String getFilePath() {
        /*
         * Get the file's content URI from the incoming Intent,
         * then query the server app to get the file's display name
         * and size.
         */
        Cursor returnCursor =
                getContentResolver().query(mUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
        returnCursor.moveToFirst();
        String fileName = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

        return fileName;
    }

    /*
    * Bitmap.CompressFormat can be PNG,JPEG or WEBP.
    *
    * quality goes from 1 to 100. (Percentage).
    *
    * dir you can get from many places like Environment.getExternalStorageDirectory() or mContext.getFilesDir()
    * depending on where you want to save the image.
    */
    public boolean saveBitmapToFile(File dir, String fileName, Bitmap bm,
                                    Bitmap.CompressFormat format, int quality) {
        File imageFile = new File(dir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            bm.compress(format, quality, fos);
            fos.close();

            return true;
        } catch (IOException e) {
            Log.e("app", e.getMessage());
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return false;
    }

    public void createFile(View view) {
        File directory = FileUtils.getAppDir(this, "_nebo");
        File file = FileUtils.getUniqueFile(directory, "Welcome", ".nebo");
        mLocal = FileUtils.createFile(directory, file);
    }


    public void exportFile(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        File directory = FileUtils.getPublicAppTempDir(this, "_nebo");
        File file = FileUtils.getUniqueFile(directory, "Welcome", ".nebo");
        File external = FileUtils.streamToFile(directory, file, FileUtils.getStream(mLocal));
        Uri uri = Uri.fromFile(external);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("*/*");
        startActivity(Intent.createChooser(intent, "Export"));
    }

    public void importFile(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE);
    }

    private String importUri(Uri uri) {
        if (uri != null) //We are opening a "new" .nebo
        {
            Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "uri " + uri);
            String name = null;
            InputStream in = null;
            if (uri.getScheme().equals("file")) //We are opening a file
            {
                Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "We are opening a File");
                String path = uri.getPath(); //public directory path
                File external = new File(path);
                name = external.getName();
                in = FileUtils.getStream(external);
            } else //We are opening a mail attachment
            {
                Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "We are opening an attachement");
                name = getNotebookName(mContext, uri);
                in = FileUtils.getStream(mContext, uri);
            }
            Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "notebookFullName " + name);
            String fullName[] = splitNameFromExtension(name);
            name = fullName[0];
            String extension = fullName[1];
            if (isExtensionValid(extension, ".nebo")) {
                File directory = FileUtils.getAppDir(this, "_nebo");
                File file = FileUtils.getUniqueFile(directory, name, ".nebo");
                File local = FileUtils.streamToFile(directory, file, in);
                Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "File imported " + local);
                if (isFileSizeValid(local)) {
                    return local.getAbsolutePath(); //local directory path
                } else {
                    return getLastNotebookOpenPath();
                }
            } else {
                Toast.makeText(mContext, "File can't be imported", Toast.LENGTH_LONG).show();
                return getLastNotebookOpenPath();
            }
        } else {
            return getLastNotebookOpenPath();
        }
    }

    private boolean isFileSizeValid(File file) {
        return file.length() > 0;
    }

    private String getLastNotebookOpenPath() {
        //We are reopening the last notebook opened
        // SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        // return sp.getString(context.getResources().getString(R.string.last_opened_notebook_tag), null);
        Log.e("Nebo", Thread.currentThread().getStackTrace()[2] + "no file imported");
        return null;
    }

    private boolean isExtensionValid(String actual, String expected) {
        return actual.equals(expected);
    }

    private String[] splitNameFromExtension(String name) {
        int insertion = name.lastIndexOf(".");
        String[] fullName = new String[2];
        if (insertion == -1) {
            insertion = name.length();
        }
        fullName[0] = name.substring(0, insertion);
        fullName[1] = name.substring(insertion, name.length());
        return fullName;
    }


    private static String getNotebookName(Context context, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(projection[0]));
            }
        } finally {
            cursor.close();
        }
        return null;
    }

}

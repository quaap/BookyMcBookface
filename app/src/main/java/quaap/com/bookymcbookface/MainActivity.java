package quaap.com.bookymcbookface;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    EpubBook book;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView)findViewById(R.id.page_view);

        webView.setNetworkAvailable(false);


        findFile();

    }

    private void loadFile(File file) {
        book = new EpubBook(MainActivity.this, getFilesDir());
        Log.d("Main", "File " + file);
        book.load(file);

        Log.d("Main", "trying to load " + book.getPage(0).file.toURI().toString());

        webView.loadUrl(book.getPage(0).file.toURI().toString());
    }


    private void findFile() {

        FsTools fsTools = new FsTools(this);

        fsTools.selectExternalLocation(new FsTools.SelectionMadeListener() {
            @Override
            public void selected(File selection) {
                loadFile(selection);

            }
        }, "epub", false, ".*\\.epub");
    }

//    private static final int FILE_SELECT_CODE = 0;
//
//    private void showFileChooser() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("*/*");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//
//        try {
//            startActivityForResult(
//                    Intent.createChooser(intent, "Select a File to Upload"),
//                    FILE_SELECT_CODE);
//        } catch (android.content.ActivityNotFoundException ex) {
//            // Potentially direct the user to the Market with a Dialog
//            Toast.makeText(this, "Please install a File Manager.",
//                    Toast.LENGTH_SHORT).show();
//        }
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case FILE_SELECT_CODE:
//                if (resultCode == RESULT_OK) {
//                    // Get the Uri of the selected file
//                    Uri uri = data.getData();
//                    Log.d(TAG, "File Uri: " + uri.toString());
//                    // Get the path
//                    String path = null;
//                    try {
//                        path = getPath(this, uri);
//                    } catch (URISyntaxException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d(TAG, "File Path: " + path);
//
//                    if (path!=null) {
//                        book = new EpubBook(this, getFilesDir());
//                        book.load(new File(path));
//                        webView.loadUrl(book.getPage(0).file.toURI().toString());
//                    }
//
//
//                    // Get the file instance
//                    // File file = new File(path);
//                    // Initiate the upload
//                }
//                break;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    public static String getPath(Context context, Uri uri) throws URISyntaxException {
//        if ("content".equalsIgnoreCase(uri.getScheme())) {
//            String[] projection = { "_data" };
//            Cursor cursor = null;
//
//            try {
//                cursor = context.getContentResolver().query(uri, projection, null, null, null);
//                int column_index = cursor.getColumnIndexOrThrow("_data");
//                if (cursor.moveToFirst()) {
//                    return cursor.getString(column_index);
//                }
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage(), e);
//            }
//        }
//        else if ("file".equalsIgnoreCase(uri.getScheme())) {
//            return uri.getPath();
//        } else {
//            Log.e(TAG, "Don't know what to do with " + uri);
//        }
//
//        return null;
//    }
}

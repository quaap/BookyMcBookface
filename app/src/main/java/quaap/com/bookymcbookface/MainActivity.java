package quaap.com.bookymcbookface;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.util.List;

import quaap.com.bookymcbookface.book.Book;
import quaap.com.bookymcbookface.book.EpubBook;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    Book book;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView)findViewById(R.id.page_view);

        webView.setNetworkAvailable(false);
        //webView.setScrollContainer(false);
        webView.setOnTouchListener(new View.OnTouchListener() {
            float x,y;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_UP:
                        float diffx = motionEvent.getX() - x;
                        float diffy = motionEvent.getY() - y;

                        if (diffx>100 || diffy>100) {
                            prevPage();
                        } else if (diffx<-100 || diffy<-100) {
                            nextPage();
                        } else {
                            return false;
                        }


                    case MotionEvent.ACTION_DOWN:
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        return false;
                }


                return true;
            }
        });

        if (Build.VERSION.SDK_INT>=24) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    if (uri.getScheme().equals("file")) {
                        gotoSection(uri.toString());
                        return true;
                    }
                    return false;
                }
            });
        } else {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.i("WebView", "Attempting to load URL: " + url);

                    gotoSection(url);
                    //view.loadUrl(url);
                    return true;
                }
            });

        }

        findViewById(R.id.prev_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prevPage();
            }
        });

        findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPage();
            }
        });

        //findFile();
        Intent intent = getIntent();
        String filename = intent.getStringExtra("filename");
        if (filename!=null) {
            loadFile(new File(filename));
            //loadFile(new File("/storage/emulated/0/Download/pg345-images.epub"));
        }

    }

    private void prevPage() {
        if(webView.canScrollVertically(-1)) {
            webView.pageUp(false);
        } else {
            showFile(book.getPreviousSection());
        }
        book.setSectionOffset(webView.getScrollY());
    }

    private void nextPage() {

        if(webView.canScrollVertically(1)) {
            webView.pageDown(false);
        } else {
            showFile(book.getNextSection());
        }
        book.setSectionOffset(webView.getScrollY());

    }


    private void loadFile(File file) {
        book = new EpubBook(MainActivity.this, getFilesDir());
        Log.d("Main", "File " + file);
        book.load(file);
        showFile(book.getCurrentSection());
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                webView.computeScroll();
                webView.scrollTo(0, book.getSectionOffset());

            }
        }, 1000);


    }

    private void showFile(File file) {
        if (file !=null) {
            Log.d("Main", "trying to load " + file);
            webView.loadUrl(file.toURI().toString());
        }
    }

    private void showUri(String uri) {
        if (uri !=null) {
            Log.d("Main", "trying to load " + uri);
            webView.loadUrl(uri);
        }
    }

    private void gotoSection(String sectionURI) {
        Log.d("Main", "clicked on " + sectionURI);

        book.gotoSectionFile(sectionURI);
        showUri(sectionURI);

        book.setSectionOffset(webView.getScrollY());
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

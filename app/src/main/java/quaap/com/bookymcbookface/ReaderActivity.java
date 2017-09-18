package quaap.com.bookymcbookface;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.IOException;

import quaap.com.bookymcbookface.book.Book;
import quaap.com.bookymcbookface.book.EpubBook;

public class ReaderActivity extends Activity {

    private static final String TAG = "ReaderActivity";

    Book book;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

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

                public void onPageFinished(WebView view, String url) {
                    restoreScrollOffsetDelayed(100);
                }

            });
        } else {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.i("WebView", "Attempting to load URL: " + url);

                    gotoSection(url);
                    return true;
                }

                public void onPageFinished(WebView view, String url) {
                    restoreScrollOffsetDelayed(100);
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
        }

    }

    private void prevPage() {
        if(webView.canScrollVertically(-1)) {
            webView.pageUp(false);
            //webView.scrollBy(0,-webView.getHeight()-14);

        } else {
            showFile(book.getPreviousSection());
        }
        saveScrollOffsetDelayed(1500);

    }

    private void nextPage() {

        if(webView.canScrollVertically(1)) {
            webView.pageDown(false);
            //webView.scrollBy(0,webView.getHeight()-14);
        } else {
            showFile(book.getNextSection());
        }

        saveScrollOffsetDelayed(1500);
    }

    private void saveScrollOffsetDelayed(int delay) {
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                saveScrollOffset();
            }
        }, delay);
    }

    private void saveScrollOffset() {
        webView.computeScroll();
        book.setSectionOffset(webView.getScrollY());
    }

    private void restoreScrollOffsetDelayed(int delay) {
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                restoreScrollOffset();
            }
        }, delay);
    }

    private void restoreScrollOffset() {
        webView.computeScroll();
        webView.scrollTo(0, book.getSectionOffset());
    }

    private void loadFile(File file) {
        try {
            book = Book.getBookHandler(ReaderActivity.this, file.getPath());
            Log.d("Main", "File " + file);
            book.load(file);
            showFile(book.getCurrentSection());
            //restoreScrollOffset();
        } catch (IOException e) {
            e.printStackTrace();
        }


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
        //saveScrollOffset();

    }


    @Override
    protected void onPause() {
        saveScrollOffset();
        super.onPause();
    }
}

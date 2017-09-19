package quaap.com.bookymcbookface;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupMenu;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import quaap.com.bookymcbookface.book.Book;

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
                        handleLink(uri.toString());
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

                    handleLink(url);
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

        findViewById(R.id.contents_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToc();
            }
        });

        findViewById(R.id.zoom_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFontSize();
            }
        });

        //findFile();
        Intent intent = getIntent();
        String filename = intent.getStringExtra("filename");
        if (filename!=null) {
            loadFile(new File(filename));
        }

    }

    boolean isPagingDown;
    boolean isPagingUp;

    private void prevPage() {
        isPagingDown = false;
        if(webView.canScrollVertically(-1)) {
            webView.pageUp(false);
            //webView.scrollBy(0,-webView.getHeight()-14);

        } else {
            isPagingUp = true;
            showUri(book.getPreviousSection());

        }
        //saveScrollOffsetDelayed(1500);

    }

    private void nextPage() {
        isPagingUp = false;
        if(webView.canScrollVertically(1)) {
            webView.pageDown(false);
            //webView.scrollBy(0,webView.getHeight()-14);
        } else {
            isPagingDown = true;
            showUri(book.getNextSection());


        }

        //saveScrollOffsetDelayed(1500);
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
        saveScrollOffset(webView.getScrollY());
    }

    private void saveScrollOffset(int offset) {
        book.setSectionOffset(offset);
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
        int spos = book.getSectionOffset();
        webView.computeScroll();
        if (spos>=0) {
            webView.scrollTo(0, spos);
            Log.d("READER", "restoreScrollOffset " + spos);
        } else if (isPagingUp){
            webView.pageDown(true);
        } else if (isPagingDown){
            webView.pageUp(true);
        }
        isPagingUp = false;
        isPagingDown = false;
    }

    private void loadFile(File file) {
        try {
            book = Book.getBookHandler(ReaderActivity.this, file.getPath());
            Log.d("Main", "File " + file);
            book.load(file);
            int fontsize = book.getFontsize();
            if (fontsize!=-1) {
                setFontSize(fontsize);
            }
            showUri(book.getCurrentSection());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void showUri(Uri uri) {
        if (uri !=null) {
            Log.d("Main", "trying to load " + uri);

            //book.clearSectionOffset();
            webView.loadUrl(uri.toString());
        }
    }
//
//    private void showUri(String uri) {
//        if (uri !=null) {
//            Log.d("Main", "trying to load " + uri);
//            webView.loadUrl(uri);
//        }
//    }

    private void handleLink(String clickedLink) {
        Log.d("Main", "clicked on " + clickedLink);
        showUri(book.handleClickedLink(clickedLink));

    }


    private void fontSizeToggle() {

        int defsize = webView.getSettings().getDefaultFontSize();
        int minsize = webView.getSettings().getMinimumFontSize();

        defsize += 4;
        if (defsize>40) {
            defsize = minsize;
        }

        setFontSize(defsize);

    }

    private void setFontSize(int size) {
        book.setFontsize(size);
        webView.getSettings().setDefaultFontSize(size);
        webView.getSettings().setDefaultFixedFontSize(size);
    }

    private void selectFontSize() {
        final int defsize = webView.getSettings().getDefaultFontSize();
        int minsize = webView.getSettings().getMinimumFontSize();
        final float scale = getResources().getDisplayMetrics().density/160f;


        Log.d("READER", "def " + defsize + " " + scale);
        final PopupMenu sizemenu = new PopupMenu(this, findViewById(R.id.zoom_button));
        for (int size=minsize; size<=36; size+=2) {
            final int s = size;

            MenuItem mi = sizemenu.getMenu().add(" "+size);
            mi.setCheckable(true);
            mi.setChecked(size==defsize);

            mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    Log.d("READER", "def " + (defsize-s));
                    int scrolloffset = (int)(-webView.getScrollY()*(defsize - s)*1.3*scale*4);
                    Log.d("READER", "scrollby " + scrolloffset);

                    setFontSize(s);
                    webView.scrollBy(0, scrolloffset);
                    sizemenu.dismiss();
                    return true;
                }
            });
        }
        sizemenu.show();


    }

    @Override
    protected void onPause() {
        saveScrollOffset();
        super.onPause();
    }


    protected void showToc() {
        Map<String,String> tocmap = book.getToc();
        PopupMenu tocmenu = new PopupMenu(this, findViewById(R.id.contents_button));
        for (final String point: tocmap.keySet()) {
            String text = tocmap.get(point);
            MenuItem m = tocmenu.getMenu().add(text);
            //Log.d("EPUB", "TOC2: " + text + ". File: " + point);
            m.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    handleLink(point);
                    return true;
                }
            });
        }

        tocmenu.show();

    }

}

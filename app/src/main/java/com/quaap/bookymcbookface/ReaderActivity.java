package com.quaap.bookymcbookface;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import com.quaap.bookymcbookface.book.Book;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of BookyMcBookface and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

public class ReaderActivity extends Activity {

    private static final String TAG = "ReaderActivity";
    public static final String READEREXITEDNORMALLY = "readerexitednormally";
    private static final String FULLSCREEN = "fullscreen";

    private Book book;

    private WebView webView;

    public static final String FILENAME = "filename";
    public static final String SCREEN_PAGING = "screenpaging";
    public static final String DRAG_SCROLL= "dragscroll";



    private final Object timerSync = new Object();
    private Timer timer;

    private TimerTask nowakeTask = null;
    private TimerTask scrollTask = null;

    private volatile int scrollDir;

    private final Handler handler = new Handler();

    private CheckBox fullscreenBox;

    private ProgressBar progressBar;

    private Point mScreenDim;

    private Throwable exception;

    private int currentDimColor = Color.TRANSPARENT;

    private boolean hasLightSensor = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        final Intent intent = getIntent();

        ActionBar ab = getActionBar();
        if (ab!=null) ab.hide();
        Display display = getWindowManager().getDefaultDisplay();
        mScreenDim = new Point();
        display.getSize(mScreenDim);

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {
            hasLightSensor = true;
        }

        final ImageButton showMore = findViewById(R.id.control_view_more);

        webView = findViewById(R.id.page_view);

        webView.getSettings().setDefaultFontSize(18);
        webView.getSettings().setDefaultFixedFontSize(18);

        webView.setNetworkAvailable(false);

        final boolean drag_scroll = intent.getBooleanExtra(DRAG_SCROLL,true);

        if (intent.getBooleanExtra(SCREEN_PAGING,true)) webView.setOnTouchListener(new View.OnTouchListener() {
            float x,y;
            long time;
            final long TIMEALLOWED = 300;
            final int MINSWIPE = 150;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float diffx = 0;
                float diffy = 0;

                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_UP:

                        if (drag_scroll) cancelScrollTask();
                        //Log.d("TIME", "t " + (System.currentTimeMillis() - time));
                        if (System.currentTimeMillis() - time >TIMEALLOWED) return false;

                        diffx = motionEvent.getX() - x;
                        diffy = motionEvent.getY() - y;
                        float absdiffx = Math.abs(diffx);
                        float absdiffy = Math.abs(diffy);


                        if ((absdiffx>absdiffy && diffx>MINSWIPE) || (absdiffy>absdiffx && diffy>MINSWIPE)) {
                            prevPage();
                        } else if ((absdiffx>absdiffy && diffx<-MINSWIPE) || (absdiffy>absdiffx && diffy<-MINSWIPE)) {
                            nextPage();
                        } else {
                            return false;
                        }


                    case MotionEvent.ACTION_DOWN:
                        if (drag_scroll) cancelScrollTask();
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        time = System.currentTimeMillis();
                        setAwake();
                        if (y>mScreenDim.y/3 && x>mScreenDim.x/3 &&
                                y<mScreenDim.y*2/3 && x<mScreenDim.x*2/3) {
                            mkFull();
                            hideMenu();

                            if (currentDimColor!=Color.TRANSPARENT) {
                                setDimLevel(showMore, Color.LTGRAY);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        setDimLevel(showMore, currentDimColor);
                                    }
                                }, 2000);
                            }
                        }
                        return false;

                    case MotionEvent.ACTION_MOVE:

                        if (drag_scroll) {
                            diffy = motionEvent.getY() - y;

                            if (Math.abs(diffy) > 30) {
                                if (System.currentTimeMillis() - time > TIMEALLOWED * 1.5) {
                                    scrollDir = (int) ((-diffy / webView.getHeight()) * webView.getSettings().getDefaultFontSize() * 5);
                                    startScrollTask();
                                    webView.clearMatches();
                                }
                            } else {
                                cancelScrollTask();
                            }
                        }

                        return true;

                }


                return true;
            }



        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i(TAG, "Attempting to load URL: " + url);

                handleLink(url);
                return true;
            }


            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri.getScheme()!=null && uri.getScheme().equals("file")) {
                    handleLink(uri.toString());
                    return true;
                }
                return false;
            }


            public void onPageFinished(WebView view, String url) {
               // addEOCPadding();
                try {
                    restoreBgColor();
                    restoreScrollOffsetDelayed(100);
                } catch (Throwable t) {
                    Log.e(TAG, t.getMessage(), t);
                }
            }

        });


        progressBar = findViewById(R.id.progressBar);

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
                //hideMenu();
            }
        });

        findViewById(R.id.zoom_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFontSize();
                //hideMenu();
            }
        });
        findViewById(R.id.brightness_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBrightnessControl();
                //hideMenu();
            }
        });

        showMore.setOnClickListener(morelessControls);
        findViewById(R.id.control_view_less).setOnClickListener(morelessControls);

        fullscreenBox = findViewById(R.id.fullscreen_box);

        fullscreenBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setFullscreen(b);
                if (b) {
                    fullscreenBox.postDelayed(
                        new Runnable() {
                              @Override
                              public void run() {
                                  mkFull();
                                  hideMenu();
                              }
                        }, 500);
                } else {
                    fullscreenBox.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                mkReg();
                                hideMenu();
                            }
                        }, 500);
                }
            }
        });

        findViewById(R.id.fullscreen_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fullscreenBox.setChecked(!fullscreenBox.isChecked());
            }
        });

        //findFile();
        String filename = intent.getStringExtra(FILENAME);
        if (filename!=null) {
            //if the app crashes on this book,
            // this flag will remain to let the booklist activity know not to auto start it again.
            // it gets set to true in onPause.
            if (getSharedPreferences(BookListActivity.prefname, Context.MODE_PRIVATE).edit().putBoolean(READEREXITEDNORMALLY, false).commit()) {
                loadFile(new File(filename));
            }
        }

    }

    @Override
    public void onBackPressed() {
        finish();
        Intent main = new Intent(this, BookListActivity.class);
        main.setAction(BookListActivity.ACTION_SHOW_LAST_STATUS);
        startActivity(main);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void addEOCPadding() {
        //Add padding to end of section to reduce confusing partial page scrolls
        webView.getSettings().setJavaScriptEnabled(true);
        webView.evaluateJavascript("document.getElementsByTagName('body')[0].innerHTML += '<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>'", null);
        webView.getSettings().setJavaScriptEnabled(false);
    }

    private final View.OnClickListener morelessControls = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            View v = findViewById(R.id.slide_menu);
            if (v.getVisibility()==View.GONE) {
                showMenu();
            } else {
                hideMenu();
            }
        }
    };
    private void setFullscreenMode() {
        if (book!=null && book.hasDataDir()) {
            setFullscreen(book.getFlag(FULLSCREEN, true));
        }
    }

    private void setFullscreen(boolean full) {
        if (book!=null && book.hasDataDir()) book.setFlag(FULLSCREEN, full);

        fullscreenBox.setChecked(full);
    }

    private void showMenu() {
        View v = findViewById(R.id.slide_menu);
        v.setVisibility(View.VISIBLE);
        findViewById(R.id.control_view_more).setVisibility(View.GONE);
        findViewById(R.id.control_view_less).setVisibility(View.VISIBLE);
        mkReg();
    }

    private void hideMenu() {
        View v = findViewById(R.id.slide_menu);
        v.setVisibility(View.GONE);
        findViewById(R.id.control_view_more).setVisibility(View.VISIBLE);
        findViewById(R.id.control_view_less).setVisibility(View.GONE);
        mkFull();
    }

    int scrollTaskCounter = 0;

    private void startScrollTask() {
        synchronized (timerSync) {
            if (scrollTask == null) {
                scrollTask = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.scrollBy(0, scrollDir);
                            }
                        });
                    }
                };
                try {
                    if (timer!=null) timer.schedule(scrollTask, 0, 100);
                } catch(IllegalStateException e) {
                    Log.d(TAG, e.getMessage(), e);
                    Toast.makeText(this,"Something went wrong. Please report a 'scroll' bug.",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void cancelScrollTask() {
        if (scrollTask!=null) {
            scrollTask.cancel();
            scrollTask = null;
        }
    }

    private boolean isPagingDown;
    private boolean isPagingUp;

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
        hideMenu();

    }

    private void nextPage() {
        isPagingUp = false;
        if(webView.canScrollVertically(1)) {
            webView.pageDown(false);
            //webView.scrollBy(0,webView.getHeight()-14);
        } else {
            isPagingDown = true;
            if (book!=null) showUri(book.getNextSection());


        }
        //saveScrollOffsetDelayed(1500);
        hideMenu();
    }

    private void saveScrollOffsetDelayed(int delay) {
        handler.postDelayed(new Runnable() {
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
        if (book==null) return;
        book.setSectionOffset(offset);
    }

    private void restoreScrollOffsetDelayed(int delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                restoreScrollOffset();
            }
        }, delay);
    }

    private void restoreScrollOffset() {
        if (book==null) return;
        int spos = book.getSectionOffset();
        webView.computeScroll();
        if (spos>=0) {
            webView.scrollTo(0, spos);
            Log.d(TAG, "restoreScrollOffset " + spos);
        } else if (isPagingUp){
            webView.pageDown(true);
            //webView.scrollTo(0,webView.getContentHeight());
        } else if (isPagingDown){
            webView.pageUp(true);
        }
        isPagingUp = false;
        isPagingDown = false;
    }

    private void loadFile(File file) {

        webView.loadData("Loading " + file.getPath(),"text/plain", "utf-8");

        new LoaderTask(this, file).execute();

    }


    private static class LoaderTask extends  AsyncTask<Void,Integer,Book>  {

        private final File file;
        private final WeakReference<ReaderActivity> ractref;

        LoaderTask(ReaderActivity ract, File file) {
            this.file = file;
            this.ractref = new WeakReference<>(ract);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ReaderActivity ract = ractref.get();
            if (ract!=null) {
                ract.progressBar.setProgress(0);
                ract.progressBar.setVisibility(View.VISIBLE);

            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            ReaderActivity ract = ractref.get();
            if (ract!=null) {
                ract.progressBar.setProgress(values[0]);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            ReaderActivity ract = ractref.get();
            if (ract!=null) {
                ract.progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected Book doInBackground(Void... voids) {
            ReaderActivity ract = ractref.get();
            if (ract==null) return null;
            try {
                ract.book = Book.getBookHandler(ract, file.getPath());
                Log.d(TAG, "File " + file);
                if (ract.book!=null) {
                    ract.book.load(file);
                    return ract.book;
                }

                //publishProgress(1);

            } catch (Throwable e) {
                ract.exception = e;
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onPostExecute(Book book) {

            ReaderActivity ract = ractref.get();
            if (ract==null) return;

            String badtext = ract.getString(R.string.book_bug);
            try {
                ract.progressBar.setVisibility(View.GONE);

                if (book==null && ract.exception!=null) {
                    ract.webView.setOnTouchListener(null);
                    ract.webView.setWebViewClient(null);
                    ract.webView.loadData(badtext + ract.exception.getLocalizedMessage(),"text/plain", "utf-8");
                    throw ract.exception;
                }
                if (book !=null && ract.book != null && ract.book.hasDataDir()) {
                    int fontsize = ract.book.getFontsize();
                    if (fontsize != -1) {
                        ract.setFontSize(fontsize);
                    }
                    Uri uri = ract.book.getCurrentSection();
                    if (uri != null) {
                        ract.showUri(uri);
                    } else {
                        Toast.makeText(ract, badtext + " (no sections)", Toast.LENGTH_LONG).show();
                    }
                    if (ract.book.getFlag(FULLSCREEN, true)) {
                        ract.mkFull();
                    } else {
                        ract.mkReg();
                    }
                    ract.setFullscreenMode();
                    ract.setAwake();
                }
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(ract, badtext + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }
    }


    private void showUri(Uri uri) {
        if (uri !=null) {
            Log.d(TAG, "trying to load " + uri);

            //book.clearSectionOffset();
            webView.loadUrl(uri.toString());
        }
    }

    private void handleLink(String clickedLink) {
        if (clickedLink!=null) {
            Log.d(TAG, "clicked on " + clickedLink);
            showUri(book.handleClickedLink(clickedLink));
        }

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
        final float scale = getResources().getDisplayMetrics().density;


       // Log.d(TAG, "def " + defsize + " " + scale);
        final PopupMenu sizemenu = new PopupMenu(this, findViewById(R.id.zoom_button));
        for (int size=minsize; size<=36; size+=2) {
            final int s = size;

            MenuItem mi = sizemenu.getMenu().add(" "+size);
            mi.setCheckable(true);
            mi.setChecked(size==defsize);

            mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    Log.d(TAG, "def " + (defsize-s));
                    int scrolloffset = (int)(-webView.getScrollY()*(defsize - s)/scale/2.7);
                    Log.d(TAG, "scrollby " + scrolloffset);

                    setFontSize(s);

                    //attempt to adjust the scroll to keep the same text position.
                    //  needs much work
                    webView.scrollBy(0, scrolloffset);
                    sizemenu.dismiss();
                    return true;
                }
            });
        }
        sizemenu.show();


    }

    private void mkFull() {

        if (book==null || !book.getFlag(FULLSCREEN, true)) return;
//        findViewById(R.id.fullscreen_no_button).setVisibility(View.VISIBLE);
//        findViewById(R.id.fullscreen_button).setVisibility(View.GONE);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void mkReg() {

//        findViewById(R.id.fullscreen_button).setVisibility(View.VISIBLE);
//        findViewById(R.id.fullscreen_no_button).setVisibility(View.GONE);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();

        synchronized (timerSync) {
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
        }
        restoreBgColor();
    }

    @Override
    protected void onPause() {
        setNoAwake();
        unlistenLight();
        synchronized (timerSync) {
            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }
        }

        if (exception==null) {
            try {
                saveScrollOffset();
            } catch (Throwable t) {
                Log.e(TAG, t.getMessage(), t);
            }
            getSharedPreferences(BookListActivity.prefname, Context.MODE_PRIVATE).edit().putBoolean(READEREXITEDNORMALLY, true).apply();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (timer!=null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        super.onDestroy();
    }

    //    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        //if (hasFocus) mkFull();
//    }

    private void showToc() {
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
        if (tocmap.size()==0) {
            tocmenu.getMenu().add(R.string.no_toc_found);
        }

        tocmenu.show();

    }


    //keep the screen on for a few minutes, but not forever
    private void setAwake() {
        try {
            Window w = this.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            synchronized (timerSync) {
                if (nowakeTask != null) {
                    nowakeTask.cancel();
                    if (timer==null)  {
                        timer = new Timer();
                        Log.d(TAG, "timer was null?");
                    }
                    timer.purge();
                }
                nowakeTask = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setNoAwake();
                                    Log.d(TAG, "Clear FLAG_KEEP_SCREEN_ON");
                                } catch (Throwable t) {
                                    Log.e(TAG, t.getMessage(), t);
                                }

                            }
                        });
                    }
                };

                try {
                    if (timer==null)  return;
                    timer.schedule(nowakeTask, 3 * 60 * 1000);
                } catch (IllegalStateException e) {
                    Log.d(TAG, e.getMessage(), e);
                    //Toast.makeText(this, "Something went wrong. Please report a 'setAwake' bug.", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
            setNoAwake();
        }

    }

    private void setNoAwake() {
        try {
            Window w = ReaderActivity.this.getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
    }

    private SensorEventListener lightSensorListener;


    private void listenLight() {

        unlistenLight();

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {

            lightSensorListener = new SensorEventListener() {

                private final AtomicInteger currentLux = new AtomicInteger(0);
                private int lastCol = 0;

                private final int mincol = 30;
                private final int maxcol = 240;
                private final double luxThreshold = 50;
                private final double multfac = (maxcol-mincol)/luxThreshold;

                private Runnable changer;

                @Override
                public void onSensorChanged(SensorEvent event) {

                    try {
                        currentLux.set((int) event.values[0]);

                        if (changer == null) {
                            changer = new Runnable() {
                                @Override
                                public void run() {
                                    changer = null;
                                    try {
                                        float lux = currentLux.get();

                                        int col = maxcol;
                                        if (lux < luxThreshold) {

                                            col = (int) (lux * multfac + mincol);
                                            if (col < mincol) col = mincol;
                                            if (col > maxcol) col = maxcol;

                                        }
                                        Log.d(TAG, "lightval " + lux + " grey " + col);

                                        if (Math.abs(lastCol - col) > 1 * multfac) {

                                            lastCol = col;
                                            int color = Color.argb(255, col + 15, col + 10, (int) (col + Math.min(lux / luxThreshold * 10, 10)));

                                            applyColor(color);
                                        }
                                    } catch (Throwable t) {
                                        Log.e(TAG, t.getMessage(), t);
                                    }

                                }
                            };
                            handler.postDelayed(changer, 3000);


                        }
                    } catch (Throwable t) {
                        Log.e(TAG, t.getMessage(), t);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

            sensorManager.registerListener(
                    lightSensorListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    private void unlistenLight() {
        try {
            if (lightSensorListener != null) {
                SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                sensorManager.unregisterListener(lightSensorListener);
                lightSensorListener = null;
            }
        }  catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
    }


    private void showBrightnessControl() {
        if (book==null) return;

        PopupMenu bmenu = new PopupMenu(this, findViewById(R.id.brightness_button));
        int bg = book.getBackgroundColor();

        MenuItem norm = bmenu.getMenu().add(R.string.book_default);

        if (bg==Integer.MAX_VALUE) {
            norm.setCheckable(true);
            norm.setChecked(true);
        }

        norm.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                unlistenLight();
                saveScrollOffset();
                book.clearBackgroundColor();
                resetColor();
                webView.reload();
                return true;
            }
        });


        if (hasLightSensor) {
            MenuItem auto = bmenu.getMenu().add(getString(R.string.auto_bright));

            if (bg == Color.TRANSPARENT) {
                auto.setCheckable(true);
                auto.setChecked(true);
            }

            auto.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    book.setBackgroundColor(Color.TRANSPARENT);
                    restoreBgColor();
                    return true;
                }
            });

        }


        for (int i = 0; i<7; i++) {
            int b = i*33;
            final int color = Color.argb(255, 255-b, 250-b, 250-i-b);
            String strcolor;
            switch (i) {
                case 0:
                    strcolor = (i+1) + " - " + getString(R.string.bright);
                    break;
                case 3:
                    strcolor = (i+1) + " - " + getString(R.string.bright_medium);
                    break;
                case 6:
                    strcolor = (i+1) + " - " + getString(R.string.dim);
                    break;
                default:
                    strcolor = (i+1) + "";

            }

            MenuItem m = bmenu.getMenu().add(strcolor);
            m.setIcon(new ColorDrawable(color));
            if (bg==color) {
                m.setCheckable(true);
                m.setChecked(true);
            }

            m.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    unlistenLight();
                    book.setBackgroundColor(color);
                    restoreBgColor();
                    return true;
                }
            });
        }
        bmenu.show();
    }

    private void restoreBgColor() {
        if (book!=null && book.hasDataDir()) {
            int bgcolor = book.getBackgroundColor();
            switch (bgcolor) {
                case Color.TRANSPARENT:
                    listenLight();
                    break;
                case Integer.MAX_VALUE:
                    unlistenLight();
                    resetColor();
                    //book.clearBackgroundColor();
                    //webView.reload();
                    break;
                default:
                    unlistenLight();
                    applyColor(bgcolor);
            }
        }
    }

    private void applyColor(int color) {
        applyColor(color, false);
    }

    private void resetColor() {
        applyColor(Color.argb(255,245,245,245), true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void applyColor(int color, boolean controlsonly) {
        currentDimColor = color;
        try {

            ViewGroup controls = findViewById(R.id.controls_layout);
            setDimLevel(controls, color);
            for (int i = 0; i < controls.getChildCount(); i++) {
                View button = controls.getChildAt(i);
                setDimLevel(button, color);
            }

//            ViewGroup extracontrols = findViewById(R.id.slide_menu);
//            for (int i = 0; i < extracontrols.getChildCount(); i++) {
//                View button = extracontrols.getChildAt(i);
//                setDimLevel(button, color);
//            }

            ReaderActivity.this.getWindow().setBackgroundDrawable(null);
            webView.setBackgroundColor(color);
            ReaderActivity.this.getWindow().setBackgroundDrawable(new ColorDrawable(color));

            if (!controlsonly) {
                //Log.d("GG", String.format("#%6X", color & 0xFFFFFF));
                webView.getSettings().setJavaScriptEnabled(true);
                webView.evaluateJavascript("(function(){var newSS, styles='* { background: " + String.format("#%6X", color & 0xFFFFFF) + " ! important; color: black !important } :link, :link * { color: #000088 !important } :visited, :visited * { color: #44097A !important }'; if(document.createStyleSheet) {document.createStyleSheet(\"javascript:'\"+styles+\"'\");} else { newSS=document.createElement('link'); newSS.rel='stylesheet'; newSS.href='data:text/css,'+escape(styles); document.getElementsByTagName(\"head\")[0].appendChild(newSS); } })();", null);
                webView.getSettings().setJavaScriptEnabled(false);
            }
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
            Toast.makeText(this, t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setDimLevel(View button, int color) {
        try {
            button.setBackground(null);
            Drawable btn = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                btn = getResources().getDrawable(android.R.drawable.btn_default, null).mutate();
            } else {
                btn = getResources().getDrawable(android.R.drawable.btn_default).mutate();
            }
            btn.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            button.setBackground(btn);
            if (button instanceof ImageButton) {
                ((ImageButton) button).getDrawable().mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
    }
}

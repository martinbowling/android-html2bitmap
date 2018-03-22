package com.izettle.html2bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Html2Bitmap {

    private static final String TAG = "Html2Bitmap";
    private static final int MSG_MEASURE = 2;
    private static final int MSG_SCREENSHOT = 5;
    private final HandlerThread handlerThread;
    private final Handler backgroundHandler;
    private final Handler mainHandler;

    private WebView webView;
    private Callback listener;

    private Html2Bitmap(@NonNull final Context context, @NonNull String html, int paperWidth, @NonNull Callback callback) {
        this.listener = callback;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }

        webView = new WebView(context);

        webView.setInitialScale(100);

        webView.setVerticalScrollBarEnabled(false);

        final WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);

        handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();

        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                if (webView.getContentHeight() == 0) {
                    return;
                }

                // set the correct height of the webview and do measure and layout using it before taking the screenshot
                int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(paperWidth, View.MeasureSpec.EXACTLY);
                int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(webView.getContentHeight(), View.MeasureSpec.EXACTLY);
                webView.measure(widthMeasureSpec, heightMeasureSpec);
                webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());

                backgroundHandler.removeMessages(MSG_SCREENSHOT);
                backgroundHandler.sendEmptyMessageDelayed(MSG_SCREENSHOT, 200);
            }
        };

        backgroundHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (webView.getMeasuredHeight() == 0) {
                    return;
                }
                try {
                    Bitmap screenshot = screenshot(webView);

                    listener.finished(screenshot);
                } catch (Throwable t) {

                    listener.error(t);
                }
                handlerThread.interrupt();
            }
        };

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinished(50);
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                pageFinished(50);
            }
        });

        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
    }

    @Nullable
    public static Bitmap getBitmap(Context context, String html, int width) {
        Html2Bitmap.BitmapCallable bitmapCallable = new Html2Bitmap.BitmapCallable();
        FutureTask<Bitmap> bitmapFutureTask = new FutureTask<>(bitmapCallable);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(bitmapFutureTask);

        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable myRunnable = () -> new Html2Bitmap(context, html, width, bitmapCallable);
        mainHandler.post(myRunnable);


        try {
            return bitmapFutureTask.get(15, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    private void pageFinished(int delay) {

        backgroundHandler.removeMessages(MSG_SCREENSHOT);
        mainHandler.removeMessages(MSG_MEASURE);
        mainHandler.sendEmptyMessageDelayed(MSG_MEASURE, delay);
    }

    private Bitmap screenshot(WebView webView) throws Throwable {

        Bitmap bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(), webView.getMeasuredHeight(), Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);

        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, 0));

        webView.draw(canvas);
        return bitmap;
    }

    private interface Callback {
        void finished(Bitmap bitmap);

        void error(Throwable error);
    }

    private static class BitmapCallable implements Callable<Bitmap>, Callback {

        CountDownLatch latch = new CountDownLatch(1);
        private Bitmap bitmap;

        private BitmapCallable() {
        }

        @Override
        public Bitmap call() throws Exception {
            latch.await();
            return bitmap;
        }

        @Override
        public void finished(Bitmap bitmap) {
            this.bitmap = bitmap;
            latch.countDown();
        }

        @Override
        public void error(Throwable error) {
            Log.e(TAG, "", error);
            latch.countDown();
        }
    }
}

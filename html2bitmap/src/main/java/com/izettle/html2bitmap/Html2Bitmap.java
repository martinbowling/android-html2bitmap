package com.izettle.html2bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Html2Bitmap {

    private static final String TAG = "Html2Bitmap";
    private final Context context;
    private final String html;
    private final int bitmapWidth;
    private final int delayMeasure;
    private final int delayScreenShot;
    private boolean strictMode;
    private long timeout;

    private Html2Bitmap(Context context, String html, int bitmapWidth, int delayMeasure, int delayScreenShot, boolean strictMode, long timeout) {
        this.context = context;
        this.html = html;
        this.bitmapWidth = bitmapWidth;
        this.delayMeasure = delayMeasure;
        this.delayScreenShot = delayScreenShot;
        this.strictMode = strictMode;
        this.timeout = timeout;
    }

    @Nullable
    private static Bitmap getBitmap(final Html2Bitmap html2Bitmap) {
        final BitmapCallable bitmapCallable = new BitmapCallable();
        FutureTask<Bitmap> bitmapFutureTask = new FutureTask<>(bitmapCallable);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(bitmapFutureTask);

        Handler mainHandler = new Handler(html2Bitmap.context.getMainLooper());

        final Html2BitmapWebView html2BitmapWebView = new Html2BitmapWebView(html2Bitmap.context, html2Bitmap.html, html2Bitmap.bitmapWidth, html2Bitmap.delayMeasure, html2Bitmap.delayScreenShot, html2Bitmap.strictMode);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                html2BitmapWebView.load(bitmapCallable);
            }
        });

        try {
            return bitmapFutureTask.get(html2Bitmap.timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            Log.e(TAG, "", e);
        } finally {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    html2BitmapWebView.cleanup();
                }
            });
        }
        return null;
    }

    public Bitmap getBitmap() {
        return getBitmap(this);
    }

    public static class Builder {
        private Context context;
        private String html;
        private int bitmapWidth = 480;
        private int delayMeasure = 30;
        private int delayScreenShot = 30;
        private boolean strictMode = false;
        private long timeout = 15;

        public Builder() {

        }

        public Builder(@NonNull Context context, @NonNull String html) {
            setContext(context);
            setHtml(html);
        }

        public Builder setContext(@NonNull Context context) {
            this.context = context;
            return this;
        }

        public Builder setHtml(@NonNull String html) {
            this.html = html;
            return this;
        }

        public Builder setBitmapWidth(int bitmapWidth) {
            this.bitmapWidth = bitmapWidth;
            return this;
        }

        public Builder setDelayMeasure(int delayMeasure) {
            this.delayMeasure = delayMeasure;
            return this;
        }

        public Builder setDelayScreenShot(int delayScreenShot) {
            this.delayScreenShot = delayScreenShot;
            return this;
        }

        public Builder setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
            return this;
        }

        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Html2Bitmap build() {
            if (context == null) {
                throw new NullPointerException();
            }
            if (html == null) {
                throw new NullPointerException();
            }
            return new Html2Bitmap(context, html, bitmapWidth, delayMeasure, delayScreenShot, strictMode, timeout);
        }
    }
}

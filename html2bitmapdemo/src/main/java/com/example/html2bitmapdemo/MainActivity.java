package com.example.html2bitmapdemo;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import android.util.Log;

import android.content.Context;

import android.content.ContextWrapper;
import 	android.os.Environment;
import android.net.Uri;

import 	java.text.SimpleDateFormat;
import 	java.text.DateFormat;
import 	java.util.Date;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import 	java.io.OutputStream;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements BitmapGeneratingAsyncTask.Callback {

    private EditText htmlEditText;
    private ImageView imageView;
    private EditText widthEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        htmlEditText = findViewById(R.id.html_edit_text);
        widthEditText = findViewById(R.id.width_edit_text);

        updateBitmap();
        findViewById(R.id.generate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBitmap();


            }
        });


    }

    private void updateBitmap() {
        getEnteredWidthOrDefault();
        new BitmapGeneratingAsyncTask(this, htmlEditText.getText().toString(), getEnteredWidthOrDefault(), this).execute();
    }

    @Override
    public void done(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);

        imageView.buildDrawingCache();
        Bitmap bm=imageView.getDrawingCache();

        Uri imageuri = saveImageToInternalStorage(this,bm);

        Log.v("html2bitmap", imageuri.toString());

    }

    public static Uri saveImageToInternalStorage(Context mContext, Bitmap bitmap){

        String mTimeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());

        String mImageName = "snap_"+mTimeStamp+".jpg";

        ContextWrapper wrapper = new ContextWrapper(mContext);

        File file = wrapper.getDir("Images",MODE_PRIVATE);

        file = new File(file, mImageName);

        try{

            OutputStream stream = null;

            stream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);

            stream.flush();

            stream.close();

        }catch (IOException e)
        {
            e.printStackTrace();
        }

        Uri mImageUri = Uri.parse(file.getAbsolutePath());

        return mImageUri;
    }





    public int getEnteredWidthOrDefault() {
        String enteredValue = widthEditText.getText().toString();
        if (!TextUtils.isEmpty(enteredValue)) {
            return Integer.parseInt(enteredValue);
        } else {
            return 2550;
        }
    }



}

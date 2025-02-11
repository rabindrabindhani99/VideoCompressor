package com.rabindradev.videocompressor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.getExternalStoragePublicDirectory;

import com.iceteck.silicompressorr.SiliCompressor;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_TAKE_VIDEO = 200;
    Uri compressUri = null;
    ImageView imageView;
    TextView picDescription;
    ImageView videoImageView;
    LinearLayout compressionMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoImageView = findViewById(R.id.videoImageView);
        picDescription = findViewById(R.id.pic_description);
        compressionMsg = findViewById(R.id.compressionMsg);

        videoImageView.setOnClickListener(view -> dispatchTakeVideoIntent());
    }
    private void dispatchTakeVideoIntent() {
        Intent pickVideoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        pickVideoIntent.setType("video/*");
        startActivityForResult(pickVideoIntent, REQUEST_TAKE_VIDEO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_VIDEO && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedVideoUri = data.getData();
                File videoDirectory = new File(getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/StudentMemories/videos");
                if (videoDirectory.mkdirs() || videoDirectory.isDirectory()) {
                    new VideoCompressAsyncTask(this.getApplicationContext()).execute("false", selectedVideoUri.toString(), videoDirectory.getPath());
                }
            }
        }

    }
    class VideoCompressAsyncTask extends AsyncTask<String, String, String> {

        Context mContext;

        public VideoCompressAsyncTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            compressionMsg.setVisibility(View.VISIBLE);
            picDescription.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... paths) {
            String filePath = null;
            try {
                boolean b = Boolean.parseBoolean(paths[0]);
                if (b) {
                    filePath = SiliCompressor.with(mContext).compressVideo(paths[1], paths[2]);
                } else {
                    Uri videoContentUri = Uri.parse(paths[1]);
                    filePath = SiliCompressor.with(mContext).compressVideo(videoContentUri, paths[2]);
                }


            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return filePath;

        }


        @Override
        protected void onPostExecute(String compressedFilePath) {
            super.onPostExecute(compressedFilePath);
            Log.i("RabindraDev Compressor", "Path: " + compressedFilePath);
        }
    }


}

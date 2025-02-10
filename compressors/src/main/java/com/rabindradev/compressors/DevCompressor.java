package com.rabindradev.compressors;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.rabindradev.compressors.videocompression.MediaController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DevCompressor {

    private static final String LOG_TAG = DevCompressor.class.getSimpleName();
    public static String videoCompressionPath;
    private static final String IMAGE_DESTINATION = "StudentMemories/images";

    static volatile DevCompressor singleton = null;
    private static Context mContext;
    private static final String FILE_PROVIDER_AUTHORITY = ".rabindradev.compressors.provider";

    public DevCompressor(Context context) {
        mContext = context;
    }

    // initialise the class and set the context
    public static DevCompressor with(Context context) {
        if (singleton == null) {
            synchronized (DevCompressor.class) {
                if (singleton == null) {
                    singleton = new Builder(context).build();
                }
            }
        }
        return singleton;

    }
    private static boolean deleteImageFile(String imageUri) {
        Uri uri = Uri.parse(imageUri);
        int cnt = mContext.getContentResolver().delete(uri, null, null);
        return cnt > 0;
    }
    public String compress(String uriString, File destination) {
        return compressImage(uriString, destination);
    }

    protected static String getAuthorities(@NonNull Context context) {
        return context.getPackageName() + FILE_PROVIDER_AUTHORITY;
    }
    public Bitmap getCompressBitmap(String imagePath) throws IOException {
        return getCompressBitmap(imagePath, false);
    }

    public String compress(String uriString, File destination, boolean deleteSourceImage) {

        String compressedImagePath = compressImage(uriString, destination);

        if (deleteSourceImage) {
            boolean isdeleted = deleteImageFile(uriString);
            Log.d(LOG_TAG, (isdeleted) ? "Source image file deleted" : "Error: Source image file not deleted.");

        }

        return compressedImagePath;
    }
    public Bitmap getCompressBitmap(String imageUri, boolean deleteSourceImage) throws IOException {

        String compressedImageUriString = compressImage(imageUri, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DESTINATION));
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), Uri.parse(compressedImageUriString));
        } else {
            bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), Uri.fromFile(new File(compressedImageUriString)));
        }

        if (deleteSourceImage) {
            boolean isdeleted = deleteImageFile(imageUri);
            Log.d(LOG_TAG, (isdeleted) ? "Source image file deleted" : "Error: Source image file not deleted.");
        }
        return bitmap;
    }
    public String compressVideo(String videoFilePath, String destinationDir) throws URISyntaxException {
        return compressVideo(videoFilePath, destinationDir, 0, 0, 0);
    }

    public String compressVideo(Uri videoContentUri, String destinationDir) throws URISyntaxException {
        return compressVideo(videoContentUri, destinationDir, 0, 0, 0);
    }

    public String compressVideo(String videoFilePath, String destinationDir, int outWidth, int outHeight, int bitrate) throws URISyntaxException {
        boolean isconverted = MediaController.getInstance(mContext).convertVideo(videoFilePath, new File(destinationDir), outWidth, outHeight, bitrate);
        if (isconverted) {
            Log.v(LOG_TAG, "Video Conversion Complete");
        } else {
            Log.v(LOG_TAG, "Video conversion in progress");
        }

        return MediaController.cachedFile.getPath();

    }
    public String compressVideo(Uri videoContentUri, String destinationDir, int outWidth, int outHeight, int bitrate) throws URISyntaxException {
        boolean isConverted = MediaController.getInstance(mContext).convertVideo(mContext, videoContentUri, new File(destinationDir), outWidth, outHeight, bitrate);
        if (isConverted) {
            Log.v(LOG_TAG, "Video Conversion Complete");
        } else {
            Log.v(LOG_TAG, "Video conversion in progress");
        }

        return MediaController.cachedFile.getPath();
    }


    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }
    private String getFilename(String filename, File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
        String ext = ".jpg";
        String nameFirstPart = file.getAbsolutePath() + "/IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nameFull = nameFirstPart + ext;
        int x = 1;
        while (new File(nameFull).exists()) {
            nameFull = nameFirstPart + "_" + x + ext;
            x++;
        }

        return nameFull;
    }
    private String compressImage(String uriString, File destDirectory) {
        try {
            Uri imageUri = Uri.parse(uriString);
            Bitmap scaledBitmap = null;

            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = true;
            Bitmap bmp = BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(imageUri), null, options);

            int actualHeight = options.outHeight;
            int actualWidth = options.outWidth;

            float maxHeight = 816.0f;
            float maxWidth = 612.0f;
            float imgRatio = actualWidth / actualHeight;
            float maxRatio = maxWidth / maxHeight;

            if (actualHeight > maxHeight || actualWidth > maxWidth) {
                if (imgRatio < maxRatio) {
                    imgRatio = maxHeight / actualHeight;
                    actualWidth = (int) (imgRatio * actualWidth);
                    actualHeight = (int) maxHeight;
                } else if (imgRatio > maxRatio) {
                    imgRatio = maxWidth / actualWidth;
                    actualHeight = (int) (imgRatio * actualHeight);
                    actualWidth = (int) maxWidth;
                } else {
                    actualHeight = (int) maxHeight;
                    actualWidth = (int) maxWidth;

                }
            }

            options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

            options.inJustDecodeBounds = false;

            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inTempStorage = new byte[16 * 1024];

            bmp = BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(imageUri), null, options);
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);


            float ratioX = actualWidth / (float) options.outWidth;
            float ratioY = actualHeight / (float) options.outHeight;
            float middleX = actualWidth / 2.0f;
            float middleY = actualHeight / 2.0f;

            Matrix scaleMatrix = new Matrix();
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

            Canvas canvas = new Canvas(scaledBitmap);
            canvas.setMatrix(scaleMatrix);
            canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

            ExifInterface exif;
            try {
                exif = new ExifInterface(mContext.getContentResolver().openInputStream(imageUri));

                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, 0);
                Log.d("EXIF", "Exif: " + orientation);
                Matrix matrix = new Matrix();
                if (orientation == 6) {
                    matrix.postRotate(90);
                    Log.d("EXIF", "Exif: " + orientation);
                } else if (orientation == 3) {
                    matrix.postRotate(180);
                    Log.d("EXIF", "Exif: " + orientation);
                } else if (orientation == 8) {
                    matrix.postRotate(270);
                    Log.d("EXIF", "Exif: " + orientation);
                }
                scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                        scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                        true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StudentMemories/");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri resultUri = mContext.getContentResolver().insert(collection, values);

                OutputStream out = mContext.getContentResolver().openOutputStream(resultUri);

                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                mContext.getContentResolver().update(resultUri, values, null, null);

                return resultUri.toString();

            } else {
                String filename = getFilename(uriString, destDirectory);

                FileOutputStream out = new FileOutputStream(filename);

                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

                return filename;
            }

        } catch (FileNotFoundException | OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

    }

    public String compress(int drawableID) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getApplicationContext().getResources(), drawableID);
            if (null != bitmap) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File image = File.createTempFile(imageFileName,".jpg", storageDir);

                FileOutputStream out = new FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();

                Uri copyImageUri = FileProvider.getUriForFile(mContext, getAuthorities(mContext), image);

                String compressedImagePath = compressImage(copyImageUri.toString(), new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DESTINATION));

                if (image.exists()) {
                    deleteImageFile(copyImageUri.toString());
                }

                return compressedImagePath;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    public static class Builder {
        private final Context context;
        Builder(@NonNull Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            this.context = context.getApplicationContext();
        }
        public DevCompressor build() {
            Context context = this.context;

            return new DevCompressor(context);
        }
    }
}

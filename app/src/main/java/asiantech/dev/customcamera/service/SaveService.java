package asiantech.dev.customcamera.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import asiantech.dev.customcamera.CameraActivity;
import asiantech.dev.customcamera.Helpers.Helpers;
import asiantech.dev.customcamera.camera.SaveTakeImage;

/**
 * Copyright © 2015 AsianTech inc.
 * Created by PhuQuy on 8/20/15.
 */
public class SaveService extends Service {

    private static final String TAG = SaveService.class.getSimpleName();
    private int mRotation;
    private int cameraId;
    private byte [] mData = new byte[1250];

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRotation = Integer.parseInt(intent.getExtras().get(CameraActivity.KEY_ROTATION).toString());
        cameraId = Integer.parseInt(intent.getExtras().get(CameraActivity.KEY_CAMERA_ID).toString());
        mData = intent.getExtras().getByteArray(CameraActivity.KEY_CAMERA_DATA);
        SaveImage(mData);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void SaveImage(byte[] mData) {
        File pictureFiles = SaveTakeImage.getOutputMediaFile(SaveTakeImage.MEDIA_TYPE_IMAGE);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFiles);
            if (mRotation == 0 && Integer.parseInt(Build.VERSION.SDK) >= 22
                    && cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Bitmap bitmap = decodeSampledBitmapFromResource(mData);
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                bitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        false
                );
                // save data into file
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                try {
                    bitmap.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (mRotation == 0) {
                    fos.write(mData);
                } else {
                    //  Bitmap bitmap = BitmapFactory.decodeByteArray(mData, 0, mData.length);
                    Bitmap bitmap = decodeSampledBitmapFromResource(mData);
                    Matrix matrix = new Matrix();
                    if (Integer.parseInt(Build.VERSION.SDK) < 22 && cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        float[] mirrorY = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                        Matrix matrixMirrorY = new Matrix();
                        matrixMirrorY.setValues(mirrorY);
                        matrix.postConcat(matrixMirrorY);
                    }
                    matrix.postRotate(mRotation);
                    bitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            matrix,
                            false
                    );
                    // save data into file
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    try {
                        bitmap.recycle();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{pictureFiles.toString()},
                    new String[]{"image/jpeg"},
                    null
            );
            fos.close();
            Log.d("qqq", "" + pictureFiles.getAbsolutePath());
//            mBtnTakeImage.setEnabled(true);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public Bitmap decodeSampledBitmapFromResource(
            byte[] mData) {
        int reqWidth, reqHeight;
        reqWidth = Helpers.getScreenWidth(getApplicationContext());
        reqWidth = (reqWidth / 5) * 2;
        reqHeight = reqWidth;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        //  BitmapFactory.decodeStream(is, null, options);
        BitmapFactory.decodeByteArray(mData, 0, mData.length, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(mData, 0, mData.length, options);
    }

    public int calculateInSampleSize(BitmapFactory.Options options,
                                     int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;

    }
}

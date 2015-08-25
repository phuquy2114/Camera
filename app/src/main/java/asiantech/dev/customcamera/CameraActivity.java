package asiantech.dev.customcamera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asiantech.dev.customcamera.Helpers.Helpers;
import asiantech.dev.customcamera.camera.OrientationListener;
import asiantech.dev.customcamera.camera.ResizeSurfaceView;
import asiantech.dev.customcamera.camera.SaveTakeImage;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;


/**
 * Copyright © 2015 AsianTech inc.
 * Created by PhuQuy on 8/19/15.
 */
@EActivity(R.layout.activity_camera)
public class CameraActivity extends Activity implements Callback, OnClickListener {

    @ViewById(R.id.surfaceView)
    protected SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    @ViewById(R.id.btnSwapCamera)
    protected Button mBtnSwapCamera;
    @ViewById(R.id.btnFlash)
    protected Button mBtnFlashCamera;
    @ViewById(R.id.btnTakeImage)
    protected Button mBtnTakeImage;
    private int cameraId;
    private CameraMode mEnumFlashCamera = CameraMode.On;
    private int mRotation;
    public static final String KEY_ROTATION = "key_rotation";
    public static final String KEY_CAMERA_ID = "key_camera_id";
    public static final String KEY_CAMERA_DATA = "key_camera_data";
    private byte [] mData;
    private OrientationListener mOrientationListener;
    private static final String TAG = CameraActivity.class.getSimpleName();
    private PackageManager mPackageManager;

    private enum CameraMode {
        Off, On, Auto
    }

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
        }
    };

    @AfterViews
    public void afterViews() {
        mPackageManager = getApplicationContext().getPackageManager();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initialize();
        setValues();
        setEvents();
    }

    public void initialize() {
        mOrientationListener = new OrientationListener(this);
    }

    public void setValues() {
        cameraId = CameraInfo.CAMERA_FACING_BACK;
        mSurfaceHolder = mSurfaceView.getHolder();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Check mCamera big one then
        if (Camera.getNumberOfCameras() > 1) {
            mBtnSwapCamera.setVisibility(View.VISIBLE);
        }
        if (!getBaseContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH)) {
            mBtnFlashCamera.setVisibility(View.GONE);
        }
    }

    public void setEvents() {
        mSurfaceHolder.addCallback(this);
        mBtnSwapCamera.setOnClickListener(this);
        mBtnTakeImage.setOnClickListener(this);
        mBtnFlashCamera.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        if (!openCamera(CameraInfo.CAMERA_FACING_BACK)) {
            alertCameraDialog();
        }

    }

    private boolean openCamera(int id) {
        boolean result = false;
        cameraId = id;
        releaseCamera();
        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCamera != null) {
            try {

                new ResizeSurfaceView(getApplicationContext(), mCamera, cameraId);
                setUpCamera(mCamera);
                mCamera.setErrorCallback(new ErrorCallback() {

                    @Override
                    public void onError(int error, Camera camera) {

                    }
                });

                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
                result = false;
                releaseCamera();
            }
        }
        if (mCamera != null) {
            new ResizeSurfaceView(getApplicationContext(), mCamera, cameraId);
        }
        return result;
    }


    private void setUpCamera(Camera c) {
        CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        mRotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
            default:
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // frontFacing
            mRotation = (info.orientation + degree) % 360;
            mRotation = (360 - mRotation) % 360;
        } else {
            // Back-facing
            mRotation = (info.orientation - degree + 360) % 360;
        }
        c.setDisplayOrientation(mRotation);
        Parameters params = c.getParameters();

        showFlashButton(params);

        List<String> focusModes = params.getSupportedFlashModes();
        if (focusModes != null) {
            if (focusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFlashMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }

        params.setRotation(mRotation);
    }

    private void showFlashButton(Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
                && params.getSupportedFlashModes() != null
                && params.getSupportedFocusModes().size() > 1;

        mBtnFlashCamera.setVisibility(showFlash ? View.VISIBLE
                : View.INVISIBLE);

    }

    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.setErrorCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnFlash:
                flashOnButton();
                break;
            case R.id.btnSwapCamera:
                if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                    flipCamera();
                } else {
                    Toast.makeText(getApplicationContext(), "Not ", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnTakeImage:
                mOrientationListener.rememberOrientation();
                mRotation = (mOrientationListener.getRememberedOrientation() + 90) % 360;
                //  new TakeImage(getApplicationContext(), mCamera , mRotation , cameraId);
                TakeImageSave();
//                mBtnTakeImage.setEnabled(false);
//                Intent intent = new Intent(this, SaveService.class);
//                intent.putExtra(CameraActivity.KEY_ROTATION, mRotation);
//                intent.putExtra(CameraActivity.KEY_CAMERA_ID, cameraId);
//                intent.putExtra(CameraActivity.KEY_CAMERA_DATA, mData);
//                startService(intent);
                break;
            default:
                break;
        }
    }

    private void flipCamera() {
        int id = (cameraId == CameraInfo.CAMERA_FACING_BACK ? CameraInfo.CAMERA_FACING_FRONT
                : CameraInfo.CAMERA_FACING_BACK);
        if (!openCamera(id)) {
            alertCameraDialog();
        }
    }

    private void alertCameraDialog() {
        AlertDialog.Builder dialog = createAlert(CameraActivity.this,
                "Camera info", "error to open mCamera");
        dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });

        dialog.show();
    }

    private Builder createAlert(Context context, String title, String message) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(context,
                        android.R.style.Theme_Holo_Light_Dialog));
        dialog.setIcon(R.mipmap.ic_launcher);
        if (title != null)
            dialog.setTitle(title);
        else
            dialog.setTitle("Information");
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;

    }

    private void flashOnButton() {
        if (mCamera != null) {
            try {
                Parameters param = mCamera.getParameters();
                param.setFlashMode(mEnumFlashCamera == CameraMode.On ? Parameters.FLASH_MODE_ON
                        : (mEnumFlashCamera == CameraMode.Off ? Parameters.FLASH_MODE_OFF : Parameters.FLASH_MODE_AUTO));
                mCamera.setParameters(param);
                switch (mEnumFlashCamera) {
                    case On:
                        mBtnFlashCamera.setBackgroundResource(R.drawable.ic_camera_on);
                        //    param.setFlashMode(Parameters.FLASH_MODE_ON);
                        mEnumFlashCamera = CameraMode.Auto;
                        break;
                    case Off:
                        mBtnFlashCamera.setBackgroundResource(R.drawable.ic_camera_off);
                        //       param.setFlashMode(Parameters.FLASH_MODE_OFF);
                        mEnumFlashCamera = CameraMode.On;
                        break;
                    case Auto:
                        mBtnFlashCamera.setBackgroundResource(R.drawable.ic_camera_auto);
                        //   param.setFlashMode(Parameters.FLASH_MODE_AUTO);
                        mEnumFlashCamera = CameraMode.Off;
                        break;
                }
                //  mCamera.setParameters(param);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationListener.disable();
        releaseCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            onFocusAreasClick();
        }
        return true;

    }

    @TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
    private void onFocusAreasClick() {
        int focusSize = getResources().getDimensionPixelSize(R.dimen.focus_size);
        if (VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mCamera.getParameters().getFocusMode()
                    .equals(Parameters.FOCUS_MODE_AUTO)) {
                mCamera.autoFocus(mAutoFocusCallback);
            }
            return;
        }
        // set Camera parameters
        Parameters params = mCamera.getParameters();
        mCamera.cancelAutoFocus();
        if (params.getMaxNumMeteringAreas() > 0) { // check that metering areas are supported
            List<Area> meteringAreas = new ArrayList<Area>();
            Rect areaRect1 = new Rect(-focusSize / 2, -focusSize / 2, focusSize / 2, focusSize / 2);    // specify an area in center of image
            meteringAreas.add(new Camera.Area(areaRect1, 1000)); // set weight to 100%
            params.setMeteringAreas(meteringAreas);
            params.setFocusAreas(meteringAreas);
            mCamera.setParameters(params);
        }
        if (params.getFocusMode()
                .equals(Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus(mAutoFocusCallback);

        }
    }


    public void TakeImageSave() {
//        mCamera.takePicture(null, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(final byte[] mData, Camera camera) {
//                final File pictureFiles = SaveTakeImage.getOutputMediaFile(SaveTakeImage.MEDIA_TYPE_IMAGE);
//                if (pictureFiles == null) {
//                    Log.d(TAG, "Error creating media file, check storage permissions: ");
//                }
//                SaveImage(pictureFiles, mData);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                final File pictureFiles = SaveTakeImage.getOutputMediaFile(SaveTakeImage.MEDIA_TYPE_IMAGE);
                Log.d("qqq", pictureFiles.getPath());
                mData = data;
                saveBitmap(camera, pictureFiles.getPath(), data);
            }
        });

//        mCamera.takePicture(null, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                final File pictureFiles = SaveTakeImage.getOutputMediaFile(SaveTakeImage.MEDIA_TYPE_IMAGE);
//                Log.d("qqq", pictureFiles.getPath());
//                mData = data;
//            }
//        });

    }

    synchronized void saveBitmap(Camera camera, String filename, byte[] data) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, camera
                .getParameters().getPreviewSize().width, camera
                .getParameters().getPreviewSize().height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Rect rectImage = new Rect(0, 0, camera.getParameters()
                .getPreviewSize().width, camera.getParameters()
                .getPreviewSize().height);
        yuvimage.compressToJpeg(rectImage, 50, baos);
        byte[] bytes = baos.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            try {
                bitmap.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void SaveImage(File pictureFile, byte[] mData) {
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            Log.d("qqq", "" + mRotation);
            if (mRotation == 0 && Integer.parseInt(VERSION.SDK) >= 22
                    && cameraId == CameraInfo.CAMERA_FACING_FRONT) {
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
                    Log.d(TAG, "Data.length: " + mData.length);
                    //  Bitmap bitmap = BitmapFactory.decodeByteArray(mData, 0, mData.length);
                    Bitmap bitmap = decodeSampledBitmapFromResource(mData);
                    Matrix matrix = new Matrix();
                    if (Integer.parseInt(VERSION.SDK) < 22 && cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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
                    getApplicationContext(),
                    new String[]{pictureFile.toString()},
                    new String[]{"image/jpeg"},
                    null
            );
            fos.close();
            Log.d("qqq", "" + pictureFile.getAbsolutePath());
            openCamera(cameraId);
            mBtnTakeImage.setEnabled(true);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public Bitmap decodeSampledBitmapFromResource(
            byte[] mData) {
        int reqWidth, reqHeight;
        reqWidth = Helpers.getScreenWidth(this);
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

package asiantech.dev.customcamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;


public class CameraOpenCvActivity extends Activity implements
        CvCameraViewListener2, OnTouchListener {

    private Mat mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;
    private int mCountImage = 0;
    private int mCountCamera = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initComponent();
        eventComponent();

    }

    public void eventComponent() {
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
    }

    private void convertMattoBitmap(Mat mat) {
        Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
            saveImageToExternalStorage(bmp);

        } catch (Exception e) {
            Log.i("TAG", e.getMessage());
        }
    }


    public void saveImageToExternalStorage(Bitmap image) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/camera";

        try {
            if (mCountImage > 0) {
                File dir = new File(fullPath);
                boolean isCheck = true;
                if (!dir.exists()) {
                    isCheck = dir.mkdirs();
                }
                String filename = mCountImage + ".jpg";
                OutputStream fOut;
                File file = new File(fullPath, filename);
                if (file.exists() && isCheck) {
                    isCheck = file.delete();
                }
                if (file.createNewFile() && isCheck) {
                    fOut = new FileOutputStream(file);
                    image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    fOut.flush();
                    fOut.close();

                }
            }
            mCountImage++;
            Log.i("TAG", "" + mCountImage);
        } catch (Exception e) {
            Log.i("TAG", "" + mCountImage + e);
        }
    }

    public void initComponent() {

        setContentView(R.layout.activity_cameraopencv);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraview);
    }

    @Override
    public void onPause() {
        super.onPause();
        thoatcamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
                mLoaderCallback);
    }

    public void thoatcamera() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mRgba.release();
    }

    public void onDestroy() {
        super.onDestroy();
        thoatcamera();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        if (mCountCamera % 7 == 0) {
            convertMattoBitmap(mRgba);

        }
        mCountCamera++;
        return mRgba;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }
}

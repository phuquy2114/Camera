package asiantech.dev.customcamera.camera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

import java.util.List;

import asiantech.dev.customcamera.Helpers.Helpers;

/**
 * Created by PhuQuy on 4/17/15.
 */
public class ResizeSurfaceView {

    private Camera camera;
    private Context mContext;
    private List<Size> mSupportedPreviewSizes;
    private String TAG = ResizeSurfaceView.class.getSimpleName();
    private int cameraId ;



    public ResizeSurfaceView(Context mContext, Camera camera, int cameraId) {
        this.mContext = mContext;
        this.camera = camera;
        this.cameraId  = cameraId;
        setSizeSurfaceView();
    }

    //set size surfaceView
    public void setSizeSurfaceView() {

        Parameters parameters = camera.getParameters();

        // setting auto focus
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            // Autofocus mode is supported
            parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        }

        // get size preview
        mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for(Size size: mSupportedPreviewSizes)
            Log.d(TAG, "mSupportedPreviewSizes: " + size.width + "/" + size.height);
        Log.d(TAG, "=============================================");

        // get size picture
        List<Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        for(Size size: supportedPictureSizes)
            Log.d(TAG, "supportedPictureSizes: " + size.width + "/" + size.height);
        Log.d(TAG, "=============================================");

        // get size screen with width > height
        int screenWidth = Helpers.getScreenWidth(mContext);
        int screenHeight = Helpers.getScreenHeight(mContext);
       // int screenHeight = screenWidth * 4 / 3;
        if (screenWidth < screenHeight) {
            int temp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = temp;
        }

        // set size for picture
        Size largestSizeFullScreen = getMaxPictureSize(supportedPictureSizes,
                screenWidth,
                screenHeight);
        Log.d(TAG, "Height" + screenHeight);
        Log.d(TAG, "Width" + screenWidth);

        parameters.setPictureSize(largestSizeFullScreen.width, largestSizeFullScreen.height);
        // set size for preview
//        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
//            parameters.setPreviewSize(screenWidth,screenHeight);
//        } else {
            Size previewSize = getMaxPictureSize(mSupportedPreviewSizes,
                    largestSizeFullScreen.width, largestSizeFullScreen.height);
            Log.d(TAG, "Width" + previewSize.width);
            Log.d(TAG, "Height" + previewSize.height);
            parameters.setPreviewSize(previewSize.width, previewSize.height);

        camera.setParameters(parameters);

    }

    private Size getMaxPictureSize(List<Size> supportedPictureSizes,
                                   int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;

        if (supportedPictureSizes == null) return null;

        Size optimalSize = null;
        int maxWidth = 0;
        double ratio;

        for (Size size : supportedPictureSizes) {
            ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (size.width > maxWidth) {
                optimalSize = size;
                maxWidth = size.width;
            }
        }

        double minDifference = Double.MAX_VALUE;
        if (optimalSize == null) {

            for (Size size : supportedPictureSizes) {
                ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) < minDifference) {
                    optimalSize = size;
                    minDifference = Math.abs(ratio - targetRatio);
                }
            }
        }

        return optimalSize;
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}

package asiantech.dev.customcamera.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asiantech.dev.customcamera.R;


public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	public static final String TAG = CameraPreview.class.getSimpleName();
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Context mContext;
	
	private List<Size> mSupportedPreviewSizes;
	
	private AutoFocusCallback mAutoFocusCallback;
	
	public CameraPreview(Context context, Camera camera, AutoFocusCallback autoFocusCallback) {
		super(context);
		
		mContext = context;
		mCamera = camera;
		mAutoFocusCallback = autoFocusCallback;
		mHolder = getHolder();

		setFaceDetectionListener();
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
		int screenWidth = ScreenSize.getScreenWidth(context);
		int screenHeight = ScreenSize.getScreenHeight(context);
//		int screenHeight = screenWidth * 4 / 3;
		if (screenWidth < screenHeight) {
			int temp = screenWidth;
			screenWidth = screenHeight;
			screenHeight = temp;
		}

		// set size for picture
		Size largestSizeFullScreen = getMaxPictureSize(supportedPictureSizes,
				screenWidth,
				screenHeight);
		parameters.setPictureSize(largestSizeFullScreen.width, largestSizeFullScreen.height);
		Log.d(TAG, "largestSize.width: " + largestSizeFullScreen.width);
		Log.d(TAG, "largestSize.height: " + largestSizeFullScreen.height);
		Log.d(TAG, "=============================================");

		// set size for preview
		Size previewSize = getMaxPictureSize(mSupportedPreviewSizes,
				largestSizeFullScreen.width, largestSizeFullScreen.height);
		parameters.setPreviewSize(previewSize.width, previewSize.height);
		Log.d(TAG, "previewSize.width: " + previewSize.width);
		Log.d(TAG, "previewSize.height: " + previewSize.height);
		Log.d(TAG, "=============================================");

		mCamera.setParameters(parameters);

		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		setFocusable(true);
		setFocusableInTouchMode(true);
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the preview.
		setCameraDisplayOrientation((Activity) mContext, 0, mCamera);
		
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.autoFocus(mAutoFocusCallback);
			startFaceDetection(); // start face detection feature
			
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
			e.printStackTrace();
		}
		
		
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "surfaceChanged");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
		
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }
        
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }
        
        // TODO set preview size and make any resize, rotate or
        // reformatting changes here
		//====================== start ========================
//        Camera.Parameters parameters = mCamera.getParameters();
//        parameters.setFocusMode(FLASH_MODE);
//        
//        mCamera.setParameters(parameters);
        //====================== end ==========================
        
        
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            setFaceDetectionListener();
            onFocusAreasClick();
            startFaceDetection(); // re-start face detection feature
            
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            e.printStackTrace();
        }
        
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}
	
	public void setCamera(Camera camera) {
		mCamera = camera;
	}
	
	Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        
        return optimalSize;
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
	
	public static void setCameraDisplayOrientation(Activity activity,
	         int cameraId, Camera camera) {
		
		if (camera == null) {
			return;
		}

		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		camera.setDisplayOrientation(result);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setFaceDetectionListener() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return;
		}

		mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void startFaceDetection(){
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return;
		}
		
	    // Try starting Face Detection
	    Parameters params = mCamera.getParameters();

	    // start face detection only *after* preview has started
	    if (params.getMaxNumDetectedFaces() > 0){
	        // camera supports face detection, so can start it:
	        mCamera.startFaceDetection();
	    }
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public class MyFaceDetectionListener implements Camera.FaceDetectionListener {

		@Override
		public void onFaceDetection(Face[] faces, Camera camera) {
			// TODO Auto-generated method stub
			if (faces.length > 0) {
				Log.d("FaceDetection", "face detected: " + faces.length +
						" Face 1 Location X: " + faces[0].rect.centerX() +
						"Y: " + faces[0].rect.centerY());
				try {
					onFocusAreasClick(faces[0].rect);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void onFocusAreasClick() {
		int focusSize = getResources().getDimensionPixelSize(R.dimen.focus_size);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mCamera.getParameters().getFocusMode()
					.equals(Parameters.FOCUS_MODE_AUTO)) {
				mCamera.autoFocus(mAutoFocusCallback);
			}
			
			return;
		}
		
		// set Camera parameters
		Parameters params = mCamera.getParameters();
		mCamera.cancelAutoFocus();
		
		if (params.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
		    List<Area> meteringAreas = new ArrayList<Area>();
		    
		    Rect areaRect1 = new Rect(-focusSize/2, -focusSize/2, focusSize/2, focusSize/2);    // specify an area in center of image
		    meteringAreas.add(new Area(areaRect1, 1000)); // set weight to 100%
		    params.setMeteringAreas(meteringAreas);
		    params.setFocusAreas(meteringAreas);
		    mCamera.setParameters(params);
		    
		}
		
		if (params.getFocusMode()
				.equals(Parameters.FOCUS_MODE_AUTO)) {
			mCamera.autoFocus(mAutoFocusCallback);
			
		}
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void onFocusAreasClick(Rect areaRect1) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mCamera.getParameters().getFocusMode()
					.equals(Parameters.FOCUS_MODE_AUTO)) {
				mCamera.autoFocus(mAutoFocusCallback);
			}
			
			return;
		}
		
		// set Camera parameters
		Parameters params = mCamera.getParameters();
		mCamera.cancelAutoFocus();
		
		if (params.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
		    List<Area> meteringAreas = new ArrayList<Area>();
		    
		    meteringAreas.add(new Area(areaRect1, 1000)); // set weight to 100%
		    params.setMeteringAreas(meteringAreas);
		    params.setFocusAreas(meteringAreas);
		    mCamera.setParameters(params);
		    
		}
		
		if (params.getFocusMode()
				.equals(Parameters.FOCUS_MODE_AUTO)) {
			mCamera.autoFocus(mAutoFocusCallback);
			
		}
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if(event.getAction() == MotionEvent.ACTION_DOWN){
	        onFocusAreasClick();
	    }
	    
		return true;
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void focusOnTouch(MotionEvent event) {
		// TODO con bug, chua dung dc...
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			onFocusAreasClick();
			return;
		}
		
	    if (mCamera != null) {

	    	mCamera.cancelAutoFocus();
	        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
	        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);
	        Log.d(TAG, focusRect.toString());

	        List<Area> focusAreas = new ArrayList<Area>();
	        List<Area> meteringAreas = new ArrayList<Area>();
	        
	        focusAreas.add(new Area(focusRect, 1000));
	        meteringAreas.add(new Area(meteringRect, 1000));
	        
	        Parameters parameters = mCamera.getParameters();
	        parameters.setFocusAreas(focusAreas);
	        

	        if (parameters.getMaxNumMeteringAreas() > 0) {
	        	parameters.setMeteringAreas(meteringAreas);
	        }

	        mCamera.setParameters(parameters);
	        mCamera.autoFocus(mAutoFocusCallback);
	    }
	}
	
	/**
	 * Convert touch position x:y to {@link Area} position -1000:-1000 to 1000:1000.
	 */
	private Rect calculateTapArea(float x, float y, float coefficient) {
		int focusAreaSize = getResources().getDimensionPixelSize(R.dimen.focus_size);
	    int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

	    int left = clamp((int) x - areaSize / 2, 0, this.getWidth() - areaSize);
	    int top = clamp((int) y - areaSize / 2, 0, this.getHeight() - areaSize);

	    RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
	    
	    Matrix matrix = new Matrix();
	    matrix.mapRect(rectF);

	    return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
	}
	
	private int clamp(int x, int min, int max) {
	    if (x > max) {
	        return max;
	    }
	    if (x < min) {
	        return min;
	    }
	    return x;
	}



}

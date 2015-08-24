package asiantech.dev.customcamera.camera;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SavingPhotoTask extends AsyncTask<Void, Void, String> {


	public interface OnSavingPhotoTaskListener {
		void onSavedSuccessfully(String path);
	}


	public static final String TAG = SavingPhotoTask.class.getSimpleName();
	
	private Context mContext;
	private byte[] mData;
	private Camera mCamera;
	private View mBtnTakePhoto;
	private int mRotation;
	private ProgressDialog mProgressDialog;
	private OnSavingPhotoTaskListener mOnSavingPhotoTaskListener;
	
	public SavingPhotoTask(Context context, byte[] data, int rotation, Camera camera,
						   View buttonTakePhoto, OnSavingPhotoTaskListener l) {
		mContext = context;
		mData = data;
		mRotation = rotation;
		mCamera = camera;
		mBtnTakePhoto = buttonTakePhoto;
		mOnSavingPhotoTaskListener = l;
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage("Please wait...");
		mProgressDialog.setCancelable(false);
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mBtnTakePhoto.setEnabled(false);
		mProgressDialog.show();
	}

	@Override
	protected String doInBackground(Void... params) {
        File pictureFile = SavingCameraFiles.getOutputMediaFile(SavingCameraFiles.MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return null;
        }
        
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            
            if (mRotation == 0) {
            	fos.write(mData);
			} else {
				Log.d(TAG, "Data.length: " + mData.length);
				Bitmap bitmap = BitmapFactory.decodeByteArray(mData, 0, mData.length);
                Matrix matrix = new Matrix();
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
            
            MediaScannerConnection.scanFile(
					mContext,
					new String[]{pictureFile.toString()},
					new String[]{"image/jpeg"},
					null
			);
            
            fos.close();

//			MediaHelper.INSTANCE.attachLocationToMediaFile(pictureFile.getAbsolutePath(), LocationHelper.getInstance().getImageLocation());
//			MediaHelper.INSTANCE.checkLogImageLocation(pictureFile.getAbsolutePath());
//
//			TakeImageEvent takeImageEvent = new TakeImageEvent(pictureFile.getAbsoluteFile());
//			EventBus.getDefault().post(takeImageEvent);
//
           // return pictureFile.getAbsolutePath();
            
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
		
        
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		mBtnTakePhoto.setEnabled(true);
		mProgressDialog.dismiss();
		mOnSavingPhotoTaskListener.onSavedSuccessfully("file://" + result);
	}
	
}

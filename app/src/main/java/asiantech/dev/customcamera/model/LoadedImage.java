package asiantech.dev.customcamera.model;

import android.graphics.Bitmap;

/**
 * A LoadedImage contains the Bitmap loaded for the image.
 */
public class LoadedImage {
    Bitmap mBitmap;

    public LoadedImage(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }
}

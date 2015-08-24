package asiantech.dev.customcamera.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

import asiantech.dev.customcamera.model.LoadedImage;

/**
 * Created by CuongNV on 20/08/2015.
 */
public class ImageAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<LoadedImage> mPhotos = new ArrayList<>();

    public ImageAdapter(Context context, ArrayList<LoadedImage> photos) {
        this.mContext = context;
        this.mPhotos = photos;
    }

    public int getCount() {
        Log.d("xxxxxx", mPhotos.size() + "");
        return mPhotos.size();
    }

    public Object getItem(int position) {
        return mPhotos.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setPadding(8, 8, 8, 8);
        imageView.setImageBitmap(mPhotos.get(position).getBitmap());
        return imageView;
    }
}
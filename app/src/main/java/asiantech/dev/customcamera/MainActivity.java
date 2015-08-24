package asiantech.dev.customcamera;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import asiantech.dev.customcamera.adapter.CustomListAdapter;
import asiantech.dev.customcamera.adapter.ImageAdapter;
import asiantech.dev.customcamera.model.LoadedImage;
import asiantech.dev.customcamera.views.CenterLockHorizontalScrollview;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener {
    /**
     * Grid view holding the images.
     */
    private GridView sdcardImages;
    /**
     * Image adapter for the grid view.
     */
    private ImageAdapter imageAdapter;
    /**
     * Display used for getting the width of the screen.
     */
    private Display display;

    /**
     * Creates the content view, sets up the grid, the adapter, and the click listener.
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    private ArrayList<LoadedImage> mArrayList;

    private CenterLockHorizontalScrollview centerLockHorizontalScrollview;
    private CustomListAdapter customListAdapter;

    private ProgressDialog progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        progress = ProgressDialog.show(this, "Loading Image",
                "Please wait for loading", true);
        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mArrayList = new ArrayList<>();

        setProgressBarIndeterminateVisibility(true);
        loadImages();
        setupViews();
    }

    /**
     * Free up bitmap related resources.
     */
    protected void onDestroy() {
        super.onDestroy();
        final GridView grid = sdcardImages;
        final int count = grid.getChildCount();
        ImageView view;
        for (int i = 0; i < count; i++) {
            view = (ImageView) grid.getChildAt(i);
            ((BitmapDrawable) view.getDrawable()).setCallback(null);
        }
    }

    /**
     * Setup the grid view.
     */
    private void setupViews() {
        centerLockHorizontalScrollview = (CenterLockHorizontalScrollview) findViewById(R.id.scrollView);
        sdcardImages = (GridView) findViewById(R.id.sdcard);
//        sdcardImages.setNumColumns(display.getWidth());
        sdcardImages.setClipToPadding(false);
        sdcardImages.setOnItemClickListener(this);

        imageAdapter = new ImageAdapter(this, mArrayList);
        sdcardImages.setAdapter(imageAdapter);
        //custom list adapter
        customListAdapter = new CustomListAdapter(getApplicationContext(), mArrayList);
        centerLockHorizontalScrollview.setAdapter(customListAdapter);
    }

    /**
     * Load images.
     */
    private void loadImages() {
        final Object data = getLastNonConfigurationInstance();
        if (data == null) {
            new LoadImagesFromSDCard().execute();
        } else {
            final LoadedImage[] photos = (LoadedImage[]) data;
            if (photos.length == 0) {
                new LoadImagesFromSDCard().execute();
            }
            for (LoadedImage photo : photos) {
                addImage(photo);
            }
        }
    }

    /**
     * Add image(s) to the grid view adapter.
     *
     * @param value Array of LoadedImages references
     */
    private void addImage(LoadedImage... value) {
        for (LoadedImage image : value) {
            mArrayList.add(image);
            imageAdapter.notifyDataSetChanged();
        }
    }

    private void addLoader(LoadedImage... value) {
        for (LoadedImage image : value) {
            mArrayList.add(image);
            imageAdapter.notifyDataSetChanged();
            customListAdapter.notifyDataSetChanged();
            centerLockHorizontalScrollview.setAdapter(customListAdapter);
        }
    }

    /**
     * Save bitmap images into a list and return that list.
     *
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        final GridView grid = sdcardImages;
        final int count = grid.getChildCount();
        final LoadedImage[] list = new LoadedImage[count];
        for (int i = 0; i < count; i++) {
            final ImageView v = (ImageView) grid.getChildAt(i);
            list[i] = new LoadedImage(((BitmapDrawable) v.getDrawable()).getBitmap());
        }
        return list;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        progress.dismiss();
    }

    /**
     * Async task for loading the images from the SD card.
     *
     * @author Mihai Fonoage
     */
    class LoadImagesFromSDCard extends AsyncTask<Object, LoadedImage, Object> {

        /**
         * Load images from SD Card in the background, and display each image on the screen.
         */
        @Override
        protected Object doInBackground(Object... params) {
            //setProgressBarIndeterminateVisibility(true);
            Bitmap bitmap = null;
            Bitmap newBitmap = null;
            Uri uri = null;

            // Set up an array of the Thumbnail Image ID column we want
            String[] projection = {MediaStore.Images.Thumbnails._ID};
            // Create the cursor pointing to the SDCard
            Cursor cursor = managedQuery(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    projection, // Which columns to return
                    null,       // Return all rows
                    null,
                    null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID);
            int size = cursor.getCount();
            // If size is 0, there are no images on the SD Card.
            if (size == 0) {
                //No Images available, post some message to the user
            }
            int imageID = 0;
            for (int i = 0; i < size; i++) {
                cursor.moveToPosition(i);
                imageID = cursor.getInt(columnIndex);
                uri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID);
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    if (bitmap != null) {
                        newBitmap = Bitmap.createScaledBitmap(bitmap, 70, 70, true);
                        bitmap.recycle();
                        if (newBitmap != null) {
                            publishProgress(new LoadedImage(newBitmap));
                        }
                    }
                } catch (IOException e) {
                    //Error fetching image, try to recover
                }
            }
            cursor.close();
            return null;
        }

        /**
         * Add a new LoadedImage in the images grid.
         *
         * @param value The image.
         */
        @Override
        public void onProgressUpdate(LoadedImage... value) {
            addLoader(value);
        }

        /**
         * Set the visibility of the progress bar to false.
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result) {
            setProgressBarIndeterminateVisibility(false);
            progress.dismiss();
        }
    }

    /**
     * When an image is clicked, load that image as a puzzle.
     */
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        int columnIndex = 0;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor != null) {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToPosition(position);
            String imagePath = cursor.getString(columnIndex);

            FileInputStream is = null;
            BufferedInputStream bis = null;
            try {
                is = new FileInputStream(new File(imagePath));
                bis = new BufferedInputStream(is);
                Bitmap bitmap = BitmapFactory.decodeStream(bis);
                Bitmap useThisBitmap = Bitmap.createScaledBitmap(bitmap, parent.getWidth(), parent.getHeight(), true);
                bitmap.recycle();
                //Display bitmap (useThisBitmap)
            } catch (Exception e) {
                //Try to recover
            } finally {
                try {
                    if (bis != null) {
                        bis.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                    cursor.close();
                    projection = null;
                } catch (Exception e) {
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

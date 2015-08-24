package asiantech.dev.customcamera.Helpers;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Copyright © 2015 AsianTech inc.
 * Created by PhuQuy on 8/19/15.
 */
public class Helpers {


    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dimenson = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dimenson);
        return dimenson.heightPixels;
    }

    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dimenson = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dimenson);
        return dimenson.widthPixels;
    }

}

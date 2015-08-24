package asiantech.dev.customcamera.Helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import asiantech.dev.customcamera.MainActivity;


/**
 * Created by tientun on 3/5/15.
 */
public class BaseFragment extends Fragment {

    /**
     * Show dialog with OK button
     *
     * @param msg             message to display
     * @param onClickListener listener for OK button
     */
    protected void showDialog(String msg, DialogInterface.OnClickListener onClickListener) {

        if (null == getActivity()) {
            return;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setCancelable(false)
                .create();
        alertDialog.show();
    }

    /**
     * Show dialog with OK and cancel button
     *
     * @param msg
     * @param okClickListener
     * @param cancelClickListener
     */
    protected void showDialog(String msg,
                              DialogInterface.OnClickListener okClickListener,
                              DialogInterface.OnClickListener cancelClickListener) {

        if (null == getActivity()) {
            return;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, okClickListener)
                .setNegativeButton(android.R.string.cancel, cancelClickListener)
                .setCancelable(false)
                .create();
        alertDialog.show();
    }

    /**
     * Get MainActivity to work
     *
     * @return Activity
     */
    protected MainActivity getMainActivity() {
        Activity activity = getActivity();
        if (activity != null && MainActivity.class.isInstance(activity)) {
            return (MainActivity) activity;
        }
        return null;
    }
}

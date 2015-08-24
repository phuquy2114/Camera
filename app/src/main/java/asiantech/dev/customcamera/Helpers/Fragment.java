package asiantech.dev.customcamera.Helpers;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import asiantech.dev.customcamera.R;


/**
 * Copyright © 2015 AsianTech inc.
 * Created by PhuQuy on 8/6/15.
 */
public class Fragment extends android.support.v4.app.Fragment {
    private final SparseArray<Object> mRequestCodes = new SparseArray<Object>();
    private int mLastPopFragmentID = 0;

    List<WeakReference<android.support.v4.app.Fragment>> fragList = new ArrayList<WeakReference<android.support.v4.app.Fragment>>();

    /**
     * Called when fragment is appeard.
     */
    public void refresh() {
        // If need refresh, override.
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (getParentFragment() instanceof Fragment) {
            if (getId() > 0) {
                ((Fragment) getParentFragment()).registerRequestCode(requestCode, getId());
            } else if (getTag() != null) {
                ((Fragment) getParentFragment()).registerRequestCode(requestCode, getTag());
            }
            getParentFragment().startActivityForResult(intent, requestCode);
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!checkNestedFragmentsForResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean checkNestedFragmentsForResult(int requestCode, int resultCode, Intent data) {
        Object obj = mRequestCodes.get(requestCode);
        if (obj == null) {
            return false;
        }
        mRequestCodes.remove(requestCode);
        android.support.v4.app.Fragment fragment;
        if (obj instanceof Integer) {
            fragment = getChildFragmentManager().findFragmentById((Integer) obj);
        } else if (obj instanceof String) {
            fragment = getChildFragmentManager().findFragmentByTag((String) obj);
        } else {
            return false;
        }
        fragment.onActivityResult(requestCode, resultCode, data);
        return true;
    }

    private void registerRequestCode(int requestCode, int fragmentId) {
        mRequestCodes.put(requestCode, fragmentId);
    }

    private void registerRequestCode(int requestCode, String fragmentTag) {
        mRequestCodes.put(requestCode, fragmentTag);
    }

    /**
     * Add new fragment to current fragment
     *
     * @param fragment
     */
    public void addChildFragment(android.support.v4.app.Fragment fragment) {
        if (getParentFragment() != null && getParentFragment() instanceof Fragment) {
            ((Fragment) getParentFragment()).addChildFragment(fragment);
            return;
        }
        if (getView().getId() < 0) {
            Log.e(getClass().getSimpleName(), "Please set ID for fragment layout");
            return;
        }
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_right);
        fragmentTransaction.add(getView().getId(), fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        fragList.add(new WeakReference(fragment));
    }

    /**
     * Pop back to next fragment
     *
     * @return
     */
    public boolean popChildFragment() {
        if (getParentFragment() != null && getParentFragment() instanceof Fragment) {
            return ((Fragment) getParentFragment()).popChildFragment();
        }
        boolean isPop = false;
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            isPop = true;
            int lastPopFragmentIDTmp = getChildFragmentManager().getBackStackEntryAt(getChildFragmentManager().getBackStackEntryCount() - 1).hashCode();
            if (lastPopFragmentIDTmp != mLastPopFragmentID) {
                getChildFragmentManager().popBackStack();
                fragList.remove(fragList.size() - 1);
                mLastPopFragmentID = lastPopFragmentIDTmp;

                if (fragList.size() == 0) {
                    // refresh self
                    refresh();
                } else {
                    // refresh next top
                    Fragment frontFragment = (Fragment) fragList.get(fragList.size() - 1).get();
                    frontFragment.refresh();
                }
            }
        }
        return isPop;
    }

    /**
     * Get last child fragment
     *
     * @return fragment
     */
    public android.support.v4.app.Fragment getLastChildFragment() {
        if (fragList.size() > 0) {
            return fragList.get(fragList.size() - 1).get();
        }
        return this;
    }

    /**
     * Pop back to first fragment
     *
     * @return
     */
    public void popChildFragmentToTop() {
        if (getParentFragment() != null && getParentFragment() instanceof Fragment) {
            ((Fragment) getParentFragment()).popChildFragmentToTop();
            return;
        }
        if (getChildFragmentManager().getBackStackEntryCount() == 0) {
            // refresh self
            refresh();
            return;
        }
        FragmentManager.BackStackEntry backStackEntry = getChildFragmentManager().getBackStackEntryAt(0);
        getChildFragmentManager().popBackStack(backStackEntry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragList.clear();
        refresh();
    }

    /**
     * Get first fragment
     *
     * @return fragment
     */
    public Fragment getFirstFragment() {
        if (getParentFragment() != null && getParentFragment() instanceof Fragment) {
            return ((Fragment) getParentFragment()).getFirstFragment();
        }
        return this;
    }
}

package com.ldt.navigation;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.ldt.navigation.FragNavigationController;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by burt on 2016. 5. 24..
 */
@SuppressLint("ValidFragment")
public class FragNavigationController extends NavigationFragment {

    private static final String TAG = "FragNavigationController";
    private FragmentManager mFragManager = null;
    private Stack<NavigationFragment> mFragStack = new Stack<>();
    private @IdRes
    int containerViewId;
    private final Object sync = new Object();
    public String mTag;
    private Stack<String> mTagStack = new Stack<>();
    private static int sIdCount = 1;
    private static int nextId() {
        return ++sIdCount;
    }

    private static String nextNavigationControllerTag() {
        return "navigation.fragment:"+nextId()+"-controller";
    }

    private static String nextNavigationFragmentTag() {
        return "navigation.fragment:"+nextId();
    }

    private TimeInterpolator interpolator = new LinearInterpolator();
    public final NavigationFragment getFragmentAt(int i) {
        return mFragStack.get(i);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt("container-view-id", containerViewId);
    	outState.putString("controller-tag", mTag);
        ArrayList list = new ArrayList<>(mTagStack);
        outState.putStringArrayList("fragment-navigation-tags", list);
        
    }
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if(savedInstanceState!=null) {
    	containerViewId = savedInstanceState.getInt("container-view-id",-1);
    	mTag = savedInstanceState.getString("controller-tag");
    
      ArrayList list;
      list = savedInstanceState.getStringArrayList("fragment-navigation-tags");
      if(list!=null)
      mTagStack.clear();
      mTagStack.addAll(list);
      }
    }
    
    protected void restoreFragmentStack() {
      if(mFragManager==null) return;
      int size = mTagStack.size();
      String t;
      NavigationFragment f;
      mFragStack.clear();
      for(int i = 0; i < size; i++) {
        t = mTagStack.elementAt(i);
        f = (NavigationFragment)mFragManager.findFragmentByTag(t);
        if(f != null) {
        f.setNavigationController(this);
        mFragStack.push(f);
        }
      }
    }
    
    public static FragNavigationController getInstance(@NonNull FragmentManager fragmentManager, @IdRes int containerViewId, String tag) {
      FragNavigationController f = restoreInstance(fragmentManager, containerViewId, tag);
      if(f==null) f = newInstance(fragmentManager, containerViewId, tag);
      return f;
    }    
    
    protected static FragNavigationController restoreInstance(@NonNull FragmentManager fragmentManager, @IdRes int containerViewId, String tag) {
      
      // find restored controller if any
      FragNavigationController f = (FragNavigationController)fragmentManager.findFragmentByTag(tag);
      if(f!=null) {
        if(f.containerViewId==-1) f.containerViewId = containerViewId;
        if(f.mTag==null || f.mTag.isEmpty()) f.mTag = tag;
        f.mFragManager = fragmentManager;
       // restore fragment stack from restored tag stack
       
       f.restoreFragmentStack();
      }
      return f;
    }

    protected static FragNavigationController newInstance(@NonNull FragmentManager fragmentManager, @IdRes int containerViewId, String tag) {
        FragNavigationController f = new FragNavigationController();
        f.containerViewId = containerViewId;
        f.mFragManager = fragmentManager;
        f.mTag = tag;
       // this.setRetainInstance(true);

        synchronized (f.sync) {
            f.mFragManager
                    .beginTransaction()
                    .add(f, tag).commit();
        }
        return f;
    }
    
    public NavigationFragment getTopFragment() {
        if(mFragStack.size() != 0)return  mFragStack.lastElement();
        return null;
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    public int getFragmentCount() {
        return mFragStack.size();
    }
    public void setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
    }
    TimeInterpolator getInterpolator() {
        return interpolator;
    }

    public void pushFragment(NavigationFragment fragment) {
        setInterpolator(new AccelerateDecelerateInterpolator());
        presentFragment(fragment);
    }

    public void popFragment() {
        dismissFragment();
    }

    public void presentFragment(NavigationFragment fragment) {
        presentFragment(fragment, true);
    }

    public void presentFragment(NavigationFragment fragment, boolean withAnimation) {

        if(mFragManager == null) return;

        synchronized (sync) {
            String eTag = nextNavigationFragmentTag();
            if (mFragStack.size() == 0) {
                fragment.setNavigationController(this);
                fragment.setAnimatable(false);
                mFragManager
                        .beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(containerViewId, fragment, eTag)
                        .commit();

            } else {

                fragment.setNavigationController(this);

                int openExit = fragment.defaultOpenExitTransition();
                if(openExit==PresentStyle.SAME_AS_OPEN)
                getTopFragment().setOpenExitPresentStyle(fragment.getPresentStyle());
                else if(openExit!=PresentStyle.REMOVED_FRAGMENT_PRESENT_STYLE)
                    getTopFragment().setOpenExitPresentStyle(PresentStyle.get(openExit));

                fragment.setAnimatable(withAnimation);
                // hide last fragment and add new fragment
                NavigationFragment hideFragment = mFragStack.peek();
                hideFragment.onHideFragment();
                mFragManager
                        .beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .hide(hideFragment)
                        .add(containerViewId, fragment, eTag)
                        .commitNow();
            }
            mFragStack.add(fragment);
            mTagStack.add(eTag);
        }
    }
    private boolean mIsAbleToPopRoot = false;
    public void setAbleToPopRoot(boolean able) {
        mIsAbleToPopRoot = able;
    }

    public boolean dismissFragment() {
        return dismissFragment(true);
    }

    public boolean dismissFragment(boolean withAnimation) {
        if(mIsAbleToPopRoot) return innerDismissFragment(withAnimation);
        else return dismissNonRootFragment(withAnimation);
    }

    protected boolean innerDismissFragment(boolean withAnimation) {

        if(mFragManager == null) return false;

        // mFragStack only has root fragment
        if(mFragStack.size() == 1) {

            // remove root
            NavigationFragment fragmentToRemove= mFragStack.pop();
            fragmentToRemove.setNavigationController(this);
            fragmentToRemove.setAnimatable(withAnimation);
            mFragManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .show(this)
                    .remove(fragmentToRemove)
                    .commit();
            return true;
        }
        else if(mFragStack.size()==0) return false;

        synchronized (sync) {

            NavigationFragment fragmentToRemove = mFragStack.pop();
            String eTagRemove = mTagStack.pop();
            fragmentToRemove.setNavigationController(this);
            fragmentToRemove.setAnimatable(withAnimation);

            NavigationFragment fragmentToShow = mFragStack.peek();
            String eTagShow = mTagStack.peek();
            fragmentToShow.setNavigationController(this);
            fragmentToShow.setAnimatable(withAnimation);
            mFragManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .show(fragmentToShow)
                    .remove(fragmentToRemove)
                    .commit();

        }
        return true;
    }
    public boolean dismissNonRootFragment(boolean withAnimation) {

        if(mFragManager == null) return false;

        // mFragStack only has root fragment
        if(mFragStack.size() == 1) {

            // show the root fragment
            NavigationFragment fragmentToShow = mFragStack.peek();
            fragmentToShow.setNavigationController(this);
            fragmentToShow.setAnimatable(withAnimation);
            mFragManager
                    .beginTransaction()
                    .show(fragmentToShow)
                    .commit();
            return false;
        }

        synchronized (sync) {

            NavigationFragment fragmentToRemove = mFragStack.pop();
            fragmentToRemove.setNavigationController(this);
            fragmentToRemove.setAnimatable(withAnimation);

            NavigationFragment fragmentToShow = mFragStack.peek();
            fragmentToShow.setNavigationController(this);
            fragmentToShow.setAnimatable(withAnimation);
            mFragManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .show(fragmentToShow)
                    .remove(fragmentToRemove)
                    .commit();

        }
        return true;
    }

    public void dismissToRootFragment() {

        while (mFragStack.size() >= 2) {
            dismissFragment();
        }
    }
    public void dismissAllFragments() {
        if(!mIsAbleToPopRoot) {
            dismissToRootFragment();
        } else {
            while (mFragStack.size()>=1)
                dismissFragment();
        }
    }

}

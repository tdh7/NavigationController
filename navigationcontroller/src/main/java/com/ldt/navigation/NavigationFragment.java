package com.ldt.navigation;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.lang.ref.WeakReference;

/**
 * Created by burt on 2016. 5. 24..
 * Updated by dtrung98
 */
public abstract class NavigationFragment extends Fragment {
    private static final String TAG ="NavigationFragment";
    private static final int DEFAULT_DURATION = 275;
    public static int PRESENT_STYLE_DEFAULT = PresentStyle.ACCORDION_LEFT;

    private WeakReference<FragNavigationController> weakFragmentNaviagationController = null;
    protected boolean animatable = true;
    private FragFrameLayout innerRootLayout = null;
    private PresentStyle presentStyle = null;
    private PresentStyle exitPresentStyle = null;
    protected View mContentView = null;

    /**
     *  Cài đặt hiệu ứng cho Fragment cũ
     * @return  0 : không dùng hiệu ứng
     * <br>  -1 : dùng cùng loại hiệu ứng với Fragment mới
     * <br> -2 : dùng hiệu ứng biến mất của Fragment cũ
     */
    public int defaultOpenExitTransition() {
        return PresentStyle.SAME_AS_OPEN;
    }

    public boolean isWhiteTheme(boolean current) {
        saveTheme(current);
        return true;
    }

    public final PresentStyle getPresentStyle() {
        if(presentStyle==null) presentStyle = PresentStyle.get(defaultTransition());
        return presentStyle;
    }

    public boolean getSavedTheme() {
        return savedTheme;
    }

    protected boolean savedTheme = true;
    public void saveTheme(boolean b) {
        Log.d(TAG, "saveTheme: b = " +b);
        savedTheme = b;
    }

    public void dismiss() {
        FragNavigationController controller = getNavigationController();
        if(controller!=null)
        controller.dismissFragment();
    }

    public void presentFragment(NavigationFragment fragment) {
        FragNavigationController controller = getNavigationController();
        if(controller!=null)
            controller.presentFragment(fragment);
    }
    ;
    public boolean isWhiteTheme() {
        return true;
    }
    public void onSetStatusBarMargin(int value) {

    }

    public FragNavigationController getNavigationController() {
        if(weakFragmentNaviagationController == null)
            return null;
        return weakFragmentNaviagationController.get();
    }

    protected void setNavigationController(FragNavigationController fragNavigationController) {
        weakFragmentNaviagationController = new WeakReference<>(fragNavigationController);
    }

    protected void setAnimatable(boolean animatable) {
        this.animatable = animatable;
    }

 /*  protected void setPresentStyle(PresentStyle presentStyle) {
        this.presentStyle = presentStyle;
    }*/

    public int defaultTransition() {
        return PRESENT_STYLE_DEFAULT;
    }

    public void setOpenExitPresentStyle(PresentStyle exitPresentStyle) {
        this.exitPresentStyle = exitPresentStyle;
    }

    public int defaultDuration() {
        return DEFAULT_DURATION;
    }

    /**
     *  This method is called when user press the back button
     * @return true to allow the fragment to be dismissed
     * <br>      false to ignore the dismissed.
     * <br> To dismiss the fragment, you need to call method <i>getNavigationController().dismiss()</i> directly
     */
    public boolean onBackPressed(){
        return true;
    }

    @Nullable
    @Override
    final public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = onCreateView(inflater, container);
        if(v == null) return null;

        if(innerRootLayout==null)
        innerRootLayout = new FragFrameLayout(getActivity());
        else
        innerRootLayout.removeAllViews();

        mContentView = v;
        innerRootLayout.addView(v);
        return innerRootLayout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        presentStyle = PresentStyle.get(defaultTransition());
        onSetStatusBarMargin(getStatusHeight(getResources()));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        StatusHeight = -1;
    }

    public static int StatusHeight = -1;
    public static int getStatusHeight(Resources myR)
    {
        if(StatusHeight!=-1) return StatusHeight;
        int height;
        int idSbHeight = myR.getIdentifier("status_bar_height", "dimen", "android");
        if (idSbHeight > 0) {
            height = myR.getDimensionPixelOffset(idSbHeight);
            //   Toast.makeText(this, "Status Bar Height = "+ height, Toast.LENGTH_SHORT).show();
        } else {
            height = 0;
            //        Toast.makeText(this,"Resources NOT found",Toast.LENGTH_LONG).show();
        }
        StatusHeight =height;
        return StatusHeight;
    }

    @Nullable
    abstract protected View onCreateView(LayoutInflater inflater, ViewGroup container);

    /**
     * This is the layout for wrapping contentView
     * @return AndroidFragmentFrameLayout
     */
    public FragFrameLayout getRootLayout() {
        return innerRootLayout;
    }

    /**
     * This is the layout-view which is defined by user.
     * @return content view
     */
    public View getContentView() {
        return mContentView;
    }

    @Override
    public Animator onCreateAnimator(final int transit, final boolean enter, int nextAnim) {
        if(!animatable) {
            animatable = true;
            return null;
        }

        FragNavigationController nav =  getNavigationController();
        if(nav == null) {
            return null; //no animatable
        }

        if(presentStyle == null) {
            return null; //no animatable
        }

        Animator animator = null;
        if(transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {

            if (enter) {
                int id = presentStyle.getOpenEnterAnimatorId();
                if(id != -1) animator = AnimatorInflater.loadAnimator(getActivity(), id);
            } else {
                int id;
                if(exitPresentStyle==null)
                    id = presentStyle.getOpenExitAnimatorId();
                else {
                    id = exitPresentStyle.getOpenExitAnimatorId();
                }
                if(id != -1) animator = AnimatorInflater.loadAnimator(getActivity(), id);
            }

        } else {

            if (enter) {
                int id;
                if(exitPresentStyle == null)
                id = presentStyle.getCloseEnterAnimatorId();
                else {
                    id = exitPresentStyle.getCloseEnterAnimatorId();
                    exitPresentStyle = null;
                }

                if(id != -1) animator = AnimatorInflater.loadAnimator(getActivity(), id);
            } else {
                int id;
                id = presentStyle.getCloseExitAnimatorId();
                if(id != -1) animator = AnimatorInflater.loadAnimator(getActivity(), id);
            }
        }
        if(animator != null) {
            animator.setInterpolator(nav.getInterpolator());
            animator.setDuration(defaultDuration());
        }

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

                if(transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {

                    if(enter) {
                        onShowFragment();
                    } else {
                        //onHideFragment();
                    }
                } else {

                    if(enter) {
                        onShowFragment();
                    } else {
                        onHideFragment();
                    }
                }


            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        return animator;
    }


    public void onShowFragment() {}
    public void onHideFragment() {}

}

package com.ldt.navigation.uicontainer;

import android.animation.Animator;
import android.os.Bundle;
import android.view.View;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.collection.SimpleArrayMap;

import com.ldt.navigation.NavigationController;
import com.ldt.navigation.PresentStyle;

import static com.ldt.navigation.NavigationFragment.DEFAULT_DURATION;

public interface UIContainer {
 SimpleArrayMap<String, Class<?>> sClassMap =
         new SimpleArrayMap<>();

 static UIContainer instantiate(Context context, String name) {
  try {
   Class<?> clazz = sClassMap.get(name);
/*   if (clazz == null) {
    // Class not found in the cache, see if it's real, and try to add it
    clazz = context.getClassLoader().loadClass(name);
    sClassMap.put(name, clazz);
   }*/
   if(clazz!=null)
    return  (UIContainer) clazz.newInstance();

  } catch (Exception ignored) {
  }
  return null;
 }

 static void save(String name, Class<?> clazz) {
  if(sClassMap.get(name)==null) sClassMap.put(name, clazz);
 }

 /**
  * Called in onCreate in NavigationController
  * @param wQualifier
  * @param hQualifier
  * @param dpUnit
  */
 default void provideQualifier(NavigationController controller, int wQualifier, int hQualifier, float dpUnit) {}

 /**
  * Equal to onCreateView
  * @param context
  * @param inflater
  * @param viewGroup
  * @param subContainerId
  * @return
  */
 View provideLayout(Context context, LayoutInflater inflater, ViewGroup viewGroup, int subContainerId);

 /**
  *  Called by Navigation Controller
  */
 default View onCreateLayout(Context context, LayoutInflater inflater, ViewGroup viewGroup, int subContainerId) {
  return provideLayout(context, inflater, viewGroup, subContainerId);
 }
 /**
  * Equal to onViewCreated
  * @param view
  */
 default void bindLayout(View view) {}

 default NavigationController getController() {return null;}

 /*
 Call in onCreate, after provideConfig
  */
 default void created(NavigationController controller, Bundle bundle) {}

 /**
  * Call in onDestroyView
  */
 default void destroy(NavigationController controller) {}
 default void saveState(NavigationController controller, Bundle bundle) {}
 default void restoreState(NavigationController controller, Bundle bundle) {}
 default void start(NavigationController controller) {};
 default void stop(NavigationController controller) {};
 default void resume(NavigationController controller) {};
 default void pause(NavigationController controller) {};
 default void destroyView(NavigationController controller) {}
 default void stackChanged(NavigationController controller) {}
 default void activityCreated(NavigationController controller, Bundle savedInstanceState) {}
 default LayoutInflater provideLayoutInflater(Bundle savedInstanceState) { return null;}
 default void executeAnimator(Animator animator, int transit, boolean enter, int nextAnim) {}

 default boolean shouldAttachToContainerView() {
  return true;
 }

 default int defaultDuration() {
  return DEFAULT_DURATION;
 }

 default int defaultTransition() {
  return PresentStyle.FADE;
 }

 default int defaultOpenExitTransition() {
  return PresentStyle.SAME_AS_OPEN;
 }

 default int[] onWindowInsetsChanged(NavigationController controller, int left, int top, int right, int bottom) {
  return null;
 }
}
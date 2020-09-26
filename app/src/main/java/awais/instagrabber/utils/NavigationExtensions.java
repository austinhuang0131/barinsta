package awais.instagrabber.utils;

import android.content.Intent;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import awais.instagrabber.R;

public class NavigationExtensions {
    // private static final String TAG = "NavigationExtensions";

    @NonNull
    public static LiveData<NavController> setupWithNavController(@NonNull final BottomNavigationView bottomNavigationView,
                                                                 @NonNull List<Integer> navGraphIds,
                                                                 @NonNull final FragmentManager fragmentManager,
                                                                 final int containerId,
                                                                 @NonNull Intent intent,
                                                                 final int firstFragmentGraphIndex) {
        final SparseArray<String> graphIdToTagMap = new SparseArray<>();
        final MutableLiveData<NavController> selectedNavController = new MutableLiveData<>();
        int firstFragmentGraphId = 0;
        for (int i = 0; i < navGraphIds.size(); i++) {
            final int navGraphId = navGraphIds.get(i);
            final String fragmentTag = getFragmentTag(navGraphId);
            final NavHostFragment navHostFragment = obtainNavHostFragment(fragmentManager, fragmentTag, navGraphId, containerId);
            final NavController navController = navHostFragment.getNavController();
            final int graphId = navController.getGraph().getId();
            if (i == firstFragmentGraphIndex) {
                firstFragmentGraphId = graphId;
            }
            graphIdToTagMap.put(graphId, fragmentTag);
            if (bottomNavigationView.getSelectedItemId() == graphId) {
                selectedNavController.setValue(navHostFragment.getNavController());
                attachNavHostFragment(fragmentManager, navHostFragment, i == firstFragmentGraphIndex);
            } else {
                detachNavHostFragment(fragmentManager, navHostFragment);
            }
        }
        final String[] selectedItemTag = {graphIdToTagMap.get(bottomNavigationView.getSelectedItemId())};
        final String firstFragmentTag = graphIdToTagMap.get(firstFragmentGraphId);
        final boolean[] isOnFirstFragment = {selectedItemTag[0] != null && selectedItemTag[0].equals(firstFragmentTag)};
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (fragmentManager.isStateSaved()) {
                return false;
            }
            String newlySelectedItemTag = graphIdToTagMap.get(item.getItemId());
            String tag = selectedItemTag[0];
            if (tag != null && !tag.equals(newlySelectedItemTag)) {
                fragmentManager.popBackStack(firstFragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                Fragment fragment = fragmentManager.findFragmentByTag(newlySelectedItemTag);
                if (fragment == null) {
                    return false;
                    // throw new RuntimeException("null cannot be cast to non-null NavHostFragment");
                }
                final NavHostFragment selectedFragment = (NavHostFragment) fragment;
                if (!firstFragmentTag.equals(newlySelectedItemTag)) {
                    FragmentTransaction fragmentTransaction = fragmentManager
                            .beginTransaction()
                            .setCustomAnimations(
                                    R.anim.nav_default_enter_anim,
                                    R.anim.nav_default_exit_anim,
                                    R.anim.nav_default_pop_enter_anim,
                                    R.anim.nav_default_pop_exit_anim
                            )
                            .attach(selectedFragment)
                            .setPrimaryNavigationFragment(selectedFragment);
                    for (int i = 0; i < graphIdToTagMap.size(); i++) {
                        final int key = graphIdToTagMap.keyAt(i);
                        final String fragmentTagForId = graphIdToTagMap.get(key);
                        if (!fragmentTagForId.equals(newlySelectedItemTag)) {
                            final Fragment fragmentByTag = fragmentManager.findFragmentByTag(firstFragmentTag);
                            if (fragmentByTag == null) {
                                continue;
                            }
                            fragmentTransaction.detach(fragmentByTag);
                        }
                    }
                    fragmentTransaction.addToBackStack(firstFragmentTag)
                                       .setReorderingAllowed(true)
                                       .commit();
                }
                selectedItemTag[0] = newlySelectedItemTag;
                isOnFirstFragment[0] = selectedItemTag[0].equals(firstFragmentTag);
                selectedNavController.setValue(selectedFragment.getNavController());
                return true;
            }
            return false;
        });
        // setupItemReselected(bottomNavigationView, graphIdToTagMap, fragmentManager);
        setupDeepLinks(bottomNavigationView, navGraphIds, fragmentManager, containerId, intent);
        final int finalFirstFragmentGraphId = firstFragmentGraphId;
        fragmentManager.addOnBackStackChangedListener(() -> {
            if (!isOnFirstFragment[0]) {
                if (firstFragmentTag == null) {
                    return;
                }
                if (!isOnBackStack(fragmentManager, firstFragmentTag)) {
                    bottomNavigationView.setSelectedItemId(finalFirstFragmentGraphId);
                }
            }

            final NavController navController = selectedNavController.getValue();
            if (navController != null && navController.getCurrentDestination() == null) {
                final NavGraph navControllerGraph = navController.getGraph();
                navController.navigate(navControllerGraph.getId());
            }
        });
        return selectedNavController;
    }

    private static NavHostFragment obtainNavHostFragment(final FragmentManager fragmentManager,
                                                         final String fragmentTag,
                                                         final int navGraphId,
                                                         final int containerId) {
        final NavHostFragment existingFragment = (NavHostFragment) fragmentManager.findFragmentByTag(fragmentTag);
        if (existingFragment != null) {
            return existingFragment;
        }
        final NavHostFragment navHostFragment = NavHostFragment.create(navGraphId);
        fragmentManager.beginTransaction()
                       .add(containerId, navHostFragment, fragmentTag)
                       .commitNow();
        return navHostFragment;
    }

    private static void attachNavHostFragment(final FragmentManager fragmentManager,
                                              final NavHostFragment navHostFragment,
                                              final boolean isPrimaryNavFragment) {
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                                                                       .attach(navHostFragment);
        if (isPrimaryNavFragment) {
            fragmentTransaction.setPrimaryNavigationFragment(navHostFragment);
        }
        fragmentTransaction.commitNow();
    }

    private static void detachNavHostFragment(final FragmentManager fragmentManager, final NavHostFragment navHostFragment) {
        fragmentManager.beginTransaction()
                       .detach(navHostFragment)
                       .commitNow();
    }

    private static void setupItemReselected(final BottomNavigationView bottomNavigationView,
                                            final SparseArray<String> graphIdToTagMap,
                                            final FragmentManager fragmentManager) {
        bottomNavigationView.setOnNavigationItemReselectedListener(item -> {
            final String newlySelectedItemTag = graphIdToTagMap.get(item.getItemId());
            final Fragment fragmentByTag = fragmentManager.findFragmentByTag(newlySelectedItemTag);
            if (fragmentByTag == null) {
                return;
                // throw new NullPointerException("null cannot be cast to non-null type NavHostFragment");
            }
            final NavHostFragment selectedFragment = (NavHostFragment) fragmentByTag;
            final NavController navController = selectedFragment.getNavController();
            final NavGraph navControllerGraph = navController.getGraph();
            navController.popBackStack(navControllerGraph.getStartDestination(), false);
        });
    }

    private static void setupDeepLinks(final BottomNavigationView bottomNavigationView,
                                       final List<Integer> navGraphIds,
                                       final FragmentManager fragmentManager,
                                       final int containerId,
                                       final Intent intent) {
        for (int i = 0; i < navGraphIds.size(); i++) {
            final int navGraphId = navGraphIds.get(i);
            final String fragmentTag = getFragmentTag(navGraphId);
            final NavHostFragment navHostFragment = obtainNavHostFragment(fragmentManager, fragmentTag, navGraphId, containerId);
            if (navHostFragment.getNavController().handleDeepLink(intent)) {
                final int selectedItemId = bottomNavigationView.getSelectedItemId();
                NavController navController = navHostFragment.getNavController();
                NavGraph graph = navController.getGraph();
                if (selectedItemId != graph.getId()) {
                    navController = navHostFragment.getNavController();
                    graph = navController.getGraph();
                    bottomNavigationView.setSelectedItemId(graph.getId());
                }
            }
        }
    }

    private static boolean isOnBackStack(final FragmentManager fragmentManager, final String backStackName) {
        int backStackCount = fragmentManager.getBackStackEntryCount();
        for (int i = 0; i < backStackCount; i++) {
            final FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(i);
            final String name = backStackEntry.getName();
            if (name != null && name.equals(backStackName)) {
                return true;
            }
        }
        return false;
    }

    private static String getFragmentTag(final int index) {
        return "bottomNavigation#" + index;
    }
}

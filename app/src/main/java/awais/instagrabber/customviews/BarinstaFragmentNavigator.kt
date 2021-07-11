package awais.instagrabber.customviews

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.navOptions
import awais.instagrabber.R
import awais.instagrabber.fragments.settings.PreferenceKeys
import awais.instagrabber.utils.Utils

private val defaultNavOptions = navOptions {
    anim {
        enter = R.anim.slide_in_right
        exit = R.anim.slide_out_left
        popEnter = android.R.anim.slide_in_left
        popExit = android.R.anim.slide_out_right
    }
}

private val emptyNavOptions = navOptions {}

/**
 * Needs to replace FragmentNavigator and replacing is done with name in annotation.
 * Navigation method will use defaults for fragments transitions animations.
 */
@Navigator.Name("fragment")
class BarinstaFragmentNavigator(
    context: Context,
    fragmentManager: FragmentManager,
    containerId: Int
) : FragmentNavigator(context, fragmentManager, containerId) {

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        val disableTransitions = Utils.settingsHelper.getBoolean(PreferenceKeys.PREF_DISABLE_SCREEN_TRANSITIONS)
        if (disableTransitions) {
            super.navigate(entries, navOptions, navigatorExtras)
            return
        }
        // this will try to fill in empty animations with defaults when no shared element transitions
        // https://developer.android.com/guide/navigation/navigation-animate-transitions#shared-element
        val hasSharedElements = navigatorExtras != null && navigatorExtras is Extras
        val navOptions1 = if (hasSharedElements) navOptions else navOptions.fillEmptyAnimationsWithDefaults()
        super.navigate(entries, navOptions1, navigatorExtras)
    }

    private fun NavOptions?.fillEmptyAnimationsWithDefaults(): NavOptions =
        this?.copyNavOptionsWithDefaultAnimations() ?: defaultNavOptions

    private fun NavOptions.copyNavOptionsWithDefaultAnimations(): NavOptions = let { originalNavOptions ->
        navOptions {
            launchSingleTop = originalNavOptions.shouldLaunchSingleTop()
            popUpTo(originalNavOptions.popUpToId) {
                inclusive = originalNavOptions.isPopUpToInclusive()
                saveState = originalNavOptions.shouldPopUpToSaveState()
            }
            originalNavOptions.popUpToRoute?.let {
                popUpTo(it) {
                    inclusive = originalNavOptions.isPopUpToInclusive()
                    saveState = originalNavOptions.shouldPopUpToSaveState()
                }
            }
            restoreState = originalNavOptions.shouldRestoreState()
            anim {
                enter =
                    if (originalNavOptions.enterAnim == emptyNavOptions.enterAnim) defaultNavOptions.enterAnim
                    else originalNavOptions.enterAnim
                exit =
                    if (originalNavOptions.exitAnim == emptyNavOptions.exitAnim) defaultNavOptions.exitAnim
                    else originalNavOptions.exitAnim
                popEnter =
                    if (originalNavOptions.popEnterAnim == emptyNavOptions.popEnterAnim) defaultNavOptions.popEnterAnim
                    else originalNavOptions.popEnterAnim
                popExit =
                    if (originalNavOptions.popExitAnim == emptyNavOptions.popExitAnim) defaultNavOptions.popExitAnim
                    else originalNavOptions.popExitAnim
            }
        }
    }

    private companion object {
        private const val TAG = "FragmentNavigator"
    }
}
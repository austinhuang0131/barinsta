package awais.instagrabber.customviews

import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment

class BarinstaNavHostFragment : NavHostFragment() {
    override fun onCreateNavHostController(navHostController: NavHostController) {
        super.onCreateNavHostController(navHostController)
        navHostController.navigatorProvider.addNavigator(
            // this replaces FragmentNavigator
            BarinstaFragmentNavigator(requireContext(), childFragmentManager, id)
        )
    }
}
package cash.z.ecc.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentAutoShieldInformationBinding
import cash.z.ecc.android.ext.requireApplicationContext
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.preference.Preferences
import cash.z.ecc.android.preference.model.put
import cash.z.ecc.android.ui.base.BaseFragment

/*
 * If the user presses the Android back button, the backstack will be popped and the user returns
 * to the app home screen.  The preference will not be set in that case, because it could be considered
 * that the user did not acknowledge this prompt.
 */
class AutoshieldingInformationFragment : BaseFragment<FragmentAutoShieldInformationBinding>() {
    override val screen = Report.Screen.AUTO_SHIELD_INFORMATION

    override fun inflate(inflater: LayoutInflater): FragmentAutoShieldInformationBinding =
        FragmentAutoShieldInformationBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonAutoshieldDismiss.setOnClickListener {
            Preferences.isAcknowledgedAutoshieldingInformationPrompt.put(
                requireApplicationContext(),
                true
            )
            findNavController().navigate(R.id.action_nav_autoshielding_info_to_home)
        }
        binding.buttonAutoshieldMoreInfo.setOnClickListener {
            Preferences.isAcknowledgedAutoshieldingInformationPrompt.put(
                requireApplicationContext(),
                true
            )
            try {
                findNavController().navigate(R.id.action_nav_autoshielding_info_to_browser)
            } catch (e: Exception) {
                // ActivityNotFoundException could happen on certain devices, like Android TV, Android Things, etc.

                // SecurityException shouldn't occur, but just in case we catch all exceptions to
                // prevent another package on the device from crashing us if that package tries to be malicious
                // by adding permissions or changing export status dynamically.

                // In the future, it might also be desirable to display a Toast or Snackbar indicating
                // that the browser couldn't be launched

                findNavController().navigate(R.id.action_nav_autoshielding_info_to_home)
            }
        }
    }
}

package cash.z.ecc.android.ui.history

import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeTransform
import androidx.transition.Transition
import androidx.transition.TransitionSet
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentTransactionBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ext.gone
import cash.z.ecc.android.ext.invisible
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.ext.toAppColor
import cash.z.ecc.android.ext.toColoredSpan
import cash.z.ecc.android.ext.visible
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.history.HistoryViewModel.UiModel
import cash.z.ecc.android.util.twig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionFragment : BaseFragment<FragmentTransactionBinding>() {
    override val screen = Report.Screen.TRANSACTION
    private val viewModel: HistoryViewModel by activityViewModel()

    var isMemoExpanded: Boolean = false

    override fun inflate(inflater: LayoutInflater): FragmentTransactionBinding =
        FragmentTransactionBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val transition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
//        sharedElementEnterTransition = transition
//        sharedElementReturnTransition = transition

//        sharedElementEnterTransition = createSharedElementTransition()
//        sharedElementReturnTransition = createSharedElementTransition()

//        sharedElementEnterTransition = ChangeBounds().apply { duration = 1500 }
//        sharedElementReturnTransition = ChangeBounds().apply { duration = 1500 }
//        enterTransition = Fade().apply {
//            duration = 1800
// //            slideEdge = Gravity.END
//        }
    }

    private fun createSharedElementTransition(duration: Long = 800L): Transition {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            this.duration = duration
//            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.2f, 1f)
            addTransition(ChangeBounds())
            addTransition(ChangeClipBounds())
            addTransition(ChangeTransform())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            ViewCompat.setTransitionName(topBoxValue, "test_amount_anim_${viewModel.selectedTransaction.value?.id}")
            ViewCompat.setTransitionName(topBoxBackground, "test_bg_anim_${viewModel.selectedTransaction.value?.id}")
            backButtonHitArea.onClickNavBack { tapped(Report.Tap.TRANSACTION_BACK) }

            lifecycleScope.launch {
                viewModel.uiModels.stateIn(lifecycleScope).collect { uiModel ->
                    topBoxLabel.text = uiModel.topLabel
                    topBoxValue.text = uiModel.topValue
                    bottomBoxLabel.text = uiModel.bottomLabel
                    bottomBoxValue.text = uiModel.bottomValue
                    textBlockHeight.text = uiModel.minedHeight
                    textTimestamp.text = uiModel.timestamp
                    if (uiModel.iconRotation < 0) {
                        topBoxIcon.gone()
                    } else {
                        topBoxIcon.rotation = uiModel.iconRotation
                        topBoxIcon.visible()
                    }

                    if (!uiModel.isMined) {
                        textBlockHeight.invisible()
                        textBlockHeightPrefix.invisible()
                    }

                    val exploreOnClick = View.OnClickListener {
                        uiModel.txId?.let { txId ->
                            mainActivity?.showFirstUseWarning(
                                Const.Pref.FIRST_USE_VIEW_TX,
                                titleResId = R.string.dialog_first_use_view_tx_title,
                                msgResId = R.string.dialog_first_use_view_tx_message,
                                positiveResId = R.string.dialog_first_use_view_tx_positive,
                                negativeResId = R.string.dialog_first_use_view_tx_negative
                            ) {
                                onLaunchUrl(txId.toTransactionUrl())
                            }
                        }
                    }
                    buttonExplore.setOnClickListener(exploreOnClick)
                    textBlockHeight.setOnClickListener(exploreOnClick)

                    uiModel.fee?.let { subwaySpotFee.visible(); subwayLabelFee.visible(); subwayLabelFee.text = it }
                    uiModel.source?.let { subwaySpotSource.visible(); subwayLabelSource.visible(); subwayLabelSource.text = it }
                    uiModel.toAddressLabel()?.let { subwaySpotAddress.visible(); subwayLabelAddress.visible(); subwayLabelAddress.text = it }
                    uiModel.toAddressClickListener()?.let { subwayLabelAddress.setOnClickListener(it) }

                    // TODO: remove logic from sections below and add more fields or extension functions to UiModel
                    uiModel.confirmation?.let {
                        subwaySpotConfirmations.visible(); subwayLabelConfirmations.visible()
                        subwayLabelConfirmations.text = it
                        if (it.equals(getString(R.string.transaction_status_confirmed), true)) {
                            subwayLabelConfirmations.setTextColor(R.color.tx_primary.toAppColor())
                        } else {
                            subwayLabelConfirmations.setTextColor(R.color.tx_text_light_dimmed.toAppColor())
                        }
                    }

                    uiModel.memo?.let {
                        hitAreaMemoSubway.setOnClickListener { _ -> onToggleMemo(!isMemoExpanded, it) }
                        hitAreaMemoIcon.setOnClickListener { _ -> onToggleMemo(!isMemoExpanded, it) }
                        subwayLabelMemo.setOnClickListener { _ -> onToggleMemo(!isMemoExpanded, it) }
                        subwayLabelMemo.setOnLongClickListener { _ ->
                            mainActivity?.copyText(it, "Memo")
                            true
                        }
                        subwayLabelMemo.movementMethod = ScrollingMovementMethod()
                        subwaySpotMemoContent.visible()
                        subwayLabelMemo.visible()
                        hitAreaMemoSubway.visible()
                        onToggleMemo(false)
                    }
                }
            }
        }
    }

    val invertingMatrix = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    private fun onToggleMemo(isExpanded: Boolean, memo: String = "") {
        twig("onToggleMemo($isExpanded, $memo)")
        if (isExpanded) {
            twig("setting memo text to: $memo")
            binding.subwayLabelMemo.setText(memo)
            binding.subwayLabelMemo.invalidate()
            // don't impede the ability to scroll
            binding.groupMemoIcon.gone()
            binding.subwayLabelMemo.backgroundTintList = ColorStateList.valueOf(R.color.tx_text_light_dimmed.toAppColor())
            binding.subwaySpotMemoContent.colorFilter = invertingMatrix
            binding.subwaySpotMemoContent.rotation = 90.0f
        } else {
            binding.subwayLabelMemo.setText(getString(R.string.transaction_with_memo))
            binding.subwayLabelMemo.scrollTo(0, 0)
            binding.subwayLabelMemo.invalidate()
            twig("setting memo text to: with a memo")
            binding.groupMemoIcon.visible()
            binding.subwayLabelMemo.backgroundTintList = ColorStateList.valueOf(R.color.tx_primary.toAppColor())
            binding.subwaySpotMemoContent.colorFilter = null
            binding.subwaySpotMemoContent.rotation = 0.0f
        }
        isMemoExpanded = isExpanded
    }

    private fun String.toTransactionUrl(): String {
        return getString(R.string.api_block_explorer, this)
    }

    private fun UiModel?.toAddressClickListener(): View.OnClickListener? {
        return this?.address?.let { addr ->
            View.OnClickListener { mainActivity?.copyText(addr, "Address") }
        }
    }

    private fun UiModel?.toAddressLabel(): CharSequence? {
        if (this == null || this.address == null || this.isInbound == null) return null
        val prefix = getString(
            if (isInbound == true) {
                R.string.transaction_prefix_from
            } else {
                R.string.transaction_prefix_to
            }
        )
        return "$prefix ${address?.toAbbreviatedAddress() ?: "Unknown" }".let {
            it.toColoredSpan(R.color.tx_text_light_dimmed, if (address == null) it else prefix)
        }
    }
}

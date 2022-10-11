package cash.z.ecc.android.ui.send

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentSendFinalBinding
import cash.z.ecc.android.ext.WalletZecFormmatter
import cash.z.ecc.android.ext.goneIf
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.SEND_FINAL_CLOSE
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.model.PendingTransaction
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.isCreating
import cash.z.ecc.android.sdk.model.isFailedEncoding
import cash.z.ecc.android.sdk.model.isFailure
import cash.z.ecc.android.sdk.model.isSubmitSuccess
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.util.twig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SendFinalFragment : BaseFragment<FragmentSendFinalBinding>() {
    override val screen = Report.Screen.SEND_FINAL

    private val sendViewModel: SendViewModel by activityViewModels()

    override fun inflate(inflater: LayoutInflater): FragmentSendFinalBinding =
        FragmentSendFinalBinding.inflate(inflater)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonPrimary.setOnClickListener {
            onReturnToSend()
        }
        binding.backButtonHitArea.setOnClickListener {
            onExit().also { tapped(SEND_FINAL_CLOSE) }
        }
        binding.textConfirmation.text =
            "${getString(R.string.send_final_sending)} ${
                WalletZecFormmatter.toZecStringFull(
                    sendViewModel.zatoshiAmount
                )
            } ${getString(R.string.symbol)}\n${getString(R.string.send_final_to)}\n${sendViewModel.toAddress.toAbbreviatedAddress()}"
        mainActivity?.preventBackPress(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity?.apply {
            lifecycleScope.launchWhenStarted {
                sendViewModel.send().onEach {
                    onPendingTxUpdated(it)
                }.launchIn(this)
            }
        }
    }

    private fun onPendingTxUpdated(tx: PendingTransaction?) {
        if (tx == null || !isResumed) return // TODO: maybe log this

        try {
            tx.toUiModel().let { model ->
                updateUi(model)
            }

            // only hold onto the view model if the transaction failed so that the user can retry
            if (tx.isSubmitSuccess()) {
                sendViewModel.reset()
                // celebrate
                mainActivity?.vibrate(0, 100, 100, 200, 200, 400)
            }
        } catch (t: Throwable) {
            val message = "ERROR: error while handling pending transaction update! $t"
            twig(message)
            mainActivity?.feedback?.report(Report.Error.NonFatal.TxUpdateFailed(t))
            mainActivity?.feedback?.report(t)
        }
    }

    private fun onExit() {
        sendViewModel.reset()
        mainActivity?.safeNavigate(R.id.action_nav_send_final_to_nav_home)
    }

    private fun onCancel(tx: PendingTransaction) {
        sendViewModel.cancel(tx.id)
    }

    private fun onReturnToSend() {
        mainActivity?.safeNavigate(R.id.action_nav_send_final_to_nav_send)
    }

    private fun onSeeDetails() {
        sendViewModel.reset()
        mainActivity?.safeNavigate(R.id.action_nav_send_final_to_nav_history)
    }

    private fun updateUi(model: UiModel) {
        binding.apply {
            backButton.goneIf(!model.showCloseIcon)
            backButtonHitArea.goneIf(!model.showCloseIcon)

            textConfirmation.text = model.title
            lottieSending.goneIf(!model.showProgress)
            if (!model.showProgress) lottieSending.pauseAnimation() else lottieSending.playAnimation()
            errorMessage.text = model.errorMessage
            buttonPrimary.apply {
                text = model.primaryButtonText
                setOnClickListener { model.primaryAction() }
            }
            buttonMoreInfo.apply {
                goneIf(!model.showSecondaryButton)
                text = getString(R.string.send_more_info)
                setOnClickListener {
                    binding.textMoreInfo.text = model.errorDescription
                    text = getString(R.string.done)
                    setOnClickListener { onExit() }
                }
            }
        }
    }

    private fun PendingTransaction.toUiModel() = UiModel().also { model ->
        when {
            isSubmitSuccess() -> {
                model.title = getString(R.string.send_final_button_primary_sent)
                model.primaryButtonText = getString(R.string.send_final_button_primary_details)
                model.primaryAction = { onSeeDetails() }
            }
            isFailure() -> {
                model.title = getString(R.string.send_final_button_primary_failed)
                model.errorMessage =
                    if (isFailedEncoding()) getString(R.string.send_final_error_encoding) else getString(
                        R.string.send_final_error_submitting
                    )
                model.errorDescription = errorMessage.toString()
                model.primaryButtonText = getString(R.string.send_final_button_primary_retry)
                model.primaryAction = { onReturnToSend() }
                model.showSecondaryButton = true
            }
            else -> {
                val toAddress = if (recipient is TransactionRecipient.Address) {
                    (recipient as TransactionRecipient.Address).addressValue
                } else {
                    ""
                }
                model.title = "${getString(R.string.send_final_sending)} ${
                    WalletZecFormmatter.toZecStringFull(value)
                } ${getString(R.string.symbol)} ${getString(R.string.send_final_to)}\n${toAddress.toAbbreviatedAddress()}"
                model.showProgress = true
                if (isCreating()) {
                    model.showCloseIcon = false
                    model.primaryButtonText = getString(R.string.send_final_button_primary_cancel)
                    model.primaryAction = { onCancel(this) }
                } else {
                    model.primaryButtonText = getString(R.string.send_final_button_primary_details)
                    model.primaryAction = { onSeeDetails() }
                }
            }
        }
    }

    // fields are ordered, as they appear, top-to-bottom in the UI because that makes it easier to reason about each screen state
    data class UiModel(
        var showCloseIcon: Boolean = true,
        var title: String = "",
        var errorDescription: String = "",
        var showProgress: Boolean = false,
        var errorMessage: String = "",
        var primaryButtonText: String = "See Details",
        var primaryAction: () -> Unit = {},
        var showSecondaryButton: Boolean = false,
    )
}

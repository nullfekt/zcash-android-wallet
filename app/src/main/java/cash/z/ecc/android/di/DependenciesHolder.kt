package cash.z.ecc.android.di

import android.content.ClipboardManager
import android.content.Context
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.feedback.*
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.ui.util.DebugFileTwig
import cash.z.ecc.android.util.SilentTwig
import cash.z.ecc.android.util.Twig
import cash.z.ecc.kotlin.mnemonic.Mnemonics

object DependenciesHolder {

    fun provideAppContext(): Context = ZcashWalletApp.instance

    val initializerComponent by lazy { InitializerComponent() }

    val synchronizer by lazy { Synchronizer.newBlocking(initializerComponent.initializer) }

    val clipboardManager by lazy { provideAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val lockBox by lazy { LockBox(provideAppContext()) }

    val prefs by lazy { LockBox(provideAppContext()) }

    val feedback by lazy { Feedback() }

    val feedbackCoordinator by lazy {
        lockBox.getBoolean(Const.Pref.FEEDBACK_ENABLED).let { isEnabled ->
            // observe nothing unless feedback is enabled
            Twig.plant(if (isEnabled) DebugFileTwig() else SilentTwig())
            FeedbackCoordinator(feedback)
        }
    }

    val feedbackFile by lazy { FeedbackFile() }

    val feedbackConsole by lazy { FeedbackConsole() }

    val mnemonics by lazy { Mnemonics() }
}

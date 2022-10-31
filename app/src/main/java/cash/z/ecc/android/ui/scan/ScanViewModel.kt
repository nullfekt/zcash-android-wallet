package cash.z.ecc.android.ui.scan

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.di.DependenciesHolder
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.util.twig

class ScanViewModel : ViewModel() {

    private val synchronizer: Synchronizer = DependenciesHolder.synchronizer

    val networkName get() = synchronizer.network.networkName

    suspend fun parse(qrCode: String): String? {
        // temporary parse code to allow both plain addresses and those that start with zcash:
        // TODO: replace with more robust ZIP-321 handling of QR codes
        val address = if (qrCode.startsWith("zcash:")) {
            qrCode.substring(6, qrCode.indexOf("?").takeUnless { it == -1 } ?: qrCode.length)
        } else {
            qrCode
        }
        return if (synchronizer.validateAddress(address).isNotValid) null else address
    }

    override fun onCleared() {
        super.onCleared()
        twig("${javaClass.simpleName} cleared!")
    }
}

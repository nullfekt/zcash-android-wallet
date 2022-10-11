package cash.z.ecc.android.di

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork

class SynchronizerComponent {

    lateinit var synchronizer: Synchronizer
        private set

    suspend fun createSynchronizer(
        zcashNetwork: ZcashNetwork,
        alias: String = ZcashSdk.DEFAULT_ALIAS,
        lightWalletEndpoint: LightWalletEndpoint,
        seed: ByteArray?,
        birthday: BlockHeight?
    ): Synchronizer {
        synchronizer = Synchronizer.new(
            context = DependenciesHolder.provideAppContext(),
            zcashNetwork = zcashNetwork,
            alias = alias,
            lightWalletEndpoint = lightWalletEndpoint,
            seed = seed,
            birthday = birthday
        )

        return synchronizer
    }
}
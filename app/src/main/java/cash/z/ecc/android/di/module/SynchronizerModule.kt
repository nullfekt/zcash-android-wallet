package cash.z.ecc.android.di.module

import cash.z.ecc.android.di.annotation.SynchronizerScope
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import dagger.Module
import dagger.Provides

/**
 * Module that creates the synchronizer from an initializer and also everything that depends on the
 * synchronizer (because it doesn't exist prior to this module being installed).
 */
@Module(includes = [ViewModelsSynchronizerModule::class])
class SynchronizerModule {

    private var synchronizer: Synchronizer? = null

    @Provides
    @SynchronizerScope
    fun provideSynchronizer(initializer: Initializer): Synchronizer {
        return synchronizer ?: Synchronizer.newBlocking(initializer).also { synchronizer = it }
    }
}

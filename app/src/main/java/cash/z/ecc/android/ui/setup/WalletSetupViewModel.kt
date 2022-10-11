package cash.z.ecc.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.di.DependenciesHolder
import cash.z.ecc.android.ext.Const
import cash.z.ecc.android.ext.failWith
import cash.z.ecc.android.feedback.Feedback
import cash.z.ecc.android.lockbox.LockBox
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.NO_SEED
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITHOUT_BACKUP
import cash.z.ecc.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITH_BACKUP
import cash.z.ecc.android.util.twig
import cash.z.ecc.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class WalletSetupViewModel : ViewModel() {

    private val mnemonics: Mnemonics = DependenciesHolder.mnemonics

    private val lockBox: LockBox = DependenciesHolder.lockBox

    private val prefs: LockBox = DependenciesHolder.prefs

    private val feedback: Feedback = DependenciesHolder.feedback

    enum class WalletSetupState {
        SEED_WITH_BACKUP, SEED_WITHOUT_BACKUP, NO_SEED
    }

    fun checkSeed(): Flow<WalletSetupState> = flow {
        when {
            lockBox.getBoolean(Const.Backup.HAS_BACKUP) -> emit(SEED_WITH_BACKUP)
            lockBox.getBoolean(Const.Backup.HAS_SEED) -> emit(SEED_WITHOUT_BACKUP)
            else -> emit(NO_SEED)
        }
    }

    /**
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }


    fun loadBirthdayHeight(): BlockHeight? {
        val h: Int? = lockBox[Const.Backup.BIRTHDAY_HEIGHT]
        twig("Loaded birthday with key ${Const.Backup.BIRTHDAY_HEIGHT} and found $h")
        h?.let {
            return BlockHeight.new(ZcashWalletApp.instance.defaultNetwork, it.toLong())
        }
        return null
    }

    suspend fun newWallet(): SyncConfig {
        val network = ZcashWalletApp.instance.defaultNetwork
        twig("Initializing new ${network.networkName} wallet")
        with(mnemonics) {
            storeWallet(nextMnemonic(nextEntropy()), network, loadNearestBirthday(network))
        }
        return openStoredWallet()
    }

    suspend fun importWallet(
        seedPhrase: String,
        birthdayHeight: BlockHeight?
    ): SyncConfig {
        val network = ZcashWalletApp.instance.defaultNetwork
        twig("Importing ${network.networkName} wallet. Requested birthday: $birthdayHeight")
        storeWallet(
            seedPhrase.toCharArray(),
            network,
            birthdayHeight ?: loadNearestBirthday(network)
        )
        return openStoredWallet()
    }

    suspend fun openStoredWallet(): SyncConfig {
        val network = ZcashWalletApp.instance.defaultNetwork
        val birthdayHeight = loadBirthdayHeight() ?: onMissingBirthday(network)

        val seed =  lockBox.getBytes(Const.Backup.SEED)!!
        return SyncConfig(
            network,
            LightWalletEndpoint.defaultForNetwork(network),
            seed,
            birthdayHeight
        )
    }

    private suspend fun onMissingBirthday(network: ZcashNetwork): BlockHeight =
        failWith(InitializeException.MissingBirthdayException) {
            twig("Recover Birthday: falling back to sapling birthday")
            loadNearestBirthday(network)
        }

    private suspend fun loadNearestBirthday(network: ZcashNetwork) =
        BlockHeight.ofLatestCheckpoint(
            ZcashWalletApp.instance,
            network,
        )

    //
    // Storage Helpers
    //

    /**
     * Entry point for all storage. Takes a seed phrase and stores all the parts so that we can
     * selectively use them, the next time the app is opened. Although we store everything, we
     * primarily only work with the viewing key and spending key. The seed is only accessed when
     * presenting backup information to the user.
     */
    private suspend fun storeWallet(
        seedPhraseChars: CharArray,
        network: ZcashNetwork,
        birthday: BlockHeight
    ) {
        check(!lockBox.getBoolean(Const.Backup.HAS_SEED)) {
            "Error! Cannot store a seed when one already exists! This would overwrite the" +
                    " existing seed and could lead to a loss of funds if the user has no backup!"
        }

        storeBirthday(birthday)

        mnemonics.toSeed(seedPhraseChars).let { bip39Seed ->
            DerivationTool.deriveUnifiedFullViewingKeys(bip39Seed, network)[0].let { viewingKey ->
                storeSeedPhrase(seedPhraseChars)
                storeSeed(bip39Seed)
                storeUnifiedViewingKey(viewingKey)
            }
        }
    }

    private suspend fun storeBirthday(birthday: BlockHeight) = withContext(IO) {
        twig("Storing birthday ${birthday.value} with and key ${Const.Backup.BIRTHDAY_HEIGHT}")
        lockBox[Const.Backup.BIRTHDAY_HEIGHT] = birthday.value
    }

    private suspend fun storeSeedPhrase(seedPhrase: CharArray) = withContext(IO) {
        twig("Storing seedphrase: ${seedPhrase.size}")
        lockBox[Const.Backup.SEED_PHRASE] = seedPhrase
        lockBox[Const.Backup.HAS_SEED_PHRASE] = true
    }

    private suspend fun storeSeed(bip39Seed: ByteArray) = withContext(IO) {
        twig("Storing seed: ${bip39Seed.size}")
        lockBox.setBytes(Const.Backup.SEED, bip39Seed)
        lockBox[Const.Backup.HAS_SEED] = true
    }

    private suspend fun storeUnifiedViewingKey(vk: UnifiedFullViewingKey) = withContext(IO) {
        twig("storeViewingKey vk: ${vk.encoding.length}")
        lockBox[Const.Backup.UNIFIED_VIEWING_KEY] = vk.encoding
    }

    data class SyncConfig(
        val network: ZcashNetwork,
        val lightWalletEndpoint: LightWalletEndpoint,
        val seed: ByteArray?,
        val birthday: BlockHeight?
    ){
        override fun toString() = "SyncConfig"
    }
}

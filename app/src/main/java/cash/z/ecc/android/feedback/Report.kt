package cash.z.ecc.android.feedback

import cash.z.ecc.android.ZcashWalletApp
import cash.z.ecc.android.sdk.model.BlockHeight

object Report {

    object Funnel {
        sealed class Send(stepName: String, step: Int, vararg properties: Pair<String, Any>) : Feedback.Funnel("send", stepName, step, *properties) {
            object AddressPageComplete : Send("addresspagecomplete", 10)
            object MemoPageComplete : Send("memopagecomplete", 20)
            object ConfirmPageComplete : Send("confirmpagecomplete", 30)

            // Beginning of send
            object SendSelected : Send("sendselected", 50)
            object SpendingKeyFound : Send("keyfound", 60)
            object Creating : Send("creating", 70)
            object Cancelled : Send("cancelled", 72)
            class Created(id: Long) : Send("created", 80, "id" to id)
            object Submitted : Send("submitted", 90)
            class Mined(minedHeight: Int) : Send("mined", 100, "minedHeight" to minedHeight)

            // Errors
            abstract class Error(stepName: String, step: Int, val errorCode: Int?, val errorMessage: String?, vararg properties: Pair<String, Any>) : Send("error.$stepName", step, "isError" to true, *properties)
            object ErrorNotFound : Error("notfound", 51, null, "Key not found")
            class ErrorEncoding(errorCode: Int? = null, errorMessage: String? = null) : Error(
                "encode", 71, errorCode, errorMessage,
                "errorCode" to (errorCode ?: -1),
                "errorMessage" to (errorMessage ?: "None")
            )
            class ErrorSubmitting(errorCode: Int? = null, errorMessage: String? = null) : Error(
                "submit", 81, errorCode, errorMessage,
                "errorCode" to (errorCode ?: -1),
                "errorMessage" to (errorMessage ?: "None")
            )
        }

        sealed class Restore(stepName: String, step: Int, vararg properties: Pair<String, Any>) : Feedback.Funnel("restore", stepName, step, *properties) {
            object Initiated : Restore("initiated", 0)
            object SeedWordsStarted : Restore("wordsstarted", 10)
            class SeedWordCount(wordCount: Int) : Restore("wordsmodified", 15, "seedWordCount" to wordCount)
            object SeedWordsCompleted : Restore("wordscompleted", 20)
            object Stay : Restore("stay", 21)
            object Exit : Restore("stay", 22)
            object Done : Restore("doneselected", 30)
            object ImportStarted : Restore("importstarted", 40)
            object ImportCompleted : Restore("importcompleted", 50)
            object Success : Restore("success", 100)
        }

        sealed class UserFeedback(stepName: String, step: Int, vararg properties: Pair<String, Any>) : Feedback.Funnel("feedback", stepName, step, *properties) {
            object Started : UserFeedback("started", 0)
            object Cancelled : UserFeedback("cancelled", 1)
            class Submitted(rating: Int, question1: String, question2: String, question3: String, isSolicited: Boolean) : UserFeedback("submitted", 100, "rating" to rating, "question1" to question1, "question2" to question2, "question3" to question3, "isSolicited" to isSolicited)
        }
    }

    object Error {
        object NonFatal {
            class Reorg(errorBlockHeight: BlockHeight, rewindBlockHeight: BlockHeight) : Feedback.AppError(
                "reorg",
                "Chain error detected at height $errorBlockHeight, rewinding to $rewindBlockHeight",
                false,
                "errorHeight" to errorBlockHeight,
                "rewindHeight" to rewindBlockHeight
            ) {
                val errorHeight: Int by propertyMap
                val rewindHeight: Int by propertyMap
            }
            class TxUpdateFailed(t: Throwable) : Feedback.AppError("txupdate", t, false)
            abstract class TxError(action: String, val errorCode: Int?, val errorMessage: String?) : Feedback.AppError(
                "tx.$action",
                "Failed to $action transaction due to $errorMessage",
                false,
                "errorCode" to (errorCode ?: 1)
            )
            class TxEncodeError(errorCode: Int?, errorMessage: String?) : TxError("encode", errorCode, errorMessage)
            class TxSubmitError(errorCode: Int?, errorMessage: String?) : TxError("submit", errorCode, errorMessage)
        }
    }

    sealed class Performance(name: String, vararg properties: Pair<String, Any>) : Feedback.MappedAction(
        "metricName" to name,
        "isPerformanceMetric" to true,
        *properties
    ) {
        override val key = "performance.$name"
        override fun toString() = "$key: ${toMap().let { if (it.size > 1) "${it.entries}" else "" }}"

        class ScanRate(network: String, cumulativeItems: Int, cumulativeTime: Long, cumulativeIps: Float) : Performance("scan.bps", "network" to network, "totalBlocks" to cumulativeItems, "totalTime" to cumulativeTime, "blocksPerSecond" to cumulativeIps)
    }

    // placeholder for things that we want to monitor
    sealed class Issue(name: String, vararg properties: Pair<String, Any>) : Feedback.MappedAction(
        "issueName" to name,
        "isIssue" to true,
        *properties
    ) {
        override val key = "issue.$name"
        override fun toString() = "occurrence of ${key.replace('.', ' ')}${toMap().let { if (it.size > 1) " with ${it.entries}" else "" }}"

        // Issues with sending worth monitoring
        object SelfSend : Issue("self.send")
        object TinyAmount : Issue("tiny.amount")
        object MicroAmount : Issue("micro.amount")
        object MinimumAmount : Issue("minimum.amount")
        class TruncatedMemo(memoSize: Int) : Issue("truncated.memo", "memoSize" to memoSize)
        class LargeMemo(memoSize: Int) : Issue("large.memo", "memoSize" to memoSize)
        class MissingViewkey(recovered: Boolean, needle: String, haystack: String, hasKey: Boolean) : Issue(
            "missing.viewkey", "wasAbleToRecover" to recovered, "needle" to needle, "haystack" to haystack, "hasKey" to hasKey
        )
    }

    enum class Screen(val id: String? = null) : Feedback.Action {
        BACKUP,
        HOME,
        HISTORY("wallet.history"),
        TRANSACTION("wallet.transaction"),
        LANDING,
        PROFILE,
        AWESOME,
        FEEDBACK,
        RECEIVE,
        RESTORE,
        SCAN,
        AUTO_SHIELD_FINAL("autoshield.final"),
        AUTO_SHIELD_AVAILABLE("autoshield.available"),
        AUTO_SHIELD_INFORMATION("autoshield.information"),
        SEND_ADDRESS("send.address"),
        SEND_CONFIRM("send.confirm"),
        SEND_FINAL("send.final"),
        SEND_MEMO("send.memo");

        override val key = "screen.${id ?: name.toLowerCase()}"
        override fun toString() = "viewed the ${key.substring(7).replace('.', ' ')} screen"
    }

    enum class Tap(val id: String) : Feedback.Action {
        BACKUP_DONE("backup.done"),
        BACKUP_VERIFY("backup.verify"),
        DEVELOPER_WALLET_PROMPT("landing.devwallet.prompt"),
        DEVELOPER_WALLET_IMPORT("landing.devwallet.import"),
        DEVELOPER_WALLET_CANCEL("landing.devwallet.cancel"),
        LANDING_RESTORE("landing.restore"),
        LANDING_NEW("landing.new"),
        LANDING_BACKUP("landing.backup"),
        LANDING_BACKUP_SKIPPED_1("landing.backup.skip.1"),
        LANDING_BACKUP_SKIPPED_2("landing.backup.skip.2"),
        LANDING_BACKUP_SKIPPED_3("landing.backup.skip.3"),
        HOME_PROFILE("home.profile"),
        HOME_HISTORY("home.history"),
        HOME_RECEIVE("home.receive"),
        HOME_BALANCE_DETAIL("home.balance.detail"),
        TAB_LAYOUT("tab.layout"),
        HOME_SCAN("home.scan"),
        HOME_SEND("home.send"),
        HOME_FUND_NOW("home.fund.now"),
        HOME_CLEAR_AMOUNT("home.clear.amount"),
        HISTORY_BACK("history.back"),
        TRANSACTION_BACK("transaction.back"),
        PROFILE_CLOSE("profile.close"),
        AWESOME_OPEN("profile.awesome"),
        AWESOME_CLOSE("awesome.close"),
        AWESOME_SHIELD("awesome.shield"),
        PROFILE_BACKUP("profile.backup"),
        PROFILE_RESCAN("profile.rescan"),
        PROFILE_VIEW_USER_LOGS("profile.view.user.logs"),
        PROFILE_VIEW_DEV_LOGS("profile.view.dev.logs"),
        PROFILE_SEND_FEEDBACK("profile.send.feedback"),
        FEEDBACK_CANCEL("feedback.cancel"),
        FEEDBACK_SUBMIT("feedback.submit"),
        RECEIVE_BACK("receive.back"),
        RESTORE_DONE("restore.done"),
        RESTORE_CLEAR("restore.clear"),
        RESTORE_SUCCESS("restore.success"),
        RESTORE_BACK("restore.back"),
        SCAN_BACK("scan.back"),
        AUTO_SHIELD_FINAL_CLOSE("autoshield.final.close"),
        AUTO_SHIELD_FINAL_DONE("autoshield.final.done"),
        SEND_ADDRESS_MAX("send.address.max"),
        SEND_ADDRESS_NEXT("send.address.next"),
        SEND_ADDRESS_PASTE("send.address.paste"),
        SEND_ADDRESS_REUSE("send.address.reuse"),
        SEND_ADDRESS_BACK("send.address.back"),
        SEND_ADDRESS_DONE_ADDRESS("send.address.done.address"),
        SEND_ADDRESS_DONE_AMOUNT("send.address.done.amount"),
        SEND_ADDRESS_SCAN("send.address.scan"),
        SEND_CONFIRM_BACK("send.confirm.back"),
        SEND_CONFIRM_NEXT("send.confirm.next"),
        SEND_FINAL_EXIT("send.final.exit"),
        SEND_FINAL_RETRY("send.final.retry"),
        SEND_FINAL_CLOSE("send.final.close"),
        SEND_MEMO_INCLUDE("send.memo.include"),
        SEND_MEMO_EXCLUDE("send.memo.exclude"),
        SEND_MEMO_NEXT("send.memo.next"),
        SEND_MEMO_SKIP("send.memo.skip"),
        SEND_MEMO_CLEAR("send.memo.clear"),
        SEND_MEMO_BACK("send.memo.back"),

        SEND_SUBMIT("send.submit"),

        // General events
        COPY_ADDRESS("copy.address"),
        COPY_TRANSPARENT_ADDRESS("copy.address.transparent");

        override val key = "tap.$id"
        override fun toString() = "${key.replace('.', ' ')} button".replace("tap ", "tapped the ")
    }

    enum class NonUserAction(override val key: String, val description: String) : Feedback.Action {
        FEEDBACK_STARTED("action.feedback.start", "feedback started"),
        FEEDBACK_STOPPED("action.feedback.stop", "feedback stopped"),
        SYNC_START("action.feedback.synchronizer.start", "sync started");

        override fun toString(): String = description
    }

    enum class MetricType(override val key: String, val description: String) : Feedback.Action {
        ENTROPY_CREATED("metric.entropy.created", "entropy created"),
        SEED_CREATED("metric.seed.created", "seed created"),
        SEED_IMPORTED("metric.seed.imported", "seed imported"),
        SEED_PHRASE_CREATED("metric.seedphrase.created", "seed phrase created"),
        SEED_PHRASE_LOADED("metric.seedphrase.loaded", "seed phrase loaded"),
        WALLET_CREATED("metric.wallet.created", "wallet created"),
        WALLET_IMPORTED("metric.wallet.imported", "wallet imported"),
        ACCOUNT_CREATED("metric.account.created", "account created"),

        // Transactions
        TRANSACTION_INITIALIZED("metric.tx.initialized", "transaction initialized"),
        TRANSACTION_CREATED("metric.tx.created", "transaction created successfully"),
        TRANSACTION_SUBMITTED("metric.tx.submitted", "transaction submitted successfully"),
        TRANSACTION_MINED("metric.tx.mined", "transaction mined")
    }
}

/**
 * Creates a metric with a start time of ZcashWalletApp.creationTime and an end time of when this
 * instance was created. This can then be passed to [Feedback.report].
 */
class LaunchMetric private constructor(private val metric: Feedback.TimeMetric) :
    Feedback.Metric by metric {
    constructor() : this(
        Feedback
            .TimeMetric(
                "metric.app.launch",
                "app launched",
                mutableListOf(ZcashWalletApp.instance.creationTime)
            )
            .markTime()
    )
    override fun toString(): String = metric.toString()
}

inline fun <T> Feedback.measure(type: Report.MetricType, block: () -> T): T =
    this.measure(type.key, type.description, block)

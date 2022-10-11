package cash.z.ecc.android.ui.util

import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.PendingTransaction
import java.nio.charset.StandardCharsets

/**
 * The prefix that this wallet uses whenever the user chooses to include their address in the memo.
 * This is the one we standardize around.
 */
const val INCLUDE_MEMO_PREFIX_STANDARD = "Reply-To:"

/**
 * The non-standard prefixes that we will parse if other wallets send them our way.
 */
val INCLUDE_MEMO_PREFIXES_RECOGNIZED = arrayOf(
    INCLUDE_MEMO_PREFIX_STANDARD, // standard
    "reply-to", // standard w/o colon
    "reply to:", // space instead of dash
    "reply to", // space instead of dash w/o colon
    "sent from:", // previous standard
    "sent from" // previous standard w/o colon
)

object MemoUtil {

    suspend fun findAddressInMemo(
        memo: String?,
        addressValidator: suspend (String) -> Boolean
    ): String? {
        // note: t-addr min length is 35, plus we're expecting prefixes
        return memo?.takeUnless { it.length < 35 }?.let {
            // start with what we accept as prefixes
            INCLUDE_MEMO_PREFIXES_RECOGNIZED.mapNotNull {
                val maybeMemo = it.substringAfterLast(it)
                if (addressValidator(maybeMemo)) maybeMemo else null
            }.firstOrNull { !it.isNullOrBlank() }
        }
    }

    // note: cannot use substringAfterLast, directly because we want to ignore case. perhaps submit a feature request to kotlin for adding `ignoreCase`
    private fun String.substringAfterLast(prefix: String): String {
        return lastIndexOf(prefix, ignoreCase = true).takeUnless { it == -1 }?.let { i ->
            substring(i + prefix.length).trimStart()
        } ?: ""
    }
}

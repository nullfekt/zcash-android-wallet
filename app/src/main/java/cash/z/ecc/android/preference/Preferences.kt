package cash.z.ecc.android.preference

import cash.z.ecc.android.preference.model.BooleanDefaultValue

object Preferences {
    val isAcknowledgedAutoshieldingInformationPrompt =
        BooleanDefaultValue(PreferenceKeys.IS_AUTOSHIELDING_INFO_ACKNOWLEDGED, false)
}

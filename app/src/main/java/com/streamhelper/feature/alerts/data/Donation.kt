package com.streamhelper.feature.alerts.data

import java.math.BigDecimal

/** `message_type` of a DonationAlerts donation. Only [Text] donations are listed. */
enum class DonationMessageType {
    Text,
    Audio,

    /** Any value this app doesn't know yet. */
    Other;

    companion object {
        fun fromApi(value: String?): DonationMessageType = when (value) {
            "text" -> Text
            "audio" -> Audio
            else -> Other
        }
    }
}

/** A DonationAlerts donation (RN `AlertModel`). [userName] is `null` for anonymous donors. */
data class Donation(
    val id: Long,
    val userName: String?,
    val message: String?,
    val amount: Double,
    val currency: String,
    val messageType: DonationMessageType,
)

/** Keeps only donations that can be read out: text ones (audio is out of scope). Pure. */
fun List<Donation>.onlyText(): List<Donation> = filter { it.messageType == DonationMessageType.Text }

/**
 * `amount currency` like RN's `${amount} ${currency}`: whole amounts have no decimal part
 * (`100 RUB`, `12.5 USD`). Deliberately not locale-formatted (parity). Pure.
 */
fun Donation.amountText(): String =
    "${BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()} $currency"

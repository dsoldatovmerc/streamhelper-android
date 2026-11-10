package com.streamhelper.feature.alerts.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** DonationAlerts API v1 (relative to the DonationAlerts base URL). */
interface AlertsApi {
    /** First page of the token owner's donations, newest first. `links`/`meta` are ignored (no pagination). */
    @GET("alerts/donations")
    suspend fun getDonations(): DonationsResponse
}

@Serializable
data class DonationsResponse(val data: List<DonationDto> = emptyList())

@Serializable
data class DonationDto(
    val id: Long,
    val username: String? = null,
    @SerialName("message_type") val messageType: String? = null,
    val message: String? = null,
    val amount: Double = 0.0,
    val currency: String = "",
)

fun DonationDto.toDonation(): Donation = Donation(
    id = id,
    userName = username,
    message = message,
    amount = amount,
    currency = currency,
    messageType = DonationMessageType.fromApi(messageType),
)

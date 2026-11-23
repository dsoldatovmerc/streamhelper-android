package com.streamhelper.feature.alerts.data

import javax.inject.Inject

/** DonationAlerts donations (RN `modules/Alerts`). */
class AlertsRepository @Inject constructor(private val api: AlertsApi) {
    /**
     * Loads the first page of donations, keeping text donations only (audio is left out).
     * Throws on HTTP/parse errors.
     */
    suspend fun loadDonations(): List<Donation> =
        api.getDonations().data.map { it.toDonation() }.onlyText()
}

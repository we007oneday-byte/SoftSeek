package io.github.we007oneday_byte.softseek.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponseDto(
    @SerialName("is_available") val isAvailable: Boolean = false,
    @SerialName("balance_infos") val balanceInfos: List<BalanceInfoDto> = emptyList(),
)

@Serializable
data class BalanceInfoDto(
    val currency: String,
    @SerialName("total_balance") val totalBalance: String,
    @SerialName("granted_balance") val grantedBalance: String,
    @SerialName("topped_up_balance") val toppedUpBalance: String,
)

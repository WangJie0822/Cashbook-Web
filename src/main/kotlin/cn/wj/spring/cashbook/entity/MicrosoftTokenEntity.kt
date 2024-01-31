package cn.wj.spring.cashbook.entity

data class MicrosoftTokenEntity(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Long?,
    val scope: String?,
    val refresh_token: String?,
    val id_token: String?,
    val error: String?,
    val error_description: String?
)
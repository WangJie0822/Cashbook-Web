package cn.wj.spring.cashbook.entity

data class CbResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {

    val success: Boolean
        get() = code == RESPONSE_CODE_SUCCESS

    companion object {

        const val RESPONSE_CODE_SUCCESS = 0
        const val RESPONSE_CODE_FAILED = -1001
        const val RESPONSE_CODE_FAILED_NO_RES = -1002

        fun failed(code: Int = RESPONSE_CODE_FAILED, message: String = "Error"): CbResponse<Any> {
            return CbResponse(
                code = code,
                message = message,
                data = null
            )
        }

        fun <T> success(data: T, code: Int = RESPONSE_CODE_SUCCESS, message: String = "Success"): CbResponse<T> {
            return CbResponse(
                code = code,
                message = message,
                data = data
            )
        }
    }
}

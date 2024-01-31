package cn.wj.spring.cashbook.enums

/**
 * 数据源枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/1/30
 */
enum class SourceEnum(
    val value: String,
    val url: String,
) {
    GITHUB(
        value = "0",
        url = "https://api.github.com/repos/WangJie0822/Cashbook/releases?page=1&per_page=50&direction=desc",
    ),
    GITEE(
        value = "1",
        url = "https://gitee.com/api/v5/repos/wangjie0822/Cashbook/releases?page=1&per_page=50&direction=desc",
    ),
    ONE_DRIVE(
        value = "2",
        url = "",
    ),
    ;

    companion object {
        fun fromValue(value: String): SourceEnum {
            return entries.firstOrNull { it.value == value } ?: GITHUB
        }
    }
}
package moe.fuqiuluo.mamu.floating.event

/**
 * 地址值变更事件
 */
data class AddressValueChangedEvent(
    val address: Long,
    val newValue: String,
    val valueType: Int,
    val source: Source
) {
    enum class Source {
        SEARCH,         // 来自搜索结果界面
        SAVED_ADDRESS,  // 来自保存地址界面
        MEMORY_PREVIEW  // 来自内存预览界面
    }
}

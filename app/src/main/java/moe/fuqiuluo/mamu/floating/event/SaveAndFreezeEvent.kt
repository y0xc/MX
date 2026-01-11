package moe.fuqiuluo.mamu.floating.event

import moe.fuqiuluo.mamu.floating.data.model.DisplayMemRegionEntry
import moe.fuqiuluo.mamu.floating.data.model.DisplayValueType

/**
 * 保存并冻结地址事件
 * 用于从搜索结果或内存预览界面修改值时勾选"冻结"选项
 */
data class SaveAndFreezeEvent(
    val address: Long,
    val value: String,
    val valueType: DisplayValueType,
    val range: DisplayMemRegionEntry?
)

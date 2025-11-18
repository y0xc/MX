package moe.fuqiuluo.mamu.driver

import moe.fuqiuluo.mamu.floating.model.DisplayValueType

interface SearchResultItem {
    val nativePosition: Long // 原始索引，目的是方便
}

data class ExactSearchResultItem(
    override val nativePosition: Long,
    val address: Long,
    val valueType: Int,
    val value: String,
): SearchResultItem {
    val displayValueType: DisplayValueType?
        get() = DisplayValueType.fromNativeId(valueType)
}

data class FuzzySearchResultItem(
    override val nativePosition: Long,
    val address: Long,
    val value: String,
    val valueType: Int
): SearchResultItem {
    val displayValueType: DisplayValueType?
        get() = DisplayValueType.fromNativeId(valueType)
}
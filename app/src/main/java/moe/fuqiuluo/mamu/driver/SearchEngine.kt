@file:Suppress("KotlinJniMissingFunction")

package moe.fuqiuluo.mamu.driver

import moe.fuqiuluo.mamu.floating.ext.divideToSimpleMemoryRange
import moe.fuqiuluo.mamu.floating.model.DisplayValueType
import moe.fuqiuluo.mamu.floating.model.MemoryRange

object SearchEngine {
    /**
     * 初始化搜索引擎
     * @param bufferSize 搜索缓冲区大小，单位字节 （缓存搜索结果）
     * @param cacheFileDir 缓存文件目录
     * @param chunkSize 分块大小，单位字节，默认512KB
     * @return 初始化是否成功
     */
    fun initSearchEngine(
        bufferSize: Long,
        cacheFileDir: String,
        chunkSize: Long = 512 * 1024, // Default: 512KB
    ): Boolean {
        if(nativeInitSearchEngine(bufferSize, cacheFileDir, chunkSize)) {
            return true
        }
        return false
    }

    /**
     * 执行精确搜索/联合搜索
     * @param query 搜索内容
     * @param type 数据类型
     * @param ranges 内存区域集合
     * @param memoryMode 内存搜索模式
     * @param cb 搜索进度回调
     * @return 搜索到的结果数量
     */
    fun searchExact(
        query: String,
        type: DisplayValueType,
        ranges: Set<MemoryRange>,
        memoryMode: Int,
        cb: SearchProgressCallback
    ): Long {
        val nativeRegions = mutableListOf<Long>()

        WuwaDriver.queryMemRegions()
            .divideToSimpleMemoryRange()
            .filter { ranges.contains(it.range) }
            .forEach {
                nativeRegions.add(it.start)
                nativeRegions.add(it.end)
            }

        return nativeSearch(query, type.nativeId, nativeRegions.toLongArray(), memoryMode, cb)
    }

    /**
     * 执行精确搜索/联合搜索，使用自定义内存区域
     * @param query 搜索内容
     * @param type 数据类型
     * @param regions 内存区域数组，格式为[start1, end1, start2, end2, ...]
     * @param memoryMode 内存搜索模式
     * @param cb 搜索进度回调
     * @return 搜索到的结果数量
     */
    fun exactSearchWithCustomRange(
        query: String,
        type: DisplayValueType,
        regions: LongArray,
        memoryMode: Int,
        cb: SearchProgressCallback
    ): Long {
        return nativeSearch(query, type.nativeId, regions, memoryMode, cb)
    }

    /**
     * 获取搜索结果
     * @param start 起始索引
     * @param count 获取数量
     * @return 搜索结果数组
     */
    fun getResults(start: Int, count: Int): Array<SearchResultItem> {
        return nativeGetResults(start, count)
    }

    /**
     * 获取搜索结果总数
     * @return 搜索结果总数
     */
    fun getTotalResultCount(): Long {
        return nativeGetTotalResultCount()
    }

    /**
     * 清除搜索结果
     */
    fun clearSearchResults() {
        nativeClearSearchResults()
    }

    /**
     * 移除单个搜索结果
     * @param index 搜索结果索引
     * @return 是否移除成功
     */
    fun removeResult(index: Int): Boolean {
        return nativeRemoveResult(index)
    }

    /**
     * 移除多个搜索结果
     * @param indices 搜索结果索引数组
     * @return 是否移除成功
     */
    fun removeResults(indices: IntArray): Boolean {
        return nativeRemoveResults(indices)
    }

    /**
     * 设置过滤条件 (地址范围、值范围、数据类型、权限)！
     *
     * 仅作用于搜索结果的过滤，不会影响实际搜索过程！
     *
     * @param enableAddressFilter 是否启用地址过滤
     * @param addressStart 地址范围起始
     * @param addressEnd 地址范围结束
     * @param enableTypeFilter 是否启用数据类型过滤
     * @param typeIds 数据类型ID数组
     */
    fun setFilter(
        enableAddressFilter: Boolean,
        addressStart: Long,
        addressEnd: Long,
        enableTypeFilter: Boolean,
        typeIds: IntArray,
    ) {
        nativeSetFilter(
            enableAddressFilter, addressStart, addressEnd,
            enableTypeFilter, typeIds,
        )
    }

    /**
     * 清除所有过滤条件
     */
    fun clearFilter() {
        nativeClearFilter()
    }

    private external fun nativeInitSearchEngine(bufferSize: Long, cacheFileDir: String, chunkSize: Long): Boolean
    private external fun nativeSearch(
        query: String,
        defaultType: Int,
        regions: LongArray,
        memoryMode: Int,
        cb: SearchProgressCallback
    ): Long

    private external fun nativeGetResults(start: Int, count: Int): Array<SearchResultItem>
    private external fun nativeGetTotalResultCount(): Long
    private external fun nativeClearSearchResults()
    private external fun nativeRemoveResult(index: Int): Boolean
    private external fun nativeRemoveResults(indices: IntArray): Boolean
    private external fun nativeSetFilter(
        enableAddressFilter: Boolean,
        addressStart: Long,
        addressEnd: Long,
        enableTypeFilter: Boolean,
        typeIds: IntArray,
    )
    private external fun nativeClearFilter()
}
package moe.fuqiuluo.mamu.driver

interface SearchProgressCallback {
    /**
     * 搜索全部完成
     * @param totalFound 总共找到的结果数
     * @param totalRegions 总区域数
     * @param elapsedMillis 总耗时（毫秒）
     */
    fun onSearchComplete(totalFound: Long, totalRegions: Int, elapsedMillis: Long)
}

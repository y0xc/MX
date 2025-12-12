package moe.fuqiuluo.mamu.floating.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.fuqiuluo.mamu.databinding.ItemSearchResultBinding
import moe.fuqiuluo.mamu.driver.ExactSearchResultItem
import moe.fuqiuluo.mamu.driver.FuzzySearchResultItem
import moe.fuqiuluo.mamu.driver.SearchResultItem
import moe.fuqiuluo.mamu.floating.data.local.MemoryBackupManager
import moe.fuqiuluo.mamu.floating.data.model.DisplayMemRegionEntry
import moe.fuqiuluo.mamu.floating.data.model.DisplayValueType
import moe.fuqiuluo.mamu.floating.data.model.MemoryRange

class SearchResultAdapter(
    // 点击事件回调
    private val onItemClick: (SearchResultItem, Int) -> Unit = { _, _ -> },
    // 长按事件回调
    private val onItemLongClick: (SearchResultItem, Int) -> Boolean = { _, _ -> false },
    // 选中状态变化回调
    private val onSelectionChanged: (Int) -> Unit = {},
    // 删除项回调
    private val onItemDelete: (SearchResultItem) -> Unit = { _ -> }
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    // 搜索结果列表
    private val results = mutableListOf<SearchResultItem>()
    // 选中位置集合
    private val selectedPositions = mutableSetOf<Int>()
    // 内存范围列表 (保存主要是为了显示内存范围简称和颜色)
    private var ranges: List<DisplayMemRegionEntry>? = null
    // 预排序的范围列表，用于二分查找 (按 start 地址排序)
    private var sortedRanges: List<DisplayMemRegionEntry>? = null

    init {
        // 启用稳定ID，提升RecyclerView刷新性能
        setHasStableIds(true)
    }

    /**
     * 设置搜索结果列表
     * @param newResults 新的搜索结果列表
     */
    fun setResults(newResults: List<SearchResultItem>) {
        val oldSize = results.size
        results.clear()
        selectedPositions.clear()

        if (oldSize > 0) {
            // 执行一个带动画的移除动画？有没有必要呢？
            notifyItemRangeRemoved(0, oldSize)
        }

        results.addAll(newResults)
        if (newResults.isNotEmpty()) {
            notifyItemRangeInserted(0, newResults.size)
        }

        onSelectionChanged(0) // 通知选中状态变化
    }

    /**
     * 根据地址更新单个搜索结果项的值
     * @param address 要更新的地址
     * @param newValue 新的值（不包含备份信息，备份值会在 ViewHolder 中自动显示）
     * @return 是否找到并更新了该项
     */
    fun updateItemValueByAddress(address: Long, newValue: String): Boolean {
        val position = results.indexOfFirst {
            when (it) {
                is ExactSearchResultItem -> it.address == address
                is FuzzySearchResultItem -> it.address == address
                else -> false
            }
        }

        if (position == -1) {
            return false
        }

        val newItem = when (val oldItem = results[position]) {
            is ExactSearchResultItem -> oldItem.copy(value = newValue)
            is FuzzySearchResultItem -> oldItem.copy(value = newValue)
            else -> return false
        }

        results[position] = newItem
        // 刷新该项，ViewHolder 的 bind 方法会自动检查并显示备份值
        notifyItemChanged(position)
        return true
    }

    /**
     * 获取所有选中项的原生地址数组
     * @return 选中项的原生地址数组
     */
    fun getNativePositions(): LongArray {
        return selectedPositions.map { results[it].nativePosition }.toLongArray()
    }

    /**
     * 设置内存范围列表
     * @param newRanges 新的内存范围列表
     */
    fun setRanges(newRanges: List<DisplayMemRegionEntry>?) {
        ranges = newRanges
        // 预排序用于二分查找，避免每次 ViewHolder 绑定时 O(n) 线性查找
        sortedRanges = newRanges?.sortedBy { it.start }
    }

    /**
     * 使用二分查找定位地址所属的内存范围
     * 时间复杂度: O(log n)，相比之前的 O(n) 线性查找大幅提升
     *
     * @param address 要查找的地址
     * @return 对应的 MemoryRange，找不到返回 MemoryRange.O
     */
    private fun findMemoryRangeByAddress(address: Long): MemoryRange {
        val sorted = sortedRanges ?: return MemoryRange.O
        if (sorted.isEmpty()) return MemoryRange.O

        // 二分查找：找到最后一个 start <= address 的范围
        var low = 0
        var high = sorted.size - 1
        var result: DisplayMemRegionEntry? = null

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midEntry = sorted[mid]

            if (midEntry.start <= address) {
                result = midEntry
                low = mid + 1  // 继续向右找，看是否有更接近的
            } else {
                high = mid - 1
            }
        }

        // 检查找到的范围是否真的包含该地址
        return if (result != null && result.containsAddress(address)) {
            result.range
        } else {
            MemoryRange.O
        }
    }

    /**
     * 获取内存范围列表
     * @return 内存范围列表
     */
    fun getRanges(): List<DisplayMemRegionEntry>? {
        return ranges
    }

    /**
     * 添加搜索结果
     * @param newResults 新的搜索结果列表
     */
    fun addResults(newResults: List<SearchResultItem>) {
        val oldSize = results.size
        results.addAll(newResults)
        notifyItemRangeInserted(oldSize, newResults.size)
    }

    /**
     * 清空搜索结果
     */
    fun clearResults() {
        val oldSize = results.size
        results.clear()
        selectedPositions.clear()
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize)
        }
        onSelectionChanged(0)
    }

    /**
     * 获取所有选中项
     * @return 选中项列表
     */
    fun getSelectedItems(): List<SearchResultItem> {
        return selectedPositions.map { results[it] }
    }

    /**
     * 获取所有选中位置
     * @return 选中位置集合
     */
    fun getSelectedPositions(): Set<Int> {
        return selectedPositions.toSet()
    }

    /**
     * 全选 - 使用 payload 只更新选择状态，避免完全重绑定导致的闪烁
     */
    fun selectAll() {
        if (selectedPositions.size == results.size) {
            // 已经全选，无需操作
            return
        }

        selectedPositions.clear()
        selectedPositions.addAll(results.indices)

        // 使用 payload 机制：只触发 onBindViewHolder 的 payload 分支
        // 这样只更新 checkbox 状态，不重新绑定其他内容，避免闪烁
        notifyItemRangeChanged(0, results.size, PAYLOAD_SELECTION_CHANGED)
        onSelectionChanged(selectedPositions.size)
    }

    /**
     * 全不选 - 使用 payload 只更新选择状态，避免完全重绑定导致的闪烁
     */
    fun deselectAll() {
        if (selectedPositions.isEmpty()) {
            // 已经是全不选状态，直接返回
            return
        }

        selectedPositions.clear()
        // 使用 payload 机制，只更新选择状态
        notifyItemRangeChanged(0, results.size, PAYLOAD_SELECTION_CHANGED)
        onSelectionChanged(0)
    }

    /**
     * 反选 - 使用 payload 只更新选择状态，避免完全重绑定导致的闪烁
     */
    fun invertSelection() {
        val totalSize = results.size

        // 避免创建 (0 until totalSize).toSet() 这样的大集合
        // 遍历所有索引，在集合中的删除，不在的添加
        val newSelection = hashSetOf<Int>()
        for (i in 0 until totalSize) {
            if (i !in selectedPositions) {
                newSelection.add(i)
            }
        }

        selectedPositions.clear()
        selectedPositions.addAll(newSelection)

        // 使用 payload 机制，只更新选择状态
        notifyItemRangeChanged(0, totalSize, PAYLOAD_SELECTION_CHANGED)
        onSelectionChanged(selectedPositions.size)
    }

    companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position], position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (payload in payloads) {
                if (payload == PAYLOAD_SELECTION_CHANGED) {
                    holder.updateSelection(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = results.size

    override fun getItemId(position: Int): Long {
        // 使用nativePosition作为稳定ID，帮助RecyclerView优化刷新性能
        return results[position].nativePosition
    }

    inner class ViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResultItem, position: Int) {
            when (item) {
                is ExactSearchResultItem -> {
                    binding.apply {
                        // 地址
                        addressText.text = item.address.toString(16).uppercase()

                        // 当前值
                        valueText.text = item.value

                        // 备份值（旧值）
                        val backup = MemoryBackupManager.getBackup(item.address)
                        if (backup != null) {
                            backupValueText.text = "(${backup.originalValue})"
                            backupValueText.visibility = View.VISIBLE
                        } else {
                            backupValueText.visibility = View.GONE
                        }

                        // 类型简称和颜色
                        val valueType = item.displayValueType ?: DisplayValueType.DWORD
                        typeText.apply {
                            text = valueType.code
                            setTextColor(valueType.textColor)
                        }

                        // 内存范围简称和颜色 - 使用二分查找 O(log n) 替代线性查找 O(n)
                        val memoryRange = findMemoryRangeByAddress(item.address)
                        rangeText.apply {
                            text = memoryRange.code
                            setTextColor(memoryRange.color)
                        }
                    }
                }

                is FuzzySearchResultItem -> {
                    binding.apply {
                        // 地址
                        addressText.text = item.address.toString(16).uppercase()

                        // 当前值
                        valueText.text = item.value

                        // 备份值（旧值）
                        val backup = MemoryBackupManager.getBackup(item.address)
                        if (backup != null) {
                            backupValueText.text = "(${backup.originalValue})"
                            backupValueText.visibility = View.VISIBLE
                        } else {
                            backupValueText.visibility = View.GONE
                        }

                        // 类型简称和颜色
                        val valueType = item.displayValueType ?: DisplayValueType.DWORD
                        typeText.apply {
                            text = valueType.code
                            setTextColor(valueType.textColor)
                        }

                        // 内存范围简称和颜色 - 使用二分查找 O(log n) 替代线性查找 O(n)
                        val memoryRange = findMemoryRangeByAddress(item.address)
                        rangeText.apply {
                            text = memoryRange.code
                            setTextColor(memoryRange.color)
                        }
                    }
                }

                else -> {
                    binding.apply {
                        addressText.text = "0"
                        valueText.text = "0"
                        backupValueText.visibility = View.GONE
                        typeText.text = "?"
                        rangeText.text = "?"
                    }
                }
            }

            // Checkbox选中状态
            val isSelected = position in selectedPositions
            binding.checkbox.apply {
                setOnCheckedChangeListener(null) // 先移除监听器避免触发
                isChecked = isSelected
                setOnCheckedChangeListener { _, isChecked ->
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        if (isChecked) selectedPositions.add(pos) else selectedPositions.remove(pos)
                        updateItemBackground(isChecked)
                        onSelectionChanged(selectedPositions.size)
                    }
                }
            }
            updateItemBackground(isSelected)

            // 删除按钮
            binding.deleteButton.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    onItemDelete(results[pos])
                }
            }

            // Item点击事件
            binding.root.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    onItemClick(results[pos], pos)
                }
            }

            // Item长按事件
            binding.root.setOnLongClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                    onItemLongClick(results[pos], pos)
                } ?: false
            }
        }

        fun updateSelection(position: Int) {
            val isSelected = position in selectedPositions
            binding.checkbox.apply {
                setOnCheckedChangeListener(null)
                isChecked = isSelected
                updateItemBackground(isSelected)
                setOnCheckedChangeListener { _, isChecked ->
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        if (isChecked) selectedPositions.add(pos) else selectedPositions.remove(pos)
                        updateItemBackground(isChecked)
                        onSelectionChanged(selectedPositions.size)
                    }
                }
            }
        }

        private fun updateItemBackground(isSelected: Boolean) {
            binding.itemContainer.setBackgroundColor(
                if (isSelected) 0x33448AFF else 0x00000000
            )
        }
    }
}
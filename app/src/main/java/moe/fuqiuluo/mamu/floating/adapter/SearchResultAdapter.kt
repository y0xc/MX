package moe.fuqiuluo.mamu.floating.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.fuqiuluo.mamu.databinding.ItemSearchResultBinding
import moe.fuqiuluo.mamu.driver.ExactSearchResultItem
import moe.fuqiuluo.mamu.driver.SearchResultItem
import moe.fuqiuluo.mamu.floating.model.DisplayMemRegionEntry
import moe.fuqiuluo.mamu.floating.model.DisplayValueType
import moe.fuqiuluo.mamu.floating.model.MemoryRange

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
     * 全选
     */
    fun selectAll() {
        selectedPositions.clear()
        selectedPositions.addAll(results.indices)
        notifyDataSetChanged()
        onSelectionChanged(selectedPositions.size)
    }

    /**
     * 全不选
     */
    fun deselectAll() {
        selectedPositions.clear()
        notifyItemRangeChanged(0, results.size.coerceAtMost(50), PAYLOAD_SELECTION_CHANGED)
        onSelectionChanged(0)
    }

    /**
     * 反选
     */
    fun invertSelection() {
        val newSelection = mutableSetOf<Int>()
        for (i in results.indices) {
            if (i !in selectedPositions) {
                newSelection.add(i)
            }
        }
        selectedPositions.clear()
        selectedPositions.addAll(newSelection)
        notifyDataSetChanged()
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

    inner class ViewHolder(
        private val binding: ItemSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResultItem, position: Int) {
            when (item) {
                is ExactSearchResultItem -> {
                    // 地址
                    binding.addressText.text = String.format("%X", item.address)

                    // 当前值
                    binding.valueText.text = item.value

                    // 类型简称和颜色
                    val valueType = item.displayValueType ?: DisplayValueType.DWORD
                    binding.typeText.text = valueType.code
                    binding.typeText.setTextColor(valueType.textColor)

                    // 内存范围简称和颜色
                    val memoryRange = ranges?.find {
                        it.containsAddress(item.address)
                    }?.range ?: MemoryRange.O
                    binding.rangeText.text = memoryRange.code
                    binding.rangeText.setTextColor(memoryRange.color)
                }

                else -> {
                    binding.addressText.text = String.format("%X", 0L)
                    binding.valueText.text = "0"
                    binding.typeText.text = "?"
                    binding.rangeText.text = "?"
                }
            }

            // Checkbox选中状态
            val isSelected = position in selectedPositions
            binding.checkbox.isChecked = isSelected
            updateItemBackground(isSelected)

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (isChecked) {
                        selectedPositions.add(currentPosition)
                    } else {
                        selectedPositions.remove(currentPosition)
                    }
                    updateItemBackground(isChecked)
                    onSelectionChanged(selectedPositions.size)
                }
            }

            // 删除按钮
            binding.deleteButton.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemDelete(results[currentPosition])
                }
            }

            // Item点击事件
            binding.root.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClick(results[currentPosition], currentPosition)
                }
            }

            // Item长按事件
            binding.root.setOnLongClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemLongClick(results[currentPosition], currentPosition)
                } else {
                    false
                }
            }
        }

        fun updateSelection(position: Int) {
            val isSelected = position in selectedPositions
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.isChecked = isSelected
            updateItemBackground(isSelected)
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (isChecked) {
                        selectedPositions.add(currentPosition)
                    } else {
                        selectedPositions.remove(currentPosition)
                    }
                    updateItemBackground(isChecked)
                    onSelectionChanged(selectedPositions.size)
                }
            }
        }

        private fun updateItemBackground(isSelected: Boolean) {
            if (isSelected) {
                binding.itemContainer.setBackgroundColor(0x33448AFF)
            } else {
                binding.itemContainer.setBackgroundColor(0x00000000)
            }
        }
    }
}
package moe.fuqiuluo.mamu.floating.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import moe.fuqiuluo.mamu.databinding.ItemProcessListBinding
import moe.fuqiuluo.mamu.floating.model.DisplayProcessInfo
import moe.fuqiuluo.mamu.utils.ByteFormatUtils.formatBytes

class ProcessListAdapter(
    private val context: Context,
    private val processList: List<DisplayProcessInfo>
): BaseAdapter() {
    override fun getCount(): Int = processList.size
    override fun getItem(position: Int): Any = processList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = if (convertView == null) {
            ItemProcessListBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        } else {
            ItemProcessListBinding.bind(convertView)
        }

        val processInfo = processList[position]
        binding.processRss.text = formatBytes(processInfo.rss * 4096, 0)
        binding.processName.text = processInfo.validName
        binding.processDetails.text = "[${processInfo.pid}]"
        binding.processIcon.setImageDrawable(processInfo.icon)

        return binding.root
    }
}
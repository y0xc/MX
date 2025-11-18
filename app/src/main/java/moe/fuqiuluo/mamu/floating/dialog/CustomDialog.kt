package moe.fuqiuluo.mamu.floating.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.BaseAdapter
import moe.fuqiuluo.mamu.databinding.DialogProcessSelectionBinding

class CustomDialog(
    context: Context,
    private val title: String = "",
    private val adapter: BaseAdapter,
): BaseDialog(context) {
    var onItemClick: ((Int) -> Unit)? = null

    override fun setupDialog() {
        val binding = DialogProcessSelectionBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // 设置标题
        binding.dialogTitle.text = title

        // 设置自定义适配器
        binding.processList.adapter = adapter
        binding.processList.setOnItemClickListener { _, _, position, _ ->
            onItemClick?.invoke(position)
            dialog.dismiss()
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            onCancel?.invoke()
            dialog.dismiss()
        }
    }
}

fun Context.customDialog(
    title: String,
    adapter: BaseAdapter,
    onItemClick: (Int) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val dialog = CustomDialog(
        context = this,
        title = title,
        adapter = adapter,
    )
    dialog.onItemClick = onItemClick
    dialog.onCancel = onCancel
    dialog.show()
}

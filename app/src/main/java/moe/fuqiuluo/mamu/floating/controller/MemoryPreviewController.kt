package moe.fuqiuluo.mamu.floating.controller

import android.content.Context
import moe.fuqiuluo.mamu.databinding.FloatingMemoryPreviewLayoutBinding
import moe.fuqiuluo.mamu.widget.NotificationOverlay

class MemoryPreviewController(
    context: Context,
    binding: FloatingMemoryPreviewLayoutBinding,
    notification: NotificationOverlay
) : FloatingController<FloatingMemoryPreviewLayoutBinding>(context, binding, notification) {

    override fun initialize() {
        // TODO: 实现内存预览功能
    }
}
package moe.fuqiuluo.mamu.floating.ext

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.tencent.mmkv.MMKV
import moe.fuqiuluo.mamu.databinding.FloatingFullscreenLayoutBinding
import moe.fuqiuluo.mamu.ext.floatingOpacity

/**
 * 应用浮动窗口透明度设置
 */
fun FloatingFullscreenLayoutBinding.applyOpacity() {
    /**
     * 递归应用透明度到所有使用 bg_settings_card 背景的视图
     */
    fun applyOpacityToCards(parent: View, alpha: Int) {
        if (parent is ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)

                // 检查是否使用了卡片背景（通过tag或背景drawable判断）
                child.background?.let { drawable ->
                    // 如果有背景，应用透明度
                    if (child is LinearLayout) {
                        drawable.alpha = alpha
                    }
                }

                // 递归处理子视图
                applyOpacityToCards(child, alpha)
            }
        }
    }

    val config = MMKV.defaultMMKV()
    val opacity = config.floatingOpacity
    val alpha = (opacity * 255).toInt()

    // 应用到根布局背景
    rootLayout.background?.alpha = alpha

    // 应用到设置界面的所有卡片
    applyOpacityToCards(contentContainer, alpha)
}
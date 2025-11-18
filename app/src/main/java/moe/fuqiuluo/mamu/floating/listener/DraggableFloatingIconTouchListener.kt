package moe.fuqiuluo.mamu.floating.listener

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class DraggableFloatingIconTouchListener(
    private val floatingIconView: View,
    private val params: WindowManager.LayoutParams,
    private val windowManager: WindowManager,
    private var touchSlop: Int = 0,
    private val showFullscreen: () -> Unit,
): View.OnTouchListener {
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (!isDragging && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                    isDragging = true
                }

                if (isDragging) {
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    windowManager.updateViewLayout(floatingIconView, params)
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    showFullscreen()
                }
                true
            }

            else -> false
        }
    }
}
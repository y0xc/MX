package moe.fuqiuluo.mamu.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import moe.fuqiuluo.mamu.R
import androidx.core.content.withStyledAttributes

class BuiltinKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class KeyboardState {
        COLLAPSED,
        EXPANDED,
        FUNCTION
    }

    interface KeyboardListener {
        fun onKeyInput(key: String)
        fun onDelete()
        fun onSelectAll()
        fun onMoveLeft()
        fun onMoveRight()
        fun onHistory()
        fun onPaste()
    }

    var listener: KeyboardListener? = null
    private var currentState = KeyboardState.EXPANDED
    private var isPortrait = true

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.BuiltinKeyboard) {
                val defaultExpanded = getBoolean(R.styleable.BuiltinKeyboard_defaultExpanded, true)
                currentState =
                    if (defaultExpanded) KeyboardState.EXPANDED else KeyboardState.COLLAPSED
            }
        }
        buildKeyboard()
    }

    fun setState(state: KeyboardState) {
        if (currentState != state) {
            currentState = state
            buildKeyboard()
        }
    }

    fun setScreenOrientation(portrait: Boolean) {
        if (isPortrait != portrait) {
            isPortrait = portrait
            buildKeyboard()
        }
    }

    private fun buildKeyboard() {
        removeAllViews()

        val layoutId = getLayoutId()
        val view = LayoutInflater.from(context).inflate(layoutId, this, true)

        setupKeyListeners(view)
    }

    private fun getLayoutId(): Int {
        return if (isPortrait) {
            when (currentState) {
                KeyboardState.COLLAPSED -> R.layout.keyboard_portrait_collapsed
                KeyboardState.EXPANDED -> R.layout.keyboard_portrait_expanded
                KeyboardState.FUNCTION -> R.layout.keyboard_portrait_function
            }
        } else {
            when (currentState) {
                KeyboardState.COLLAPSED -> R.layout.keyboard_landscape_collapsed
                KeyboardState.EXPANDED -> R.layout.keyboard_landscape_expanded
                KeyboardState.FUNCTION -> R.layout.keyboard_landscape_function
            }
        }
    }

    private fun setupKeyListeners(view: View) {
        val keyMap = mapOf(
            R.id.key_1 to "1", R.id.key_2 to "2", R.id.key_3 to "3", R.id.key_4 to "4",
            R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7", R.id.key_8 to "8",
            R.id.key_9 to "9", R.id.key_0 to "0",
            R.id.key_a to "A", R.id.key_b to "B", R.id.key_c to "C", R.id.key_d to "D",
            R.id.key_e to "E", R.id.key_f to "F", R.id.key_g to "G",
            R.id.key_h to "h", R.id.key_q to "Q", R.id.key_r to "r", R.id.key_w to "W", R.id.key_x to "X",
            R.id.key_colon to ":", R.id.key_semicolon to ";", R.id.key_tilde to "~",
            R.id.key_dot to ".", R.id.key_minus to "-", R.id.key_comma to ",",
            R.id.key_equal to "=", R.id.key_plus to "+", R.id.key_and to "&",
            R.id.key_lparen to "(", R.id.key_rparen to ")", R.id.key_star to "*",
            R.id.key_slash to "/", R.id.key_pipe to "|", R.id.key_lt to "<", R.id.key_gt to ">",
            R.id.key_caret to "^", R.id.key_percent to "%", R.id.key_space to " ",
            R.id.key_quote to "'", R.id.key_backslash to "\\", R.id.key_hash to "#",
            R.id.key_lbracket to "[", R.id.key_rbracket to "]",
            R.id.key_lbrace to "{", R.id.key_rbrace to "}",
            R.id.key_dquote to "\""
        )

        keyMap.forEach { (id, key) ->
            view.findViewById<View>(id)?.setOnClickListener {
                listener?.onKeyInput(key)
            }
        }

        view.findViewById<View>(R.id.key_delete)?.setOnClickListener {
            listener?.onDelete()
        }

        view.findViewById<View>(R.id.key_select_all)?.setOnClickListener {
            listener?.onSelectAll()
        }

        view.findViewById<View>(R.id.key_move_left)?.setOnClickListener {
            listener?.onMoveLeft()
        }

        view.findViewById<View>(R.id.key_move_right)?.setOnClickListener {
            listener?.onMoveRight()
        }

        view.findViewById<View>(R.id.key_history)?.setOnClickListener {
            listener?.onHistory()
        }

        view.findViewById<View>(R.id.key_paste)?.setOnClickListener {
            listener?.onPaste()
        }

        view.findViewById<View>(R.id.key_expand)?.setOnClickListener {
            setState(KeyboardState.EXPANDED)
        }

        view.findViewById<View>(R.id.key_collapse)?.setOnClickListener {
            setState(KeyboardState.COLLAPSED)
        }

        view.findViewById<View>(R.id.key_function)?.setOnClickListener {
            setState(KeyboardState.FUNCTION)
        }

        view.findViewById<View>(R.id.key_number_panel)?.setOnClickListener {
            setState(KeyboardState.EXPANDED)
        }
    }
}

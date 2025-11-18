package moe.fuqiuluo.mamu.floating.model

import android.graphics.Color
import moe.fuqiuluo.mamu.R

/**
 * GameGuardian compatible value types for memory search
 */
enum class DisplayValueType(
    val code: String,
    val displayName: String,
    val rangeDescription: String,
    val iconRes: Int,
    val textColor: Int,
    val nativeId: Int
) {
    AUTO(
        code = "A",
        displayName = "Auto (-1.8e+308 - 1.8e+308)",
        rangeDescription = "输入从-1.8e+308到1.8e+308的值",
        iconRes = R.drawable.type_auto_24px,
        textColor = Color.WHITE,
        nativeId = 6
    ),
    DWORD(
        code = "D",
        displayName = "Dword (-2,147,483,648 - 4,294,967,295)",
        rangeDescription = "输入从-2,147,483,648到4,294,967,295的值",
        iconRes = R.drawable.type_integer_24px,
        textColor = 0xFF87CEEB.toInt(),
        nativeId = 2
    ),
    FLOAT(
        code = "F",
        displayName = "Float (-3.4e+38 - 3.4e+38)",
        rangeDescription = "输入从-3.4e+38到3.4e+38的值",
        iconRes = R.drawable.type_float_24px,
        textColor = 0xFFFF69B4.toInt(),
        nativeId = 4
    ),
    DOUBLE(
        code = "E",
        displayName = "Double (-1.8e+308 - 1.8e+308)",
        rangeDescription = "输入从-1.8e+308到1.8e+308的值",
        iconRes = R.drawable.type_float_24px,
        textColor = 0xFFFFFF00.toInt(),
        nativeId = 5
    ),
    WORD(
        code = "W",
        displayName = "Word (-32,768 - 65,535)",
        rangeDescription = "输入从-32,768到65,535的值",
        iconRes = R.drawable.type_integer_24px,
        textColor = 0xFF00CED1.toInt(),
        nativeId = 1
    ),
    BYTE(
        code = "B",
        displayName = "Byte (-128 - 255)",
        rangeDescription = "输入从-128到255的值",
        iconRes = R.drawable.type_integer_24px,
        textColor = 0xFFDA70D6.toInt(),
        nativeId = 0
    ),
    QWORD(
        code = "Q",
        displayName = "Qword (-9,223,372,036,854,775,808 - 18,446,744,073,709,551,615)",
        rangeDescription = "输入从-9,223,372,036,854,775,808到18,446,744,073,709,551,615的值",
        iconRes = R.drawable.type_integer_24px,
        textColor = 0xFF00BFFF.toInt(),
        nativeId = 3
    ),
    XOR(
        code = "X",
        displayName = "Xor (-2,147,483,648 - 4,294,967,295)",
        rangeDescription = "输入从-2,147,483,648到4,294,967,295的值",
        iconRes = R.drawable.type_xor_24px,
        textColor = 0xFF9370DB.toInt(),
        nativeId = 7
    ),
    UTF_8(
        code = "UTF-8",
        displayName = "文本 UTF-8",
        rangeDescription = "输入UTF-8编码的文本",
        iconRes = R.drawable.type_text_24px,
        textColor = Color.WHITE,
        nativeId = 100
    ),
    UTF_16LE(
        code = "UTF-16LE",
        displayName = "文本 UTF-16LE",
        rangeDescription = "输入UTF-16LE编码的文本",
        iconRes = R.drawable.type_text_24px,
        textColor = Color.WHITE,
        nativeId = 101
    ),
    HEX(
        code = "HEX",
        displayName = "HEX",
        rangeDescription = "输入十六进制值",
        iconRes = R.drawable.type_hex_24px,
        textColor = Color.WHITE,
        nativeId = 102
    ),
    HEX_MIXED(
        code = "HEX_MIXED",
        displayName = "HEX + UTF-8 + UTF-16LE",
        rangeDescription = "输入十六进制或文本值",
        iconRes = R.drawable.type_mixed_24px,
        textColor = Color.WHITE,
        nativeId = 103
    ),
    ARM(
        code = "ARM",
        displayName = "ARM",
        rangeDescription = "输入ARM指令",
        iconRes = R.drawable.type_code_24px,
        textColor = Color.WHITE,
        nativeId = 104
    ),
    ARM64(
        code = "ARM64",
        displayName = "ARM64",
        rangeDescription = "输入ARM64指令",
        iconRes = R.drawable.type_code_24px,
        textColor = Color.WHITE,
        nativeId = 105
    );

    companion object {
        fun fromCode(code: String): DisplayValueType? {
            return entries.find { it.code == code }
        }

        fun fromNativeId(id: Int): DisplayValueType? {
            return entries.find { it.nativeId == id }
        }
    }
}
package moe.fuqiuluo.mamu.floating.model

import moe.fuqiuluo.mamu.floating.model.MemoryRange

data class DisplayMemRegionEntry(
    val start: Long, val end: Long, val type: Int, val name: String, val range: MemoryRange
) {
    companion object {
        const val MEM_READABLE = 0x01
        const val MEM_WRITABLE = 0x02
        const val MEM_EXECUTABLE = 0x04
        const val MEM_SHARED = 0x08
    }

    val isReadable: Boolean
        get() = (type and MEM_READABLE) != 0

    val isWritable: Boolean
        get() = (type and MEM_WRITABLE) != 0

    val isExecutable: Boolean
        get() = (type and MEM_EXECUTABLE) != 0

    val isShared: Boolean
        get() = (type and MEM_SHARED) != 0

    val hasProt: Boolean
        get() = type != 0

    val nonProt: Boolean
        get() = !isReadable && !isWritable && !isExecutable // wtf?

    val permissionString: String
        get() = buildString {
            append(if (isReadable) 'r' else '-')
            append(if (isWritable) 'w' else '-')
            append(if (isExecutable) 'x' else '-')
            append(if (isShared) 's' else 'p')
        }

    val size: Long
        get() = end - start

    fun containsAddress(address: Long): Boolean {
        return address in start until end
    }

    override fun toString(): String {
        return "%s (0x%016X-0x%016X [%s] %s, size=%d)".format(
            range.code, start, end, permissionString, name, size
        )
    }
}
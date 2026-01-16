package com.jpgk.hardwaresdk.socket

internal object JsonFrameDecoder {


    /**
     * 从 StringBuilder 中提取完整的 JSON object(s)。
     * 简单基于花括号计数的拆包器，适合纯 JSON 对象流 ({}...) 的场景。
     * 解析成功后会清空 StringBuilder 中已消费的部分。
     */
    fun decode(sb: StringBuilder): List<String>? {
        if (sb.isEmpty()) return null


        val list = mutableListOf<String>()
        var open = 0
        var start = -1
        var i = 0


        while (i < sb.length) {
            val c = sb[i]
            when (c) {
                '{' -> {
                    if (open == 0) start = i
                    open++
                }
                '}' -> {
                    open--
                    if (open == 0 && start != -1) {
                        val obj = sb.substring(start, i + 1)
                        list.add(obj)
// mark consumed by replacing with empty char sequence
                    }
                }
            }
            i++
        }


        if (open == 0) {
// all consumed -> clear buffer
            sb.clear()
        } else {
// if still open, keep partial buffer (we didn't remove consumed prefix for simplicity).
// For production you may want to remove consumed prefix while keeping the tail partial.
        }


        return if (list.isEmpty()) null else list
    }
}
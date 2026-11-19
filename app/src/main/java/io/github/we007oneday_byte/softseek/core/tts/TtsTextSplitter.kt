package io.github.we007oneday_byte.softseek.core.tts

/**
 * 将模型回复切成适合 TTS 的短句：优先按中英文句末标点断开，
 * 超长无标点文本按逗号/空格或固定长度硬切，避免单次 utterance 过长。
 */
object TtsTextSplitter {
    private val sentenceEnds = setOf('。', '！', '？', '!', '?', '；', ';', '\n')
    private val softBreaks = charArrayOf('，', ',', '、', '：', ':', ' ')

    fun split(
        text: String,
        maxChars: Int = 80,
    ): List<String> {
        require(maxChars > 0) { "maxChars 必须大于 0" }
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        val buffer = StringBuilder()
        fun flush() {
            val sentence = buffer.toString().trim()
            buffer.clear()
            if (sentence.isNotEmpty()) appendChunks(sentence, maxChars, chunks)
        }
        normalized.forEach { char ->
            buffer.append(char)
            if (char in sentenceEnds) flush()
        }
        flush()
        return chunks
    }

    private fun appendChunks(
        sentence: String,
        maxChars: Int,
        out: MutableList<String>,
    ) {
        var rest = sentence.trim()
        while (rest.length > maxChars) {
            var cut = maxChars
            val soft = rest.lastIndexOfAny(softBreaks, cut - 1)
            if (soft > maxChars / 2) cut = soft + 1
            val head = rest.substring(0, cut).trim()
            if (head.isNotEmpty()) out.add(head)
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) out.add(rest)
    }
}

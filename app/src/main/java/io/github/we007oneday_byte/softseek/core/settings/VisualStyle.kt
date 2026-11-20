package io.github.we007oneday_byte.softseek.core.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 十套视觉主题。枚举顺序为任务书固定顺序，新增主题时直接追加在末尾。
 *
 * 旧版本使用全大写枚举名，@SerialName 负责保持旧设置的 JSON 兼容；
 * fromName 额外兼容直接以 name 保存的旧值。
 */
@Suppress("ktlint:standard:enum-entry-name-case")
@Serializable
enum class VisualStyle(
    val label: String,
    val description: String,
    val legacyName: String,
) {
    @SerialName("MINIMAL")
    Minimal("极简", "近白底、细线分隔、紧凑平铺，默认风格", "MINIMAL"),

    @SerialName("CLOUD")
    Cloud("云朵", "大圆角悬浮卡片，雾白与浅灰蓝，留白宽松", "CLOUD"),

    @SerialName("PUDDING")
    Pudding("布丁", "圆润堆叠卡片，奶油黄与焦糖棕，气泡近圆形", "PUDDING"),

    @SerialName("MONO")
    Mono("黑白", "纯黑纯白、高对比、无阴影", "MONO"),

    @SerialName("TAIJI")
    Taiji("太极", "对称构图、圆形分割、阴阳式明暗分区", "TAIJI"),

    @SerialName("HACKER")
    Hacker("黑客", "终端式等宽字体、网格扫描线、炭黑与荧光绿", "HACKER"),

    @SerialName("LINE")
    LineArt("线条", "线框卡片、细描边、透明填充", "LINE"),

    @SerialName("CANDY")
    Candy("糖果", "半透明玻璃卡片、糖果色渐变、光泽胶囊按钮", "CANDY"),

    @SerialName("NEON")
    Neon("霓虹", "深色网格、霓虹描边、发光圆环、胶囊按钮", "NEON"),

    @SerialName("PAPER_INK")
    PaperInk("纸墨", "纸张纹理、胶带便签卡片、手写感标题、印章式强调", "PAPER_INK"),
    ;

    companion object {
        fun fromName(name: String?): VisualStyle {
            val direct = runCatching { valueOf(name.orEmpty()) }.getOrNull()
            if (direct != null) return direct
            return entries.firstOrNull { it.legacyName == name } ?: Minimal
        }
    }
}

/**
 * 通话收音动画。逐帧 progress 由 Compose 动画驱动，不写回 ViewModel。
 */
@Serializable
enum class VoiceAnimationStyle(val label: String) {
    WAVE("波形"),
    PULSE("脉冲"),
    RING("环形"),
    PARTICLES("粒子"),
    SPECTRUM("频谱"),
    TYPEWRITER("打字机"),
    GLOW("光晕"),
    SCAN("扫描线"),
    ELASTIC("弹性字"),
    RIPPLES("涟漪"),
    ;

    companion object {
        fun fromName(name: String?): VoiceAnimationStyle =
            entries.firstOrNull { it.name == name } ?: WAVE
    }
}

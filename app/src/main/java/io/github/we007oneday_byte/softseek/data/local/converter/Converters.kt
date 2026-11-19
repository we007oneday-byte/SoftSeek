package io.github.we007oneday_byte.softseek.data.local.converter

import androidx.room.TypeConverter
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.ImageDetail
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun tagsToJson(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun jsonToTags(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    if (!array.isNull(index)) add(array.getString(index))
                }
            }
        }.getOrElse { emptyList() }
    }

    @TypeConverter
    fun roleToString(value: MessageRole): String = value.name

    @TypeConverter
    fun stringToRole(value: String): MessageRole = enumValue(value, MessageRole.USER)

    @TypeConverter
    fun statusToString(value: MessageStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): MessageStatus = enumValue(value, MessageStatus.COMPLETE)

    @TypeConverter
    fun attachmentTypeToString(value: AttachmentType): String = value.name

    @TypeConverter
    fun stringToAttachmentType(value: String): AttachmentType = enumValue(value, AttachmentType.FILE)

    @TypeConverter
    fun effortToString(value: ReasoningEffort): String = value.name

    @TypeConverter
    fun stringToEffort(value: String): ReasoningEffort = enumValue(value, ReasoningEffort.HIGH)

    @TypeConverter
    fun imageDetailToString(value: ImageDetail): String = value.name

    @TypeConverter
    fun stringToImageDetail(value: String): ImageDetail = enumValue(value, ImageDetail.AUTO)

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}

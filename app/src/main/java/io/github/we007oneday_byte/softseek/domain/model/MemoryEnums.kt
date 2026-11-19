package io.github.we007oneday_byte.softseek.domain.model

import kotlinx.serialization.Serializable

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
}

enum class MessageStatus {
    COMPLETE,
    FAILED,
    PARTIAL,
}

enum class AttachmentType {
    IMAGE,
    FILE,
}

@Serializable
enum class ReasoningEffort {
    NONE,
    LOW,
    HIGH,
    MAX,
}

@Serializable
enum class ImageDetail {
    AUTO,
    LOW,
    HIGH,
    ORIGINAL,
}

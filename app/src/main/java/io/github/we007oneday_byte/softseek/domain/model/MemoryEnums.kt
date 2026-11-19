package io.github.we007oneday_byte.softseek.domain.model

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

enum class ReasoningEffort {
    NONE,
    LOW,
    HIGH,
    MAX,
}

enum class ImageDetail {
    AUTO,
    LOW,
    HIGH,
    ORIGINAL,
}

package io.github.we007oneday_byte.softseek.domain.provider

import io.github.we007oneday_byte.softseek.domain.model.ChatError
import io.github.we007oneday_byte.softseek.domain.model.userMessage
import java.io.IOException

class AiProviderException(
    val error: ChatError,
    cause: Throwable? = null,
) : IOException(error.userMessage(), cause)

package io.github.we007oneday_byte.softseek.ui.components.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekButton

@Composable
fun CommonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SoftSeekButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = dimensionResource(R.dimen.button_min_height)),
        enabled = enabled,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

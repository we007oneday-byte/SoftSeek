package io.github.we007oneday_byte.softseek.ui.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(R.dimen.screen_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.softSeekColors.textSecondary,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.screen_padding)))
            Text(
                text = stringResource(R.string.about_version_value, AppConstants.APP_VERSION),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.item_spacing)))
            Text(
                text = stringResource(
                    R.string.about_license_line,
                    stringResource(R.string.about_license),
                    stringResource(R.string.about_license_value),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.softSeekColors.textSecondary,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.item_spacing)))
            Text(
                text = stringResource(R.string.about_project_value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.softSeekColors.textSecondary,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.screen_padding)))
            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.softSeekColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

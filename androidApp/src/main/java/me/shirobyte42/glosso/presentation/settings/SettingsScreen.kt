package me.shirobyte42.glosso.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.SUPPORTED_LANGUAGES

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (state.pendingLatinSwitch) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLatinWarning() },
            title = { Text(stringResource(R.string.settings_latin_warning_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.settings_latin_warning_body))
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmLatinWarning() }) {
                    Text(stringResource(R.string.settings_btn_latin_understand))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLatinWarning() }) {
                    Text(stringResource(R.string.btn_cancel_mixed))
                }
            }
        )
    }

    if (state.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetConfirmation() },
            title = { Text(stringResource(R.string.settings_reset_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmResetProgress() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_btn_reset_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetConfirmation() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Language section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_language), icon = Icons.Default.Language)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.settings_language_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.settings_language_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        SUPPORTED_LANGUAGES.forEach { lang ->
                            val isSelected = state.targetLanguage == lang.code
                            val suffix = if (lang.experimental) " ⚠" else ""
                            val label = "${lang.flag} ${lang.displayName}$suffix"
                            if (isSelected) {
                                Button(
                                    onClick = { viewModel.setTargetLanguage(lang.code) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setTargetLanguage(lang.code) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // UI language section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_ui_language), icon = Icons.Default.Translate)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.settings_ui_language_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.settings_ui_language_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val uiLangs = listOf(
                        "" to stringResource(R.string.settings_ui_lang_system),
                        "en" to stringResource(R.string.settings_ui_lang_en),
                        "es" to stringResource(R.string.settings_ui_lang_es),
                        "fr" to stringResource(R.string.settings_ui_lang_fr),
                        "de" to stringResource(R.string.settings_ui_lang_de),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        uiLangs.forEach { (tag, label) ->
                            val isSelected = state.uiLanguage == tag
                            if (isSelected) {
                                Button(
                                    onClick = { viewModel.setUiLanguage(tag) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(label, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setUiLanguage(tag) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Appearance section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_appearance), icon = Icons.Default.Palette)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.settings_theme_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.settings_theme_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            stringResource(R.string.settings_theme_system) to Icons.Default.BrightnessAuto,
                            stringResource(R.string.settings_theme_light) to Icons.Default.LightMode,
                            stringResource(R.string.settings_theme_dark) to Icons.Default.DarkMode
                        ).forEachIndexed { index, (label, icon) ->
                            val isSelected = state.themeMode == index
                            if (isSelected) {
                                Button(
                                    onClick = { viewModel.setThemeMode(index) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setThemeMode(index) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_playback), icon = Icons.Default.Tune)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_speed_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (state.playbackSpeed < 1.0f) stringResource(R.string.settings_speed_slow) else stringResource(R.string.settings_speed_normal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.playbackSpeed < 1.0f,
                            onCheckedChange = { slow ->
                                viewModel.setPlaybackSpeed(if (slow) 0.75f else 1.0f)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Learning section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_learning), icon = Icons.Default.School)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_ipa_label),
                        description = stringResource(R.string.settings_ipa_desc),
                        checked = state.isIpaVisible,
                        onCheckedChange = { viewModel.setIpaVisible(it) }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_translation_label),
                        description = stringResource(R.string.settings_translation_desc),
                        checked = state.isTranslationVisible,
                        onCheckedChange = { viewModel.setTranslationVisible(it) }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    SettingsActionRow(
                        label = stringResource(R.string.settings_tutorial_label),
                        description = stringResource(R.string.settings_tutorial_desc),
                        icon = Icons.Default.Info,
                        onClick = {
                            viewModel.resetTutorial()
                            onNavigateBack()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danger zone
            SettingsSectionHeader(text = stringResource(R.string.settings_section_data), icon = Icons.Default.Storage)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                )
            ) {
                SettingsActionRow(
                    label = stringResource(R.string.settings_reset_label),
                    description = stringResource(R.string.settings_reset_desc),
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = { viewModel.requestResetProgress() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legal section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_legal), icon = Icons.Default.Gavel)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    SettingsActionRow(
                        label = stringResource(R.string.settings_privacy_label),
                        description = stringResource(R.string.settings_privacy_desc),
                        icon = Icons.Default.PrivacyTip,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://glossostudio.com/privacy-policy/")))
                        }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    SettingsActionRow(
                        label = stringResource(R.string.settings_terms_label),
                        description = stringResource(R.string.settings_terms_desc),
                        icon = Icons.Default.Description,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://glossostudio.com/terms/")))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // About section
            SettingsSectionHeader(text = stringResource(R.string.settings_section_about), icon = Icons.Default.Info)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                SettingsActionRow(
                    label = stringResource(R.string.settings_about_label),
                    description = stringResource(R.string.settings_about_desc),
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    description: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

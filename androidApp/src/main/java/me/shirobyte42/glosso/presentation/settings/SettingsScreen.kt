package me.shirobyte42.glosso.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.SUPPORTED_LANGUAGES
import me.shirobyte42.glosso.presentation.components.GlossoCard
import me.shirobyte42.glosso.presentation.components.GlossoSectionLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)?,
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (state.pendingLatinSwitch) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLatinWarning() },
            title = { Text(stringResource(R.string.settings_latin_warning_title), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.settings_latin_warning_body)) },
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
            title = { Text(stringResource(R.string.settings_reset_confirm_title), fontWeight = FontWeight.SemiBold) },
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsGroup(
                title = stringResource(R.string.settings_section_language),
                icon = Icons.Default.Language
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        SUPPORTED_LANGUAGES.forEach { lang ->
                            val isSelected = state.targetLanguage == lang.code
                            val suffix = if (lang.experimental) " ⚠" else ""
                            ChoiceButton(
                                label = "${lang.flag} ${lang.displayName}$suffix",
                                selected = isSelected,
                                onClick = { viewModel.setTargetLanguage(lang.code) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_section_ui_language),
                icon = Icons.Default.Translate
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                            ChoiceButton(
                                label = label,
                                selected = state.uiLanguage == tag,
                                onClick = { viewModel.setUiLanguage(tag) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_section_appearance),
                icon = Icons.Default.Palette
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            stringResource(R.string.settings_theme_system) to Icons.Default.BrightnessAuto,
                            stringResource(R.string.settings_theme_light) to Icons.Default.LightMode,
                            stringResource(R.string.settings_theme_dark) to Icons.Default.DarkMode
                        ).forEachIndexed { index, (label, icon) ->
                            ChoiceButton(
                                label = label,
                                selected = state.themeMode == index,
                                onClick = { viewModel.setThemeMode(index) },
                                icon = icon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SettingsGroup(
                title = stringResource(R.string.settings_section_playback),
                icon = Icons.Default.Tune
            ) {
                SettingsToggleRow(
                    label = stringResource(R.string.settings_speed_label),
                    description = if (state.playbackSpeed < 1.0f)
                        stringResource(R.string.settings_speed_slow)
                    else
                        stringResource(R.string.settings_speed_normal),
                    checked = state.playbackSpeed < 1.0f,
                    onCheckedChange = { slow ->
                        viewModel.setPlaybackSpeed(if (slow) 0.75f else 1.0f)
                    }
                )
            }

            SettingsGroup(
                title = stringResource(R.string.settings_section_learning),
                icon = Icons.Default.School
            ) {
                SettingsToggleRow(
                    label = stringResource(R.string.settings_ipa_label),
                    description = stringResource(R.string.settings_ipa_desc),
                    checked = state.isIpaVisible,
                    onCheckedChange = { viewModel.setIpaVisible(it) }
                )
                SettingsDivider()
                SettingsToggleRow(
                    label = stringResource(R.string.settings_translation_label),
                    description = stringResource(R.string.settings_translation_desc),
                    checked = state.isTranslationVisible,
                    onCheckedChange = { viewModel.setTranslationVisible(it) }
                )
                SettingsDivider()
                SettingsActionRow(
                    label = stringResource(R.string.settings_tutorial_label),
                    description = stringResource(R.string.settings_tutorial_desc),
                    icon = Icons.Default.Info,
                    onClick = {
                        viewModel.resetTutorial()
                        onNavigateBack?.invoke()
                    }
                )
            }

            SettingsGroup(
                title = stringResource(R.string.settings_section_data),
                icon = Icons.Default.Storage
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

            SettingsGroup(
                title = stringResource(R.string.settings_section_legal),
                icon = Icons.Default.Gavel
            ) {
                SettingsActionRow(
                    label = stringResource(R.string.settings_privacy_label),
                    description = stringResource(R.string.settings_privacy_desc),
                    icon = Icons.Default.PrivacyTip,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://glossostudio.com/privacy-policy/")))
                    }
                )
                SettingsDivider()
                SettingsActionRow(
                    label = stringResource(R.string.settings_terms_label),
                    description = stringResource(R.string.settings_terms_desc),
                    icon = Icons.Default.Description,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://glossostudio.com/terms/")))
                    }
                )
            }

            SettingsGroup(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Default.Info
            ) {
                SettingsActionRow(
                    label = stringResource(R.string.settings_about_label),
                    description = stringResource(R.string.settings_about_desc),
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(116.dp))
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            GlossoSectionLabel(text = title)
        }
        GlossoCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = contentPadding
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = contentPadding
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
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
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    description: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = labelColor)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

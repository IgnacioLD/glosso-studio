package me.shirobyte42.glosso.presentation.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import me.shirobyte42.glosso.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.about_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Hero — replicate adaptive icon: gradient background + foreground vector
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .then(
                        Modifier.border(
                            width = 1.dp,
                            color = Color.LightGray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.about_app_logo_desc),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    stringResource(R.string.about_version),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // How It Works
            AboutSectionHeader(stringResource(R.string.about_section_how_it_works))
            Spacer(modifier = Modifier.height(12.dp))
            HowItWorksCard()

            Spacer(modifier = Modifier.height(24.dp))

            // Features
            AboutSectionHeader(stringResource(R.string.about_section_features))
            Spacer(modifier = Modifier.height(12.dp))
            FeatureGrid()

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy
            AboutSectionHeader(stringResource(R.string.about_section_privacy))
            Spacer(modifier = Modifier.height(12.dp))
            AboutCard(
                icon = Icons.Default.Lock,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.about_privacy_title),
                body = stringResource(R.string.about_privacy_body)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Open Source
            AboutSectionHeader(stringResource(R.string.about_section_source_code))
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.about_source_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.about_source_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinkChip(
                            label = stringResource(R.string.about_link_source_code),
                            icon = Icons.Default.OpenInNew,
                            onClick = { uriHandler.openUri("https://gitlab.com/shirobyte421/glosso-studio") }
                        )
                        LinkChip(
                            label = stringResource(R.string.about_link_report_issue),
                            icon = Icons.Default.BugReport,
                            onClick = { uriHandler.openUri("https://gitlab.com/shirobyte421/glosso-studio/-/issues") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Credits
            AboutSectionHeader(stringResource(R.string.about_section_credits))
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.about_credits_intro), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                    CreditItem(
                        title = "Allosaurus",
                        subtitle = stringResource(R.string.about_credit_allosaurus_subtitle),
                        url = "https://github.com/xinjli/allosaurus",
                        uriHandler = uriHandler
                    )
                    CreditItem(
                        title = stringResource(R.string.about_credit_wav2vec2_title),
                        subtitle = stringResource(R.string.about_credit_wav2vec2_subtitle),
                        url = "https://huggingface.co/facebook/wav2vec2-base",
                        uriHandler = uriHandler
                    )
                    CreditItem(
                        title = "Qwen3-TTS",
                        subtitle = stringResource(R.string.about_credit_qwen_subtitle),
                        url = "https://github.com/QwenLM/Qwen3-TTS",
                        uriHandler = uriHandler
                    )
                    CreditItem(
                        title = "ai-pronunciation-trainer",
                        subtitle = stringResource(R.string.about_credit_aipron_subtitle),
                        url = "https://github.com/Thiagohgl/ai-pronunciation-trainer",
                        uriHandler = uriHandler
                    )
                    CreditItem(
                        title = "ONNX Runtime",
                        subtitle = stringResource(R.string.about_credit_onnx_subtitle),
                        url = "https://github.com/microsoft/onnxruntime",
                        uriHandler = uriHandler
                    )
                    CreditItem(
                        title = "Koin",
                        subtitle = stringResource(R.string.about_credit_koin_subtitle),
                        url = "https://insert-koin.io",
                        uriHandler = uriHandler
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Libraries
            AboutSectionHeader(stringResource(R.string.about_section_libraries))
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    LibraryItem("Kotlin", "JetBrains", "Apache-2.0")
                    LibraryItem("Jetpack Compose", "Google", "Apache-2.0")
                    LibraryItem("AndroidX Core KTX", "Google", "Apache-2.0")
                    LibraryItem("AndroidX Activity", "Google", "Apache-2.0")
                    LibraryItem("AndroidX Navigation", "Google", "Apache-2.0")
                    LibraryItem("Room", "Google", "Apache-2.0")
                    LibraryItem("Koin", "insert-koin.io", "Apache-2.0")
                    LibraryItem("Ktor", "JetBrains", "Apache-2.0")
                    LibraryItem("kotlinx.serialization", "JetBrains", "Apache-2.0")
                    LibraryItem("Lottie", "Airbnb", "Apache-2.0")
                    LibraryItem("ONNX Runtime", "Microsoft", "MIT")
                    LibraryItem("Allosaurus", "xinjli", "GPL-3.0", isLast = true)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.about_team_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                            Text(
                                stringResource(R.string.about_team_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinkChip(
                            label = stringResource(R.string.about_link_website),
                            icon = Icons.Default.Language,
                            onClick = { uriHandler.openUri("https://glossostudio.com") }
                        )
                        LinkChip(
                            label = stringResource(R.string.about_link_gitlab),
                            icon = Icons.Default.Code,
                            onClick = { uriHandler.openUri("https://gitlab.com/shirobyte421") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AboutSectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = Color.Gray,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun AboutCard(icon: ImageVector, iconTint: Color, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StepItem(
                number = "1",
                title = stringResource(R.string.about_step1_title),
                description = stringResource(R.string.about_step1_desc)
            )
            StepDivider()
            StepItem(
                number = "2",
                title = stringResource(R.string.about_step2_title),
                description = stringResource(R.string.about_step2_desc)
            )
            StepDivider()
            StepItem(
                number = "3",
                title = stringResource(R.string.about_step3_title),
                description = stringResource(R.string.about_step3_desc)
            )
            StepDivider()
            StepItem(
                number = "4",
                title = stringResource(R.string.about_step4_title),
                description = stringResource(R.string.about_step4_desc)
            )
            StepDivider()
            StepItem(
                number = "5",
                title = stringResource(R.string.about_step5_title),
                description = stringResource(R.string.about_step5_desc)
            )
        }
    }
}

@Composable
private fun StepItem(number: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StepDivider() {
    Box(modifier = Modifier.padding(start = 13.dp).width(2.dp).height(8.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
}

@Composable
private fun FeatureGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureChip(modifier = Modifier.weight(1f), icon = Icons.Default.Mic, label = stringResource(R.string.about_feature_offline))
            FeatureChip(modifier = Modifier.weight(1f), icon = Icons.Default.School, label = stringResource(R.string.about_feature_cefr))
            FeatureChip(modifier = Modifier.weight(1f), icon = Icons.Default.Category, label = stringResource(R.string.about_feature_topic))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureChip(modifier = Modifier.weight(1f), icon = Icons.Default.Layers, label = stringResource(R.string.about_feature_batch))
            FeatureChip(modifier = Modifier.weight(1f), icon = Icons.Default.Language, label = stringResource(R.string.about_feature_multilang))
            FeatureChip(modifier = Modifier.weight(1f), icon = Icons.Default.Star, label = stringResource(R.string.about_feature_mastery))
        }
    }
}

@Composable
private fun FeatureChip(modifier: Modifier = Modifier, icon: ImageVector, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun LinkChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LibraryItem(name: String, author: String, license: String, isLast: Boolean = false) {
    val licenseColor = when {
        license.startsWith("GPL") -> MaterialTheme.colorScheme.error
        license == "MIT" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(author, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Surface(
                color = licenseColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    license,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = licenseColor
                )
            }
        }
        if (!isLast) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun CreditItem(title: String, subtitle: String, url: String, uriHandler: androidx.compose.ui.platform.UriHandler) {
    val annotated = buildAnnotatedString {
        pushStringAnnotation("URL", url)
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
            append(title)
        }
        pop()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            androidx.compose.foundation.text.ClickableText(
                text = annotated,
                style = MaterialTheme.typography.bodyMedium,
                onClick = { offset ->
                    annotated.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

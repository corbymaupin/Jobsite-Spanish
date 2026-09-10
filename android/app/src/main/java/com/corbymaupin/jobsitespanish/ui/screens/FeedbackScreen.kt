package com.corbymaupin.jobsitespanish.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteText

private enum class FeedbackCategory(val label: String) {
    Bug("Bug"),
    Idea("Idea"),
    Praise("Praise")
}

private const val FEEDBACK_TO = "james.corby.maupin@gmail.com"
private const val FEEDBACK_SUBJECT = "[Jobsite Spanish Feedback]"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen() {
    val context = LocalContext.current
    var category by remember { mutableStateOf(FeedbackCategory.Idea) }
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }
    val sdk = Build.VERSION.SDK_INT

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Feedback", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Send a bug report, idea, or praise. Opens your email app with a prefilled message — nothing leaves the device until you hit send.",
            color = JobsiteMuted,
            fontSize = 13.sp
        )

        Text("Category", color = JobsiteMuted, fontSize = 13.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeedbackCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JobsiteAccent.copy(alpha = 0.25f),
                        selectedLabelColor = JobsiteAccent
                    )
                )
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text("Message") },
            placeholder = { Text("What should we know?") },
            colors = feedbackFieldColors()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your email (optional)") },
            placeholder = { Text("so we can reply") },
            singleLine = true,
            colors = feedbackFieldColors()
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                val body = buildFeedbackBody(category.label, message, email, appVersion, sdk)
                openFeedbackEmail(context, body)
            },
            enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = JobsiteAccent)
        ) {
            Text("Email feedback", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = {
                val body = buildFeedbackBody(category.label, message, email, appVersion, sdk)
                copyFeedback(context, body)
                Toast.makeText(context, "Feedback copied", Toast.LENGTH_SHORT).show()
            },
            enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy feedback text")
        }

        Text(
            "Goes to $FEEDBACK_TO · Ada reviews that inbox",
            color = JobsiteMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun feedbackFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JobsiteAccent,
    unfocusedBorderColor = JobsiteMuted.copy(alpha = 0.4f),
    focusedLabelColor = JobsiteAccent,
    unfocusedLabelColor = JobsiteMuted,
    cursorColor = JobsiteAccent,
    focusedTextColor = JobsiteText,
    unfocusedTextColor = JobsiteText
)

private fun buildFeedbackBody(
    category: String,
    message: String,
    replyEmail: String,
    appVersion: String,
    sdk: Int
): String = buildString {
    appendLine("Category: $category")
    appendLine("App version: $appVersion")
    appendLine("Android SDK: $sdk")
    if (replyEmail.isNotBlank()) appendLine("Reply-to: $replyEmail")
    appendLine()
    appendLine(message.trim())
}

private fun copyFeedback(context: Context, body: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Jobsite Spanish feedback", body))
}

private fun openFeedbackEmail(context: Context, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$FEEDBACK_TO")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_TO))
        putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_SUBJECT)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Send feedback"))
    } catch (_: ActivityNotFoundException) {
        copyFeedback(context, body)
        Toast.makeText(
            context,
            "No email app found — feedback copied. Paste to $FEEDBACK_TO",
            Toast.LENGTH_LONG
        ).show()
    }
}

// app/src/main/java/com/corbymaupin/jobsitespanish/ui/screens/HomeScreen.kt
package com.corbymaupin.jobsitespanish.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corbymaupin.jobsitespanish.ui.AppViewModel
import com.corbymaupin.jobsitespanish.ui.LearningDirection
import com.corbymaupin.jobsitespanish.ui.StudyPhase
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteCard
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteText

private val LobbyMaxWidth = 600.dp


/**
 * The app's starting place (start destination). Setup scrolls at the top; the Start lesson /
 * Continue session actions are pinned above the bottom bar so they are always reachable.
 *
 * Requires the 1.0.3 AppViewModel: learningDirection, setLearningDirection, study.ready.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, onOpenStudy: () -> Unit) {
    val study by vm.study.collectAsState()
    val stats by vm.stats.collectAsState()
    val trades by vm.trades.collectAsState()
    val direction by vm.learningDirection.collectAsState()

    val lessonInProgress = study.phase == StudyPhase.Session
    val showContinue = lessonInProgress || study.canResume

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // ---- Setup (scrolls if the trade list is long) ----
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = LobbyMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 16.dp)
        ) {
            Text(
                "Jobsite Spanish",
                style = MaterialTheme.typography.headlineLarge,

                fontWeight = FontWeight.Bold,
                color = JobsiteText
            )
            Spacer(Modifier.height(6.dp))
            Text(
                statusLine(
                    ready = study.ready,
                    due = study.dueCount,
                    fresh = study.newPoolCount,
                    trade = study.trade,
                    streak = stats.streak
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = JobsiteMuted
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Learning direction")
            Spacer(Modifier.height(10.dp))
            DirectionToggle(selected = direction, onSelect = vm::setLearningDirection)
            Spacer(Modifier.height(8.dp))
            Text(
                "Cards show ${direction.promptLanguage}. Reveal plays the ${direction.answerLanguage} answer aloud.",
                color = JobsiteMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Focus a trade")
            Spacer(Modifier.height(10.dp))
            FlowRow(

                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trades.forEach { t ->
                    FilterChip(
                        selected = study.trade == t,
                        onClick = { vm.setStudyTrade(t) },
                        label = { Text(t) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JobsiteAccent.copy(alpha = 0.25f),
                            selectedLabelColor = JobsiteAccent
                        )
                    )
                }
            }
            if (study.trade != "All") {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        vm.startSession(categoryMode = true)
                        onOpenStudy()
                    },
                    enabled = study.ready
                ) {
                    Text("Practice ${study.trade} (no SRS)")
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = JobsiteCard)

        // ---- Actions (pinned, always visible) ----

        Column(
            Modifier
                .widthIn(max = LobbyMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    vm.startSession(categoryMode = false)
                    onOpenStudy()
                },
                enabled = study.ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JobsiteAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start lesson", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            if (showContinue) {
                OutlinedButton(
                    onClick = {
                        // An in-memory lesson just needs the Study tab; a saved one is resumed first.
                        if (!lessonInProgress) vm.resumeSession()
                        onOpenStudy()

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, JobsiteAccent)
                ) {
                    Text(
                        if (lessonInProgress && study.remaining > 0) {
                            "Continue session · ${study.remaining} left"
                        } else {
                            "Continue session"
                        },
                        color = JobsiteAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = JobsiteMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

private fun statusLine(ready: Boolean, due: Int, fresh: Int, trade: String, streak: Int): AnnotatedString =
    buildAnnotatedString {
        if (!ready) {
            append("Loading your deck…")
        } else {
            withStyle(SpanStyle(color = JobsiteAccent, fontWeight = FontWeight.SemiBold)) {

                append(due.toString())
            }
            append(" due, $fresh new to learn ")
            append(if (trade == "All") "across all trades." else "in $trade.")
            append(" ")
            append(if (streak > 0) "$streak-day streak." else "Study today to start a streak.")
        }
    }

@Composable
private fun DirectionToggle(selected: LearningDirection, onSelect: (LearningDirection) -> Unit) {
    val outer = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(outer)
            .background(JobsiteCard)
            .border(1.dp, JobsiteMuted.copy(alpha = 0.16f), outer)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LearningDirection.entries.forEach { option ->
            DirectionSegment(
                option = option,
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun DirectionSegment(
    option: LearningDirection,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val container by animateColorAsState(
        targetValue = if (selected) JobsiteAccent else Color.Transparent,
        label = "directionSegmentContainer"
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) onAccent else JobsiteText,
        label = "directionSegmentTitle"
    )
    val captionColor by animateColorAsState(
        targetValue = if (selected) onAccent.copy(alpha = 0.75f) else JobsiteMuted,
        label = "directionSegmentCaption"
    )
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            option.label,
            color = titleColor,

            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${option.promptLanguage} → ${option.answerLanguage}",
            color = captionColor,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.semantics {
                contentDescription = "${option.promptLanguage} to ${option.answerLanguage}"
            }
        )
    }
}


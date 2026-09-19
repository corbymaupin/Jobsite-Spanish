// app/src/main/java/com/corbymaupin/jobsitespanish/ui/screens/StudyScreen.kt
package com.corbymaupin.jobsitespanish.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corbymaupin.jobsitespanish.data.DeckCard
import com.corbymaupin.jobsitespanish.ui.AppViewModel
import com.corbymaupin.jobsitespanish.ui.LearningDirection
import com.corbymaupin.jobsitespanish.ui.StudyPhase
import com.corbymaupin.jobsitespanish.ui.StudyUiState
import com.corbymaupin.jobsitespanish.ui.components.FlipCard
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteBad
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteCard
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted

/** Fixed card height: identical for front and back, so revealing can never resize the card. */
private val CardHeight = 320.dp

/** Reserved action area under the card; fits both "Show answer" and the grade row + hear button. */
private val FooterMinHeight = 120.dp

/**
 * Lesson workspace. The lobby (direction, trades, Start lesson) lives on HomeScreen.
 */
@Composable
fun StudyScreen(vm: AppViewModel, onGoHome: () -> Unit) {
    val state by vm.study.collectAsState()
    val direction by vm.learningDirection.collectAsState()

    when (state.phase) {
        StudyPhase.Home -> NoActiveLesson(onGoHome = onGoHome)

        StudyPhase.Session -> LessonSession(

            state = state,
            direction = direction,
            onReveal = { if (!state.revealed) vm.reveal() },
            onGrade = { right -> vm.grade(right) },
            onHear = { vm.hearAnswer() },
            onEnd = { vm.endSession() }
        )

        StudyPhase.Done -> LessonDone(
            state = state,
            onKeepGoing = { vm.startSession() },
            onBackToHome = {
                vm.backToHome()
                onGoHome()
            }
        )
    }
}

@Composable
private fun NoActiveLesson(onGoHome: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Study",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold

        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Lessons start on Home",
            color = JobsiteMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGoHome,
            colors = ButtonDefaults.buttonColors(
                containerColor = JobsiteAccent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Go to Home", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LessonSession(
    state: StudyUiState,
    direction: LearningDirection,
    onReveal: () -> Unit,
    onGrade: (Boolean) -> Unit,
    onHear: () -> Unit,
    onEnd: () -> Unit
) {
    val card: DeckCard? = state.current
    val prompt = card?.let { direction.promptOf(it) } ?: ""
    val answer = card?.let { direction.answerOf(it) } ?: ""

    val region = card?.region ?: ""

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val trade = card?.trade ?: ""
            Text(
                if (state.isNew) "New · $trade" else trade,
                color = if (state.isNew) JobsiteAccent else JobsiteMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (card == null) "" else "${state.remaining} left · ✓${state.right} ✗${state.wrong}",
                color = JobsiteMuted,
                maxLines = 1
            )
        }

        FlipCard(
            flipped = state.revealed,

            onClick = onReveal,
            resetKey = card?.id,
            containerColor = JobsiteCard,
            onClickLabel = "Show answer",
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight),
            front = {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        prompt,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    if (card != null) {
                        Spacer(Modifier.height(24.dp))
                        Text("Tap to show answer", color = JobsiteMuted)
                    }
                }
            },
            back = {
                Column(
                    Modifier

                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        prompt,
                        color = JobsiteMuted,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        answer,
                        color = JobsiteAccent,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    if (region.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Also: $region",
                            color = JobsiteMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }
        )

        // Grades live outside the flipping card, in a reserved area that fits both states.
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = FooterMinHeight)
        ) {
            if (!state.revealed) {
                Button(
                    onClick = onReveal,
                    enabled = card != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JobsiteAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Show answer", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Button(
                            onClick = { onGrade(false) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JobsiteBad)
                        ) {
                            Text("Missed it", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onGrade(true) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JobsiteAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Knew it", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(
                        onClick = onHear,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))

                        Text(direction.hearAnswerLabel)
                    }
                }
            }
        }

        TextButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
            Text("End session", color = JobsiteMuted)
        }
    }
}

@Composable
private fun LessonDone(
    state: StudyUiState,
    onKeepGoing: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Session complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        val total = state.right + state.wrong

        val pct = if (total == 0) 0 else (state.right * 100 / total)
        Text(
            "Right ${state.right}  ·  Missed ${state.wrong}  ·  $pct%",
            color = JobsiteMuted
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onKeepGoing,
            colors = ButtonDefaults.buttonColors(
                containerColor = JobsiteAccent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Keep going", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onBackToHome) {
            Text("Back to Home")
        }
    }
}


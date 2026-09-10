package com.corbymaupin.jobsitespanish.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corbymaupin.jobsitespanish.ui.AppViewModel
import com.corbymaupin.jobsitespanish.ui.StudyPhase
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteBad
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteCard
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyScreen(vm: AppViewModel) {
    val state by vm.study.collectAsState()
    val trades by vm.trades.collectAsState()

    when (state.phase) {
        StudyPhase.Home -> Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Study", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Due ${state.dueCount} · New pool ${state.newPoolCount}",
                color = JobsiteMuted
            )
            Text("Focus a trade", color = JobsiteMuted, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                trades.forEach { t ->
                    FilterChip(
                        selected = state.trade == t,
                        onClick = { vm.setStudyTrade(t) },
                        label = { Text(t) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JobsiteAccent.copy(alpha = 0.25f),
                            selectedLabelColor = JobsiteAccent
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.startSession(categoryMode = false) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = JobsiteAccent)
            ) {
                Text(
                    if (state.dueCount + state.newPoolCount == 0) "Nothing due"
                    else "Start · ${state.dueCount} due",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.trade != "All") {
                OutlinedButton(
                    onClick = { vm.startSession(categoryMode = true) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Practice ${state.trade} (no SRS)") }
            }
            if (state.canResume) {
                OutlinedButton(
                    onClick = { vm.resumeSession() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue session") }
            }
        }

        StudyPhase.Session -> {
            val card = state.current
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (state.isNew) "New · ${card?.trade ?: ""}" else (card?.trade ?: ""),
                        color = if (state.isNew) JobsiteAccent else JobsiteMuted
                    )
                    Text("${state.remaining} left · ✓${state.right} ✗${state.wrong}", color = JobsiteMuted)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(JobsiteCard, RoundedCornerShape(16.dp))
                        .clickable { if (!state.revealed) vm.reveal() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val prompt = if (state.promptEnFirst) card?.en else card?.es
                        val answer = if (state.promptEnFirst) card?.es else card?.en
                        Text(
                            prompt ?: "",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        if (state.revealed) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                answer ?: "",
                                fontSize = 26.sp,
                                color = JobsiteAccent,
                                textAlign = TextAlign.Center
                            )
                            if (!card?.region.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(card?.region ?: "", color = JobsiteMuted, fontSize = 13.sp)
                            }
                        } else {
                            Spacer(Modifier.height(24.dp))
                            Text("Tap to show answer", color = JobsiteMuted)
                        }
                    }
                }
                if (!state.revealed) {
                    Button(
                        onClick = { vm.reveal() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = JobsiteAccent)
                    ) { Text("Show answer", color = MaterialTheme.colorScheme.onPrimary) }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { vm.grade(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = JobsiteBad)
                        ) { Text("Missed it") }
                        Button(
                            onClick = { vm.grade(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = JobsiteAccent)
                        ) { Text("Knew it", color = MaterialTheme.colorScheme.onPrimary) }
                    }
                    TextButton(onClick = { vm.hearCurrentSpanish() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Hear Spanish")
                    }
                }
                TextButton(onClick = { vm.endSession() }, modifier = Modifier.fillMaxWidth()) {
                    Text("End session", color = JobsiteMuted)
                }
            }
        }

        StudyPhase.Done -> Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Session complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            val total = state.right + state.wrong
            val pct = if (total == 0) 0 else (state.right * 100 / total)
            Text("Right ${state.right}  ·  Missed ${state.wrong}  ·  $pct%", color = JobsiteMuted)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.startSession() },
                colors = ButtonDefaults.buttonColors(containerColor = JobsiteAccent)
            ) { Text("Keep going", color = MaterialTheme.colorScheme.onPrimary) }
            TextButton(onClick = { vm.backToHome() }) { Text("Back to start") }
        }
    }
}

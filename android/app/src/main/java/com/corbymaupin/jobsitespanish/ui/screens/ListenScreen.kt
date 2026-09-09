package com.corbymaupin.jobsitespanish.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteCard
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListenScreen(vm: AppViewModel) {
    val state by vm.listen.collectAsState()
    val trades by vm.trades.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Listen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Hands-free exposure — does not move Leitner boxes.", color = JobsiteMuted, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            trades.forEach { t ->
                FilterChip(
                    selected = state.trade == t,
                    onClick = { vm.setListenTrade(t) },
                    label = { Text(t) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JobsiteAccent.copy(alpha = 0.25f),
                        selectedLabelColor = JobsiteAccent
                    )
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(JobsiteCard, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.sideLabel, color = JobsiteMuted)
                Spacer(Modifier.height(12.dp))
                Text(
                    state.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = if (state.sideLabel == "Spanish") JobsiteAccent else MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                if (state.total > 0) {
                    Text("${state.cardTrade} · ${state.index} of ${state.total}", color = JobsiteMuted)
                }
            }
        }
        Button(
            onClick = { vm.toggleListen() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = JobsiteAccent)
        ) {
            Text(if (state.playing) "Pause" else "Play", color = MaterialTheme.colorScheme.onPrimary)
        }
        OutlinedButton(
            onClick = { vm.listenAdvance() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Next side / card") }
    }
}

package com.corbymaupin.jobsitespanish.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corbymaupin.jobsitespanish.ui.AppViewModel
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteCard
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted

@Composable
fun StatsScreen(vm: AppViewModel) {
    val s by vm.stats.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Stats", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("Streak", "${s.streak}d", Modifier.weight(1f))
            Kpi("Started", "${s.introduced}", Modifier.weight(1f))
            Kpi("Due", "${s.due}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("Mastered", "${s.mastered}", Modifier.weight(1f))
            Kpi("Deck", "${s.total}", Modifier.weight(1f))
        }
        Text("Where your cards sit", color = JobsiteMuted, fontSize = 13.sp)
        val max = (s.boxCounts.maxOrNull() ?: 1).coerceAtLeast(1)
        Row(
            Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            s.boxCounts.forEachIndexed { i, n ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$n", fontSize = 12.sp, color = JobsiteMuted)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(((n.toFloat() / max) * 80).dp.coerceAtLeast(4.dp))
                            .background(JobsiteAccent.copy(alpha = 0.35f + i * 0.12f), RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("B${i + 1}", fontSize = 12.sp, color = JobsiteMuted)
                }
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(JobsiteCard, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = JobsiteAccent)
        Text(label, color = JobsiteMuted, fontSize = 12.sp)
    }
}

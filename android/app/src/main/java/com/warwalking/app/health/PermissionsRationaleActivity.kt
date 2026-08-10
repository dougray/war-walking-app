package com.warwalking.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Health Connect launches this when the user asks "why does this app want my
 * step data" from the system permissions screen. Required for HC permission
 * grants to be selectable at all - see AndroidManifest.xml's intent-filter.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RationaleScreen() }
    }
}

@Composable
private fun RationaleScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "War Walking reads your step count from Health Connect only for the " +
                        "duration of an active walking session, to verify that scanned " +
                        "networks were reached on foot rather than by vehicle. Steps are " +
                        "never written back or shared outside your own session score."
                )
            }
        }
    }
}

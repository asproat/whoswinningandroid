package com.sproatcentral.whoswinningredux

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity

class SettingsActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Settings()
        }
    }
}

@Composable
fun Settings() {
    Column(verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxSize(1.0f)
        ) {
        Text("Settings")
        Button(onClick = {

        }) {
            Icon(imageVector = Icons.Default.Done, "")
        }
    }
}
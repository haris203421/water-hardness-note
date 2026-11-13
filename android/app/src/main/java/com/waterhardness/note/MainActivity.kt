package com.waterhardness.note

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waterhardness.note.core.di.ServiceLocator
import com.waterhardness.note.core.theme.AppTheme
import com.waterhardness.note.presentation.app.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ServiceLocator.initialize(applicationContext)
        if (savedInstanceState == null) acceptIntent(intent)
        setContent { AppTheme { AppRoot() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptIntent(intent)
    }
    private fun acceptIntent(intent: Intent?) {
        if (intent?.action == "open_plan") intent.getStringExtra("applianceId")?.let(ServiceLocator::openPlan)
    }
}

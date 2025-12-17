package com.mrunicorn.sb.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.mrunicorn.sb.App
import com.mrunicorn.sb.ui.inbox.InboxRoute
import com.mrunicorn.sb.ui.inbox.InboxViewModelFactory
import com.mrunicorn.sb.ui.theme.ShareBuddyTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private val repo by lazy { (application as App).repo }

    private val requestNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val initialItemId = intent.getStringExtra("openItemId")?.also {
            intent.removeExtra("openItemId")
        }

        setContent {
            ShareBuddyTheme {
                val colorScheme = MaterialTheme.colorScheme
                val view = LocalView.current
                SideEffect {
                    window.statusBarColor = colorScheme.surface.toArgb()
                    val controller = WindowInsetsControllerCompat(window, view)
                    controller.isAppearanceLightStatusBars = colorScheme.surface.luminance() > 0.5f
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }
                val viewModel: com.mrunicorn.sb.ui.inbox.InboxViewModel = viewModel(
                    factory = InboxViewModelFactory(application, repo)
                )
                InboxRoute(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    initialScrollItemId = initialItemId
                )
            }
        }
    }
}

package com.flathike.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.flathike.app.model.ThemeMode
import com.flathike.app.ui.FlatHikeMainScreen
import com.flathike.app.ui.FlatHikeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlatHikeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming GPS track file via Intent
        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            FlatHikeTheme(themeMode = uiState.themeMode) {
                FlatHikeMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        try {
            var filename = "shared_track.gpx"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    filename = cursor.getString(nameIndex) ?: filename
                }
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                viewModel.loadTrackFromStream(inputStream, filename)
            }
        } catch (_: Exception) {}
    }
}

// Nature / Alpine themed color scheme
private val AlpinePrimary = Color(0xFF1E6B52) // Mountain Forest Green
private val AlpineOnPrimary = Color(0xFFFFFFFF)
private val AlpinePrimaryContainer = Color(0xFFA7F2D2)
private val AlpineSecondary = Color(0xFF3B6455)
private val AlpineTertiary = Color(0xFF944A00) // Trail Rust
private val AlpineTertiaryContainer = Color(0xFFFFDCC5)
private val AlpineBackground = Color(0xFFF6FBF7)
private val AlpineSurface = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6B52), // Mountain Forest Green
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F2D2),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF3B6455),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDECE3),
    onSecondaryContainer = Color(0xFF062017),
    tertiary = Color(0xFF944A00), // Trail Rust
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC5),
    onTertiaryContainer = Color(0xFF321300),
    background = Color(0xFFF6FBF7),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5B7),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFFA7F2D2),
    secondary = Color(0xFFB1CCBF),
    onSecondary = Color(0xFF1D352B),
    secondaryContainer = Color(0xFF334B41),
    onSecondaryContainer = Color(0xFFCDD9D1),
    tertiary = Color(0xFFFFB786),
    onTertiary = Color(0xFF502400),
    tertiaryContainer = Color(0xFF723600),
    onTertiaryContainer = Color(0xFFFFDCC5),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF171E1B),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404944),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun FlatHikeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

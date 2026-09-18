package br.com.faceclass.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FaceClassColors = lightColorScheme(
    primary = Color(0xFF6B46C1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E8FF),
    onPrimaryContainer = Color(0xFF352066),
    secondary = Color(0xFF718096),
    tertiary = Color(0xFF38A169),
    background = Color(0xFFF8F9FE),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun FaceClassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FaceClassColors, content = content)
}

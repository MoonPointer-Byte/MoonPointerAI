package com.englishai.translator.ui.theme



import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.darkColorScheme

import androidx.compose.material3.lightColorScheme

import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color



private val DarkColors = darkColorScheme(

    primary = Color(0xFF818CF8),

    secondary = Color(0xFF67E8F9),

    tertiary = Color(0xFFA5B4FC),

    background = Color(0xFF0A0A12),

    surface = Color(0xFF16161F),

    onPrimary = Color.White,

    onBackground = Color(0xFFF4F4F5),

    onSurface = Color(0xFFF4F4F5)

)



private val LightColors = lightColorScheme(

    primary = Color(0xFF4F46E5),

    secondary = Color(0xFF0891B2),

    tertiary = Color(0xFF6366F1),

    background = Color(0xFFEEF0F4),

    surface = Color(0xFFFFFFFF),

    onPrimary = Color.White,

    onBackground = Color(0xFF18181B),

    onSurface = Color(0xFF27272A)

)



@Composable

fun MoonPointerTheme(

    darkTheme: Boolean = true,

    content: @Composable () -> Unit

) {

    MaterialTheme(

        colorScheme = if (darkTheme) DarkColors else LightColors,

        content = content

    )

}



/** @deprecated Use MoonPointerTheme */

@Composable

fun AITranslatorTheme(content: @Composable () -> Unit) = MoonPointerTheme(content = content)



package com.indiewalkabout.moneymagic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.indiewalkabout.moneymagic.presentation.navigation.MoneyMagicNavHost
import com.indiewalkabout.moneymagic.ui.theme.MoneyMagicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyMagicTheme {
                MoneyMagicNavHost()
            }
        }
    }
}

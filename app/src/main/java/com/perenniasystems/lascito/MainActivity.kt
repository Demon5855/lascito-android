package com.perenniasystems.lascito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.perenniasystems.lascito.navigation.LascitoNavGraph
import com.perenniasystems.lascito.ui.theme.LascitoTheme

/**
 * Single-Activity entry point for Lascito.
 * Hosts the Compose Navigation graph inside [LascitoTheme].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LascitoTheme {
                val navController = rememberNavController()
                LascitoNavGraph(navController = navController)
            }
        }
    }
}

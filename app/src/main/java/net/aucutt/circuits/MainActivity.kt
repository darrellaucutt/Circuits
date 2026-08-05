package net.aucutt.circuits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.aucutt.circuits.ui.CircuitsApp
import net.aucutt.circuits.ui.theme.CircuitsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CircuitsTheme {
                CircuitsApp()
            }
        }
    }
}

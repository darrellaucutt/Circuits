package net.aucutt.circuits.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.ongoing.OngoingActivity
import net.aucutt.circuits.wear.ui.WearMirrorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OngoingActivity.recoverOngoingActivity(this)
        setContent {
            WearMirrorScreen()
        }
    }
}

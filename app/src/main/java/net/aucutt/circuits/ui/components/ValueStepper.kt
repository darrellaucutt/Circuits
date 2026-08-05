package net.aucutt.circuits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.aucutt.circuits.ui.theme.BannerBlack
import net.aucutt.circuits.ui.theme.CircuitCyan
import net.aucutt.circuits.ui.theme.OnPrimaryLight
import net.aucutt.circuits.ui.theme.RobotSilverDark

@Composable
fun ValueStepper(
    label: String,
    valueText: String,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    modifier: Modifier = Modifier,
) {
    val stepperShape = RoundedCornerShape(10.dp)
    val buttonShape = RoundedCornerShape(8.dp)
    val stepperButtonColors = ButtonDefaults.buttonColors(
        containerColor = CircuitCyan,
        contentColor = OnPrimaryLight,
        disabledContainerColor = BannerBlack.copy(alpha = 0.6f),
        disabledContentColor = RobotSilverDark,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnPrimaryLight,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .clip(stepperShape)
                .background(BannerBlack.copy(alpha = 0.82f))
                .border(1.dp, CircuitCyan.copy(alpha = 0.7f), stepperShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = onDecrement,
                enabled = canDecrement,
                shape = buttonShape,
                colors = stepperButtonColors,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimaryLight,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnPrimaryLight,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Button(
                onClick = onIncrement,
                shape = buttonShape,
                colors = stepperButtonColors,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

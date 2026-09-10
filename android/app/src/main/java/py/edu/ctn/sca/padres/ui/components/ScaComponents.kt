package py.edu.ctn.sca.padres.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** Web `.parent-*-heading > div > span`: an all-caps accent eyebrow above a title. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = scaColors.accentDeep,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp,
    )
}

/** Web `.panel`: a bordered paper surface with a soft shadow. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

/**
 * Web `.auth-form`: a paper card with a 5px accent strip along the top edge and a
 * pronounced shadow. Used by the login flow.
 */
@Composable
fun AccentTopCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 14.dp,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Column(Modifier.padding(horizontal = 22.dp, vertical = 26.dp)) { content() }
        }
    }
}

/** Web `.grade-chip--N`: a solid pill on the grade colour scale, white text. */
@Composable
fun GradeChip(nota: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(scaColors.grade(nota))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            text = nota.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Web `.parent-task-status`: a bordered, tinted status pill. */
@Composable
fun StatusPill(text: String, tone: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tone,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A vertical accent bar, matching the web `.nav-card::before` / selected-card inset. */
@Composable
fun AccentBar(color: Color = MaterialTheme.colorScheme.primary, width: Int = 4) {
    Box(
        Modifier
            .width(width.dp)
            .fillMaxHeight()
            .background(color),
    )
}

/** Web `.parent-subject-progress`: rounded track with an accent fill. */
@Composable
fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(scaColors.bgSoft)
            .height(7.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** Web `.parent-overview-metrics article`: a soft-filled stat box. */
@Composable
fun MetricBox(label: String, value: String, modifier: Modifier = Modifier, hint: String? = null, hintTone: Color? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(scaColors.bgSoft)
            .padding(15.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            hint ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = hintTone ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

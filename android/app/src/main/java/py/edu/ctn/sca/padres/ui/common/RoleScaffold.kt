package py.edu.ctn.sca.padres.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import py.edu.ctn.sca.padres.ui.theme.scaColors

/** Ítem de la barra inferior. [badge] > 0 muestra el contador en rojo. */
data class RoleTab(val label: String, val icon: ImageVector, val badge: Int = 0)

/** Acción de texto subrayada a la derecha del header (ej. "Marcar leídos"). */
data class HeaderAction(val label: String, val onClick: () -> Unit)

/**
 * Marco común de las apps por rol: línea de acento, header (ícono o flecha atrás + título +
 * subtítulo + acción), [content], [bottomAction] opcional (barra de acción pegajosa) y barra
 * inferior. Sin [tabs] no hay barra inferior (pantallas de detalle).
 */
@Composable
fun RoleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    headerAction: HeaderAction? = null,
    tabs: List<RoleTab> = emptyList(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.background(surface).statusBarsPadding()) {
            AccentTopLine()
            RoleHeader(title, subtitle, onBack, headerAction)
        }
        HorizontalDivider(color = scaColors.line)
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (bottomAction != null) {
            HorizontalDivider(color = scaColors.line)
            Box(Modifier.fillMaxWidth().background(surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
                bottomAction()
            }
        }
        if (tabs.isNotEmpty()) {
            RoleBottomBar(tabs, selectedTab, onTabSelected)
        } else {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun RoleHeader(title: String, subtitle: String?, onBack: (() -> Unit)?, action: HeaderAction?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Volver", tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            Icon(Icons.Filled.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scaColors.muted, maxLines = 1)
            }
        }
        if (action != null) {
            Text(
                action.label,
                modifier = Modifier.clickable(onClick = action.onClick).padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Composable
private fun RoleBottomBar(tabs: List<RoleTab>, selected: Int, onSelected: (Int) -> Unit) {
    Column {
        HorizontalDivider(color = scaColors.line)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            tabs.forEachIndexed { i, tab ->
                NavigationBarItem(
                    selected = i == selected,
                    onClick = { onSelected(i) },
                    icon = {
                        BadgedBox(badge = {
                            if (tab.badge > 0) {
                                Badge(containerColor = scaColors.danger.fg, contentColor = MaterialTheme.colorScheme.surface) {
                                    Text(if (tab.badge > 99) "99+" else tab.badge.toString())
                                }
                            }
                        }) { Icon(tab.icon, contentDescription = null) }
                    },
                    label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = scaColors.accentSoft,
                        unselectedIconColor = scaColors.muted,
                        unselectedTextColor = scaColors.muted,
                    ),
                )
            }
        }
    }
}

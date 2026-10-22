package hr.bertlab.fluteassistant.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R

/**
 * Two small round flag buttons, one per [AppLanguage]; the active one is ringed in coral and
 * the other is faded. Flags and endonyms need no translation, so a child who cannot read
 * the current language still finds their own. Stateless: the host applies the choice.
 */
@Composable
fun LanguageSwitch(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(R.string.language)
    Row(
        modifier = modifier
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == current
            Surface(
                selected = isSelected,
                onClick = { if (!isSelected) onSelect(language) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .size(44.dp)
                    .alpha(if (isSelected) 1f else 0.55f)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = language.endonym
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(language.flag, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

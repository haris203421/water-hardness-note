package com.waterhardness.note.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R

@Composable
fun LargeStepper(value: String, minusEnabled: Boolean, plusEnabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    val decrease = stringResource(R.string.decrease)
    val increase = stringResource(R.string.increase)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onMinus, enabled = minusEnabled, contentPadding = PaddingValues(8.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = decrease }) { Text(stringResource(R.string.minus)) }
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = onPlus, enabled = plusEnabled, contentPadding = PaddingValues(8.dp), modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = increase }) { Text(stringResource(R.string.plus)) }
    }
}

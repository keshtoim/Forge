package com.keshtoim.forge.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.keshtoim.forge.R
import com.keshtoim.forge.data.ai.AiFailure
import com.keshtoim.forge.data.ai.AiStatus
import com.keshtoim.forge.data.ai.AiTurn
import com.keshtoim.forge.data.ai.SetAdvice
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.ui.AiIndicator
import com.keshtoim.forge.ui.labelRes
import com.keshtoim.forge.ui.summary

private val SUGGESTIONS = listOf(R.string.coach_q_weight, R.string.coach_q_rest, R.string.coach_q_swap, R.string.coach_q_form)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoachSheet(
    status: AiStatus,
    chat: List<AiTurn>,
    busy: Boolean,
    failure: AiFailure?,
    onAsk: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    // Counted from the data, not from layoutInfo, which still describes the previous frame.
    val itemCount = (if (chat.isEmpty()) 1 else 0) + chat.size + (if (busy) 1 else 0) + (if (failure != null) 1 else 0)
    LaunchedEffect(itemCount) { list.animateScrollToItem((itemCount - 1).coerceAtLeast(0)) }
    val send = { text: String ->
        onAsk(text)
        draft = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainer,
    ) {
        Column(Modifier.fillMaxHeight(0.85f).imePadding()) {
            Row(Modifier.padding(start = 20.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.coach_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                AiIndicator(status)
            }
            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (chat.isEmpty()) {
                    item {
                        Text(stringResource(R.string.coach_intro), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SUGGESTIONS.forEach { res ->
                                val question = stringResource(res)
                                SuggestionChip(onClick = { send(question) }, enabled = !busy, label = { Text(question) })
                            }
                        }
                    }
                }
                items(chat) { Bubble(it) }
                if (busy) item { Bubble(AiTurn(fromUser = false, text = stringResource(R.string.coach_thinking)), muted = true) }
                failure?.let {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(it.labelRes), style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.weight(1f))
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.coach_retry)) }
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.coach_hint)) },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = { send(draft) }, enabled = draft.isNotBlank() && !busy, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.coach_send))
                }
            }
        }
    }
}

@Composable
private fun Bubble(turn: AiTurn, muted: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (turn.fromUser) colors.primaryContainer else colors.surfaceContainerHighest,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            SelectionContainer {
                Text(
                    turn.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        muted -> colors.onSurfaceVariant
                        turn.fromUser -> colors.onPrimaryContainer
                        else -> colors.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

// Sits at the bottom of the current set's card: ask, wait, then apply or dismiss the suggestion.
@Composable
fun AdviceRow(state: AdviceState?, type: ExerciseType, onAsk: () -> Unit, onApply: (SetAdvice) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    when (state) {
        null -> TextButton(onClick = onAsk) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.advice_ask), modifier = Modifier.padding(start = 8.dp))
        }
        AdviceState.Loading -> Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.advice_loading), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
        }
        is AdviceState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(state.failure.labelRes), style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.weight(1f))
            TextButton(onClick = onAsk) { Text(stringResource(R.string.coach_retry)) }
        }
        is AdviceState.Ready -> Surface(shape = RoundedCornerShape(14.dp), color = colors.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp)) {
                val target = state.advice.applyTo(WorkoutSet(workoutExerciseId = 0, position = 0)).summary(type, context)
                Text(stringResource(R.string.advice_target, target), style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
                if (state.advice.reason.isNotBlank()) {
                    Text(state.advice.reason, style = MaterialTheme.typography.bodySmall, color = colors.onSecondaryContainer, modifier = Modifier.padding(top = 2.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.advice_dismiss)) }
                    TextButton(onClick = { onApply(state.advice) }) { Text(stringResource(R.string.advice_apply)) }
                }
            }
        }
    }
}

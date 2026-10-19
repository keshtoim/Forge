package com.keshtoim.forge.ui.workout

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.keshtoim.forge.R
import com.keshtoim.forge.data.ai.AiException
import com.keshtoim.forge.data.ai.AiStatus
import com.keshtoim.forge.data.ai.AiTurn
import com.keshtoim.forge.data.ai.SetAdvice
import com.keshtoim.forge.data.ai.preparePhoto
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.WorkoutSet
import com.keshtoim.forge.ui.AiErrorText
import com.keshtoim.forge.ui.AiIndicator
import com.keshtoim.forge.ui.summary
import kotlinx.coroutines.launch
import java.io.File

private val SUGGESTIONS = listOf(R.string.coach_q_weight, R.string.coach_q_rest, R.string.coach_q_swap, R.string.coach_q_form)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoachSheet(
    status: AiStatus,
    chat: List<AiTurn>,
    busy: Boolean,
    error: AiException?,
    photos: List<ByteArray>,
    onAttach: (ByteArray) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onAsk: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf("") }
    var attachMenu by remember { mutableStateOf(false) }
    var preparing by remember { mutableIntStateOf(0) }
    val canAttach = photos.size + preparing < COACH_MAX_PHOTOS && !busy
    val attach = { uris: List<Uri> ->
        uris.take(COACH_MAX_PHOTOS - photos.size - preparing).forEach { uri ->
            preparing++
            scope.launch {
                preparePhoto(context.contentResolver, uri)?.let(onAttach)
                preparing--
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(COACH_MAX_PHOTOS)) { attach(it) }
    // The camera writes into a file we own; its address has to outlive a rotation while the camera app is open.
    var shot by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        shot?.takeIf { saved }?.let { attach(listOf(it.toUri())) }
    }
    val list = rememberLazyListState()
    // Counted from the data, not from layoutInfo, which still describes the previous frame.
    val itemCount = (if (chat.isEmpty()) 1 else 0) + chat.size + (if (busy) 1 else 0) + (if (error != null) 1 else 0)
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
                error?.let {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AiErrorText(it, Modifier.weight(1f))
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.coach_retry)) }
                        }
                    }
                }
            }
            if (photos.isNotEmpty() || preparing > 0) {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEachIndexed { index, photo -> PhotoThumb(photo, 64.dp, onRemove = { onRemovePhoto(index) }) }
                    repeat(preparing) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { attachMenu = true }, enabled = canAttach) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = stringResource(R.string.coach_attach))
                    }
                    DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.coach_gallery)) },
                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                attachMenu = false
                                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.coach_camera)) },
                            leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                            onClick = {
                                attachMenu = false
                                val uri = newPhotoUri(context)
                                shot = uri.toString()
                                camera.launch(uri)
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.coach_hint)) },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = { send(draft) },
                    enabled = (draft.isNotBlank() || photos.isNotEmpty()) && preparing == 0 && !busy,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
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
            Column {
                if (turn.images.isNotEmpty()) {
                    Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        turn.images.forEach { PhotoThumb(it, 88.dp) }
                    }
                }
                if (turn.text.isNotBlank()) {
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
                } else {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PhotoThumb(photo: ByteArray, size: Dp, onRemove: (() -> Unit)? = null) {
    // A quarter-size decode is plenty for a thumbnail and keeps the main thread quick.
    val bitmap = remember(photo) {
        BitmapFactory.decodeByteArray(photo, 0, photo.size, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
    }
    Box(Modifier.size(size)) {
        bitmap?.let {
            Image(
                it,
                contentDescription = stringResource(R.string.coach_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)),
            )
        }
        onRemove?.let {
            Surface(onClick = it, shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(22.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.coach_photo_remove),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(3.dp),
                )
            }
        }
    }
}

// Each shot gets its own file so a new photo never overwrites one still being prepared; earlier shots are long sent by then.
private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, "coach-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
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
            AiErrorText(state.error, Modifier.weight(1f))
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

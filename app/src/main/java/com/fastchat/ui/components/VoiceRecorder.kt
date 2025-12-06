package com.fastchat.ui.components

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fastchat.utils.AudioRecorder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecorderButton(
    onVoiceRecorded: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }

    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO) { granted ->
        if (granted) {
            audioRecorder.startRecording()
            isRecording = true
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        } else {
            recordingDuration = 0
        }
    }

    if (isRecording) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    audioRecorder.cancelRecording()
                    isRecording = false
                }
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(
                onClick = {
                    val audioBytes = audioRecorder.stopRecording()
                    isRecording = false
                    audioBytes?.let { onVoiceRecorded(it) }
                }
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        IconButton(
            onClick = {
                when {
                    audioPermissionState.status == com.google.accompanist.permissions.PermissionStatus.Granted -> {
                        audioRecorder.startRecording()
                        isRecording = true
                    }
                    else -> {
                        audioPermissionState.launchPermissionRequest()
                    }
                }
            }
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Record voice")
        }
    }
}

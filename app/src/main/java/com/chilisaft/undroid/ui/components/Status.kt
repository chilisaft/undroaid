package com.chilisaft.undroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chilisaft.undroid.data.models.DockerContainerState
import com.chilisaft.undroid.data.models.VmDomain
import com.chilisaft.undroid.data.models.VmRunState
import com.chilisaft.undroid.ui.theme.StatusTone
import com.chilisaft.undroid.ui.theme.color
import com.chilisaft.undroid.ui.theme.spacing

@Composable
fun StatusDot(tone: StatusTone, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier = modifier.size(size).background(tone.color, CircleShape))
}

/** A [StatusDot] followed by its label - the one way every list row/header shows its state. */
@Composable
fun StatusLine(
    tone: StatusTone,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StatusDot(tone)
        Spacer(Modifier.width(MaterialTheme.spacing.extraSmall))
        Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

val DockerContainerState.statusTone: StatusTone
    get() = when (this) {
        DockerContainerState.RUNNING -> StatusTone.Ok
        DockerContainerState.PAUSED -> StatusTone.Warning
        DockerContainerState.EXITED -> StatusTone.Neutral
    }

/** For places that only have the coarse state, not Docker's own status text (e.g. "Up 3 hours"). */
val DockerContainerState.label: String
    get() = when (this) {
        DockerContainerState.RUNNING -> "Running"
        DockerContainerState.PAUSED -> "Paused"
        DockerContainerState.EXITED -> "Stopped"
    }

val VmDomain.statusTone: StatusTone
    get() = when {
        crashed -> StatusTone.Error
        state == VmRunState.RUNNING -> StatusTone.Ok
        state == VmRunState.PAUSED -> StatusTone.Warning
        else -> StatusTone.Neutral
    }

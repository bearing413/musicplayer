package com.localmp3.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localmp3.player.R
import com.localmp3.player.ui.theme.Copper
import com.localmp3.player.ui.theme.JadeDeep
import com.localmp3.player.ui.theme.JadeSoft
import com.localmp3.player.ui.theme.Mist
import com.localmp3.player.ui.theme.MistWarm

@Composable
fun PermissionRequestScreen(onGrant: () -> Unit) {
    StatusScaffold(
        icon = Icons.Rounded.LibraryMusic,
        title = stringResource(R.string.permission_title),
        body = stringResource(R.string.permission_body),
        primaryLabel = stringResource(R.string.permission_grant),
        onPrimary = onGrant,
    )
}

@Composable
fun PermissionDeniedScreen(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    StatusScaffold(
        icon = Icons.Rounded.Lock,
        title = stringResource(R.string.permission_denied_title),
        body = stringResource(R.string.permission_denied_body),
        primaryLabel = if (permanentlyDenied) {
            stringResource(R.string.open_settings)
        } else {
            stringResource(R.string.permission_grant)
        },
        onPrimary = if (permanentlyDenied) onOpenSettings else onGrant,
        secondaryLabel = if (permanentlyDenied) stringResource(R.string.permission_grant) else null,
        onSecondary = if (permanentlyDenied) onGrant else null,
    )
}

@Composable
private fun StatusScaffold(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MistWarm, Mist, JadeSoft.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = JadeDeep,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(JadeSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JadeDeep,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JadeDeep,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = secondaryLabel,
                        color = Copper,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

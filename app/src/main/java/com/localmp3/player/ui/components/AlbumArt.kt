package com.localmp3.player.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.localmp3.player.R
import com.localmp3.player.ui.theme.JadeDeep
import com.localmp3.player.ui.theme.JadeSoft
import com.localmp3.player.ui.theme.MistWarm

@Composable
fun AlbumArt(
    artUri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevated: Boolean = true,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(elevation = 18.dp, shape = shape, clip = false)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(JadeSoft, MistWarm),
                ),
            ),
    ) {
        if (artUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artUri)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = painterResource(R.drawable.ic_album_placeholder),
                placeholder = painterResource(R.drawable.ic_album_placeholder),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_album_placeholder),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(JadeDeep.copy(alpha = 0.08f)),
            )
        }
    }
}

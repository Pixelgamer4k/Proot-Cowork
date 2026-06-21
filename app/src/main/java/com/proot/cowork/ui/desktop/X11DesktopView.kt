package com.proot.cowork.ui.desktop

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.x11.LorieView
import com.termux.x11.X11ServerManager
import kotlinx.coroutines.delay

@Composable
fun X11DesktopView(
    modifier: Modifier = Modifier,
) {
    val lorieView = remember {
        LorieViewHolder()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LorieView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                lorieView.view = this
            }
        },
        update = { view ->
            lorieView.view = view
        },
    )

    LaunchedEffect(Unit) {
        repeat(40) {
            if (lorieView.view != null && X11ServerManager.connectLorieView(lorieView.view!!)) {
                return@LaunchedEffect
            }
            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            lorieView.view = null
        }
    }
}

private class LorieViewHolder {
    var view: LorieView? = null
}

package eu.kanade.presentation.util

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

// https://issuetracker.google.com/352584409
// Keep the context receiver on its own line for Ktlint 1.7.0.
context(itemScope: LazyItemScope)
fun Modifier.animateItemFastScroll() = with(itemScope) {
    this@animateItemFastScroll.animateItem(fadeInSpec = null, fadeOutSpec = null)
}

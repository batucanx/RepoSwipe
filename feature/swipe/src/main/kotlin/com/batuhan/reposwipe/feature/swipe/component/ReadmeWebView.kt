package com.batuhan.reposwipe.feature.swipe.component

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

/**
 * Every http/https navigation is handed off to Chrome Custom Tabs rather than loaded in place —
 * unlike the rest of this detail sheet (see [RepoDetailSheet]'s doc), a README's links/badges are
 * GitHub's own content and tapping one is expected to go somewhere, the same as it would on
 * github.com itself. The one exception is an in-page `#heading` anchor (GitHub's own generated
 * heading-permalink icons use these): that's left for the WebView to resolve natively as a
 * same-document scroll instead of bouncing out to a browser for it.
 *
 * [onContentHeightChanged] is how [ReadmeWebView] gets told how tall to lay itself out — see that
 * composable's doc for why this replaced a `WebView.onMeasure` override.
 */
private class ReadmeWebViewClient(
    private val context: Context,
    private val onContentHeightChanged: (Int) -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url
        if (url.scheme?.lowercase() !in setOf("http", "https")) return true
        if (url.toString().substringBefore('#') == README_BASE_URL) return false
        CustomTabsIntent.Builder().build().launchUrl(context, url)
        return true
    }

    override fun onPageFinished(
        view: WebView,
        url: String?,
    ) {
        reportHeight(view)
        // onPageFinished fires once the HTML/CSS parses — a README's own images (badges,
        // screenshots) are still streaming in from raw.githubusercontent.com at that point, so
        // the very first read is routinely shorter than the page ends up being once they land.
        // This re-checks after they've had a moment to arrive and reflow the page, without
        // needing JavaScript enabled just to listen for a real "images loaded" event.
        view.postDelayed({ reportHeight(view) }, IMAGE_SETTLE_DELAY_MS)
    }

    // WebView.getScale() is deprecated with no synchronous replacement (only an async
    // onScaleChanged callback) — still the correct CSS-px-to-device-px conversion for a one-off
    // height read, so this suppresses the warning rather than adding callback plumbing for it.
    @Suppress("DEPRECATION")
    private fun reportHeight(view: WebView) {
        val heightPx = (view.contentHeight * view.scale).roundToInt()
        // Coerced: Compose's Constraints can't represent every value a real Int can hold (it packs
        // width/height into a fixed bit budget) and throws IllegalArgumentException past a certain
        // magnitude — hit in practice by a fully expanded ("Devamını gör") README long enough that
        // its real WebView content height landed just over that ceiling, crashing the app outright.
        // MAX_REPORTED_HEIGHT_PX sits far below where Compose's limit actually is, so this only
        // ever engages for a pathologically long document, and even then the WebView's own internal
        // scrolling still makes the rest of the page reachable — capping the *container* doesn't
        // clip content, since [ReadmeWebView] isn't the one that scrolls the sheet.
        if (heightPx > 0) onContentHeightChanged(heightPx.coerceAtMost(MAX_REPORTED_HEIGHT_PX))
    }
}

/**
 * Renders GitHub's own already-rendered README HTML (see `RepoRepository.getReadmeHtml`) as-is,
 * styled with a locally vendored copy of GitHub's own `github-markdown-css` — this is what makes
 * the README look exactly like it does on github.com instead of an app-specific reinterpretation
 * of the markdown. JavaScript stays off: nothing in a README needs it to render, and it's a free
 * defense-in-depth layer against whatever a repo owner put in their README.
 *
 * **The document is loaded exactly once and never reloaded.** [maxVisibleHeightPx] collapses the
 * view by *clipping* an already-rendered full-height WebView, not by re-rendering a shortened copy
 * of the HTML. Earlier versions did the latter — swapping a truncated prefix document in and out
 * via `loadDataWithBaseURL` on every "Devamını gör" tap — and every jump/flicker bug this
 * composable accumulated traced back to it: a reload blanks the page and resets
 * [WebView.getContentHeight], so the container had to guess a height for the gap, and any guess
 * lower than the current one shrinks this item inside the detail sheet's scrolling `LazyColumn`,
 * which immediately clamps the list's scroll offset down to fit. That clamp *is* the "tapping
 * Devamını gör jumps back to the top" bug — the viewport got yanked, not just the content. With no
 * reload there is no gap to guess at: the full content is already laid out and measured behind the
 * clip, so expanding only ever grows the visible box and the user's scroll position stays put.
 *
 * The WebView is laid out at its full measured content height and the *parent* box clips it, so
 * the WebView never has anything to scroll internally — important, because an internally
 * scrollable WebView would swallow vertical drags that belong to the sheet's `LazyColumn`.
 *
 * That `LazyColumn` disposes this composable (and destroys its WebView) whenever the README
 * scrolls far enough out of the viewport, then rebuilds it on the way back. [initialContentHeightPx]
 * lets the caller hand the rebuilt instance the height it already knows this README measured, so
 * the rebuild doesn't collapse to [README_LOADING_PLACEHOLDER_DP] and re-grow — that shrink would
 * clamp the list's scroll offset, i.e. the same jump described above, just triggered by scrolling
 * instead of by tapping.
 *
 * Height is measured explicitly ([WebView.getContentHeight], reported from
 * [ReadmeWebViewClient.onPageFinished]) rather than by a `WebView` subclass self-reporting through
 * `onMeasure`/`computeVerticalScrollRange` — that approach (this composable's first version)
 * measures *during* Android's layout pass, which can run before the page has finished laying out
 * at its real width, yielding a stale, wrong-sized gap.
 */
@Composable
fun ReadmeWebView(
    html: String,
    modifier: Modifier = Modifier,
    maxVisibleHeightPx: Int? = null,
    initialContentHeightPx: Int = 0,
    onContentHeightMeasured: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val darkTheme = isSystemInDarkTheme()
    val css = remember(context, darkTheme) { loadMarkdownCss(context, darkTheme) }
    val document = remember(html, css) { wrapReadmeHtml(html, css) }
    var contentHeightPx by remember { mutableIntStateOf(initialContentHeightPx) }
    var loadedDocument by remember { mutableStateOf<String?>(null) }
    // Real Android WebView engine spin-up (especially the very first one in the process) plus
    // parsing the document is genuinely async work with nothing driving Compose recomposition in
    // between — until [ReadmeWebViewClient.onPageFinished] fires and reports a real height, this
    // composable had nothing to say about it, so it rendered at its initial 0dp: no spinner, no
    // content, nothing — reading as the README having silently failed to load rather than still
    // being on its way. [contentHeightPx] doubles as that signal, and since the document is loaded
    // only once it can never fall back to 0 later, so the placeholder + spinner show exactly for
    // that first-load gap and never again — a caller-seeded [initialContentHeightPx] skips even
    // that, since a re-inflated WebView is re-rendering something the reader has already seen.
    val isAwaitingFirstMeasurement = contentHeightPx == 0
    val placeholderHeightPx = with(density) { README_LOADING_PLACEHOLDER_DP.dp.roundToPx() }
    val fullHeightPx = if (isAwaitingFirstMeasurement) placeholderHeightPx else contentHeightPx
    val visibleHeightPx = maxVisibleHeightPx?.coerceAtMost(fullHeightPx) ?: fullHeightPx
    // Animates the initial grow-in and the "Devamını gör" reveal. Because the content behind the
    // clip is already rendered, this is a pure size animation over finished pixels — nothing is
    // loading or reflowing while it runs, which is what makes the reveal read as smooth rather
    // than as a layout glitch, matching the rest of the app's animated transitions (e.g. SwipeDeck).
    val animatedVisibleHeightPx by
        animateIntAsState(
            targetValue = visibleHeightPx,
            animationSpec = tween(durationMillis = EXPAND_ANIMATION_MS, easing = FastOutSlowInEasing),
            label = "readmeVisibleHeight",
        )

    Box(
        modifier = modifier.fillMaxWidth().height(with(density) { animatedVisibleHeightPx.toDp() }).clipToBounds(),
        contentAlignment = Alignment.TopStart,
    ) {
        AndroidView(
            // Full content height, deliberately overflowing the clipped parent when collapsed —
            // see this composable's doc for why the WebView must never be the thing that scrolls.
            modifier = Modifier.fillMaxWidth().height(with(density) { fullHeightPx.toDp() }),
            factory = {
                WebView(context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    settings.javaScriptEnabled = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient =
                        ReadmeWebViewClient(context) { measuredPx ->
                            contentHeightPx = measuredPx
                            onContentHeightMeasured(measuredPx)
                        }
                }
            },
            update = { webView ->
                // AndroidView re-invokes `update` on every recomposition, not just when [document]
                // actually changed — without this guard, an unrelated recomposition (the sheet
                // scrolling, or the expand animation ticking) would reload the exact same page and
                // undo the "load once" property this composable's smoothness depends on.
                if (document != loadedDocument) {
                    loadedDocument = document
                    webView.loadDataWithBaseURL(README_BASE_URL, document, "text/html", "utf-8", null)
                }
            },
            onRelease = { webView -> webView.destroy() },
        )

        if (isAwaitingFirstMeasurement) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.Center),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Fades the clipped edge out instead of slicing a line of text in half, so the collapsed
        // state reads as "there's more below" rather than as a rendering fault. Only while there
        // genuinely is more: once expanded (or for a README shorter than the collapsed height)
        // there's no cut to soften. surfaceContainer matches the README card this sits in.
        val isClipped = maxVisibleHeightPx != null && contentHeightPx > maxVisibleHeightPx
        if (isClipped) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(FADE_SCRIM_HEIGHT_DP.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainer),
                            ),
                        ),
            )
        }
    }
}

private fun loadMarkdownCss(
    context: Context,
    darkTheme: Boolean,
): String {
    val fileName = if (darkTheme) "github-markdown-dark.css" else "github-markdown-light.css"
    return context.assets
        .open(fileName)
        .bufferedReader()
        .use { it.readText() }
}

private fun wrapReadmeHtml(
    bodyHtml: String,
    css: String,
) = """
    <!DOCTYPE html>
    <html>
    <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <style>$css</style>
    <style>
    body { margin: 0; padding: 0; background-color: transparent; }
    .markdown-body { padding: 0; background-color: transparent; font-size: 15px; }
    img, video { max-width: 100%; height: auto; }
    </style>
    </head>
    <body>$bodyHtml</body>
    </html>
    """.trimIndent()

/** Base URL [ReadmeWebView] loads its document against — README-relative `src`/`href` values are
 * already rewritten to absolute URLs before this point (see `RepoRepository.resolveRelativeReadmeUrls`),
 * so this only matters for detecting an in-page `#anchor` link in [ReadmeWebViewClient]. */
private const val README_BASE_URL = "https://github.com/"

private const val IMAGE_SETTLE_DELAY_MS = 400L

// Held height (with a centered spinner) while a WebView's first-ever page load is still in
// flight — see [ReadmeWebView]'s doc for why that gap otherwise renders as nothing at all.
private const val README_LOADING_PLACEHOLDER_DP = 160

// Long enough to read as a deliberate reveal rather than a snap, short enough not to make the
// user wait for content that is already rendered and sitting behind the clip.
private const val EXPAND_ANIMATION_MS = 320

private const val FADE_SCRIM_HEIGHT_DP = 56

// A comfortable margin below wherever Compose's own Constraints packing limit actually is — see
// ReadmeWebViewClient.reportHeight's doc for the crash this prevents. In device px, not dp: a
// long expanded README easily clears six figures here on a high-density screen.
private const val MAX_REPORTED_HEIGHT_PX = 100_000

package com.batuhan.reposwipe.feature.swipe.component

import android.content.Context
import android.view.MotionEvent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.batuhan.reposwipe.core.designsystem.theme.RepoSwipeTheme
import kotlin.math.min
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

/**
 * A [WebView] whose own touch handling can be switched off. In [ReadmeViewMode.Preview] the
 * loaded document is routinely taller than this View's own (deliberately capped) height, and if
 * the WebView handled touches as it normally would, a drag starting inside it would scroll the
 * *document* internally instead of whatever scrolls around it (the detail sheet's LazyColumn) —
 * the classic embedded-scroller-steals-the-parent's-drag bug. Returning `false` from
 * [onTouchEvent] is the standard Android mechanism for "this View declines the gesture": the
 * touch then continues up to the nearest ancestor that wants it — here, a `clickable` Compose
 * modifier wrapping this WebView in [ReadmeWebView], which is how a tap anywhere on the preview
 * opens [ReadmeViewMode.Read]. Read mode flips [touchEnabled] back on, since at that point the
 * WebView is the only scroller on screen (see [ReadmeWebView]'s doc for why the sheet hides every
 * other section while reading).
 */
class ScrollGatedWebView(
    context: Context,
) : WebView(context) {
    var touchEnabled: Boolean = true

    /**
     * The preview temporarily leaves composition while the fullscreen reader is open, but this
     * native View deliberately survives that mode switch. Keep the document's render state on
     * the View as well: when the preview is attached again there is no second page-load callback
     * to rebuild short-lived Compose state from.
     */
    internal var loadedDocument: String? = null
    internal var visibleDocument: String? = null
    internal var measuredDocumentHeightPx: Int = 0

    override fun onTouchEvent(event: MotionEvent): Boolean = if (touchEnabled) super.onTouchEvent(event) else false
}

/**
 * Creates and configures a screen-scoped [ScrollGatedWebView]. Keep one instance per stable
 * [ReadmeWebView] call site: Android Views must not be shared between the preview and fullscreen
 * reader because both call sites can briefly coexist during a Compose structural transition.
 *
 * The application context avoids retaining an Activity, while [DisposableEffect] deterministically
 * releases native WebView resources when the detail screen leaves composition.
 */
@Composable
fun rememberReadmeWebView(): ScrollGatedWebView {
    val context = LocalContext.current
    val webView =
        remember {
            ScrollGatedWebView(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }
        }
    DisposableEffect(webView) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.removeAllViews()
            webView.destroy()
        }
    }
    return webView
}

/**
 * Every http/https navigation is handed off to Chrome Custom Tabs rather than loaded in place —
 * a README's links/badges are GitHub's own content and tapping one is expected to go somewhere,
 * the same as it would on github.com itself. The one exception is an in-page `#heading` anchor
 * (GitHub's own generated heading-permalink icons use these): that's left for the WebView to
 * resolve natively as a same-document scroll instead of bouncing out to a browser for it.
 *
 * [onContentHeightChanged] is how [ReadmeWebView] learns the *full* document height even while
 * [ReadmeViewMode.Preview] keeps the View's own on-screen height capped — [WebView.getContentHeight]
 * reflects the loaded page's true layout size regardless of the View's own bounds, which is what
 * lets a capped preview still know whether there's more to read.
 */
private class ReadmeWebViewClient(
    private val context: Context,
    private val onContentHeightChanged: (Int) -> Unit,
    private val onPageVisible: () -> Unit,
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
        onPageVisible()
        reportHeight(view)
        // onPageFinished fires once the HTML/CSS parses — a README's own images (badges,
        // screenshots) are still streaming in from raw.githubusercontent.com at that point, so
        // the very first read is routinely shorter than the page ends up being once they land.
        // This re-checks after they've had a moment to arrive and reflow the page, without
        // needing JavaScript enabled just to listen for a real "images loaded" event.
        view.postDelayed({ reportHeight(view) }, IMAGE_SETTLE_DELAY_MS)
    }

    override fun onPageCommitVisible(
        view: WebView,
        url: String?,
    ) {
        onPageVisible()
        view.post { reportHeight(view) }
    }

    private fun reportHeight(view: WebView) {
        val heightPx = contentHeightPx(view)
        if (heightPx > 0) onContentHeightChanged(heightPx)
    }
}

// WebView.getScale() is deprecated with no synchronous replacement (only an async onScaleChanged
// callback) — still the correct CSS-px-to-device-px conversion for a one-off height read, so this
// suppresses the warning rather than adding callback plumbing for it. Shared by
// [ReadmeWebViewClient]'s callback-driven read and [ReadmeWebView]'s own reattachment probe below.
@Suppress("DEPRECATION")
private fun contentHeightPx(view: WebView): Int = (view.contentHeight * view.scale).roundToInt()

/** Which of the two ways [ReadmeWebView] can present a loaded document. Owned by the caller
 * ([com.batuhan.reposwipe.feature.swipe.SwipeScreen]'s `RepoDetailSheet`) since switching modes
 * is a structural change — the sheet's other sections disappear entirely in [Read] — not
 * something this composable can decide on its own. */
enum class ReadmeViewMode {
    /** Capped at a small height, non-scrolling, tap-anywhere-to-expand. Sits among the sheet's
     * other sections. */
    Preview,

    /** Fills its container and scrolls internally — the sheet hides every other section while
     * this is active, so the WebView is the only scroller on screen. */
    Read,
}

/**
 * Renders GitHub's own already-rendered README HTML (see `RepoRepository.getReadmeHtml`) as-is,
 * styled with a locally vendored copy of GitHub's own `github-markdown-css` — this is what makes
 * the README look exactly like it does on github.com instead of an app-specific reinterpretation
 * of the markdown. JavaScript stays off: nothing in a README needs it to render, and it's a free
 * defense-in-depth layer against whatever a repo owner put in their README.
 *
 * **[webView] is caller-owned and not destroyed here** — [rememberReadmeWebView] owns cleanup.
 * [onRelease] only detaches the View so the same stable call site can reuse it when a LazyColumn
 * item leaves and later re-enters the viewport.
 *
 * The loaded document and its render state live on [ScrollGatedWebView], not only in `remember`,
 * because the native View survives recompositions and LazyColumn disposal/re-attachment at its
 * own stable call site. Preview and read mode deliberately use distinct native Views.
 *
 * [ReadmeViewMode.Preview] bounds the View's real height at [previewCapDp] — a genuine layout
 * constraint, not the earlier "render full height, let the parent visually clip it" trick, which
 * left the WebView's actual (unclipped) touch-receiving bounds larger than what was visible.
 * [ScrollGatedWebView.touchEnabled] independently keeps it from scrolling itself while capped, so
 * the two concerns (how tall it looks, whether it steals drags) are no longer coupled the way
 * they were in that version.
 */
@Composable
fun ReadmeWebView(
    webView: ScrollGatedWebView,
    html: String,
    mode: ReadmeViewMode,
    modifier: Modifier = Modifier,
    previewCapDp: Int = README_PREVIEW_CAP_DP,
    onExpandRequested: () -> Unit = {},
    onContentHeightMeasured: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val darkTheme = RepoSwipeTheme.isDarkTheme
    val css = remember(context, darkTheme) { loadMarkdownCss(context, darkTheme) }
    val document = remember(html, css) { wrapReadmeHtml(html, css) }

    // Keyed on html (not the repo, which the caller doesn't hand us) so a different README
    // starts the "how tall is this really" measurement over, while switching Preview ↔ Read for
    // the *same* html keeps whatever was already measured.
    //
    // A View reattached with a [document] it already finished loading (e.g. backing out of Read
    // mode) may never receive another onPageFinished/onPageCommitVisible: those fire at most once
    // per load, and onPageCommitVisible specifically needs a Surface to paint into that a detached
    // View doesn't have — see ScrollGatedWebView's doc. [contentHeightPx] needs no window to be
    // accurate, since it reflects the page's already-computed layout, so it's read directly here
    // as a one-time fallback. This is only safe to trust exactly here, at fresh composition entry:
    // `webView.loadedDocument == document` is true only when nothing in *this* recomposition pass
    // dispatched a new load for a *different* document — a genuine document change leaves
    // `webView.loadedDocument` holding the *previous* document at this point (the reload that
    // updates it hasn't run yet), which fails the check and skips the probe.
    val hasMatchingLoad = webView.loadedDocument == document
    var measuredContentHeightPx by
        remember(webView, document) {
            val recoveredHeightPx = if (hasMatchingLoad) contentHeightPx(webView).takeIf { it > 0 } else null
            mutableIntStateOf(recoveredHeightPx ?: if (hasMatchingLoad) webView.measuredDocumentHeightPx else 0)
        }
    var isPageVisible by
        remember(webView, document) {
            mutableStateOf(webView.visibleDocument == document || (hasMatchingLoad && contentHeightPx(webView) > 0))
        }

    val capPx = with(density) { previewCapDp.dp.roundToPx() }
    val placeholderHeightPx = with(density) { README_LOADING_PLACEHOLDER_DP.dp.roundToPx() }
    val targetPreviewHeightPx =
        if (measuredContentHeightPx == 0) placeholderHeightPx else min(measuredContentHeightPx, capPx)
    // Animates the initial placeholder -> measured grow-in. Only relevant the first time a given
    // README is ever shown this session — a repo revisited later already has its real height
    // known (measuredContentHeightPx survives via the html key above only within one composition
    // lifetime, but the WebView's own instant re-render from its retained document means even a
    // fresh measurement lands within a frame or two, not a visible multi-hundred-ms grow).
    val animatedPreviewHeightPx by
        animateIntAsState(
            targetValue = targetPreviewHeightPx,
            animationSpec = tween(durationMillis = EXPAND_ANIMATION_MS, easing = FastOutSlowInEasing),
            label = "readmePreviewHeight",
        )

    val isPreview = mode == ReadmeViewMode.Preview

    Box(
        modifier =
            modifier.fillMaxWidth().let {
                if (isPreview) {
                    it
                        .height(with(density) { animatedPreviewHeightPx.toDp() })
                        // See ScrollGatedWebView's doc: the WebView itself declines touches in
                        // Preview mode, so this is what actually receives the resulting tap.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onExpandRequested,
                        )
                } else {
                    it.fillMaxSize()
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        AndroidView(
            // matchParentSize (not a second, independently-computed height) so this can never
            // land a frame out of sync with the Box that establishes the actual size above.
            modifier = Modifier.matchParentSize(),
            factory = {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView
            },
            update = { view ->
                view.touchEnabled = mode == ReadmeViewMode.Read
                // Reassigned every recomposition (cheap) rather than only in `factory`, because
                // the callback closes over this composition's current height and event handlers
                // while the native View can survive LazyColumn disposal/re-attachment.
                view.webViewClient =
                    ReadmeWebViewClient(
                        context = context,
                        onContentHeightChanged = { measuredPx ->
                            if (view.loadedDocument == document) {
                                view.measuredDocumentHeightPx = measuredPx
                                measuredContentHeightPx = measuredPx
                                onContentHeightMeasured(measuredPx)
                            }
                        },
                        onPageVisible = {
                            if (view.loadedDocument == document) {
                                view.visibleDocument = document
                                isPageVisible = true
                            }
                        },
                    )
                // AndroidView re-invokes `update` on every recomposition, not just when [document]
                // actually changed. Its loaded/visible/measurement state is tracked on the View
                // itself so reattaching the preview after fullscreen read mode restores the
                // already-rendered state instead of waiting forever for a callback that will not
                // fire a second time.
                if (view.loadedDocument != document) {
                    view.loadedDocument = document
                    view.visibleDocument = null
                    view.measuredDocumentHeightPx = 0
                    view.loadDataWithBaseURL(README_BASE_URL, document, "text/html", "utf-8", null)
                }
            },
            onRelease = { view -> (view.parent as? ViewGroup)?.removeView(view) },
        )

        // A newly-created fullscreen reader parses the already-fetched HTML asynchronously.
        // Keep that short interval explicit instead of presenting another ambiguous blank screen.
        if (!isPageVisible) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.Center),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (isPreview) {
            // Fades the capped edge out instead of slicing a line of text in half, so the preview
            // reads as "there's more below" rather than a rendering fault. Only while there
            // genuinely is more: a README shorter than the cap has nothing to fade.
            if (measuredContentHeightPx > capPx) {
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

// Default preview cap — roughly a phone screen's worth: enough to judge the repo, short enough
// that the sheet's other sections stay reachable without a long scroll. Overridable per call
// site; internal (not private) so a caller deciding whether to show its own "show more" affordance
// can compare against the same threshold this composable actually caps at, instead of guessing.
internal const val README_PREVIEW_CAP_DP = 420

// Long enough to read as a deliberate reveal rather than a snap, short enough not to make the
// user wait for content that is already rendered.
private const val EXPAND_ANIMATION_MS = 320

private const val FADE_SCRIM_HEIGHT_DP = 56

package com.batuhan.reposwipe.feature.swipe

import androidx.paging.LoadState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeScreenLoadingStateTest {
    @Test
    fun `shows the spinner while the mediator is loading and nothing has been shown yet`() {
        val result = shouldShowFullScreenLoading(LoadState.Loading, LoadState.NotLoading(false), hasShownContent = false)

        assertTrue(result)
    }

    @Test
    fun `shows the spinner while the local source is loading and nothing has been shown yet`() {
        val result = shouldShowFullScreenLoading(LoadState.NotLoading(false), LoadState.Loading, hasShownContent = false)

        assertTrue(result)
    }

    @Test
    fun `shows the spinner when there is no mediator at all but the source is loading`() {
        val result =
            shouldShowFullScreenLoading(mediatorRefresh = null, sourceRefresh = LoadState.Loading, hasShownContent = false)

        assertTrue(result)
    }

    @Test
    fun `does not show the spinner once content has already been shown, even if the source flips back to loading`() {
        // This is the cold-start flicker this function exists to prevent: every RemoteMediator
        // REFRESH invalidates Room's PagingSource, and the new generation's own local reload flips
        // source.refresh back to Loading for a beat even though the deck already has cards on
        // screen — see the doc on shouldShowFullScreenLoading / its call site in SwipeScreen.
        val result = shouldShowFullScreenLoading(LoadState.NotLoading(false), LoadState.Loading, hasShownContent = true)

        assertFalse(result)
    }

    @Test
    fun `does not show the spinner once content has already been shown, even if the mediator flips back to loading`() {
        val result = shouldShowFullScreenLoading(LoadState.Loading, LoadState.NotLoading(false), hasShownContent = true)

        assertFalse(result)
    }

    @Test
    fun `does not show the spinner when neither signal is loading`() {
        val result =
            shouldShowFullScreenLoading(LoadState.NotLoading(false), LoadState.NotLoading(false), hasShownContent = false)

        assertFalse(result)
    }
}

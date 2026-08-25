package com.batuhan.reposwipe.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoRemoteMediatorTest {
    @Test
    fun `validPageRange for a zero-result query always resolves to page 1 only`() {
        val range = RepoRemoteMediator.validPageRange(totalCount = 0, pageSize = 20, pagePool = 10)

        assertEquals(1..1, range)
    }

    @Test
    fun `validPageRange for a narrow query narrower than one page still resolves to page 1 only`() {
        val range = RepoRemoteMediator.validPageRange(totalCount = 15, pageSize = 20, pagePool = 10)

        assertEquals(1..1, range)
    }

    @Test
    fun `validPageRange spans exactly the pages the total actually has, when under the pool size`() {
        val range = RepoRemoteMediator.validPageRange(totalCount = 21, pageSize = 20, pagePool = 10)

        assertEquals(1..2, range)
    }

    @Test
    fun `validPageRange never exceeds the page pool even for a huge result set`() {
        val range = RepoRemoteMediator.validPageRange(totalCount = 1_000, pageSize = 20, pagePool = 10)

        assertEquals(1..10, range)
    }

    @Test
    fun `validPageRange exactly fills the pool when the total lines up with it`() {
        val range = RepoRemoteMediator.validPageRange(totalCount = 200, pageSize = 20, pagePool = 10)

        assertEquals(1..10, range)
    }

    @Test
    fun `isEndOfPagination is true whenever the fetched page came back empty`() {
        assertTrue(RepoRemoteMediator.isEndOfPagination(itemsEmpty = true, page = 1, pageSize = 20, maxResults = 1_000))
    }

    @Test
    fun `isEndOfPagination is false while under the API's result cap with items present`() {
        assertTrue(!RepoRemoteMediator.isEndOfPagination(itemsEmpty = false, page = 5, pageSize = 20, maxResults = 1_000))
    }

    @Test
    fun `isEndOfPagination is true once the page boundary reaches the API's result cap`() {
        assertTrue(RepoRemoteMediator.isEndOfPagination(itemsEmpty = false, page = 50, pageSize = 20, maxResults = 1_000))
    }
}

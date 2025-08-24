package com.mrunicorn.sb.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RepositorySortFilterTest {
    private val items = listOf(
        Item(id = "1", type = ItemType.TEXT, text = "b", createdAt = 2),
        Item(id = "2", type = ItemType.TEXT, text = "a", createdAt = 3),
        Item(id = "3", type = ItemType.LINK, text = "c", createdAt = 1)
    )

    @Test
    fun sortsByName() {
        val sorted = Repository.sortAndFilter(items, ItemFilter.All, ItemSort.Name)
        assertEquals(listOf("a", "b", "c"), sorted.map { it.text })
    }

    @Test
    fun filtersLinks() {
        val filtered = Repository.sortAndFilter(items, ItemFilter.Links, ItemSort.Date)
        assertEquals(1, filtered.size)
        assertEquals(ItemType.LINK, filtered.first().type)
    }
}

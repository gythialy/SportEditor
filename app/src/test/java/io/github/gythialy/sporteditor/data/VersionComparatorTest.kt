package io.github.gythialy.sporteditor.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun newerPatch_isNewer() {
        assertTrue(VersionComparator.isNewer("v1.0.1", "1.0.0"))
    }

    @Test
    fun newerMinor_isNewer() {
        assertTrue(VersionComparator.isNewer("v1.1.0", "1.0.0"))
    }

    @Test
    fun sameVersion_isNotNewer() {
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.0.0"))
    }

    @Test
    fun olderVersion_isNotNewer() {
        assertFalse(VersionComparator.isNewer("v0.9.9", "1.0.0"))
    }

    @Test
    fun missingSegments_areTreatedAsZero() {
        assertTrue(VersionComparator.isNewer("v2.0", "1.9.9"))
        assertFalse(VersionComparator.isNewer("v1.0", "1.0.0"))
    }

    @Test
    fun suffix_isIgnored() {
        assertTrue(VersionComparator.isNewer("v1.1.0-rc1", "1.0.0"))
    }

    @Test
    fun invalidVersion_isNotNewer() {
        assertFalse(VersionComparator.isNewer("latest", "1.0.0"))
        assertFalse(VersionComparator.isNewer("v1.1.0", "dev"))
    }
}

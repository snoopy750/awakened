package ch.awakened.app

import ch.awakened.app.integrations.UpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateManagerTest {
    @Test
    fun `build number is parsed from release tag`() {
        assertEquals(12, UpdateManager.buildFromTag("v1.0.12"))
        assertEquals(4, UpdateManager.buildFromTag("1.0.4"))
        assertNull(UpdateManager.buildFromTag("latest"))
    }
}

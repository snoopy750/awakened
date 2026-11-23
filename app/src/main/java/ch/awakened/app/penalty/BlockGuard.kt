package ch.awakened.app.penalty

import android.content.Context
import android.content.Intent
import ch.awakened.app.container
import ch.awakened.app.data.prefs.PenaltyState

/** Gemeinsame Entscheidung für AccessibilityService und UsageStats-Fallback. */
object BlockGuard {
    @Volatile private var lastShown = 0L

    fun shouldBlock(context: Context, pkg: String, penalty: PenaltyState, blocked: Set<String>, dynamicExempt: Set<String>): Boolean {
        if (!penalty.active) return false
        if (System.currentTimeMillis() >= penalty.unlockAt) return false
        if (pkg !in blocked) return false
        return !context.container.repository.isExempt(pkg, dynamicExempt)
    }

    fun showPenaltyScreen(context: Context, blockedPkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastShown < 800) return
        lastShown = now
        val intent = Intent(context, PenaltyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(PenaltyActivity.EXTRA_BLOCKED, blockedPkg)
        }
        runCatching { context.startActivity(intent) }
    }
}

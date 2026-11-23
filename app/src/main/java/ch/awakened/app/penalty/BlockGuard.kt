package ch.awakened.app.penalty

import android.content.Context
import ch.awakened.app.container
import ch.awakened.app.data.prefs.PenaltyState

/** Entscheidet, ob eine Vordergrund-App während der Penalty Zone gesperrt wird. */
object BlockGuard {
    fun shouldBlock(context: Context, pkg: String, penalty: PenaltyState, blocked: Set<String>, dynamicExempt: Set<String>): Boolean {
        if (!penalty.active) return false
        if (System.currentTimeMillis() >= penalty.unlockAt) return false
        if (pkg !in blocked) return false
        return !context.container.repository.isExempt(pkg, dynamicExempt)
    }
}

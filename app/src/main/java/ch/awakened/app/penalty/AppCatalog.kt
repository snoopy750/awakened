package ch.awakened.app.penalty

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager

data class InstalledApp(val packageName: String, val label: String, val icon: Drawable?, val exempt: Boolean, val suggested: Boolean)

object AppCatalog {
    private val SUGGESTED = listOf(
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.google.android.youtube",
        "com.facebook.katana", "com.snapchat.android", "com.twitter.android", "com.reddit.frontpage",
        "com.netflix.mediaclient", "tv.twitch.android.app", "com.pinterest", "com.facebook.orca",
        "com.supercell.clashofclans", "com.supercell.clashroyale", "com.king.candycrushsaga",
        "com.roblox.client", "com.mojang.minecraftpe", "com.tencent.ig", "com.activision.callofduty.shooter",
        "com.dts.freefireth", "com.disney.disneyplus", "com.amazon.avod.thirdpartyclient",
    )

    /** Systemrelevante Pakete, die dynamisch bestimmt werden (Standard-Telefon, -SMS, Launcher). */
    fun dynamicExemptions(context: Context): Set<String> {
        val set = mutableSetOf<String>()
        runCatching {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(set::add)
        }
        runCatching { Telephony.Sms.getDefaultSmsPackage(context)?.let(set::add) }
        runCatching {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(home, 0).forEach { set.add(it.activityInfo.packageName) }
        }
        return set
    }

    fun isMapsApp(info: ApplicationInfo): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.category == ApplicationInfo.CATEGORY_MAPS

    fun load(context: Context, isExempt: (String) -> Boolean): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val dynamic = dynamicExemptions(context)
        return pm.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                val ai = ri.activityInfo.applicationInfo
                InstalledApp(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    icon = runCatching { ri.loadIcon(pm) }.getOrNull(),
                    exempt = isExempt(pkg) || pkg in dynamic || isMapsApp(ai),
                    suggested = pkg in SUGGESTED || (Build.VERSION.SDK_INT >= 26 && ai.category in setOf(
                        ApplicationInfo.CATEGORY_GAME, ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_VIDEO,
                    )),
                )
            }
            .sortedWith(compareBy<InstalledApp> { it.exempt }.thenByDescending { it.suggested }.thenBy { it.label.lowercase() })
    }
}

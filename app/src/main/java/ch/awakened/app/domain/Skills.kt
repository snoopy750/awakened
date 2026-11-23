package ch.awakened.app.domain

data class StatBlock(val str: Int = 10, val agi: Int = 10, val vit: Int = 10, val end: Int = 10, val int: Int = 10) {
    operator fun get(stat: Stat): Int = when (stat) {
        Stat.STR -> str; Stat.AGI -> agi; Stat.VIT -> vit; Stat.END -> end; Stat.INT -> int
    }

    fun plus(stat: Stat, amount: Int = 1): StatBlock = when (stat) {
        Stat.STR -> copy(str = str + amount)
        Stat.AGI -> copy(agi = agi + amount)
        Stat.VIT -> copy(vit = vit + amount)
        Stat.END -> copy(end = end + amount)
        Stat.INT -> copy(int = int + amount)
    }
}

enum class SkillType(val label: String) { ACTIVE("Aktiv"), PASSIVE("Passiv") }

enum class Skill(
    val displayName: String,
    val type: SkillType,
    val description: String,
    val requiredLevel: Int,
    val requiredStat: Stat? = null,
    val requiredStatValue: Int = 0,
) {
    SECOND_WIND(
        "Zweiter Atem", SkillType.PASSIVE,
        "1 Joker pro Woche: Eine verpasste Daily Quest löst keine Penalty Zone aus.",
        requiredLevel = 5,
    ),
    DOUBLE_XP_DAY(
        "Doppel-XP-Tag", SkillType.ACTIVE,
        "Einmal pro Woche aktivierbar: Alle XP des heutigen Tages werden verdoppelt.",
        requiredLevel = 10,
    ),
    ENDURANCE_BOOST(
        "Ausdauer-Boost", SkillType.PASSIVE,
        "Ausdauer-Einheiten zählen 25 % mehr für die Daily Quest.",
        requiredLevel = 8, requiredStat = Stat.END, requiredStatValue = 15,
    ),
    IRON_WILL(
        "Eiserner Wille", SkillType.PASSIVE,
        "Die Sperrzeit der Penalty Zone wird halbiert.",
        requiredLevel = 12, requiredStat = Stat.VIT, requiredStatValue = 18,
    ),
    POWER_SURGE(
        "Kraftschub", SkillType.PASSIVE,
        "+10 % XP auf alle Kraftübungen.",
        requiredLevel = 6, requiredStat = Stat.STR, requiredStatValue = 15,
    ),
    SHARP_MIND(
        "Scharfer Verstand", SkillType.PASSIVE,
        "+20 % XP auf Challenges der Kategorie „Anderes“.",
        requiredLevel = 4, requiredStat = Stat.INT, requiredStatValue = 14,
    ),
    SHADOW_STEP(
        "Schattenschritt", SkillType.PASSIVE,
        "Pausen im Training werden um 20 % verkürzt, Training gibt +10 % XP.",
        requiredLevel = 15, requiredStat = Stat.AGI, requiredStatValue = 20,
    ),
    MONARCH_AURA(
        "Aura des Erwachten", SkillType.PASSIVE,
        "+15 % XP auf alles. Nur für die Stärksten.",
        requiredLevel = 40,
    );

    fun isUnlocked(level: Int, stats: StatBlock): Boolean =
        level >= requiredLevel && (requiredStat == null || stats[requiredStat] >= requiredStatValue)

    fun requirementText(): String = buildString {
        append("Level $requiredLevel")
        if (requiredStat != null) append(" · ${requiredStat.short} ≥ $requiredStatValue")
    }

    companion object {
        fun unlocked(level: Int, stats: StatBlock): Set<Skill> = entries.filter { it.isUnlocked(level, stats) }.toSet()
    }
}

/** Zusammenfassung des Spielerfortschritts für Titel-Freischaltungen. */
data class Achievements(
    val level: Int,
    val bestStreak: Int,
    val totalPushups: Long,
    val totalCardioMinutes: Long,
    val totalWorkouts: Int,
    val penaltiesCleared: Int,
    val challengesCompleted: Int,
)

enum class HunterTitle(val displayName: String, val condition: String, val check: (Achievements) -> Boolean) {
    AWAKENED("Der Erwachte", "Profil erstellt", { true }),
    PERSISTENT("Unermüdlich", "7-Tage-Streak", { it.bestStreak >= 7 }),
    IRON_DISCIPLINE("Eiserne Disziplin", "30-Tage-Streak", { it.bestStreak >= 30 }),
    IRON_FIST("Eisenfaust", "1000 Liegestütze insgesamt", { it.totalPushups >= 1000 }),
    WIND_RUNNER("Windläufer", "600 Ausdauer-Minuten insgesamt", { it.totalCardioMinutes >= 600 }),
    WARRIOR("Krieger", "25 Trainings abgeschlossen", { it.totalWorkouts >= 25 }),
    SURVIVOR("Überlebender der Penalty Zone", "Eine Straf-Quest abgeschlossen", { it.penaltiesCleared >= 1 }),
    SCHOLAR("Gelehrter", "10 Challenges abgeschlossen", { it.challengesCompleted >= 10 }),
    ELITE("Elite-Hunter", "Level 35 erreicht", { it.level >= 35 }),
    SHADOW_SOVEREIGN("Herrscher der Schatten", "Level 70 erreicht", { it.level >= 70 });

    companion object {
        fun unlocked(a: Achievements): List<HunterTitle> = entries.filter { it.check(a) }
    }
}

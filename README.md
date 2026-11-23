# AWAKENED

**Ein Fitness-RPG für Android im Stil eines „System“-Interfaces.** Tägliche Quests, XP, Level, Ränge von E bis S, Skills und eine PENALTY ZONE, die deine Ablenkungs-Apps sperrt, wenn du deine Daily Quest nicht erledigst.

**[⬇ Neueste APK herunterladen](https://github.com/{{OWNER}}/awakened/releases/latest/download/awakened.apk)** · [Alle Versionen](https://github.com/{{OWNER}}/awakened/releases)

---

## Funktionen

| Bereich | Was die App macht |
|---|---|
| **Daily Quest** | Liegestütze, Sit-ups, Kniebeugen, Seilspringen und Ausdauer (Minuten oder km). Zähler mit +1/+5/+10, manuelle Eingabe, Fortschrittsbalken. Ziele wachsen langsam mit dem Level. Bonus-XP, wenn alles erledigt ist. |
| **Weekly Quests** | 3× Ausdauer ≥ 30 min, 7-Tage-Streak, Wochenziel Liegestütze |
| **Challenges** | Eigene Ziele (Fitness oder Anderes, z. B. lesen, lernen, früh aufstehen), täglich, wöchentlich oder einmalig, mit eigenem XP-Wert |
| **Level & Ränge** | Benötigte XP = 100 × Level^1.5. Level-up mit Vollbild-Animation, Sound und Vibration. Ränge E → D → C → B → A → S |
| **Stats & Skills** | 3 Stat-Punkte pro Level für STR, AGI, VIT, END, INT. Skills wie „Zweiter Atem“ (1 Joker pro Woche), „Doppel-XP-Tag“, „Ausdauer-Boost“ |
| **Training** | Übungsabfolge mit Runden, Stoppuhr, Pausen-Timer, Ausdauer-Block, Kalorienschätzung (MET × Gewicht × Dauer), XP-Gutschrift, Start der Spotify-Playlist |
| **PENALTY ZONE** | Daily Quest bis zur Deadline (Standard 23:59) verpasst oder X Tage inaktiv → gewählte Apps sind gesperrt, bis die Straf-Quest erfüllt ist oder die Sperrzeit abläuft |
| **Fitness-Tracker** | Health Connect: Schritte geben XP, Lauf- und Velo-Einheiten zählen automatisch zur Ausdauer-Quest. Funktioniert auch ganz ohne. |
| **Statistik** | XP-Verlauf, 28-Tage-Kalender, Gesamtwiederholungen, Kalorien, Gewichtsverlauf mit Diagramm, BMI und Grundumsatz (Mifflin-St Jeor) |
| **Backup** | Export und Import aller Daten als JSON-Datei |

---

## Installation (für Spieler)

Voraussetzung: Android 8.0 oder neuer.

### 1. APK herunterladen
Öffne auf dem Handy diesen Link und lade `awakened.apk` herunter:
**https://github.com/{{OWNER}}/awakened/releases/latest/download/awakened.apk**

### 2. „Unbekannte Quellen“ erlauben
Android installiert Apps ausserhalb des Play Stores nur mit deiner Erlaubnis:

1. Tippe nach dem Download auf die Datei (Benachrichtigung oder App **Dateien → Downloads**).
2. Android meldet: *„Aus Sicherheitsgründen ist die Installation unbekannter Apps aus dieser Quelle nicht zulässig.“* Tippe auf **Einstellungen**.
3. Aktiviere **„Dieser Quelle vertrauen“** bzw. **„Apps aus dieser Quelle zulassen“**.
4. Zurück und auf **Installieren** tippen.
5. Falls Google Play Protect warnt („Unbekannte App“): **Weitere Details → Trotzdem installieren**.

> Samsung: Einstellungen → Apps → ⋮ → Spezieller Zugriff → Unbekannte Apps installieren → Browser bzw. „Eigene Dateien“ erlauben.

### 3. Profil erstellen
Beim ersten Start fragt das System nach Name, Alter, Geschlecht, Grösse, Gewicht, Fitnesslevel und Ziel. Daraus berechnet es BMI, Grundumsatz und die Startwerte deiner Quests.

### 4. Berechtigungen geben
Die App führt dich durch jede Berechtigung und erklärt, wofür sie gebraucht wird:

| Berechtigung | Wozu | Wo |
|---|---|---|
| **Benachrichtigungen** | Erinnerungen („Die Daily Quest ist noch nicht abgeschlossen. Verbleibende Zeit: 3h“) | Dialog beim Start |
| **Bedienungshilfe** (wichtigste) | Erkennt, welche App geöffnet wird, und zeigt während der PENALTY ZONE den Sperrbildschirm. Liest keine Inhalte. | Einstellungen → Bedienungshilfen → Installierte Apps → **AWAKENED Penalty Zone** |
| **Über anderen Apps einblenden** | Sperrbildschirm sofort anzeigen | Wird von der App geöffnet |
| **Nutzungszugriff** | Reserve, falls die Bedienungshilfe beendet wurde | Wird von der App geöffnet |
| **Akku-Optimierung ausnehmen** | Damit die Tagesprüfung zuverlässig läuft | Wird von der App geöffnet |

> **Wichtig ab Android 13 – „Eingeschränkte Einstellung“:**
> Bei Apps ausserhalb des Play Stores ist der Schalter der Bedienungshilfe zuerst ausgegraut. Lösung:
> **Einstellungen → Apps → AWAKENED → ⋮ (oben rechts) → „Eingeschränkte Einstellungen zulassen“** und danach die Bedienungshilfe einschalten. In der App gibt es dafür den Knopf „App-Info öffnen“.

### 5. Apps für die Sperre wählen
Wähle Apps wie Instagram, TikTok, YouTube oder Spiele (Knopf „Vorschläge“). **Telefon, SMS, Karten, Banking/TWINT, Einstellungen und AWAKENED selbst werden nie gesperrt.**

### 6. Optional
- **Spotify:** Einstellungen → Training & Musik → Playlist-Link einfügen (in Spotify: Playlist → ⋮ → Teilen → Link kopieren). Beim Trainingsstart öffnet sich Spotify und spielt die Playlist ab.
- **Health Connect:** Einstellungen → Fitness-Tracker → Verbinden. Daten von Samsung Health, Google Fit, Garmin usw. fliessen automatisch ein.

### Updates
Neue Version einfach wie oben herunterladen und über die alte installieren. Alle Daten bleiben erhalten (gleiche Signatur). Vor grossen Updates empfiehlt sich ein Backup: Einstellungen → Daten → Backup.

---

## Spielregeln

### XP
| Aktion | XP |
|---|---|
| Übung der Daily Quest erfüllt | 20 |
| Alle Dailies erfüllt | 50 + 2 × Level |
| Weekly Quest | 150 |
| Training | 2 pro Minute + 1 pro 10 kcal |
| Schritte (Health Connect) | 5 pro 1000 Schritte, max. 100/Tag |
| Challenge | selbst festgelegt |

Stats erhöhen die XP: +1 % pro Punkt über 10 im passenden Stat (max. +50 %). STR → Kraftübungen, AGI → Seilspringen/Training, END → Ausdauer, INT → Challenges „Anderes“, VIT → kürzere Sperrzeit.

### Level & Ränge
Benötigte XP für das nächste Level: `100 × Level^1.5` (Level 1 → 100, Level 10 → 3162).

| Rang | ab Level |
|---|---|
| E | 1 |
| D | 10 |
| C | 20 |
| B | 35 |
| A | 50 |
| S | 70 |

### Skills
| Skill | Voraussetzung | Wirkung |
|---|---|---|
| Scharfer Verstand | Level 4, INT ≥ 14 | +20 % XP auf Challenges „Anderes“ |
| Zweiter Atem | Level 5 | 1 Joker pro Woche gegen eine verpasste Daily Quest (automatisch) |
| Kraftschub | Level 6, STR ≥ 15 | +10 % XP auf Kraftübungen |
| Ausdauer-Boost | Level 8, END ≥ 15 | Ausdauer zählt 25 % mehr |
| Doppel-XP-Tag | Level 10 | 1× pro Woche: doppelte XP für einen Tag |
| Eiserner Wille | Level 12, VIT ≥ 18 | Sperrzeit halbiert |
| Schattenschritt | Level 15, AGI ≥ 20 | Pausen −20 %, Training +10 % XP |
| Aura des Erwachten | Level 40 | +15 % XP auf alles |

### PENALTY ZONE
- **Auslöser:** Daily Quest nicht bis zur Deadline erledigt (einstellbar, Standard 23:59) oder X Tage ohne Aktivität (einstellbar, Standard 3, 0 = aus). Der Tag der Profilerstellung zählt nicht.
- **Folge:** Die gewählten Apps zeigen beim Öffnen einen roten Vollbild-Sperrbildschirm.
- **Entsperren:** Straf-Quest erfüllen (z. B. 50 Burpees + 20 min Ausdauer, abhängig vom Fitnesslevel) oder Ablauf der Sperrzeit (einstellbar, Standard 12 h).
- Die Prüfung läuft im Hintergrund alle 15 Minuten, direkt nach der Deadline und überlebt einen Neustart.

---

## Datenschutz
Alle Daten bleiben auf dem Gerät. Die App hat keine Internet-Berechtigung, kein Tracking, keine Werbung und kein Konto. Die Bedienungshilfe wertet nur den Paketnamen der geöffneten App aus, keine Bildschirminhalte.

---

## Für Entwickler

### Tech-Stack
Kotlin · Jetpack Compose · Material 3 (eigenes Dark-Theme) · MVVM · Room · DataStore · WorkManager · Health Connect · kotlinx.serialization · minSdk 26 · targetSdk 36

### Projektstruktur
```
app/src/main/java/ch/awakened/app/
├── domain/        Reine Spiellogik: XP-Kurve, Ränge, Quest-Skalierung, Penalty-Regeln, Fitnessformeln, Skills
├── data/db/       Room-Entities und DAOs
├── data/prefs/    DataStore: Einstellungen, Penalty-Zustand, Meta
├── data/repo/     GameRepository: Quests, XP, Weekly, Challenges, Training, Penalty-Auswertung
├── penalty/       AccessibilityService, UsageStats-Fallback, Sperrbildschirm, Berechtigungen
├── work/          WorkManager-Prüfung, Deadline-Job, BootReceiver
├── integrations/  Health Connect, Spotify, JSON-Backup
├── system/        Benachrichtigungen, Sound und Vibration
└── ui/            Theme, System-Komponenten, Screens, ViewModels
```

### Lokal bauen
```bash
./gradlew :app:testDebugUnitTest     # Unit-Tests (XP/Level, Quest-Skalierung, Penalty-Regeln, Formeln)
./gradlew :app:assembleDebug         # Debug-APK
./gradlew :app:assembleRelease       # Release-APK (ohne Keystore mit Debug-Schlüssel signiert)
```
Für lokal signierte Release-Builds eine `keystore.properties` im Projektordner anlegen (wird nicht eingecheckt):
```properties
storeFile=/pfad/zu/awakened-release.jks
storePassword=…
keyAlias=awakened
keyPassword=…
```

### Signierte Release-APK über GitHub Actions
Der Workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) läuft bei jedem Push auf `main`: Unit-Tests → signierte Release-APK → Artifact → GitHub Release `v1.0.<Build-Nummer>` mit `awakened.apk`.

**Einmalige Einrichtung:**

1. Keystore erzeugen (gut aufbewahren! Ohne ihn lassen sich keine Updates mehr über bestehende Installationen spielen):
   ```bash
   keytool -genkeypair -v -keystore awakened-release.jks -alias awakened \
     -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=AWAKENED"
   ```
2. Keystore in Base64 umwandeln:
   ```bash
   base64 -i awakened-release.jks | tr -d '\n' > keystore.b64
   ```
3. Auf GitHub unter **Settings → Secrets and variables → Actions → New repository secret** anlegen:

   | Secret | Inhalt |
   |---|---|
   | `KEYSTORE_BASE64` | Inhalt von `keystore.b64` |
   | `KEYSTORE_PASSWORD` | Passwort des Keystores |
   | `KEY_ALIAS` | `awakened` |
   | `KEY_PASSWORD` | Passwort des Schlüssels |

   Oder per CLI: `gh secret set KEYSTORE_BASE64 < keystore.b64` usw.
4. Pushen. Die fertige APK liegt danach unter *Actions → Build & Release APK → Artifacts* und als Release.

Ohne Secrets baut der Workflow trotzdem (mit Debug-Signatur) und lädt das Artifact hoch, erstellt aber kein Release.

### Spotify
Das Spotify App Remote SDK braucht eine registrierte Client-ID samt Signatur-Fingerprint und ist nicht über Maven verfügbar. Deshalb öffnet AWAKENED die Playlist per `spotify:`-URI-Intent und startet die Wiedergabe über die Media-Play-Taste. Danach kehrt die App in den Vordergrund zurück.

---

## Lizenzen
- Schrift [Rajdhani](https://fonts.google.com/specimen/Rajdhani) © Indian Type Foundry, SIL Open Font License 1.1 (siehe [`licenses/Rajdhani-OFL.txt`](licenses/Rajdhani-OFL.txt))
- App-Symbol, Sounds und Grafiken sind eigene Gestaltung.

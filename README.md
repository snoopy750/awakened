# AWAKENED

**Ein Fitness-RPG für Android im Stil eines „System“-Interfaces.** Tägliche Quests, XP, Level, Ränge von E bis S, Skills und eine PENALTY ZONE, die deine Ablenkungs-Apps sperrt, wenn du deine Daily Quest nicht erledigst.

**[⬇ Neueste APK herunterladen](https://github.com/snoopy750/awakened/releases/latest/download/awakened.apk)** · [Alle Versionen](https://github.com/snoopy750/awakened/releases)

**iPhone:** **[AWAKENED als Web-App öffnen](https://snoopy750.github.io/awakened/)** (Anleitung [unten](#iphone-web-app))

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
**https://github.com/snoopy750/awakened/releases/latest/download/awakened.apk**

### 2. „Unbekannte Quellen“ erlauben
Android installiert Apps ausserhalb des Play Stores nur mit deiner Erlaubnis:

1. Tippe nach dem Download auf die Datei (Benachrichtigung oder App **Dateien → Downloads**).
2. Android meldet: *„Aus Sicherheitsgründen ist die Installation unbekannter Apps aus dieser Quelle nicht zulässig.“* Tippe auf **Einstellungen**.
3. Aktiviere **„Dieser Quelle vertrauen“** bzw. **„Apps aus dieser Quelle zulassen“**.
4. Zurück und auf **Installieren** tippen.
5. Falls Google Play Protect warnt („Unbekannte App“): **Weitere Details → Trotzdem installieren**.
6. Xiaomi/HyperOS: Fragt der eigene Sicherheits-Scanner nach, **„Trotzdem installieren“** wählen.

> Samsung: Einstellungen → Apps → ⋮ → Spezieller Zugriff → Unbekannte Apps installieren → Browser bzw. „Eigene Dateien“ erlauben.

### 3. Profil erstellen
Beim ersten Start fragt das System nach Name, Alter, Geschlecht, Grösse, Gewicht, Fitnesslevel und Ziel. Daraus berechnet es BMI, Grundumsatz und die Startwerte deiner Quests.

### 4. Berechtigungen geben
Die App führt dich durch jede Berechtigung und erklärt, wofür sie gebraucht wird:

| Berechtigung | Wozu | Wo |
|---|---|---|
| **Benachrichtigungen** | Erinnerungen („Die Daily Quest ist noch nicht abgeschlossen. Verbleibende Zeit: 3h“) | Dialog beim Start |
| **Nutzungszugriff** (wichtig) | Erkennt, welche App im Vordergrund ist. Liest keine Inhalte. | Liste → **AWAKENED** → „Nutzungszugriff erlauben“ |
| **Über anderen Apps einblenden** (wichtig) | Legt den roten PENALTY-ZONE-Bildschirm über gesperrte Apps | **AWAKENED** → erlauben |
| **Akku-Optimierung ausnehmen** | Damit Tagesprüfung und Sperre nicht gestoppt werden | Dialog der App |

Die App öffnet die richtige Einstellungsseite jeweils selbst.

> **Xiaomi, Samsung, Huawei, OnePlus:** Diese Hersteller beenden Hintergrund-Apps besonders aggressiv. Zusätzlich unter **Einstellungen → Apps → AWAKENED** den **Autostart** erlauben und den **Akkusparmodus auf „Keine Einschränkungen“** stellen.

AWAKENED zeigt eine stille Benachrichtigung „[SYSTEM] aktiv“. Sie hält die Überwachung zuverlässig am Laufen. Du kannst sie unter Einstellungen → „Dauerhafte Überwachung“ abschalten; dann läuft die Überwachung nur während der PENALTY ZONE, die Prüfung der Deadline kann sich aber verzögern.

### 5. Apps für die Sperre wählen
Wähle Apps wie Instagram, TikTok, YouTube oder Spiele (Knopf „Vorschläge“). **Telefon, SMS, Karten, Banking/TWINT, Einstellungen und AWAKENED selbst werden nie gesperrt.**

### 6. Optional
- **Spotify:** Einstellungen → Training & Musik → Playlist-Link einfügen (in Spotify: Playlist → ⋮ → Teilen → Link kopieren). Beim Trainingsstart öffnet sich Spotify und spielt die Playlist ab.
- **Health Connect:** Einstellungen → Fitness-Tracker → Verbinden. Daten von Samsung Health, Google Fit, Garmin usw. fliessen automatisch ein.

### Updates
In der App: **Einstellungen → Update → „Nach Updates suchen“ → „Herunterladen & installieren“.** Beim ersten Update fragt Android einmalig, ob AWAKENED Apps installieren darf. Die App meldet sich beim Start auch selbst, wenn eine neue Version verfügbar ist.

Alternativ die neue Version wie oben herunterladen und über die alte installieren. Alle Daten bleiben erhalten (gleiche Signatur). Vor grossen Updates empfiehlt sich ein Backup: Einstellungen → Daten → Backup.

---

## iPhone (Web-App)

Für iPhone gibt es AWAKENED als Web-App. Sie ist gratis und braucht weder App Store noch Apple-Konto.

1. **[snoopy750.github.io/awakened](https://snoopy750.github.io/awakened/)** in **Safari** öffnen.
2. Unten auf das **Teilen-Symbol** tippen (Quadrat mit Pfeil) → **Zum Home-Bildschirm** → **Hinzufügen**.
3. AWAKENED ab jetzt immer über das neue Icon öffnen. Die App läuft dann im Vollbild und auch offline.

> Wichtig: Safari-Tab und Home-Bildschirm-App haben getrennte Speicher. Das Profil also erst nach Schritt 2 im Icon anlegen.

**Gleich wie auf Android:** Daily und Weekly Quests, Challenges, XP, Level-up mit Sound, Ränge, Stats, Skills, Titel, Training mit Timer und Spotify-Link, Statistik, Gewichtsverlauf, Backup als JSON-Datei.

**Unterschiede, weil iOS das für Web-Apps nicht erlaubt:**

| | iPhone (Web-App) |
|---|---|
| PENALTY ZONE | Sperrt AWAKENED selbst (nur noch Straf-Quest sichtbar), aber keine anderen Apps. Wer andere Apps sperren will, nutzt zusätzlich **Einstellungen → Bildschirmzeit → App-Limits**. |
| Erinnerungen | Kommen nicht von der App. Lösung: in der **Kurzbefehle**-App eine Automation „Tageszeit“ mit „Mitteilung anzeigen“ anlegen. Die genaue Anleitung steht in der App unter Einstellungen. |
| Apple Health, Vibration | Nicht verfügbar |

**Updates:** Die Web-App aktualisiert sich selbst. Liegt eine neue Version bereit, erscheint unten ein Hinweis zum Antippen. In den Einstellungen gibt es zusätzlich „Nach Updates suchen“.

Die Daten bleiben nur auf dem iPhone. Ab und zu unter Einstellungen → Daten eine Sicherung in iCloud Drive speichern.

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
- Die Prüfung läuft im Überwachungsdienst jede Minute, zusätzlich per WorkManager alle 15 Minuten und direkt nach der Deadline. Alles startet nach einem Neustart automatisch wieder.

---

## Datenschutz
Alle Daten bleiben auf dem Gerät. Kein Tracking, keine Werbung, kein Konto. Internet wird nur für die Update-Prüfung genutzt: Die App fragt bei GitHub die neueste Version ab und lädt bei Bedarf die APK herunter; dabei werden keine persönlichen Daten gesendet. Der Nutzungszugriff wird nur genutzt, um den Paketnamen der geöffneten App zu erkennen; Bildschirminhalte werden nie gelesen.

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
├── penalty/       Überwachungsdienst (UsageStats + Overlay), Sperrbildschirm, Berechtigungen
├── work/          WorkManager-Prüfung, Deadline-Job, BootReceiver
├── integrations/  Health Connect, Spotify, JSON-Backup
├── system/        Benachrichtigungen, Sound und Vibration
└── ui/            Theme, System-Komponenten, Screens, ViewModels

web/               iPhone-Web-App (ohne Build-Schritt, reines HTML/CSS/JavaScript)
├── js/domain.js   Spiellogik, identisch mit domain/ der Android-App
├── js/game.js     Spielzustand (localStorage), Quests, XP, Penalty-Auswertung
├── js/app.js      Oberfläche, Training, Dialoge, Updates
├── sw.js          Offline-Cache
└── test/          Tests: node --test web/test/*.test.mjs
```

Die Web-App wird bei jedem Push mit Änderungen in `web/` automatisch über GitHub Pages veröffentlicht (`.github/workflows/pages.yml`). Lokal testen: `python3 -m http.server -d web 8000`.

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
- Die Stimme 「起きろ」 im Level-up-Sound ist eine synthetische Sprachausgabe (ja-JP „Keita“) und nur für den privaten Gebrauch gedacht.

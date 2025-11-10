# Neue Funktionen: Automatische Clan-War-Erinnerungen

## Was ist neu?

Der Bot hat jetzt ein automatisches Erinnerungs-System für Clan-Kriege! Als Vize-Anführer oder Anführer kannst du jetzt automatische Nachrichten einrichten, die Spieler erinnern, ihre Decks im Clan-Krieg zu verwenden.

### Warum ist das nützlich?

- **Automatische Erinnerungen:** Der Bot erinnert Spieler selbstständig, wenn sie ihre Decks noch nicht vollständig genutzt haben
- **Flexible Zeitplanung:** Du entscheidest, wann die Erinnerungen gesendet werden (z.B. morgens, mittags, abends)
- **Mehr Kriegsteilnahme:** Weniger vergessene Decks bedeuten bessere Ergebnisse im Clan-Krieg
- **Zeitersparnis:** Du musst nicht mehr manuell nach inaktiven Spielern suchen

---

# Benutzeranleitung: Clan-War-Erinnerungen für Vize-Anführer

## So richtest du Erinnerungen ein

### Schritt 1: Erinnerung erstellen mit `/remindersadd`

Dieser Befehl erstellt eine neue automatische Erinnerung für deinen Clan.

#### Was du eingeben musst:

1. **Clan** - Wähle deinen Clan aus der Liste
2. **Channel** - Wähle den Discord-Kanal, in dem die Erinnerung erscheinen soll
3. **Time** - Gib die Uhrzeit ein im Format **HH:mm** (z.B. 14:30)

#### Schritt-für-Schritt Anleitung:

1. Gib `/remindersadd` in Discord ein
2. Bei **clan**: Tippe die ersten Buchstaben deines Clans ein und wähle ihn aus
3. Bei **channel**: Wähle den Kanal (z.B. #clan-war oder #erinnerungen)
4. Bei **time**: Gib die gewünschte Uhrzeit ein (z.B. 14:30)
5. Drücke Enter

#### Beispiel:
```
/remindersadd clan:Lost Thunder channel:#clan-war time:14:30
```

#### Was danach passiert:

Der Bot zeigt dir eine Bestätigung mit:
- ✅ Dem Clan-Namen
- ✅ Dem gewählten Kanal
- ✅ Der eingestellten Zeit
- ✅ Einer **ID-Nummer** (wichtig für später!)

**Ab jetzt sendet der Bot automatisch Erinnerungen:**
- Jeden **Donnerstag, Freitag, Samstag und Sonntag**
- Genau um die **eingestellte Uhrzeit** (z.B. 14:30 Uhr)
- Nur an Spieler, die **weniger als 4 Decks** verwendet haben
- Nur an Spieler, die mit `/link` ihr Discord-Konto verknüpft haben

---

## Deine Erinnerungen verwalten

### Schritt 2: Alle Erinnerungen anzeigen mit `/remindersinfo`

Mit diesem Befehl siehst du alle eingerichteten Erinnerungen für deinen Clan.

#### Was du eingeben musst:

- **Clan** - Wähle deinen Clan aus

#### Schritt-für-Schritt Anleitung:

1. Gib `/remindersinfo` in Discord ein
2. Bei **clan**: Wähle deinen Clan aus
3. Drücke Enter

#### Beispiel:
```
/remindersinfo clan:Lost Thunder
```

#### Was du siehst:

Eine Übersicht aller Erinnerungen für deinen Clan, zum Beispiel:

```
Reminder für Clan: Lost Thunder (#2GQQQQ8Q)

ID: 0 | Kanal: #clan-war | Zeit: 14:30:00
ID: 1 | Kanal: #clan-war | Zeit: 18:00:00
ID: 2 | Kanal: #erinnerungen | Zeit: 20:30:00

Reminder werden Donnerstag, Freitag, Samstag und Sonntag zur konfigurierten Zeit gesendet.
Sie erinnern Spieler, die heute weniger als 4 Decks verwendet haben.
```

**Wichtig:** Notiere dir die **ID-Nummern**, falls du später eine Erinnerung löschen möchtest!

---

### Schritt 3: Erinnerung löschen mit `/remindersremove`

Wenn du eine Erinnerung nicht mehr brauchst, kannst du sie mit diesem Befehl entfernen.

#### Was du eingeben musst:

- **ID** - Die Nummer der Erinnerung, die du löschen möchtest

#### Schritt-für-Schritt Anleitung:

1. Finde zuerst die ID mit `/remindersinfo` (siehe Schritt 2)
2. Gib `/remindersremove` in Discord ein
3. Bei **id**: Gib die ID-Nummer ein
4. Drücke Enter

#### Beispiel:
```
/remindersremove id:0
```

#### Was danach passiert:

Der Bot bestätigt die Löschung und zeigt:
- ✅ Den betroffenen Clan
- ✅ Die gelöschte ID

Die Erinnerung wird sofort deaktiviert und nicht mehr gesendet.

---

## Praktische Beispiele

### Beispiel 1: Eine einzelne Erinnerung einrichten

Du möchtest, dass der Bot jeden Kriegstag um 18:00 Uhr eine Erinnerung sendet.

**Was du machst:**
```
/remindersadd clan:Lost Thunder channel:#clan-war time:18:00
```

**Ergebnis:**
- Jeden Donnerstag, Freitag, Samstag und Sonntag um 18:00 Uhr
- Bekommt jeder Spieler mit weniger als 4 Decks eine Erinnerung
- Die Nachricht erscheint im #clan-war Kanal

---

### Beispiel 2: Mehrere Erinnerungen für bessere Abdeckung

Du möchtest Spieler mehrmals am Tag erinnern, damit niemand seine Decks vergisst.

**Was du machst:**
```
/remindersadd clan:Lost Thunder channel:#clan-war time:10:00
/remindersadd clan:Lost Thunder channel:#clan-war time:16:00
/remindersadd clan:Lost Thunder channel:#clan-war time:21:00
```

**Ergebnis:**
- Morgens um 10:00 Uhr: Erste Erinnerung
- Nachmittags um 16:00 Uhr: Zweite Erinnerung
- Abends um 21:00 Uhr: Letzte Erinnerung vor Kriegsende

So hast du mehrere Chancen, Spieler zu erreichen!

---

### Beispiel 3: Erinnerungen in verschiedenen Kanälen

Du möchtest wichtige Erinnerungen in unterschiedlichen Kanälen anzeigen.

**Was du machst:**
```
/remindersadd clan:Lost Thunder channel:#clan-war time:14:00
/remindersadd clan:Lost Thunder channel:#allgemein time:20:00
```

**Ergebnis:**
- Die 14:00 Uhr Erinnerung erscheint im #clan-war Kanal
- Die 20:00 Uhr Erinnerung erscheint im #allgemein Kanal (für mehr Aufmerksamkeit)

---

### Beispiel 4: Erinnerung überprüfen und löschen

Du hast mehrere Erinnerungen erstellt und möchtest eine davon entfernen.

**Schritt 1 - Übersicht anzeigen:**
```
/remindersinfo clan:Lost Thunder
```

**Du siehst:**
```
ID: 0 | Kanal: #clan-war | Zeit: 10:00:00
ID: 1 | Kanal: #clan-war | Zeit: 16:00:00
ID: 2 | Kanal: #clan-war | Zeit: 21:00:00
```

**Schritt 2 - Erinnerung löschen:**
Die 10:00 Uhr Erinnerung ist zu früh. Du löschst ID 0:
```
/remindersremove id:0
```

**Ergebnis:**
Nur die 16:00 und 21:00 Uhr Erinnerungen bleiben aktiv.

---

## Wie sieht eine Erinnerungs-Nachricht aus?

Wenn der Bot eine Erinnerung sendet, sieht die Nachricht ungefähr so aus:

```
⚠️ Clan-War-Erinnerung für Lost Thunder

Folgende Spieler haben heute weniger als 4 Decks verwendet:

@MaxMustermann (2/4 Decks verwendet)
@AnnaBecker (1/4 Decks verwendet)
@TomSchmidt (0/4 Decks verwendet)
@LauraWeber (3/4 Decks verwendet)

Bitte denkt daran, alle 4 Decks im heutigen Clan-Krieg zu verwenden! 💪
```

**Was die Nachricht enthält:**
- Den Clan-Namen
- Eine Liste aller Spieler, die noch Decks übrig haben
- Wie viele Decks jeder Spieler schon verwendet hat
- Eine freundliche Aufforderung

**Wichtig:** Nur Spieler, die ihren Discord-Account mit dem Bot verknüpft haben (mit `/link`), werden in der Liste erwähnt!

---

## Häufige Fragen und Probleme

### ❓ "Dieser Clan existiert nicht"
**Was ist passiert?**  
Der Clan-Tag, den du eingegeben hast, ist nicht im System registriert.

**Was du tun kannst:**  
- Überprüfe, ob du den richtigen Clan ausgewählt hast
- Wähle den Clan aus der Vorschlagsliste, statt ihn manuell einzutippen
- Frage einen Administrator, ob dein Clan im System registriert ist

---

### ❓ "Du musst mindestens Vize-Anführer des Clans sein"
**Was ist passiert?**  
Du hast nicht die nötigen Rechte, um Erinnerungen für diesen Clan zu verwalten.

**Was du tun kannst:**  
Nur Anführer und Vize-Anführer können Erinnerungen erstellen und löschen. Wenn du glaubst, dass du die Berechtigung haben solltest, wende dich an einen Administrator.

---

### ❓ "Ungültiges Zeitformat"
**Was ist passiert?**  
Die Uhrzeit wurde nicht richtig eingegeben.

**Was du tun kannst:**  
Verwende das Format **HH:mm** mit einem Doppelpunkt:
- ✅ Richtig: `14:30`, `09:00`, `22:45`
- ❌ Falsch: `14.30`, `9:00 Uhr`, `14:30:00`

**Tipp:** Verwende immer zweistellige Zahlen (09:00 statt 9:00)

---

### ❓ "Der angegebene Kanal existiert nicht"
**Was ist passiert?**  
Der Discord-Kanal, den du ausgewählt hast, wurde nicht gefunden.

**Was du tun kannst:**  
- Stelle sicher, dass der Kanal noch existiert
- Prüfe, ob der Bot Zugriff auf den Kanal hat
- Wähle den Kanal aus der Dropdown-Liste, statt ihn manuell einzutippen

---

### ❓ "Es existiert kein Reminder mit dieser ID"
**Was ist passiert?**  
Die ID, die du zum Löschen angegeben hast, existiert nicht.

**Was du tun kannst:**  
- Verwende `/remindersinfo` um die aktuellen IDs zu sehen
- Achte darauf, die richtige Zahl einzugeben (z.B. `0` nicht `o`)

---

### ❓ Die Erinnerungen werden nicht gesendet
**Was könnte das Problem sein?**

1. **Kriegstage:** Erinnerungen werden nur **Donnerstag, Freitag, Samstag und Sonntag** gesendet
2. **Keine Spieler:** Wenn alle Spieler bereits 4 Decks verwendet haben, wird keine Nachricht gesendet
3. **Fehlende Verlinkung:** Spieler müssen mit `/link` ihr Discord-Konto verbunden haben
4. **Falsche Zeit:** Überprüfe mit `/remindersinfo`, ob die Zeit richtig eingestellt ist

---

### ❓ Kann ich Erinnerungen auch montags bis mittwochs nutzen?
**Nein.** Das System ist speziell für Kriegstage (Donnerstag-Sonntag) konzipiert. An anderen Tagen finden keine Clan-Kriege statt, daher werden auch keine Erinnerungen gesendet.

---

### ❓ Wie viele Erinnerungen kann ich pro Clan erstellen?
**Unbegrenzt!** Du kannst so viele Erinnerungen erstellen, wie du möchtest. Empfohlen sind aber **2-3 Erinnerungen pro Tag**, um die Spieler nicht zu überfordern.

---

### ❓ Was bedeutet "weniger als 4 Decks"?
Im Clan-Krieg hat jeder Spieler **4 Decks** zur Verfügung. Die Erinnerung wird nur an Spieler gesendet, die noch nicht alle 4 Decks verwendet haben:
- 0 Decks verwendet → wird erinnert ✅
- 1 Deck verwendet → wird erinnert ✅
- 2 Decks verwendet → wird erinnert ✅
- 3 Decks verwendet → wird erinnert ✅
- 4 Decks verwendet → wird NICHT erinnert ❌

---

### ❓ Können andere Vize-Anführer meine Erinnerungen sehen und löschen?
**Ja.** Alle Vize-Anführer und Anführer des Clans können:
- Alle Erinnerungen mit `/remindersinfo` sehen
- Beliebige Erinnerungen mit `/remindersremove` löschen
- Neue Erinnerungen mit `/remindersadd` erstellen

Das ist so gedacht, damit ihr als Team zusammenarbeiten könnt.

---

## Tipps für Vize-Anführer

### 💡 Tipp 1: Wähle die richtigen Zeiten
- **Morgens (z.B. 09:00):** Gut für Spieler, die früh aktiv sind
- **Nachmittags (z.B. 16:00):** Erreicht die meisten Spieler nach Schule/Arbeit
- **Abends (z.B. 20:00 oder 21:00):** Letzte Chance vor Kriegsende

**Vermeide:** Uhrzeiten mitten in der Nacht (niemand ist wach) oder zu früh am Morgen.

---

### 💡 Tipp 2: Nutze einen dedizierten Kanal
Erstelle einen speziellen Kanal wie **#clan-war** oder **#erinnerungen**, in dem nur wichtige Kriegsnachrichten erscheinen. So übersehen Spieler die Erinnerungen nicht zwischen anderen Chat-Nachrichten.

---

### 💡 Tipp 3: Nicht zu viele Erinnerungen
**Empfohlen:** 2-3 Erinnerungen pro Tag  
**Nicht empfohlen:** Mehr als 4 Erinnerungen pro Tag

Zu viele Erinnerungen können nervig sein und Spieler ignorieren sie dann.

---

### 💡 Tipp 4: Informiere deine Clan-Mitglieder
Wenn du das System zum ersten Mal einrichtest, erkläre deinen Clan-Mitgliedern:
- Was die automatischen Erinnerungen sind
- Dass sie mit `/link` ihr Konto verknüpfen müssen, um erwähnt zu werden
- Wann die Erinnerungen kommen (z.B. "um 16:00 und 20:00 Uhr")

---

### 💡 Tipp 5: Passe die Zeiten an
Beobachte nach ein paar Wochen:
- Werden die Decks jetzt besser genutzt?
- Kommen zu viele oder zu wenige Erinnerungen?
- Sind die Zeiten passend?

Du kannst jederzeit Erinnerungen löschen und neue mit besseren Zeiten erstellen!

---

### 💡 Tipp 6: Koordiniere dich mit anderen Vize-Anführern
Wenn mehrere Vize-Anführer Zugriff haben:
- Sprecht euch ab, wer Erinnerungen erstellt
- Vermeidet doppelte Erinnerungen zur gleichen Zeit
- Nutzt `/remindersinfo` um zu sehen, was schon eingerichtet ist

---

### 💡 Tipp 7: Spieler müssen verknüpft sein
Damit Spieler in den Erinnerungen erwähnt werden, müssen sie ihren Discord-Account mit ihrem Clash Royale Account verknüpfen. 

**Der Befehl dafür ist:** `/link`

Wenn ein Spieler nicht in den Erinnerungen auftaucht, obwohl er Decks übrig hat, hat er sein Konto wahrscheinlich nicht verknüpft.

---

## Zusammenfassung: Die 3 Befehle auf einen Blick

| Befehl | Was er macht | Wann du ihn brauchst |
|--------|--------------|---------------------|
| `/remindersadd` | Erstellt eine neue Erinnerung | Beim Einrichten des Systems |
| `/remindersinfo` | Zeigt alle Erinnerungen an | Zum Überprüfen oder um IDs zu finden |
| `/remindersremove` | Löscht eine Erinnerung | Wenn eine Erinnerung nicht mehr gebraucht wird |

---

## Schnellstart-Anleitung

**Du willst sofort loslegen? Folge diesen 3 Schritten:**

1️⃣ **Erstelle eine Erinnerung am Abend:**
```
/remindersadd clan:[dein Clan] channel:#clan-war time:20:00
```

2️⃣ **Überprüfe, ob es funktioniert hat:**
```
/remindersinfo clan:[dein Clan]
```

3️⃣ **Fertig!** Ab jetzt werden jeden Kriegstag (Do-So) um 20:00 Uhr automatisch Erinnerungen gesendet.

**Optional:** Füge noch eine Nachmittagserinnerung hinzu:
```
/remindersadd clan:[dein Clan] channel:#clan-war time:16:00
```

---

## Bei Problemen oder Fragen

Wenn etwas nicht funktioniert oder du Fragen hast:
1. ✅ Lies dir die **Häufigen Fragen und Probleme** oben durch
2. ✅ Überprüfe deine Erinnerungen mit `/remindersinfo`
3. ✅ Stelle sicher, dass du Vize-Anführer oder Anführer bist
4. ✅ Kontaktiere einen Bot-Administrator, wenn das Problem weiterhin besteht

---

**Viel Erfolg beim Einsatz des Erinnerungs-Systems! 🎮⚔️**

*Letzte Aktualisierung: 10. November 2025*

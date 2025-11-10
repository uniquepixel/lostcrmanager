# Zusammenfassung der letzten 2 Commits auf Main

## Commit 1: Automatische Clan-War-Erinnerungen (2964cb66)
**Datum:** 10. November 2025, 20:30 Uhr  
**Autor:** uniquepixel  
**Titel:** Add automated clan war reminders for players with insufficient deck usage

### Was wurde hinzugefügt?
Dieser Commit fügt ein vollständiges Erinnerungs-System für Clan-Kriege hinzu. Das System ermöglicht es Clan-Anführern und Vize-Anführern, automatische Erinnerungen für Spieler zu konfigurieren, die ihre Decks im Clan-Krieg nicht ausreichend nutzen.

### Änderungen im Detail:
- **3 neue Befehle:**
  - `/remindersadd` - Fügt eine neue Erinnerung hinzu
  - `/remindersinfo` - Zeigt alle konfigurierten Erinnerungen an
  - `/remindersremove` - Entfernt eine Erinnerung
- **Datenbank-Integration:** Neue Tabelle `reminders` zur Speicherung der Konfigurationen
- **Automatisches Scheduling:** Der Bot prüft automatisch jeden Tag um die konfigurierten Zeiten
- **Intelligente Benachrichtigungen:** Nur Spieler mit weniger als 4 verwendeten Decks werden erinnert
- **Zeitbasierte Ausführung:** Erinnerungen werden nur an Kriegstagen (Donnerstag, Freitag, Samstag, Sonntag) gesendet

### Dateien geändert:
- `src/main/java/commands/reminders/remindersadd.java` (151 Zeilen hinzugefügt)
- `src/main/java/commands/reminders/remindersinfo.java` (132 Zeilen hinzugefügt)
- `src/main/java/commands/reminders/remindersremove.java` (89 Zeilen hinzugefügt)
- `src/main/java/lostcrmanager/Bot.java` (149 Zeilen hinzugefügt)
- `src/main/java/datautil/APIUtil.java` (29 Zeilen geändert)
- `src/main/java/datautil/Connection.java` (5 Zeilen geändert)

**Gesamtstatistik:** 558 Zeilen hinzugefügt, 2 Zeilen gelöscht

---

## Commit 2: Fehlerbehebungen für Reminder-System (0a20a6e)
**Datum:** 10. November 2025, 20:36 Uhr  
**Autor:** uniquepixel  
**Titel:** fixes

### Was wurde verbessert?
Dieser Commit behebt wichtige Fehler im zuvor hinzugefügten Erinnerungs-System und verbessert die Stabilität.

### Änderungen im Detail:
- **Fehlerbehebung in remindersadd.java:** Verbesserte Fehlerbehandlung und Validierung
- **Optimierung in Bot.java:** Verbessertes Scheduling und Nachrichtenverarbeitung

### Dateien geändert:
- `src/main/java/commands/reminders/remindersadd.java` (14 Zeilen hinzugefügt, 11 Zeilen gelöscht)
- `src/main/java/lostcrmanager/Bot.java` (18 Zeilen hinzugefügt, 17 Zeilen gelöscht)

**Gesamtstatistik:** 32 Zeilen hinzugefügt, 28 Zeilen gelöscht

---

# Benutzeranleitung: Clan-War-Erinnerungen

## Überblick
Das Erinnerungs-System ermöglicht es Clan-Leitern, automatische Benachrichtigungen für Spieler zu konfigurieren, die ihre Decks im Clan-Krieg nicht ausreichend nutzen. Dies hilft dabei, die Teilnahme am Clan-Krieg zu verbessern.

## Voraussetzungen
- Du musst mindestens **Vize-Anführer** oder **Anführer** des Clans sein
- Der Clan muss in der Bot-Datenbank registriert sein
- Der Bot benötigt Schreibrechte im Zielkanal

## Befehle

### 1. `/remindersadd` - Erinnerung hinzufügen

**Zweck:** Erstellt eine neue automatische Erinnerung für einen Clan.

**Parameter:**
- `clan` (erforderlich) - Der Clan-Tag des Clans (z.B. "#ABC123")
- `channel` (erforderlich) - Der Discord-Kanal, in dem die Erinnerungen gesendet werden
- `time` (erforderlich) - Die Uhrzeit im Format HH:mm (z.B. "14:30" für 14:30 Uhr)

**Beispiel:**
```
/remindersadd clan:#ABC123 channel:#clan-war time:14:30
```

**Was passiert:**
1. Der Bot erstellt eine neue Erinnerung mit einer eindeutigen ID
2. Die Konfiguration wird in der Datenbank gespeichert
3. Du erhältst eine Bestätigung mit allen Details und der ID

**Hinweise:**
- Die Erinnerungen werden nur an Kriegstagen gesendet (Donnerstag, Freitag, Samstag, Sonntag)
- Pro Clan können mehrere Erinnerungen mit unterschiedlichen Zeiten konfiguriert werden
- Die Warteliste kann nicht für Erinnerungen verwendet werden

---

### 2. `/remindersinfo` - Erinnerungen anzeigen

**Zweck:** Zeigt alle konfigurierten Erinnerungen für einen Clan an.

**Parameter:**
- `clan` (erforderlich) - Der Clan-Tag des Clans

**Beispiel:**
```
/remindersinfo clan:#ABC123
```

**Was du siehst:**
- Eine Liste aller Erinnerungen für den Clan
- Für jede Erinnerung: ID, Kanal und Uhrzeit
- Informationen darüber, wann die Erinnerungen gesendet werden
- Die Bedingungen für die Benachrichtigungen

**Ausgabebeispiel:**
```
Reminder für Clan: MeinClan (#ABC123)

ID: 0 | Kanal: #clan-war | Zeit: 14:30:00
ID: 1 | Kanal: #clan-war | Zeit: 18:00:00

Reminder werden Donnerstag, Freitag, Samstag und Sonntag zur konfigurierten Zeit gesendet.
Sie erinnern Spieler, die heute weniger als 4 Decks verwendet haben.
```

---

### 3. `/remindersremove` - Erinnerung entfernen

**Zweck:** Löscht eine bestehende Erinnerung.

**Parameter:**
- `id` (erforderlich) - Die ID der zu löschenden Erinnerung

**Beispiel:**
```
/remindersremove id:0
```

**Was passiert:**
1. Der Bot prüft, ob die Erinnerung existiert
2. Der Bot verifiziert deine Berechtigung für den betroffenen Clan
3. Die Erinnerung wird aus der Datenbank gelöscht
4. Du erhältst eine Bestätigung

**Hinweis:** Die ID findest du mit dem Befehl `/remindersinfo`

---

## Wie funktionieren die Erinnerungen?

### Zeitplan
- **Aktive Tage:** Donnerstag, Freitag, Samstag, Sonntag
- **Inaktive Tage:** Montag, Dienstag, Mittwoch (keine Erinnerungen)
- **Zeitpunkt:** Zur konfigurierten Zeit (z.B. 14:30 Uhr)

### Benachrichtigungskriterium
Spieler werden erinnert, wenn sie:
- Mitglied des Clans sind
- Heute **weniger als 4 Decks** im Clan-Krieg verwendet haben
- Mit einem Discord-Konto verknüpft sind (über den `/link` Befehl)

### Nachrichteninhalt
Die Erinnerungsnachricht enthält:
- Eine Liste aller betroffenen Spieler (mit Mention)
- Die Anzahl der bereits verwendeten Decks
- Eine Aufforderung, weitere Decks zu verwenden

**Beispiel-Nachricht:**
```
⚠️ Clan-War-Erinnerung

Folgende Spieler haben heute weniger als 4 Decks verwendet:

@Spieler1 (2 Decks)
@Spieler2 (1 Deck)
@Spieler3 (0 Decks)

Bitte denkt daran, alle 4 Decks im Clan-Krieg zu verwenden!
```

---

## Häufige Anwendungsfälle

### Morgendliche Erinnerung
```
/remindersadd clan:#MeinClan channel:#clan-war time:09:00
```
Sendet eine Erinnerung am Morgen, um Spieler frühzeitig zu motivieren.

### Nachmittagserinnerung
```
/remindersadd clan:#MeinClan channel:#clan-war time:16:00
```
Erinnert Spieler am Nachmittag, falls sie noch nicht alle Decks verwendet haben.

### Abenderinnerung
```
/remindersadd clan:#MeinClan channel:#clan-war time:20:00
```
Letzte Erinnerung vor Kriegsende für Spieler, die noch Decks übrig haben.

### Mehrere Erinnerungen kombinieren
Du kannst alle drei Zeiten für einen Clan konfigurieren:
```
/remindersadd clan:#MeinClan channel:#clan-war time:09:00
/remindersadd clan:#MeinClan channel:#clan-war time:16:00
/remindersadd clan:#MeinClan channel:#clan-war time:20:00
```

---

## Fehlermeldungen und Lösungen

### "Dieser Clan existiert nicht"
**Problem:** Der eingegebene Clan-Tag ist nicht in der Datenbank registriert.  
**Lösung:** Überprüfe den Clan-Tag und stelle sicher, dass der Clan im System registriert ist.

### "Du musst mindestens Vize-Anführer des Clans sein"
**Problem:** Du hast nicht die erforderlichen Berechtigungen.  
**Lösung:** Nur Clan-Anführer und Vize-Anführer können Erinnerungen verwalten.

### "Ungültiges Zeitformat"
**Problem:** Die eingegebene Zeit entspricht nicht dem Format HH:mm.  
**Lösung:** Verwende das 24-Stunden-Format, z.B. "14:30" oder "09:00".

### "Der angegebene Kanal existiert nicht"
**Problem:** Der ausgewählte Discord-Kanal wurde nicht gefunden.  
**Lösung:** Stelle sicher, dass der Kanal existiert und der Bot darauf Zugriff hat.

### "Es existiert kein Reminder mit dieser ID"
**Problem:** Die eingegebene ID ist ungültig.  
**Lösung:** Verwende `/remindersinfo`, um die richtigen IDs zu sehen.

---

## Tipps und Best Practices

1. **Zeitplanung:** Wähle Zeiten, die für deine Clan-Mitglieder sinnvoll sind (z.B. nicht mitten in der Nacht).

2. **Kanal-Auswahl:** Verwende einen dedizierten Kanal für Clan-War-Angelegenheiten, damit die Erinnerungen gut sichtbar sind.

3. **Anzahl der Erinnerungen:** 2-3 Erinnerungen pro Tag sind meist ausreichend (z.B. morgens, nachmittags, abends).

4. **Kommunikation:** Informiere deine Clan-Mitglieder über die neuen automatischen Erinnerungen.

5. **Anpassung:** Beobachte die Teilnahme und passe die Zeiten bei Bedarf an.

6. **Aufräumen:** Entferne Erinnerungen, die nicht mehr benötigt werden, mit `/remindersremove`.

---

## Technische Details

### Datenbank-Struktur
Die Erinnerungen werden in einer `reminders`-Tabelle gespeichert mit:
- `id` - Eindeutige ID der Erinnerung
- `clantag` - Zugehöriger Clan
- `channelid` - Discord-Kanal-ID
- `time` - Uhrzeit der Erinnerung

### Automatisierung
Der Bot verwendet einen Timer, der:
- Jede Minute prüft, ob Erinnerungen fällig sind
- Die Clash Royale API abfragt, um Deck-Verwendung zu prüfen
- Discord-Mentions für verknüpfte Spieler erstellt
- Nachrichten nur an Kriegstagen sendet

### Spieler-Verlinkung
Damit Spieler erwähnt werden können, müssen sie:
1. Mit `/link` ihr Discord-Konto mit ihrem Spieler-Tag verknüpfen
2. Im betreffenden Clan Mitglied sein

---

## Support und Fragen

Bei Problemen oder Fragen zum Erinnerungs-System:
1. Überprüfe zuerst die Fehlermeldungen
2. Verwende `/remindersinfo`, um die aktuellen Konfigurationen zu sehen
3. Stelle sicher, dass du die erforderlichen Berechtigungen hast
4. Kontaktiere den Bot-Administrator, wenn das Problem weiterhin besteht

---

**Version:** November 2025  
**Letzte Aktualisierung:** 10. November 2025

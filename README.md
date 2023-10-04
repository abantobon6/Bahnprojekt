# Bahnprojekt
Um den Code zu nutzen, muss folgendes vorher passieren.
1. In data muss der Spurplan abgelegt werden als /data/DB (/DB muss die csv-Dateien enthalten).
2. In data muss außerdem der gefilterte OSM-Germany-Datensatz als .osm Datei abgelegt werden. Dieser sollte alle Ways mit dem tag "railway" und die zugehörigen Knoten enthalten.
3. In data muss eine Ordner namens mappedDBNode angelegt werden. In diesem werden die Output-Dateien abgelegt werden.

Um zwischen den beiden beschriebenen Strategien zu wählen, muss der erste Parameter der Methode start(int strategy) beim Aufruf in der Main-Methode auf 1, bzw. 2 gesetzt werden.
Um eine rigorosere Ankerknoten-Erweiterung zu verwenden muss der zweite Parameter "boolean rigorousExtension" auf true (mit Erweiterung), bzw. false (ohne Erweiterung) gesetzt werden.
Wenn rigorousExtension auf false gesetzt wird, werden trotzdem neue Ankerknoten hinzugefügt, allerdings nur wenn der Name des DB-Knotens zum ref-Tag des OSM-Knotens passt.

Nach Ausführen des Codes sollte der Ordner /mappedNodes eine Kopie des Spurplans mit ergänzten GPS-Daten enthalten.
Die Datei mappedNodes.txt enthält dann eine Auflistung der verwendeten DB-Pfade.
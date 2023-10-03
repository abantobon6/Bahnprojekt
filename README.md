# Bahnprojekt
Um den Code zu nutzen, muss folgendes vorher passieren.
1. In data muss der Spurplan abgelegt werden als /data/DB (/DB muss die csv-Dateien enthalten).
2. In data muss außerdem der gefilterte OSM-Germany-Datensatz abgelegt werden. Dieser sollte alle Ways mit dem tag "railway" und die zugehörigen Knoten enthalten.

Um zwischen den beiden beschriebenen Strategien zu wählen, muss der Parameter der Methode mapNodes() beim Aufruf in der Main-Methode auf 1, bzw. 2 gesetzt werden.

Nach Ausführen des Codes sollte der Ordner /mappedNodes eine Kopie des Spurplans mit ergänzten GPS-Daten enthalten.
Die Datei mappedNodes.txt enthält dann eine Auflistung der verwendeten DB-Pfade.
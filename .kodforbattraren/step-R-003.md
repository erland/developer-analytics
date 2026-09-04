# R-003 – Ersätt implementationstätt activity-querytest

## Klassificering
Beteendebevarande testrefaktorering.

## Genomfört
- Tagit bort `MeActivityResourceQueryTest`, som läste Java-källkod och verifierade implementationsdetaljer med strängsökningar.
- Ingen produktionskod har ändrats.
- Motsvarande regressionsskydd finns i `MeActivityResourceCharacterizationTest`:
  - all-time-anrop utan datumparametrar verifierar att null-datum inte orsakar query-/bindningsfel,
  - månadsfilter verifierar periodnormalisering och datumavgränsning,
  - repository search-scope verifierar att endast matchande repositories påverkar aggregationerna.

## Verifiering
- `python3 scripts/check-backend-test-layers.py`: körs i leveranskontrollen.
- Källkodssökning bekräftar att inget activity-test längre läser produktions-Java för att verifiera querystruktur.
- Full Maven-testsvit behöver fortsatt bekräftas av CI i miljö med Maven/Docker.

## Status
R-003 är **klar** som testrefaktorering. Nästa planerade steg är R-004.

# Development plan – Git-baserad historisk change-kind-backfill

**Repository:** `erland/developer-analytics`  
**Branch:** `feature/git-history-backfill`  
**Status:** Steg 6 klart; steg 7 nästa  
**Mål:** Ersätta REST-anrop per historisk commit med Git-baserad lokal historikanalys för change-kind-backfill, utan att ändra den ordinarie inkrementella GitHub-synken.

## Målbild

Historisk backfill ska använda en temporär, blobless/bare Git-klon eller motsvarande fetch-strategi för att läsa commit-historik, ändrade filnamn och per-fil additions/deletions lokalt. Resultatet ska fortsätta lagras i befintliga `ContributionFileChange`-rader och klassificeras med befintlig `ChangeKindClassifier`.

Ordinarie löpande synk för metadata, languages, pull requests, issues, reviews och nya commits behåller nuvarande API-baserade flöde. Befintlig rate-limit/backoff-logik behålls.

## Avgränsningar

- Ingen permanent Git-cache i första versionen.
- Ingen full working copy ska skapas.
- Filinnehåll/blobs ska inte hämtas om analysen kan göras utan dem.
- Högst en historisk Git-backfill per worker samtidigt i första versionen.
- Befintlig REST-baserad commit-detail-funktion behålls som fallback.
- Ingen ändring av change-kind-taxonomin i denna PR.

## Steg 1 – Introducera ett Git-baserat historikinterface ✅

- [x] Skapa ett provider-neutralt interface för historisk commit-/filanalys.
- [x] Definiera resultatmodell med commit-SHA, filväg, additions och deletions.
- [x] Håll Git-processhantering utanför befintlig GitHub REST-adapter.
- [x] Lägg unit tests för modell och kontrakt.

**Klart:** `ContributionHistoryProvider` abstraherar transporten och returnerar `HistoricalCommitFileChanges` med befintliga provider-neutrala per-fil-statistikobjekt. Ingen Git-processhantering har lagts i GitHub REST-adaptern.

## Steg 2 – Säker temporär blobless clone/fetch ✅

- [x] Implementera temporär arbetsyta per backfill-jobb.
- [x] Använd bare/blobless partial clone där Git/GitHub stöder det, t.ex. `--filter=blob:none`.
- [x] Undvik working tree.
- [x] Stöd privata repositories utan att exponera token i loggar eller processargument där det går att undvika.
- [x] Säkerställ cleanup i `finally` även vid timeout/fel.
- [x] Lägg timeout för clone/fetch.
- [x] Mät clone/fetch-tid och temporär diskstorlek.

**Klart:** `GitCloneWorkspaceService` skapar ett temporärt bare repository med `--filter=blob:none --no-tags`. GitHub-token skickas via Git-konfiguration i processens miljö i stället för i clone-URL eller kommandorad. `TemporaryGitRepository` exponerar det bare repository som nästa steg ska analysera och raderar hela arbetsytan vid `close()`. Clone har timeout och arbetsytan registrerar transfer-tid samt faktisk temporär diskstorlek.

## Steg 3 – Extrahera historiska filförändringar lokalt ✅

- [x] Läs commit-SHA och tidsordning från Git.
- [x] Extrahera ändrade filnamn och per-fil additions/deletions via lokal Git `numstat`.
- [x] Hantera merge commits deterministiskt och dokumentera vald semantik.
- [x] Hantera renames/binary files konservativt.
- [x] Mata resultatet genom befintlig `ChangeKindClassifier` i tester och kommande persistensflöde.
- [x] Lägg tester med kod-, dokumentations-, CI/CD- och mixed commits.

**Klart:** `GitLocalHistoryReader` läser begärda commit-SHA:n från det bare repositoryt och använder `git diff-tree --root --numstat` för root commits samt `git diff --numstat` mot första föräldern för övriga commits. Merge commits mäts därmed mot första föräldern för att undvika dubbelräkning av sidogrenens historik. Rename detection är avstängd för stabil semantik och binära `-/-`-värden bevaras som filförändringar med 0/0 rader. `GitContributionHistoryProvider` binder ihop den temporära klonen med den provider-neutrala historikmodellen.

## Steg 4 – Byt endast historisk backfill till Git ✅

- [x] Koppla den befintliga resumable change-kind-backfillen till Git-historikprovidern.
- [x] Återanvänd befintliga `Contribution`-poster och skriv till `ContributionFileChange`.
- [x] Hoppa över commits som redan har aktuell classifier-version via befintligt DB-urval.
- [x] Behåll normal inkrementell contribution-sync oförändrad.
- [x] Behåll REST commit-detail som fallback för commits/repositories som inte kan analyseras via Git.
- [x] Markera scope/backfill färdig först när alla relevanta commits är klassificerade.

**Klart:** `GitHubChangeKindBackfillJobHandler` använder Git-historikprovidern som primär källa. Användbara Git-resultat persistieras och klassificeras lokalt; commits som saknas eller har ogiltigt resultat faller tillbaka till befintlig REST commit-detail. Om Git-hämtningen misslyckas används REST. Befintlig continuation-, deduplicerings- och scope-logik är oförändrad, och den ordinarie inkrementella contribution-synken har inte ändrats. Fokuserade worker-tester täcker primär Git-väg, partiell fallback och full REST-fallback.

## Steg 5 – Resurs- och säkerhetsskydd ✅

- [x] Begränsa Git-backfill till högst 1 samtidig clone per worker initialt.
- [x] Kontrollera ledigt diskutrymme före clone.
- [x] Inför konfigurerbar maxgräns för temporär Git-data.
- [x] Avbryt säkert vid timeout eller resursgräns.
- [x] Fall tillbaka till nuvarande REST-backfill när Git-strategin inte är lämplig.
- [x] Säkerställ att cleanup körs efter success, fallback och fel.
- [x] Logga inga access tokens eller credential-URL:er.

**Klart:** clone har konfigurerbar timeout, maxstorlek och minsta diskreserv. Ett worker-lokalt lås tillåter högst en Git-klon samtidigt; konkurrerande backfill använder REST-fallback i stället för att blockera. Clone övervakas medan den körs och stoppas om resursgränser överskrids. Cleanup sker även vid timeout/fel och Git-feltext token-redigeras. Full CI verifierar skydden.

### Steg 5B – Återanvänd clone över flera backfill-batchar ✅

- [x] Höj det bundna arbetsurvalet per backfill-jobb från 100 till högst 1 000 saknade commits.
- [x] Anropa Git-historikprovidern en gång för hela jobbets commitmängd, vilket ger en temporär clone per högst 1 000 commits i stället för per 100 commits.
- [x] Behandla resultatet vidare i logiska delgrupper om 100 commits för att behålla tydlig och bounded bearbetning.
- [x] Behåll REST-fallback per commit och continuation när mer historik återstår.
- [x] Lägg test med fler än 100 commits som verifierar ett enda Git-history-anrop.

**Effekt:** ett repository med 2 000 commits kräver normalt omkring 2 temporära clones i stället för omkring 20, utan permanent Git-cache. Jobbet är fortfarande begränsat till högst 1 000 historiska commits och continuation tar nästa segment vid behov.

## Steg 6 – Automatisk uppgraderings-/backfill-trigger ✅

- [x] Kontrollera periodiskt i worker-rollen om repositories har äldre contribution scope.
- [x] Köa endast saknade backfill-jobb och använd befintlig deduplicering.
- [x] Gör triggern idempotent så vanliga restarts inte skapar parallella backfill-kedjor.
- [x] Låt normal inkrementell sync fortsätta även medan historisk backfill pågår.

**Klart:** `ContributionScopeUpgradeService` hittar en begränsad mängd GitHub-repositories med äldre contribution scope och köar change-kind-backfill via befintlig `RepositoryDiscoveryJobService`. `BackgroundJobWorker` kör kontrollen periodiskt endast i worker-rollen. Urvalet filtrerar bort repositories som inte ingår i analysen eller har `ACCESS_REVOKED`, och befintlig jobbdeduplicering gör triggern idempotent. Full CI inklusive large-account acceptance är grön.

## Steg 7 – Acceptance, prestanda och dokumentation

- [ ] Lägg acceptance-test som jämför Git-baserat resultat mot förväntade filförändringar.
- [ ] Verifiera att blandade commits inte dubbelräknas.
- [ ] Verifiera privata/public repositories där testmiljön tillåter det.
- [ ] Mät minst:
  - clone/fetch-tid
  - temporär disk peak
  - antal analyserade commits
  - antal GitHub REST-anrop
  - antal REST-fallbacks
- [ ] Verifiera cleanup efter avbrutet jobb.
- [ ] Uppdatera drift-/sync-dokumentation.
- [ ] Kör full CI och large-account acceptance.

**Klart när:** vi kan visa att historisk backfill kraftigt minskar REST-anrop utan oacceptabel disk- eller nätverkskostnad.

## Förväntad effekt

För repositories med lång historik bör Git-baserad backfill minska GitHub REST-belastningen från ungefär ett commit-detail-anrop per historisk commit till huvudsakligen en Git-transfer per bounded jobbsegment. Med 5B omfattar ett sådant segment upp till 1 000 commits. Temporär disk begränsas genom att endast ett repo hanteras åt gången och arbetsytan raderas direkt efter analys.

## Beslutspunkt efter implementation

Efter att verkliga mätvärden samlats in ska vi ta ställning till om en liten permanent bare Git-cache är värd att införa. Det ska inte göras i denna PR om inte mätdata tydligt visar behovet.

## Rekommenderad arbetsordning

Genomför ett steg i taget på samma branch/PR. Efter varje steg:

1. kör relevanta tester,
2. uppdatera denna plan med status,
3. kontrollera att CI fortfarande är grön,
4. gå först därefter vidare till nästa steg.

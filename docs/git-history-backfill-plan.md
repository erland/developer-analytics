# Development plan – Git-baserad historisk change-kind-backfill

**Repository:** `erland/developer-analytics`  
**Branch:** `feature/git-history-backfill`  
**Status:** Implementation och full CI klara; credential-baserad operativ mätning återstår  
**Mål:** Ersätta REST-anrop per historisk commit med Git-baserad lokal historikanalys för change-kind-backfill, utan att ändra den ordinarie inkrementella GitHub-synken.

## Målbild

Historisk backfill använder en temporär, blobless/bare Git-klon för att läsa commit-historik, ändrade filnamn och per-fil additions/deletions lokalt. Resultatet lagras i befintliga `ContributionFileChange`-rader och klassificeras med befintlig `ChangeKindClassifier`.

Ordinarie löpande synk för metadata, languages, pull requests, issues, reviews och nya commits behåller API-baserat flöde. Befintlig rate-limit/backoff-logik behålls.

Repository file/manifest evidence använder numera också lokal Git som primär transport med GitHub REST som fallback. Den slutliga ansvarsfördelningen dokumenteras i `docs/sync-architecture.md`.

## Avgränsningar

- Ingen permanent Git-cache i första versionen.
- Ingen full working copy skapas.
- Filinnehåll/blobs hämtas endast när bounded manifest/config-evidence kräver det.
- Högst en Git-klon per worker samtidigt i första versionen.
- Befintlig REST-baserad commit-detail-funktion behålls som fallback.
- Ingen ändring av change-kind-taxonomin i denna PR.

## Steg 1 – Introducera ett Git-baserat historikinterface ✅

- [x] Skapa ett provider-neutralt interface för historisk commit-/filanalys.
- [x] Definiera resultatmodell med commit-SHA, filväg, additions och deletions.
- [x] Håll Git-processhantering utanför befintlig GitHub REST-adapter.
- [x] Lägg unit tests för modell och kontrakt.

**Klart:** `ContributionHistoryProvider` abstraherar transporten och returnerar `HistoricalCommitFileChanges` med provider-neutrala per-fil-statistikobjekt.

## Steg 2 – Säker temporär blobless clone/fetch ✅

- [x] Implementera temporär arbetsyta per Git-jobb/snapshot.
- [x] Använd bare/blobless partial clone med `--filter=blob:none`.
- [x] Undvik working tree.
- [x] Stöd privata repositories utan token i URL/argv.
- [x] Säkerställ cleanup vid timeout/fel.
- [x] Lägg timeout för clone/fetch.
- [x] Mät clone/fetch-tid och temporär diskstorlek.

**Klart:** `GitWorkspaceService` skapar temporärt bare repository med `--filter=blob:none --no-tags`. GitHub-token skickas via Git-konfiguration i processens miljö. `TemporaryGitRepository` raderar arbetsytan vid `close()` och exponerar transfer-tid samt temporär diskstorlek.

## Steg 3 – Extrahera historiska filförändringar lokalt ✅

- [x] Läs begärda commit-SHA:n från Git.
- [x] Extrahera ändrade filnamn och per-fil additions/deletions via lokal Git `numstat`.
- [x] Hantera merge commits deterministiskt.
- [x] Hantera renames/binary files konservativt.
- [x] Mata resultatet genom befintlig `ChangeKindClassifier`.
- [x] Lägg tester med kod-, dokumentations-, CI/CD- och mixed commits.

**Klart:** `GitLocalHistoryReader` använder `git diff-tree --root --numstat` för root commits och `git diff --numstat` mot första föräldern för övriga commits. Rename detection är avstängd och binära `-/-`-värden representeras som 0/0.

## Steg 4 – Byt endast historisk backfill till Git ✅

- [x] Koppla change-kind-backfillen till Git-historikprovidern.
- [x] Återanvänd befintliga `Contribution`-poster och skriv till `ContributionFileChange`.
- [x] Hoppa över commits med aktuell classifier-version via DB-urval.
- [x] Behåll normal inkrementell contribution-sync API-baserad.
- [x] Behåll REST commit-detail som fallback.
- [x] Markera scope färdig först när alla relevanta commits är klassificerade.

**Klart:** `GitHubChangeKindBackfillJobHandler` använder Git som primär källa och REST per commit som correctness fallback när Git-resultat saknas/är ogiltigt eller Git-providern ger `ProviderException`. Oväntade runtime-fel maskeras inte som fallback.

## Steg 5 – Resurs- och säkerhetsskydd ✅

- [x] Begränsa till högst 1 samtidig Git clone per worker.
- [x] Kontrollera ledigt diskutrymme före clone.
- [x] Inför konfigurerbar maxgräns för temporär Git-data.
- [x] Avbryt säkert vid timeout/resursgräns.
- [x] Fall tillbaka till REST där fallback uttryckligen stöds.
- [x] Säkerställ cleanup efter success/fel.
- [x] Logga inga access tokens eller credential-URL:er.

**Klart:** `GitWorkspaceService` har konfigurerbar timeout, maxstorlek, diskreserv och worker-lokal concurrency guard. Gemensamma Git-kommandon körs genom `GitCommandRunner` med timeout, credential-sanering och interrupt-bevarande.

### Steg 5B – Återanvänd clone över flera backfill-batchar ✅

- [x] Höj bounded arbetsurval till högst 1 000 saknade commits.
- [x] Anropa Git-historikprovidern en gång per bounded jobb.
- [x] Behandla resultatet i logiska delgrupper om 100 commits.
- [x] Behåll REST-fallback per commit och continuation.
- [x] Lägg test med fler än 100 commits som verifierar ett enda Git-history-anrop.

**Effekt:** ett repository med 2 000 commits kräver normalt omkring 2 temporära clones i stället för omkring 20, utan permanent Git-cache.

## Steg 6 – Automatisk uppgraderings-/backfill-trigger ✅

- [x] Kontrollera periodiskt i worker-rollen om repositories har äldre contribution scope.
- [x] Köa endast saknade backfill-jobb och använd befintlig deduplicering.
- [x] Gör triggern idempotent.
- [x] Låt normal inkrementell sync fortsätta medan historisk backfill pågår.

**Klart:** `ContributionScopeUpgradeService` hittar bounded kandidater med äldre contribution scope och köar change-kind-backfill via befintlig jobbdeduplicering.

## Steg 7 – Acceptance, prestanda och dokumentation 🚧

- [x] Acceptance-test med verkligt lokalt Git-repository.
- [x] Mixed commit med kod, dokumentation och CI/CD utan dubbla filposter.
- [ ] Verifiera privata/public GitHub-repositories i miljö med lämpliga credentials.
- [x] Strukturerad logg för clone/fetch-tid, temporär Git-storlek och analyserade commits.
- [x] Strukturerad logg för Git-analyserade commits och REST-fallbacks per jobb.
- [x] Cleanup efter normal körning och resurs-/felvägar.
- [ ] Samla verkliga mätvärden från minst ett representativt public/private repository efter deployment eller credential-baserad acceptance-körning.
- [ ] Uppdatera operativa rekommendationer med observerade mätvärden.
- [x] Full CI och large-account acceptance på slutlig refaktorerad HEAD.
- [x] Dokumentera slutlig synk-/transportarkitektur i `docs/sync-architecture.md`.

### Operativa mätfält

`git_history_backfill_transfer` loggar:

- `repository`
- `requestedCommits`
- `analyzedCommits`
- `cloneDurationMs`
- `temporaryGitBytes`

`git_history_backfill_job` loggar:

- `repositoryId`
- `processedCommits`
- `gitCommits`
- `restFallbacks`
- `continuationQueued`

**Kvar före full operativ sign-off:** credential-baserad körning mot representativ public/private GitHub-repository samt dokumenterade observerade mätvärden/rekommenderade gränser.

## Förväntad effekt

För repositories med lång historik minskar Git-baserad backfill GitHub REST-belastningen från ungefär ett commit-detail-anrop per historisk commit till huvudsakligen en Git-transfer per bounded jobbsegment på upp till 1 000 commits.

Repository file/manifest evidence använder också Git som primär transport och undviker recursive-tree + upp till 40 separata content-anrop i normalfallet. REST finns kvar som fallback.

Contributor-statistik och weekly activity hämtas från ett gemensamt `/stats/contributors`-svar. Persisted provider-login återanvänds så `/user` normalt inte behöver anropas per repository.

## Beslutspunkt efter operativ mätning

Efter verkliga mätvärden ska vi ta ställning till om en liten permanent bare Git-cache är värd att införa. Det ska inte göras utan mätdata som tydligt visar behovet.

## Arkitektur

Se `docs/sync-architecture.md` för aktuell ansvarsfördelning mellan:

- GitHub REST,
- lokal Git,
- Git workspace/process handling,
- contribution orchestration/ingestion,
- contributor snapshot/persistence,
- repository snapshot/fallback,
- repository model mapping.

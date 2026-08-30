# Functional Specification – Developer Activity & Experience Analytics

**Version:** 1.0  
**Status:** Draft functional specification  
**Scope:** Version 1 with future extension points

---

## 1. Purpose

The service shall allow a user to sign in with their source-code hosting account and obtain a private, structured and evidence-based view of their own software-development and open-source activity.

The service shall combine:

- factual activity statistics,
- project-level analysis,
- technology and project-category analysis,
- activity and technology trends over time,
- analysis of the user's involvement in external and self-owned projects,
- optional AI-assisted classification and interpretation,
- exportable reports that the user can choose to share with others.

The primary purpose of version 1 is self-analysis. The service shall not be designed as a public search or profiling service for arbitrary third parties.

The service shall work well both on large screens and on mobile phones.

---

## 2. Goals for Version 1

Version 1 shall make it possible for a user to:

1. Sign in without creating a separate service-specific password.
2. Connect their GitHub account as the primary data source.
3. Analyse their own public repositories and contributions.
4. Optionally include selected private repositories.
5. View activity, project, technology and contribution statistics over time.
6. Understand what kinds of projects they have worked on.
7. Identify technologies they appear to have used and how that usage has changed over time.
8. Distinguish between self-owned projects and contributions to projects owned by others.
9. Identify large or otherwise significant projects in which they have participated.
10. View statistics such as commits per year/month and typical commit size.
11. See clearly which information is measured directly and which conclusions are AI-generated or inferred.
12. Export selected information as a report suitable for sharing.
13. Allow an authorised external GPT/AI client to retrieve the user's selected analysis data for complementary analysis.
14. Optionally receive and store complementary AI analysis returned by such a client.
15. Control which private information may be used in analysis and export.
16. Delete their analysed data and disconnect the service from GitHub.

---

# 3. Non-goals for Version 1

Version 1 shall not primarily support:

- searching for arbitrary GitHub users,
- public ranking of developers,
- comparison or scoring of unrelated users,
- recruiter-oriented candidate screening,
- automatic publication of user profiles,
- public profile pages,
- analysis of source-code hosting platforms other than GitHub,
- organisational workforce analytics,
- team performance measurement,
- productivity scoring based solely on commit counts,
- automatic assessment of employee performance,
- exposing private repository names or contents to other users,
- permanent storage of unnecessary private source code.

These capabilities may be considered separately in future versions, but they are outside the detailed scope of version 1.

---

# 4. Core Concepts

## 4.1 User

A user is a person who has authenticated to the service and whose own development activity is analysed.

Each user shall have an isolated private analysis space.

## 4.2 Connected account

A connected account represents an external source used to obtain activity and project data.

Version 1 shall support GitHub.

A connected account may provide:

- identity,
- repositories,
- contribution activity,
- commit statistics,
- project metadata,
- language information,
- issue and pull-request activity,
- review activity,
- repository visibility,
- organisation-related information where the user has authorised access.

## 4.3 Project

A project represents a repository or a logical software project represented by a repository.

The service shall be able to distinguish at minimum between:

- user-owned repositories,
- organisation-owned repositories,
- repositories owned by other users,
- forks,
- archived repositories,
- public repositories,
- private repositories.

## 4.4 Contribution

A contribution is an observable activity associated with the user.

Examples include:

- commits,
- pull requests,
- pull-request reviews,
- issues,
- repository ownership,
- project maintenance activity,
- release-related activity where available.

Not all contribution types have to be available for all projects or all periods.

## 4.5 Technology evidence

Technology evidence is information indicating that a technology has been used in a project.

Possible evidence includes:

- programming languages,
- project metadata,
- repository topics,
- dependency or manifest information,
- configuration files,
- build files,
- deployment files,
- workflow definitions,
- project descriptions,
- README information where appropriate.

The service shall distinguish between:

- directly observed technology evidence,
- inferred technology usage,
- AI-classified technology usage.

## 4.6 Project classification

A project may be assigned one or more functional categories.

Examples include:

- end-user application,
- web application,
- mobile application,
- game,
- backend service,
- API,
- library,
- framework,
- developer tool,
- automation tool,
- infrastructure/platform,
- DevOps/CI/CD,
- security,
- observability,
- data/database,
- integration,
- AI/ML,
- architecture/modelling,
- documentation/education,
- standard/specification,
- experiment/prototype,
- other.

The classification model shall allow a project to belong to multiple categories.

## 4.7 AI assessment

An AI assessment is an interpretation produced by an AI model from factual or derived service data.

Examples include:

- likely technical roles,
- likely areas of strongest experience,
- summary of technology evolution,
- project categorisation,
- summary of open-source engagement,
- interpretation of activity trends.

AI assessments shall always be distinguishable from measured facts.

---

# 5. Authentication and Account Management

## 5.1 Sign-in

Version 1 shall support sign-in using GitHub.

The user shall not need to create a separate password for the service.

The service shall associate the signed-in service account with the authenticated GitHub identity.

## 5.2 Additional sign-in methods

Support for sign-in with Google and Apple may be added later.

If additional sign-in methods are introduced, authentication identity and GitHub data connection shall be treated as separate concepts.

Example:

- user signs in with Google,
- user separately connects GitHub as a data source.

This separation shall be considered in the information model even though GitHub sign-in is sufficient for version 1.

## 5.3 Account ownership

The authenticated user shall only be able to analyse the GitHub identity associated with or explicitly connected to their own account.

Version 1 shall not provide a general "enter GitHub username to analyse" function.

## 5.4 Disconnect

The user shall be able to disconnect GitHub.

The service shall clearly explain the consequences, such as:

- no further synchronisation,
- existing analysed information may remain until deleted,
- private repository access is revoked or becomes unusable.

## 5.5 Delete account/data

The user shall be able to request deletion of:

- their service account,
- connected-account information,
- imported repository metadata,
- derived statistics,
- AI assessments,
- stored report configurations,
- private analysis information.

The service shall clearly distinguish between:

- disconnecting a data source,
- deleting analysed data,
- deleting the entire service account.

---

# 6. Authorisation and Data Privacy

## 6.1 User isolation

All user analysis data shall be private by default.

One user shall not be able to access another user's dashboard, repositories, private statistics, AI assessments or reports unless the owner explicitly exports or shares information.

Version 1 shall not require public user profiles.

## 6.2 Public repository information

Public repository data may be used to create the user's private analysis.

The fact that source data is public shall not automatically make the service's aggregated analysis public.

## 6.3 Private repository information

Private repositories may be included only after explicit authorisation.

The user shall be able to:

- include private repositories,
- exclude private repositories,
- select only specific private repositories where supported,
- change this selection later.

Private repository data shall never be exposed to another service user.

## 6.4 Private-data indicators

The UI shall clearly indicate when a metric, conclusion or report includes private repository data.

Example labels:

- Public data only
- Includes private repositories
- Derived from public and private sources

## 6.5 Minimisation

The service shall only retain private information needed for the agreed analysis.

Raw source code and full commit diffs shall not be required for the standard version 1 analysis.

Where deeper analysis needs additional repository content, such processing shall be treated as a separate and explicit analysis action.

## 6.6 Export privacy

Before an export is generated, the user shall be able to see whether private-repository information is included.

The user shall be able to choose between at least:

- public-data-only report,
- full private report,
- private data included only as aggregated statistics.

The service shall not automatically publish exported reports.

---

# 7. Initial User Flow

## 7.1 First visit

The service shall explain:

- what it analyses,
- that version 1 analyses the user's own activity,
- that the dashboard is private,
- that private repositories are optional,
- that reports can be exported explicitly.

## 7.2 Sign-in flow

Typical first-time flow:

1. User selects "Sign in with GitHub".
2. User authenticates.
3. Service obtains the identity required to establish the user's account.
4. Service presents available analysis options.
5. User chooses whether to analyse:
   - public data only,
   - public and selected private repositories.
6. Initial data collection starts.
7. User is shown progress and partial results as they become available.
8. User is taken to their dashboard.

## 7.3 Initial analysis progress

For large accounts the complete first analysis may require multiple processing stages.

The service shall show understandable progress, for example:

- repositories discovered,
- repositories analysed,
- commits/statistics indexed,
- technologies detected,
- project classifications completed,
- AI analysis pending/completed.

The dashboard should become useful before all optional analysis has completed.

---

# 8. Data Synchronisation

## 8.1 Initial synchronisation

The service shall collect enough data to establish:

- repository inventory,
- repository ownership/visibility,
- activity periods,
- commits,
- project metadata,
- languages and other technology evidence,
- external contributions where available,
- project significance indicators.

## 8.2 Incremental synchronisation

After the initial analysis, later synchronisation shall primarily process changes since the last successful update.

The user shall be able to see:

- when data was last synchronised,
- whether analysis is current,
- whether some repositories failed to update.

## 8.3 Manual refresh

The user shall be able to request a refresh.

The service may limit excessive refresh requests, but shall explain when previously collected data is being reused.

## 8.4 Changed repository permissions

If access to a private repository is lost:

- the service shall stop refreshing it,
- the user shall be informed,
- existing stored analysis shall follow the user's retention/deletion preferences,
- it shall not silently be treated as public.

---

# 9. Dashboard

The dashboard shall provide a concise overview and navigation to deeper analysis.

## 9.1 Overview metrics

The overview should include at minimum:

- number of repositories analysed,
- number of self-owned projects,
- number of external projects contributed to,
- public/private repository counts,
- total observed commits,
- analysed time span,
- currently active projects,
- detected technologies,
- detected project categories.

The exact metrics shown may adapt to the available data.

## 9.2 Activity summary

The dashboard shall show recent and historical activity trends.

Examples:

- commits this year,
- commits previous year,
- active months,
- active projects,
- new projects started,
- projects with continued activity.

## 9.3 Important projects

The dashboard shall show a selection of important projects, not simply the largest repositories.

Importance may reflect:

- amount of user activity,
- duration of engagement,
- recency,
- project size,
- number of contributors,
- external project popularity,
- user's relative contribution,
- technical uniqueness.

The service shall explain why a project is considered important when practical.

## 9.4 Technology summary

The dashboard shall show the user's most strongly evidenced technologies.

The service shall avoid presenting a technology as a confirmed skill merely because it appears once.

---

# 10. Project Inventory

## 10.1 Project list

The user shall be able to browse analysed projects.

The list shall support filtering or grouping by:

- own vs external,
- public vs private,
- active vs inactive,
- archived,
- fork/non-fork,
- project category,
- technology,
- activity period,
- significance.

## 10.2 Project detail

Each project detail page shall show available information such as:

- project name or private-project alias,
- ownership,
- visibility,
- project description,
- active period,
- user activity,
- detected technologies,
- project categories,
- project significance indicators,
- contribution types,
- AI classifications,
- confidence/evidence where relevant.

## 10.3 Private project naming

The service shall support hiding private repository names in exported material.

The user may choose to represent them as:

- Private project 1
- Private backend project
- Aggregated private projects

rather than revealing repository names.

---

# 11. Project Categories

## 11.1 Category overview

The service shall provide an overview of the types of projects the user has been involved in.

Example:

| Category | Projects | Activity |
|---|---:|---:|
| Web applications | 42 | High |
| Developer tooling | 27 | High |
| Infrastructure/platform | 18 | Medium |
| Games | 14 | Medium |
| Libraries | 12 | Medium |

## 11.2 Multiple categories

A project may belong to more than one category.

Example:

A project may simultaneously be:

- developer tooling,
- web application,
- AI/ML.

## 11.3 Category over time

The user shall be able to see how project categories change over time.

Example questions the view should help answer:

- When did I begin working with infrastructure/platform projects?
- Have I shifted from applications toward tooling?
- Which kinds of projects have I maintained longest?

---

# 12. Technology Analysis

## 12.1 Technology inventory

The service shall identify technologies supported by available evidence.

Technology types may include:

- programming languages,
- frameworks,
- runtime platforms,
- databases,
- CI/CD technologies,
- container technologies,
- infrastructure/deployment technologies,
- build tools,
- test frameworks,
- package ecosystems,
- cloud-related technologies,
- mobile/platform technologies,
- AI/ML-related technologies.

## 12.2 Evidence levels

Technology conclusions shall be accompanied by an evidence level such as:

- Strong evidence
- Moderate evidence
- Limited evidence
- Exposure only

The service shall avoid implying formal proficiency levels that cannot be supported by repository evidence.

## 12.3 Technology detail

For each technology, the service should be able to show:

- first observed use,
- most recent observed use,
- number of projects,
- number of active periods,
- amount of associated activity,
- representative projects,
- whether evidence comes from public/private projects.

## 12.4 Technology over time

The service shall visualise how technology usage has changed.

Possible views:

- technology by year,
- technology by month,
- first/last observed usage,
- growing/declining usage,
- project count by technology over time,
- activity by technology over time.

---

# 13. Activity Timeline

## 13.1 Commits over time

The service shall support at least:

- commits per year,
- commits per month.

Where possible, the user shall be able to select a time period.

## 13.2 Active projects over time

The service shall show:

- number of active projects by period,
- projects started,
- projects becoming inactive,
- long-running projects.

## 13.3 Contribution types over time

Where data exists, the service should show trends for:

- commits,
- pull requests,
- reviews,
- issues,
- releases or equivalent maintenance activity.

---

# 14. Commit Statistics

## 14.1 Commit volume

The service shall provide:

- total analysed commits,
- commits per year,
- commits per month,
- average commits per active month.

## 14.2 Commit size

Where underlying data supports it, the service shall provide statistics for commit size using additions/deletions/changed lines.

At minimum, where available:

- average changed lines per commit,
- median changed lines per commit,
- additions,
- deletions.

The service should prefer showing median in addition to average because unusually large commits may distort the mean.

## 14.3 Commit-size trends

The user shall be able to see how typical commit size changes by:

- year,
- month,
- selected time period.

## 14.4 Exceptional commits

Very large imports, generated-code changes or other clear outliers should not silently distort interpretation.

The service may:

- identify likely outliers,
- show statistics with and without outliers,
- explain the effect on averages.

---

# 15. Own Projects vs External Contributions

The service shall distinguish between:

1. projects owned primarily by the user,
2. projects owned by an organisation in which the user participates,
3. projects owned by third parties,
4. forks.

This distinction shall be available in statistics and reports.

The service should help answer questions such as:

- How much of my activity is in my own projects?
- How much is contribution to other people's projects?
- How has this balance changed over time?
- Which external projects have I contributed to most?

---

# 16. Significant External Projects

The service shall identify externally owned projects that may be significant in the user's history.

Significance shall not be based solely on project popularity.

The analysis should consider both:

## 16.1 Project significance

Possible indicators:

- contributor count,
- project activity,
- forks,
- stars or equivalent popularity signals,
- project longevity,
- recognised organisation or ecosystem,
- release activity.

## 16.2 User involvement

Possible indicators:

- commits,
- pull requests,
- reviews,
- issues,
- duration of involvement,
- recency,
- repeated contributions,
- relative contribution where available.

The UI shall clearly distinguish:

- "large project",
- "large user contribution",

because these are not equivalent.

---

# 17. Role and Experience Interpretation

Version 1 may provide AI-assisted interpretation of likely technical roles.

Possible role labels include:

- backend developer,
- frontend developer,
- full-stack developer,
- mobile developer,
- game developer,
- platform engineer,
- DevOps engineer,
- software architect,
- library/framework developer,
- maintainer,
- reviewer,
- documentation contributor,
- release/automation contributor.

These shall be presented as inferred roles, not verified employment roles.

A role assessment should show supporting evidence where practical.

Example:

> Platform/DevOps – strong evidence  
> Repeated activity across 23 projects containing CI/CD, container and deployment-related technology evidence over six years.

---

# 18. AI-Assisted Analysis Inside the Service

## 18.1 Purpose

AI may be used for tasks that benefit from semantic interpretation rather than simple counting.

Examples:

- project categorisation,
- project summarisation,
- technology normalisation,
- likely role identification,
- technology-history summary,
- open-source engagement summary.

## 18.2 Separation from facts

Every AI-produced conclusion shall be visually or semantically distinguishable from measured statistics.

Recommended labels:

- Measured
- Derived
- AI assessment

## 18.3 Confidence

AI-based classifications should include a confidence indicator where useful.

## 18.4 Cached analysis

AI classification of unchanged project information should be reusable rather than regenerated unnecessarily.

## 18.5 Failure handling

If AI processing is unavailable or usage limits are reached:

- factual statistics shall remain available,
- previously completed AI classifications may remain visible,
- pending AI work may be marked as waiting/not updated,
- the core service shall remain usable.

---

# 19. External GPT / AI Integration

## 19.1 Purpose

The service shall expose selected user analysis information to an authorised external GPT or AI client.

The purpose is to support deeper questions than the standard dashboard provides.

Examples:

- How has my technical focus evolved during the last 15 years?
- Which technical roles are most strongly evidenced by my project history?
- Which areas show depth versus breadth?
- How relevant is my history to a particular type of technical role?
- Which projects best demonstrate a particular technology area?

## 19.2 User-scoped access

An external GPT shall only receive data belonging to the authenticated/authorised user.

Version 1 shall not provide an endpoint that allows an external GPT to request arbitrary users by username.

## 19.3 Selectable data scope

The service should support scopes such as:

- public analysis only,
- public + aggregated private analysis,
- full authorised private analysis.

The user shall remain in control of whether private information may be exposed to an external AI client.

## 19.4 Readable analysis information

The external client should be able to obtain structured information equivalent to:

- profile summary,
- project inventory,
- technology analysis,
- project categories,
- timeline,
- activity statistics,
- contribution information,
- evidence behind conclusions.

## 19.5 Returning complementary analysis

The service shall support storing complementary AI analysis returned by an authorised client.

Returned analysis shall include information identifying:

- analysis type,
- creation time,
- source/client,
- input data scope,
- whether private data was included.

## 19.6 AI analysis display

Returned AI analysis shall be shown separately from factual statistics.

The user shall be able to delete saved AI analyses.

---

# 20. Reports and Export

## 20.1 General

The service shall allow the user to create an exportable report from their analysis.

Reports shall be generated only on explicit user request.

## 20.2 Report types

Version 1 should support at least:

### Public open-source report

Uses only public information.

Suitable for sharing externally.

### Full developer report

May include private repository analysis selected by the user.

Shall be clearly marked as containing non-public evidence.

### Technology profile

Focuses on:

- technologies,
- evidence,
- technology evolution,
- representative projects.

### Activity report

Focuses on:

- commits,
- project activity,
- trends,
- commit-size statistics,
- contribution history.

## 20.3 Report content selection

Before export the user shall be able to choose whether to include:

- summary,
- project categories,
- technology analysis,
- activity timeline,
- significant projects,
- external contributions,
- role assessments,
- AI summaries,
- private project information,
- private project names.

## 20.4 Export formats

Version 1 shall support Markdown export.

PDF export is strongly desirable for version 1 if feasible and should be treated as a primary presentation format for sharing.

A future machine-readable export may include JSON or equivalent structured format.

## 20.5 Report metadata

Reports shall state at minimum:

- analysis period,
- generation date,
- number of analysed projects,
- data sources,
- whether private repositories were included,
- whether AI-generated conclusions are included.

## 20.6 Method transparency

Reports should contain a short methodology section explaining:

- what data was analysed,
- what was measured directly,
- what was inferred,
- limitations of the analysis.

---

# 21. Responsive User Experience

## 21.1 General requirement

All version 1 functionality shall be usable on:

- desktop/laptop large screens,
- tablets,
- mobile phones.

No essential function shall require a large-screen layout.

## 21.2 Large-screen behaviour

Large-screen views may use:

- multi-column dashboards,
- simultaneous filters and charts,
- richer comparison tables,
- persistent navigation.

## 21.3 Mobile behaviour

Mobile views shall prioritise:

- summary cards,
- vertically stacked charts,
- collapsible details,
- touch-friendly controls,
- simplified filters,
- readable project cards rather than overly wide tables.

Wide tables shall not be the only way to access important information.

## 21.4 Charts

Charts shall:

- remain readable on small screens,
- support shortened labels or interactive detail,
- have text equivalents or summaries for key findings,
- avoid relying only on colour to communicate meaning.

---

# 22. Navigation Structure

A possible version 1 navigation model is:

1. Overview
2. Activity
3. Projects
4. Technologies
5. Project types
6. Contributions
7. AI insights
8. Reports
9. Data sources & privacy
10. Account

The exact visual navigation may differ between mobile and large screens, but the conceptual destinations shall remain consistent.

---

# 23. Data Quality and Transparency

## 23.1 Coverage

The service shall show the user how much information was analysed.

Example:

- 237 repositories discovered
- 218 analysed successfully
- 19 unavailable or excluded
- 14 private repositories included
- activity coverage: 2009–2026

## 23.2 Missing information

If historical data is incomplete, the service shall not present an apparently complete history without qualification.

Possible states:

- Complete for available source
- Partial
- Not available
- Not yet analysed

## 23.3 Evidence

Important inferred conclusions should provide access to supporting evidence.

For example:

Technology: Kubernetes  
Evidence:
- 12 projects
- first observed: 2019
- most recent: 2026
- manifests detected in 9 projects
- Helm usage in 5 projects

## 23.4 Corrections

The user should be able to correct or suppress clearly incorrect AI classifications.

Version 1 should support at least:

- mark classification as incorrect,
- exclude a project from AI profile,
- exclude a technology inference from summaries.

Manual correction shall not alter source facts.

---

# 24. Filters and Time Ranges

Where relevant, views shall support common time filters such as:

- all time,
- current year,
- previous year,
- last 12 months,
- last 5 years,
- custom period.

The service should preserve enough context to avoid misleading comparisons between partial and full years.

---

# 25. Search

The project inventory shall support search by:

- project name,
- description,
- technology,
- category.

Search over private projects shall remain user-private.

---

# 26. User Preferences

Version 1 should retain user preferences such as:

- default time range,
- whether private repositories are included in dashboard analysis,
- preferred project exclusions,
- preferred report privacy settings.

Sensitive defaults shall favour privacy.

---

# 27. Notifications and Background Processing

Version 1 does not require extensive notification functionality.

The service should nevertheless support clear status for long-running analysis:

- queued,
- processing,
- partially complete,
- complete,
- failed.

The user shall be able to continue using already available parts of the service while optional analysis is still being processed.

Future versions may add notifications when a full re-analysis is complete.

---

# 28. Error Handling

Errors shall be understandable and scoped.

Examples:

- GitHub authentication expired
- repository access revoked
- some repositories unavailable
- data source rate limit reached
- AI classification unavailable
- report generation failed

A failure in optional AI processing shall not make factual statistics unavailable.

A failure involving one repository shall not normally invalidate the entire user's analysis.

---

# 29. Version 1 Functional Priorities

## Must have

- GitHub sign-in
- analysis of authenticated user's own profile
- public repository inventory
- optional private repository inclusion
- strict per-user privacy
- repository/project inventory
- own vs external project distinction
- commits per year/month
- commit-size statistics where available
- project activity timeline
- technology detection
- technology evolution over time
- project-type classification
- significant project identification
- responsive desktop/mobile UI
- Markdown report export
- clear public/private report scoping
- distinction between measured data and AI inference
- data deletion/disconnect controls

## Should have

- PDF report export
- role inference
- AI-generated summaries
- external GPT read access
- ability to store complementary GPT analysis
- user correction of AI classifications
- project significance explanation
- contribution-type trends

## Could have

- advanced comparison between time periods
- automatically generated yearly retrospective
- richer project similarity clustering
- deeper commit-content analysis on explicit request
- shareable public snapshot

---

# 30. Potential Future Capabilities

The following are intentionally described only at a high level and are not part of the detailed version 1 scope.

## 30.1 Additional source-code platforms

Possible future providers:

- GitLab.com
- self-managed GitLab
- Bitbucket
- Azure DevOps repositories
- Codeberg
- other relevant Git hosting services

The service should eventually be able to merge activity from multiple identities belonging to the same user.

## 30.2 Package ecosystems

Possible integration with package registries could show evidence that the user maintains or publishes reusable software.

Examples:

- npm
- Maven Central
- PyPI
- crates.io
- NuGet
- other package ecosystems.

## 30.3 Container and artifact registries

Future analysis could include:

- published container images,
- release artefacts,
- package downloads,
- project distribution activity.

## 30.4 Cross-platform history

The service may eventually provide a unified timeline covering multiple development platforms.

Duplicate repositories or mirrors would need to be detected to avoid double counting.

## 30.5 Public snapshots

A future version may allow the user to intentionally publish a selected snapshot of their analysis.

This shall be opt-in and separate from the private dashboard.

## 30.6 Yearly retrospective

The service could generate a "developer year in review" showing:

- activity,
- new technologies,
- newly started projects,
- longest-running projects,
- largest external contributions,
- technology shifts.

## 30.7 Role-specific reports

The user could request reports focused on relevance to roles such as:

- software architect,
- platform engineer,
- backend developer,
- full-stack developer,
- maintainer.

Such reports should remain evidence-based rather than functioning as opaque scoring.

## 30.8 Comparative self-analysis

The user may compare their own periods, for example:

- 2015–2019 vs 2020–2024,
- before/after adoption of a technology,
- public vs private activity,
- own projects vs external contributions.

## 30.9 Deeper repository analysis

On explicit request, a future version may analyse selected source files, architectural structure or commit diffs to improve interpretation of technical roles and experience.

This should be optional and subject to stronger privacy controls.

---

# 31. Success Criteria for Version 1

Version 1 should be considered functionally successful if a user with a long and varied GitHub history can:

1. Sign in and analyse their own account.
2. Include or exclude private repositories consciously.
3. Obtain a useful overview even with hundreds of repositories.
4. Understand how their development activity changed over time.
5. See what kinds of projects they worked on.
6. See which technologies are strongly or weakly evidenced.
7. Identify major self-owned and external projects.
8. View meaningful commit statistics rather than only raw totals.
9. Distinguish measured facts from inferred/AI conclusions.
10. Use the service comfortably on both a phone and a large screen.
11. Export a report that can be shared without unintentionally revealing private information.
12. Optionally use an external GPT for deeper analysis without exposing another user's data.

---

# 32. Guiding Functional Principles

The following principles shall guide version 1:

**Private by default**  
The analysis belongs to the user and is not public unless explicitly exported.

**Self-analysis first**  
The product helps users understand their own development history rather than profile other people.

**Facts before interpretation**  
Measured statistics and observed evidence form the foundation; AI adds interpretation.

**Evidence over simplistic scoring**  
The service should explain why it reaches a conclusion rather than reduce a person to a single score.

**Large histories must remain usable**  
Accounts with hundreds of repositories shall be treated as a normal supported case.

**Selective depth**  
Not every repository requires equally deep analysis.

**Mobile is a first-class experience**  
A phone user shall be able to access all important functionality.

**Explicit private-data control**  
Private repositories and private-derived conclusions shall never be silently exposed.

**Export instead of automatic publication**  
Version 1 shall favour user-controlled reports over public profiles.

**Extensible source model**  
GitHub is the version 1 source, but the functional model should not prevent adding other development platforms later.

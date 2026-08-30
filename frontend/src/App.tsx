export function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Developer Analytics home">
          Developer Analytics
        </a>
        <a className="sign-in-button" href="/api/auth/github/login">
          Sign in with GitHub
        </a>
      </header>

      <main className="main-content">
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Private developer history</p>
          <h1 id="page-title">Understand how your development work has evolved.</h1>
          <p className="hero-copy">
            Developer Analytics turns your GitHub activity into an evidence-based view of
            projects, technologies, contributions and change over time. Your analysis is private
            by default.
          </p>
          <p>
            <a className="primary-action" href="/api/auth/github/login">
              Sign in with GitHub
            </a>
          </p>
        </section>

        <section className="feature-grid" aria-label="Planned capabilities">
          <article className="feature-card"><h2>Activity</h2><p>Explore commits, project activity and contribution trends over time.</p></article>
          <article className="feature-card"><h2>Technologies</h2><p>See which technologies are evidenced by your projects and when you used them.</p></article>
          <article className="feature-card"><h2>Projects</h2><p>Understand the types of projects you build and significant external contributions.</p></article>
        </section>
      </main>

      <footer className="footer">Version 1 development baseline</footer>
    </div>
  )
}

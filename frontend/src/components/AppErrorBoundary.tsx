import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { hasError: boolean }

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled application error', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="error-page" role="alert">
          <div>
            <p className="eyebrow">Developer Analytics</p>
            <h1>Something went wrong.</h1>
            <p>Reload the page to try again.</p>
          </div>
        </main>
      )
    }

    return this.props.children
  }
}

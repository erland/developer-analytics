import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach } from 'vitest'

vi.mock('../../hooks/useDataFreshness', () => ({
  useDataFreshness: () => ({
    status: 'fresh',
    timestamp: '2026-08-30T12:00:00Z',
    label: 'Fresh',
  }),
}))

vi.mock('../../components/OverviewDashboard', () => ({
  OverviewDashboard: () => <div>Overview mock</div>,
}))
vi.mock('../../components/ActivityView', () => ({
  ActivityView: () => <div>Activity mock</div>,
}))
vi.mock('../../components/ProjectInventoryView', () => ({
  ProjectInventoryView: () => <div>Projects mock</div>,
}))
vi.mock('../../components/TechnologyViews', () => ({
  TechnologyViews: () => <div>Technologies mock</div>,
}))
vi.mock('../../components/ProjectTypeViews', () => ({
  ProjectTypeViews: () => <div>Project types mock</div>,
}))
vi.mock('../../components/PrivacyDataSourcesView', () => ({
  PrivacyDataSourcesView: () => <div>Privacy mock</div>,
}))
vi.mock('../../components/ReportsView', () => ({
  ReportsView: () => <div>Reports mock</div>,
}))
vi.mock('../../components/AiInsightsView', () => ({
  AiInsightsView: () => <div>AI mock</div>,
}))
vi.mock('../../components/AccountView', () => ({
  AccountView: () => <div>Account mock</div>,
}))

import { AuthenticatedShell } from '../../components/AuthenticatedShell'

describe('responsive layer: authenticated shell navigation', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/')
  })

  it('opens and closes the compact navigation without losing section selection', () => {
    render(
      <AuthenticatedShell
        user={{
          authenticated: true,
          provider: 'github',
          login: 'developer',
          displayName: 'Developer',
        }}
      />,
    )

    expect(screen.getByRole('button', { name: 'Activity' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Timeline' })).not.toBeInTheDocument()
    expect(screen.getByText('Explore')).toBeInTheDocument()
    expect(screen.getByText('Insights')).toBeInTheDocument()
    expect(screen.getByText('Settings')).toBeInTheDocument()

    const toggle = screen.getByRole('button', { name: 'Toggle navigation' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    expect(
      screen.getByRole('button', { name: 'Close navigation' }),
    ).toBeVisible()

    fireEvent.click(screen.getByRole('button', { name: 'Reports' }))

    expect(screen.getByRole('heading', { name: 'Reports', level: 1 })).toBeVisible()
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(
      screen.queryByRole('button', { name: 'Close navigation' }),
    ).not.toBeInTheDocument()
  })
  it('shows a compact Explore filter count when analysis context is active', () => {
    window.history.replaceState(null, '', '/?technology=java&year=2026&month=2026-08')
    render(
      <AuthenticatedShell
        user={{
          authenticated: true,
          provider: 'github',
          login: 'developer',
          displayName: 'Developer',
        }}
      />,
    )

    expect(screen.getByLabelText('2 active filters')).toHaveTextContent('2')
  })

  it('preserves the active AnalysisScope query while moving between Explore sections', () => {
    window.history.replaceState(null, '', '/?technology=java&year=2026')
    render(
      <AuthenticatedShell
        user={{
          authenticated: true,
          provider: 'github',
          login: 'developer',
          displayName: 'Developer',
        }}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Technologies' }))
    expect(window.location.search).toBe('?technology=java&year=2026')

    fireEvent.click(screen.getByRole('button', { name: 'Activity' }))
    expect(window.location.search).toBe('?technology=java&year=2026')

    fireEvent.click(screen.getByRole('button', { name: 'Projects' }))
    expect(window.location.search).toBe('?technology=java&year=2026')
  })

})

import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

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
vi.mock('../../components/TechnologyViews', () => ({
  TechnologyViews: () => (
    <div>
      <span data-testid="technology-scope">{window.location.search}</span>
      <button
        type="button"
        onClick={() => {
          window.history.pushState(null, '', '/?technology=java&year=2026')
          window.dispatchEvent(new Event('developer-analytics:analysis-scope-change'))
        }}
      >
        Select Java in 2026
      </button>
    </div>
  ),
}))
vi.mock('../../components/ActivityView', () => ({
  ActivityView: () => <div data-testid="activity-scope">{window.location.search}</div>,
}))
vi.mock('../../components/ProjectInventoryView', () => ({
  ProjectInventoryView: () => <div data-testid="projects-scope">{window.location.search}</div>,
}))
vi.mock('../../components/ProjectTypeViews', () => ({
  ProjectTypeViews: () => <div>Project types mock</div>,
}))
vi.mock('../../components/ContributionsView', () => ({
  ContributionsView: () => <div>Contributions mock</div>,
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
vi.mock('../../components/ProjectDetailView', () => ({
  ProjectDetailView: () => <div>Project detail mock</div>,
}))

import { AuthenticatedShell } from '../../components/AuthenticatedShell'

describe('Explore scope navigation', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/')
  })

  it('preserves a selection made in Technologies when moving to Activity and Projects', () => {
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
    expect(screen.getByTestId('technology-scope')).toHaveTextContent('')

    fireEvent.click(screen.getByRole('button', { name: 'Select Java in 2026' }))
    expect(window.location.search).toBe('?technology=java&year=2026')
    expect(screen.getByLabelText('2 active filters')).toHaveTextContent('2')

    fireEvent.click(screen.getByRole('button', { name: 'Activity' }))
    expect(screen.getByTestId('activity-scope')).toHaveTextContent('?technology=java&year=2026')
    expect(window.location.search).toBe('?technology=java&year=2026')

    fireEvent.click(screen.getByRole('button', { name: 'Projects' }))
    expect(screen.getByTestId('projects-scope')).toHaveTextContent('?technology=java&year=2026')
    expect(window.location.search).toBe('?technology=java&year=2026')
  })

  it('keeps the same scope when revisiting an Explore view', () => {
    window.history.replaceState(null, '', '/?technology=java&projectType=backend&year=2026')

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

    fireEvent.click(screen.getByRole('button', { name: 'Activity' }))
    expect(screen.getByTestId('activity-scope')).toHaveTextContent(
      '?technology=java&projectType=backend&year=2026',
    )

    fireEvent.click(screen.getByRole('button', { name: 'Projects' }))
    expect(screen.getByTestId('projects-scope')).toHaveTextContent(
      '?technology=java&projectType=backend&year=2026',
    )

    fireEvent.click(screen.getByRole('button', { name: 'Technologies' }))
    expect(screen.getByTestId('technology-scope')).toHaveTextContent(
      '?technology=java&projectType=backend&year=2026',
    )
  })
})

import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('renders the application heading and baseline capabilities', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: /understand how your development work has evolved/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Activity' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Technologies' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Projects' })).toBeInTheDocument()
  })
})

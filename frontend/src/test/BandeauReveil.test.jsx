import { act, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import BandeauReveil from '../components/BandeauReveil.jsx'

describe('BandeauReveil', () => {
  it("s'affiche quand une requete est lente et disparait ensuite", () => {
    render(<BandeauReveil />)
    expect(screen.queryByRole('status')).toBeNull()

    act(() => { window.dispatchEvent(new CustomEvent('taguora:lent', { detail: true })) })
    expect(screen.getByRole('status')).toHaveTextContent('Le serveur se réveille')

    act(() => { window.dispatchEvent(new CustomEvent('taguora:lent', { detail: false })) })
    expect(screen.queryByRole('status')).toBeNull()
  })
})

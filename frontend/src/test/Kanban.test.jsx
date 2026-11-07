import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import Kanban from '../components/Kanban.jsx'

const candidature = (id, nomEntreprise, statut) => ({
  id, nomEntreprise, statut, poste: 'Dev', etapes: [{ type: 'CANDIDATURE_ENVOYEE', date: '2026-11-01' }],
})

describe('Kanban', () => {
  it('range chaque candidature dans la colonne de son statut', () => {
    render(
      <MemoryRouter>
        <Kanban onMaj={vi.fn()} liste={[
          candidature(1, 'Atlas Studio', 'EN_ATTENTE'),
          candidature(2, 'Nordlys', 'EN_COURS'),
          candidature(3, 'Kaliop', 'OFFRE_RECUE'),
        ]} />
      </MemoryRouter>,
    )
    const colonnes = document.querySelectorAll('.kanban-col')
    expect(colonnes).toHaveLength(5)
    expect(within(colonnes[0]).getByText('Atlas Studio')).toBeInTheDocument()
    expect(within(colonnes[1]).getByText('Nordlys')).toBeInTheDocument()
    expect(within(colonnes[2]).getByText('Kaliop')).toBeInTheDocument()
    expect(within(colonnes[3]).getByText('Glisse une carte ici')).toBeInTheDocument()
    expect(screen.getAllByLabelText(/Déplacer/)).toHaveLength(3)
  })
})

import { STATUTS } from '../labels'

export default function StatutBadge({ statut }) {
  const s = STATUTS[statut] ?? { label: statut, couleur: 'gris' }
  return <span className={`badge ${s.couleur}`}>{s.label}</span>
}

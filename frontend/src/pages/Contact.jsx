import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CircleCheck, Clock, LifeBuoy, Mail, Sparkles } from 'lucide-react'
import { api } from '../api'
import { useAuth } from '../auth.jsx'
import PagePublique from '../components/PagePublique.jsx'

const CATEGORIES = [
  { valeur: 'PROBLEME', label: 'Signaler un problème' },
  { valeur: 'QUESTION', label: 'Poser une question' },
  { valeur: 'SUGGESTION', label: 'Proposer une amélioration' },
  { valeur: 'AUTRE', label: 'Autre demande' },
]

export default function Contact() {
  const { user } = useAuth()
  const [form, setForm] = useState({
    nom: user?.nom ?? '', email: user && !user.demo ? user.email : '', categorie: 'PROBLEME', sujet: '', message: '',
  })
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState('')
  const [envoye, setEnvoye] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  const onSubmit = async (e) => {
    e.preventDefault()
    setErreur('')
    setEnvoi(true)
    try {
      await api.contact(form)
      setEnvoye(true)
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <PagePublique titre="Aide et contact"
      sousTitre="Un problème, une question ou une idée ? Écris-nous : chaque message est lu par l'équipe Postulyn.">
      <div className="contact-grille">
        <aside className="contact-infos">
          <div className="contact-info">
            <Sparkles size={20} />
            <div>
              <strong>Une réponse immédiate avec ORVYNA</strong>
              <p>Connecté à ton espace, ouvre la bulle « Besoin d'aide ? » en bas à droite : l'assistante répond
                aux questions sur l'application et aide à résoudre les problèmes courants.</p>
            </div>
          </div>
          <div className="contact-info">
            <Clock size={20} />
            <div>
              <strong>Une réponse dès que possible</strong>
              <p>Pour un problème : décris ce que tu faisais, la page concernée et le message d'erreur exact.</p>
            </div>
          </div>
          <div className="contact-info">
            <Mail size={20} />
            <div>
              <strong>Accusé de réception</strong>
              <p>Un email de confirmation t'est envoyé, puis nous te répondons à l'adresse indiquée.</p>
            </div>
          </div>
        </aside>

        {envoye ? (
          <div className="card contact-merci">
            <CircleCheck size={40} />
            <h2>Message envoyé</h2>
            <p className="muted">Merci ! Nous avons bien reçu ton message et te répondons rapidement à
              <strong> {form.email}</strong>.</p>
            <Link to={user ? '/' : '/accueil'} className="btn primary">{user ? 'Retour à mon espace' : 'Retour à l’accueil'}</Link>
          </div>
        ) : (
          <form className="card contact-form" onSubmit={onSubmit}>
            <h2><LifeBuoy size={20} /> Écrire à l'équipe</h2>
            <div className="grid-2">
              <label>
                Nom
                <input name="nom" value={form.nom} onChange={onChange} required maxLength={100} autoComplete="name" />
              </label>
              <label>
                Email
                <input name="email" type="email" value={form.email} onChange={onChange} required maxLength={255}
                  autoComplete="email" />
              </label>
            </div>
            <label>
              Type de demande
              <select name="categorie" value={form.categorie} onChange={onChange}>
                {CATEGORIES.map((c) => <option key={c.valeur} value={c.valeur}>{c.label}</option>)}
              </select>
            </label>
            <label>
              Sujet
              <input name="sujet" value={form.sujet} onChange={onChange} required maxLength={150}
                placeholder="Ex. Impossible d'importer mon CV" />
            </label>
            <label>
              Message
              <textarea name="message" value={form.message} onChange={onChange} required minLength={10}
                maxLength={5000} rows={7} placeholder="Décris ta demande le plus précisément possible…" />
            </label>
            {erreur && <p className="erreur">{erreur}</p>}
            <button className="btn primary" disabled={envoi}>{envoi ? 'Envoi…' : 'Envoyer le message'}</button>
            <p className="muted small">
              Tes informations servent uniquement à te répondre. Voir la <Link to="/conditions#confidentialite">politique de confidentialité</Link>.
            </p>
          </form>
        )}
      </div>
    </PagePublique>
  )
}

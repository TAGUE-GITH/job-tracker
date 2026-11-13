import { useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import { ETAPES } from '../labels'
import { arreterVoix, dicteeDisponible, parler, syntheseDisponible, useDictee } from '../voix.js'
import OrvynaAvatar from './OrvynaAvatar.jsx'
import EspaceCode from './EspaceCode.jsx'
import { ETAPES_CODE, exercice, pourLaVoix, sansMarqueur } from '../entretien.js'
import { Code2, Mic, Square, Volume2 } from 'lucide-react'

const ETAPES_ENTRETIEN = ['ECHANGE_RH', 'TEST_TECHNIQUE', 'ENTRETIEN_TECHNIQUE', 'ENTRETIEN_MANAGER']

const DEBUT_ECRIT = "Simule l'entretien : pose-moi la première question."
const DEBUT_ORAL = "Simule l'entretien à l'oral : je réponds à voix haute et mes réponses sont transcrites " +
  "automatiquement (ignore les petites fautes de transcription). Suis le même déroulé après chaque réponse (ce qui " +
  'est bien, à améliorer, réponse conseillée, puis la question suivante), en phrases courtes et naturelles, sans ' +
  'listes ni titres : tes messages sont lus à voix haute. Les exercices de code restent écrits dans un bloc de code. ' +
  'Pose la première question.'
const BILAN = "Fais le bilan de cet entretien : une note sur 10, mes 3 points forts, 3 axes d'amélioration " +
  'concrets, et la réponse que je dois retravailler en priorité avec une meilleure version.'
// Consignes envoyees a ORVYNA, affichees sous une forme courte dans la conversation
const CONSIGNES = {
  [DEBUT_ECRIT]: 'Simulation d’entretien',
  [DEBUT_ORAL]: 'Simulation d’entretien à l’oral',
  [BILAN]: 'Mon bilan, s’il te plaît',
}

export default function IaAssistant({ candidature }) {
  const [typeEtape, setTypeEtape] = useState('ENTRETIEN_TECHNIQUE')
  const [messages, setMessages] = useState([])
  const [saisie, setSaisie] = useState('')
  const [chargement, setChargement] = useState(false)
  const [erreur, setErreur] = useState('')
  const [oral, setOral] = useState(false)
  const [simulation, setSimulation] = useState(false)
  const [parle, setParle] = useState(false)
  // Espace de code : ouvert automatiquement quand ORVYNA propose un exercice
  const [espace, setEspace] = useState(null)
  const finRef = useRef(null)
  const dictee = useDictee(setSaisie)
  const oralPossible = dicteeDisponible() && syntheseDisponible()

  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, chargement])

  // On coupe la voix en quittant la page
  useEffect(() => () => arreterVoix(), [])

  const lire = (texte) => {
    setParle(true)
    parler(pourLaVoix(texte), { onFin: () => setParle(false) })
  }

  const envoyer = async (historique, lireReponse = oral) => {
    setErreur('')
    setChargement(true)
    try {
      const { reponse } = await api.ia(candidature.id, { typeEtape, messages: historique })
      setMessages([...historique, { role: 'assistant', content: reponse }])
      const ex = exercice(reponse)
      if (ex) setEspace({ langage: ex.langage, code: ex.code, depart: ex.code })
      if (lireReponse) lire(reponse)
    } catch (e) {
      setErreur(e.message)
    } finally {
      setChargement(false)
    }
  }

  const preparer = () => {
    arreterVoix()
    setEspace(null)
    setSimulation(false)
    setMessages([])
    envoyer([], false)
  }

  const simuler = () => {
    arreterVoix()
    setEspace(null)
    setSimulation(true)
    const h = [{ role: 'user', content: oral ? DEBUT_ORAL : DEBUT_ECRIT }]
    setMessages(h)
    envoyer(h)
  }

  const repondre = (texte) => {
    if (!texte.trim()) return
    dictee.arreter()
    const h = [...messages, { role: 'user', content: texte.trim() }]
    setMessages(h)
    setSaisie('')
    envoyer(h)
  }

  const onSubmit = (e) => {
    e.preventDefault()
    repondre(saisie)
  }

  const micro = () => {
    if (dictee.ecoute) {
      dictee.arreter()
    } else {
      setParle(false)
      dictee.demarrer(saisie.trim())
    }
  }

  const envoyerCode = () => {
    const nom = espace.langage === 'texte' ? '' : espace.langage
    repondre(`Voici ma solution :\n\n\`\`\`${nom}\n${espace.code.replace(/\s+$/, '')}\n\`\`\``)
  }

  const derniere = [...messages].reverse().find((m) => m.role === 'assistant')
  const reponsesDonnees = messages.filter((m) => m.role === 'user').length - 1

  return (
    <div className="card ia">
      <div className="ia-head">
        <OrvynaAvatar size={48} etat={chargement ? 'reflechit' : parle ? 'parle' : 'repos'} />
        <div>
          <h3>ORVYNA · coach d'entretien</h3>
          <p className="muted small">
            ORVYNA s'appuie sur le texte de l'offre et tes notes d'étapes pour te préparer.
          </p>
        </div>
      </div>

      <div className="row">
        <select value={typeEtape} onChange={(e) => setTypeEtape(e.target.value)}>
          {ETAPES_ENTRETIEN.map((t) => <option key={t} value={t}>{ETAPES[t]}</option>)}
        </select>
        <button className="btn primary" onClick={preparer} disabled={chargement}>Préparer</button>
        <button className="btn ghost" onClick={simuler} disabled={chargement}>
          {oral ? 'Simuler à l’oral' : 'Simuler'}
        </button>
        {ETAPES_CODE.includes(typeEtape) && !espace && (
          <button type="button" className="btn ghost" onClick={() => setEspace({ langage: 'java', code: '', depart: '' })}>
            <Code2 size={16} /> Espace de code
          </button>
        )}
      </div>
      <p className="muted small">
        {typeEtape === 'ECHANGE_RH'
          ? 'Échange RH : ORVYNA joue le recruteur et pose les vraies questions RH. Après chaque réponse, elle te dit ce qui est réussi, ce qu’il faut améliorer et te propose une meilleure version avant de continuer.'
          : ETAPES_CODE.includes(typeEtape)
            ? 'ORVYNA joue le chef de projet : questions techniques tirées de l’offre et exercices à coder dans l’espace de code, corrigés un par un.'
            : 'ORVYNA joue ton futur manager : organisation, travail en équipe, mises en situation tirées de l’offre, avec un retour après chaque réponse.'}
      </p>

      <label className={`interrupteur ${oralPossible ? '' : 'desactive'}`}>
        <input type="checkbox" checked={oral} disabled={!oralPossible}
          onChange={(e) => { setOral(e.target.checked); if (!e.target.checked) { arreterVoix(); dictee.arreter() } }} />
        <span className="glissiere" />
        <span>
          Mode oral : ORVYNA lit ses questions à voix haute et tu réponds au micro
          {!oralPossible && <em className="muted"> · disponible sur Chrome et Edge</em>}
        </span>
      </label>

      <div className="chat">
        {messages.map((m, i) => (
          <div key={i} className={`bulle ${m.role}`}>
            {m.role === 'assistant' || m.content.includes('```')
              ? <Markdown remarkPlugins={[remarkGfm]}>{sansMarqueur(m.content)}</Markdown>
              : (CONSIGNES[m.content] ?? m.content)}
          </div>
        ))}
        {chargement && <div className="bulle assistant muted">ORVYNA réfléchit…</div>}
        {erreur && <p className="erreur">{erreur}</p>}
        <div ref={finRef} />
      </div>

      {oral && derniere && (
        <div className="row oral-controles">
          {parle
            ? <button type="button" className="btn ghost small" onClick={() => { arreterVoix(); setParle(false) }}><Square size={14} /> Couper la voix</button>
            : <button type="button" className="btn ghost small" onClick={() => lire(derniere.content)}><Volume2 size={14} /> Réécouter</button>}
          {dictee.ecoute && <span className="enregistre">● Je t'écoute…</span>}
        </div>
      )}
      {dictee.erreur && <p className="erreur">{dictee.erreur}</p>}

      {espace && (
        <EspaceCode langage={espace.langage} code={espace.code} codeDepart={espace.depart}
          onLangage={(langage) => setEspace((e) => ({ ...e, langage }))}
          onCode={(code) => setEspace((e) => ({ ...e, code }))}
          onEnvoyer={envoyerCode} onFermer={() => setEspace(null)} desactive={chargement || messages.length === 0} />
      )}

      {messages.length > 0 && (
        <form className="row" onSubmit={onSubmit}>
          {oral && (
            <button type="button" className={`btn micro ${dictee.ecoute ? 'actif' : ''}`} onClick={micro}
              disabled={chargement} aria-label={dictee.ecoute ? 'Arrêter le micro' : 'Répondre au micro'}>
              {dictee.ecoute ? <Square size={16} /> : <Mic size={16} />}
            </button>
          )}
          <input value={saisie} onChange={(e) => setSaisie(e.target.value)}
            placeholder={oral ? 'Clique sur le micro et réponds à voix haute…' : 'Ta réponse ou ta question…'}
            disabled={chargement} />
          <button className="btn primary" disabled={chargement || !saisie.trim()}>Envoyer</button>
        </form>
      )}

      {simulation && reponsesDonnees >= 2 && !chargement && (
        <button type="button" className="btn ghost bilan" onClick={() => repondre(BILAN)}>
          Terminer et obtenir mon bilan
        </button>
      )}
    </div>
  )
}

import { useEffect, useRef, useState } from 'react'

// Voix du navigateur, sans service payant : synthese vocale (speechSynthesis) pour que TAGIA parle,
// reconnaissance vocale (SpeechRecognition, Chrome et Edge) pour dicter ses reponses.

const Reconnaissance = typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition)

export const dicteeDisponible = () => !!Reconnaissance
export const syntheseDisponible = () => typeof window !== 'undefined' && 'speechSynthesis' in window

/** Retire la mise en forme Markdown pour une lecture naturelle. */
export function texteAParler(markdown) {
  return markdown
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/[#*_`>|]/g, ' ')
    .replace(/^\s*[-•]\s+/gm, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/gu, '')
    .replace(/\s+/g, ' ')
    .trim()
}

export function parler(texte, { onFin } = {}) {
  if (!syntheseDisponible()) return
  window.speechSynthesis.cancel()
  const phrase = new SpeechSynthesisUtterance(texteAParler(texte))
  phrase.lang = 'fr-FR'
  phrase.rate = 1.03
  const voix = window.speechSynthesis.getVoices().filter((v) => v.lang?.toLowerCase().startsWith('fr'))
  phrase.voice = voix.find((v) => /google|natural|online/i.test(v.name)) || voix[0] || null
  if (onFin) phrase.onend = onFin
  window.speechSynthesis.speak(phrase)
}

export function arreterVoix() {
  if (syntheseDisponible()) window.speechSynthesis.cancel()
}

/** Dictee en francais : onTexte recoit le texte reconnu (en direct), ajoute a la saisie de depart. */
export function useDictee(onTexte) {
  const [ecoute, setEcoute] = useState(false)
  const [erreur, setErreur] = useState('')
  const reco = useRef(null)
  const rappel = useRef(onTexte)

  useEffect(() => {
    rappel.current = onTexte
  }, [onTexte])

  useEffect(() => () => reco.current?.abort(), [])

  const demarrer = (depart = '') => {
    if (!Reconnaissance) return
    arreterVoix()
    setErreur('')
    const r = new Reconnaissance()
    r.lang = 'fr-FR'
    r.continuous = true
    r.interimResults = true
    r.onresult = (e) => {
      let texte = ''
      for (let i = 0; i < e.results.length; i++) texte += e.results[i][0].transcript
      rappel.current(`${depart}${depart && texte ? ' ' : ''}${texte.trim()}`)
    }
    r.onerror = (e) => {
      if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
        setErreur("Autorise l'accès au micro dans ton navigateur pour répondre à l'oral.")
      } else if (e.error === 'no-speech') {
        setErreur("Je n'ai rien entendu : rapproche-toi du micro et réessaie.")
      } else if (e.error !== 'aborted') {
        setErreur(`Micro indisponible (${e.error}).`)
      }
    }
    r.onend = () => setEcoute(false)
    reco.current = r
    r.start()
    setEcoute(true)
  }

  const arreter = () => {
    reco.current?.stop()
    setEcoute(false)
  }

  return { ecoute, erreur, demarrer, arreter }
}

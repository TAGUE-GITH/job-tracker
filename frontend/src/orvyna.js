/** Ouvre ORVYNA depuis n'importe quel bouton de la page, avec une question facultative. */
export const ouvrirOrvyna = (question) => window.dispatchEvent(new CustomEvent('orvyna:ouvrir', { detail: question }))

export const LANGAGES = [
  ['java', 'Java'], ['python', 'Python'], ['javascript', 'JavaScript'], ['typescript', 'TypeScript'],
  ['sql', 'SQL'], ['csharp', 'C#'], ['php', 'PHP'], ['c', 'C'], ['cpp', 'C++'], ['go', 'Go'],
  ['kotlin', 'Kotlin'], ['swift', 'Swift'], ['html', 'HTML'], ['css', 'CSS'], ['bash', 'Bash'], ['texte', 'Autre'],
]

/** Nom de langage donne par ORVYNA -> code connu de la liste ("Java" -> "java", "c#" -> "csharp"). */
export function normaliserLangage(nom = '') {
  const n = nom.trim().toLowerCase()
  const alias = { 'c#': 'csharp', cs: 'csharp', 'c++': 'cpp', js: 'javascript', ts: 'typescript', py: 'python', sh: 'bash', shell: 'bash' }
  const code = alias[n] ?? n
  return LANGAGES.some(([c]) => c === code) ? code : 'texte'
}

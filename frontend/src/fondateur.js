// Presentation du fondateur et de l'idee de Postulyn (page d'accueil et page "A propos").
// Tout le texte est ici : modifie-le librement pour qu'il te ressemble.

export const FONDATEUR = {
  prenom: 'Franck',
  nom: 'Tague',
  initiales: 'FT',
  role: 'Fondateur et développeur de Postulyn',
  // Photo facultative : mets une adresse d'image (ex. '/franck.jpg' dans frontend/public), sinon les initiales s'affichent
  photo: null,
  parcours: [
    {
      periode: 'Depuis 2025',
      titre: 'Cycle Expert en architecture et développement logiciel',
      lieu: '3IL Ingénieurs, Limoges',
    },
    {
      periode: '2020 - 2025',
      titre: 'Diplôme d’ingénieur en génie informatique et télécommunications',
      lieu: 'École nationale supérieure polytechnique de Douala',
    },
    {
      periode: 'Expériences',
      titre: 'Développeur fullstack en entreprise',
      lieu: 'DITS Group (France) et Yamo Group (Cameroun)',
    },
  ],
  liens: [
    { libelle: 'LinkedIn', url: 'https://www.linkedin.com/in/tague-franck' },
    { libelle: 'GitHub', url: 'https://github.com/TAGUE-GITH' },
    { libelle: 'Portfolio', url: 'https://portfoliofranck-tague.vercel.app' },
  ],
}

/** Courte presentation, reprise sur la page d'accueil. */
export const CITATION = 'J’ai créé Postulyn pour que chaque candidat puisse chercher un emploi avec méthode, '
  + 'sereinement, et avec les mêmes outils que les recruteurs.'

/** L'histoire de l'idee, paragraphe par paragraphe. */
export const HISTOIRE = [
  'Je m’appelle Franck Tague. Je suis ingénieur en génie informatique et je poursuis aujourd’hui un cycle '
    + 'd’expert en architecture logicielle à Limoges. Comme beaucoup d’étudiants et de jeunes diplômés, j’ai '
    + 'cherché des stages et une alternance, en France et à l’étranger.',
  'J’ai vite constaté que chercher un emploi demande une organisation énorme : des dizaines d’annonces, '
    + 'plusieurs versions de CV, des lettres à adapter, des relances à ne pas oublier et des entretiens à '
    + 'préparer. Les outils existants sont dispersés, et les logiciels de tri écartent de bons profils pour '
    + 'une simple question de mise en forme ou de mots-clés.',
  'Postulyn est né de cette expérience. L’idée est simple : réunir en un seul endroit tout ce dont un '
    + 'candidat a besoin, des offres jusqu’à l’entretien, et lui donner une assistante, ORVYNA, qui l’aide '
    + 'à présenter honnêtement le meilleur de son parcours pour chaque offre.',
  'Je conçois et développe Postulyn moi-même, de l’architecture du serveur à l’interface, en écoutant les '
    + 'retours de ceux qui l’utilisent. Chaque avis compte pour faire évoluer l’application.',
]

/** Les principes qui guident Postulyn. */
export const ENGAGEMENTS = [
  {
    titre: 'Gratuit et accessible',
    texte: 'Postulyn est gratuit, sans carte bancaire, sur ordinateur comme sur téléphone, pour que chacun ait les mêmes chances.',
  },
  {
    titre: 'Vos données vous appartiennent',
    texte: 'Vos candidatures et vos documents ne sont ni revendus ni partagés. Vous pouvez les exporter ou les supprimer à tout moment.',
  },
  {
    titre: 'Une IA honnête',
    texte: 'ORVYNA reformule et met en valeur votre parcours, sans jamais inventer d’expérience, de diplôme ni de compétence.',
  },
]

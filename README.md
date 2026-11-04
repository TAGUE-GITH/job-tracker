** Model de Donnees **

user
- id : Long, obligatoire
- nom : String, obligatoire
- email : String, obligatoire, unique
- password : String (hash BCrypt), obligatoire

candidature
- id : Long, obligatoire
- nomEntreprise : String, obligatoire
- poste : String, obligatoire
- lienOffre : String, facultatif
- texteOffre : TEXT, facultatif
- dateCandidature : LocalDate, obligatoire
- statut : StatutCandidature, obligatoire
- user_id : FK -> user, obligatoire
- cv_id : FK -> document, facultatif
- lettre_id : FK -> document, facultatif

etape
- id : Long, obligatoire
- type : TypeEtape, obligatoire
- date : LocalDate, obligatoire
- notes : TEXT, facultatif   (questions posées, ressenti… l'IA s'en servira)
- candidature_id : FK -> candidature, obligatoire

document
- id : Long, obligatoire
- nomFichier : String, obligatoire
- type : TypeDocument (CV, LETTRE), obligatoire
- chemin : String, obligatoire
- dateAjout : LocalDate, obligatoire

enum StatutCandidature : EN_ATTENTE, EN_COURS, OFFRE_RECUE, REFUSEE, ABANDONNEE
enum TypeEtape : CANDIDATURE_ENVOYEE, ECHANGE_RH, TEST_TECHNIQUE,
                 ENTRETIEN_TECHNIQUE, ENTRETIEN_MANAGER, OFFRE, REFUS
enum TypeDocument : CV, LETTRE
-- Documents crees par Postulyn (CV adapte, lettre) : distingues du CV importe par l'utilisateur,
-- qui reste la source de reference quand ORVYNA adapte un CV a une offre
alter table document add column if not exists genere boolean not null default false;

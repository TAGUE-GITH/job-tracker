-- Met a niveau une base creee avant Flyway par Hibernate (ddl-auto=update). Sans effet sur une base
-- creee par V1 : chaque instruction peut etre rejouee sans risque.

-- Coordonnees du profil (ajoutees pour l'en-tete des lettres de motivation)
alter table users add column if not exists telephone varchar(30);
alter table users add column if not exists adresse varchar(300);
alter table users add column if not exists ville varchar(100);

-- Hibernate avait fige la liste des types d'etape (contrainte CHECK sous PostgreSQL, type ENUM sous H2) :
-- le type RELANCE etait refuse. On revient a un simple texte, controle par l'enum Java.
alter table etape drop constraint if exists etape_type_check;
alter table etape alter column type set data type varchar(255);

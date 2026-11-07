-- Date de creation des comptes : sert a supprimer automatiquement les comptes de demonstration.
alter table users add column if not exists date_creation timestamp;

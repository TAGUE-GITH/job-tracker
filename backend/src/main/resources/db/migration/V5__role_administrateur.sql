-- Role administrateur attribue depuis la page Administration (en plus des emails de ADMIN_EMAILS,
-- administrateurs principaux qui ne peuvent pas perdre leurs droits).
alter table users add column if not exists administrateur boolean default false not null;

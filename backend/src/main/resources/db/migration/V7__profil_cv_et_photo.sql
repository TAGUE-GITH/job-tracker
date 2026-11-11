-- Profil extrait du CV par l'IA (JSON) : sert a recommander les offres qui correspondent au candidat
alter table users add column if not exists profil_cv varchar(8000);
alter table users add column if not exists profil_cv_document_id bigint;

-- Photo facultative affichee sur les CV generes
create table photo_profil (
    user_id bigint not null,
    donnees bytea not null,
    content_type varchar(50) not null,
    constraint pk_photo_profil primary key (user_id),
    constraint fk_photo_profil_user foreign key (user_id) references users (id) on delete cascade
);

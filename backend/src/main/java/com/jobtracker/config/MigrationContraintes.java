package com.jobtracker.config;

import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate cree une contrainte CHECK avec la liste des valeurs d'un enum (types d'etape), mais
 * "ddl-auto=update" ne la met jamais a jour : un nouveau type (ex. RELANCE) serait refuse par une base
 * existante. On supprime donc ces contraintes au demarrage ; l'enum Java reste le garde-fou.
 */
@Component
public class MigrationContraintes implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationContraintes.class);

    private final JdbcTemplate jdbc;

    public MigrationContraintes(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String base = jdbc.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
            List<String> contraintes = "PostgreSQL".equals(base)
                    ? jdbc.queryForList("select conname from pg_constraint where conrelid = to_regclass('etape') "
                            + "and contype = 'c' and pg_get_constraintdef(oid) like '%type%'", String.class)
                    : jdbc.queryForList("select constraint_name from information_schema.table_constraints "
                            + "where lower(table_name) = 'etape' and constraint_type = 'CHECK'", String.class);
            for (String nom : contraintes) {
                jdbc.execute("alter table etape drop constraint \"" + nom.replace("\"", "") + "\"");
                LOG.info("Contrainte {} supprimee sur la table etape", nom);
            }
            // H2 (developpement) stocke l'enum dans un vrai type ENUM : on le repasse en texte
            if (!"PostgreSQL".equals(base)) {
                List<String> types = jdbc.queryForList("select data_type from information_schema.columns "
                        + "where lower(table_name) = 'etape' and lower(column_name) = 'type'", String.class);
                if (types.stream().anyMatch("ENUM"::equalsIgnoreCase)) {
                    jdbc.execute("alter table etape alter column \"type\" set data type varchar(40)");
                    LOG.info("Colonne etape.type convertie en varchar");
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Migration des contraintes de la table etape impossible : {}", e.getMessage());
        }
    }
}

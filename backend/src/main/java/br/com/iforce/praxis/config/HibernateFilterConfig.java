package br.com.iforce.praxis.config;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.spi.ValueClassification;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HibernateFilterConfig implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        // Hibernate filters são configurados via anotações nas entidades
        // Este componente serve como ponto de hook para configurações futuras
    }
}

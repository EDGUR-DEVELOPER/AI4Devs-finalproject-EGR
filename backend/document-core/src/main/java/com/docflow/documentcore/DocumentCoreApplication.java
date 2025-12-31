package com.docflow.documentcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Document Core Service Application - Document Management for DocFlow.
 * <p>
 * This is the main entry point for the Document Core microservice.
 * DataSource auto-configuration is excluded until database setup is completed.
 * </p>
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class DocumentCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentCoreApplication.class, args);
    }

}

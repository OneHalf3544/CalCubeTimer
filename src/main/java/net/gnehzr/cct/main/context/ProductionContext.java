package net.gnehzr.cct.main.context;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ConfigurationDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/**
 * Created by OneHalf on 21.06.2016.
 */
@Import(ContextConfiguration.class)
@org.springframework.context.annotation.Configuration
@Profile("production")
public class ProductionContext {

    @Bean
    public Configuration configuration(ConfigurationDao configurationDao) throws IOException {
        return new Configuration(configurationDao);
    }
}

package net.gnehzr.cct.main.context;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ConfigurationDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;

@Import(ContextConfiguration.class)
@org.springframework.context.annotation.Configuration
@Profile("dev")
public class TestContext {

    @Bean
    public Configuration configuration(ConfigurationDao configurationDao) throws IOException {
        return new Configuration(
                new File(Configuration.getRootDirectory() + "\\..\\..\\..\\src\\main\\resources"),
                configurationDao);
    }
}

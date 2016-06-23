package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.context.ContextConfiguration;
import net.gnehzr.cct.main.context.TestContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class MainTest {
    @Test
    public void testCreateInjectionContext() throws Exception {
        System.setProperty("spring.profiles.active", "dev");
        new AnnotationConfigApplicationContext(ContextConfiguration.class);
    }
}
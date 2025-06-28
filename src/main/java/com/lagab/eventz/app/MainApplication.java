package com.lagab.eventz.app;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;

import com.lagab.eventz.app.common.config.CommonProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({ CommonProperties.class })
public class MainApplication {

    private final Environment env;

    /**
     * Initializes App.
     * <p>
     * Spring profiles can be configured with a program arguments --spring.profiles.active=your-active-profile
     */
    @PostConstruct
    public void initApplication() {
        log.info("Running with Spring profile(s) : {}", Arrays.toString(env.getActiveProfiles()));
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MainApplication.class);
        //DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        try {
            log.info("""
                            
                            ╭──────────────────────────────────────────────────────────╮
                            │                                                          │
                               Application '{}' is running!                           
                               Local:      http://localhost:{}                        
                               External:   http://{}:{}                               
                               Profile(s): {}                                         
                            │                                                          │
                            ╰──────────────────────────────────────────────────────────╯
                            """,
                    env.getProperty("spring.application.name"), env.getProperty("server.port", "8080"),
                    InetAddress.getLocalHost().getHostAddress(), env.getProperty("server.port", "8080"),
                    env.getActiveProfiles());
        } catch (UnknownHostException e) {
            log.error("Unable to retrieve localhost address", e);
            System.exit(-1);
        }
    }

}

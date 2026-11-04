package com.tomanagement.user.bootstrap;

import com.tomanagement.user.service.InitialAdminOutcome;
import com.tomanagement.user.service.InitialAdminService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the initial-admin check once the application has started. Logs only the outcome, never the
 * admin's email or password.
 */
@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    private final InitialAdminService initialAdminService;

    public InitialAdminBootstrap(InitialAdminService initialAdminService) {
        this.initialAdminService = initialAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        InitialAdminOutcome outcome = initialAdminService.ensureInitialAdmin();
        LOGGER.atLevel(outcome.getLogLevel()).log(outcome.getLogMessage());
    }
}

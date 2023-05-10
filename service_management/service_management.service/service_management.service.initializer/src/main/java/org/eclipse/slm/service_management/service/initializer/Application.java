package org.eclipse.slm.service_management.service.initializer;

import org.eclipse.slm.common.utils.serviceofferingimport.DTOConfig;
import org.eclipse.slm.service_management.service.client.handler.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;

@SpringBootApplication(
        scanBasePackages = {
                "org.eclipse.slm.service_management.service.initializer",
                "org.eclipse.slm.service_management.service.client"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                SecurityAutoConfiguration.class
        })
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final ServiceOfferingInitializer serviceOfferingInitializer;

    private final ServiceVendorsInitializer serviceVendorsInitializer;

    private final ServiceRepositoriesInitializer serviceRepositoriesInitializer;

    private final ServiceCategoriesInitializer serviceCategoriesInitializer;

    private final GitRepoInitializer gitRepoInitializer;

    @Autowired
    public Application(ServiceOfferingInitializer serviceOfferingInitializer, ServiceVendorsInitializer serviceVendorsInitializer, ServiceRepositoriesInitializer serviceRepositoriesInitializer, ServiceCategoriesInitializer serviceCategoriesInitializer, GitRepoInitializer gitRepoInitializer) {
        this.serviceOfferingInitializer = serviceOfferingInitializer;
        this.serviceVendorsInitializer = serviceVendorsInitializer;
        this.serviceRepositoriesInitializer = serviceRepositoriesInitializer;
        this.serviceCategoriesInitializer = serviceCategoriesInitializer;
        this.gitRepoInitializer = gitRepoInitializer;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() throws IOException, ApiException {
        // Configure Model Mapper
        DTOConfig.configureModelMapper();

        var clonedGitRepoDirectories = this.gitRepoInitializer.cloneGitRepos();
        var initDirectories = new ArrayList<String>();
        initDirectories.addAll(this.serviceVendorsInitializer.getInitDirectories());
        initDirectories.addAll(clonedGitRepoDirectories);

        for (var initDirectory : initDirectories) {
            if (!initDirectory.endsWith("/")) {
                initDirectory += "/";
            }

            LOG.info("Started initialization from directory '" + initDirectory + "'");
            this.serviceVendorsInitializer.init(initDirectory);
            this.serviceCategoriesInitializer.init(initDirectory);
            this.serviceRepositoriesInitializer.init(initDirectory);
            this.serviceOfferingInitializer.init(initDirectory);
            LOG.info("Finished initialization from directory '" + initDirectory + "'");
        }
    }

}

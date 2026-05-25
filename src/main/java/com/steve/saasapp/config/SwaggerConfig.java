package com.steve.saasapp.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI saasAppOpenAPI() {

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");

        Server awsServer = new Server();
        awsServer.setUrl("http://saasapp-env.eba-i334tgjp.us-east-1.elasticbeanstalk.com");
        awsServer.setDescription("AWS Elastic Beanstalk Production Server");

        Contact contact = new Contact();
        contact.setName("Stephen Ekeh");
        contact.setEmail("stevenadibee@yahoo.com");
        contact.setUrl("https://stephen-portfolio-lime.vercel.app/");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0");

        Info info = new Info()
                .title("Multi-Tenant SaaS Project Management API")
                .version("1.0")
                .description("""
                        REST API for a Multi-Tenant SaaS Project Management System API Deployed on AWS ElasticBeanStalk.

                        Features:
                        - Tenant Management
                        - User Authentication with JWT
                        - Project Management
                        - Task Management
                        - Multi-Tenant Isolation
                        - Role-Based Access
                        """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, awsServer))
                .externalDocs(new ExternalDocumentation()
                        .description("Project GitHub Repository")
                        .url("https://github.com/Stephenekeh-dev/saas-project-management-backend"));
    }
}
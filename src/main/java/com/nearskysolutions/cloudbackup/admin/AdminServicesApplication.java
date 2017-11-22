package com.nearskysolutions.cloudbackup.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages="com.nearskysolutions.cloudbackup.admin")
public class AdminServicesApplication extends SpringBootServletInitializer {
	
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AdminServicesApplication.class);
    }
	
	public static void main(String[] args) {
        SpringApplication.run(AdminServicesApplication.class, args);
    }
	
}

package com.dreamgames.backendengineeringcasestudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendEngineeringCaseStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendEngineeringCaseStudyApplication.class, args);

        System.out.println("**********************************************************************");
        System.out.println("************* Server is started. Listening port 8080 ... *************");
        System.out.println("**********************************************************************");
    }
}

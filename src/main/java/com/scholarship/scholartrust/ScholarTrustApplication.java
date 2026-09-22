package com.scholarship.scholartrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScholarTrustApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScholarTrustApplication.class, args);
        System.out.println("\n========================================================");
        System.out.println(">>> ScholarTrust Backend Application Started Successfully! <<<");
        System.out.println("========================================================\n");
    }
}

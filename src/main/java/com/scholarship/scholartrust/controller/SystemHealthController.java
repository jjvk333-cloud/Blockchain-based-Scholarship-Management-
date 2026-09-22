package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class SystemHealthController {

    private final UserRepository userRepository;
    private final ScholarshipRepository scholarshipRepository;
    private final ApplicationRepository applicationRepository;

    public SystemHealthController(UserRepository userRepository,
                                  ScholarshipRepository scholarshipRepository,
                                  ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.scholarshipRepository = scholarshipRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> statusInfo = new LinkedHashMap<>();
        statusInfo.put("status", "UP");
        statusInfo.put("service", "ScholarTrust Backend API");
        statusInfo.put("database", "Connected (MySQL 8.0)");
        statusInfo.put("userCount", userRepository.count());
        statusInfo.put("scholarshipCount", scholarshipRepository.count());
        statusInfo.put("applicationCount", applicationRepository.count());

        return ResponseEntity.ok(ApiResponse.success("ScholarTrust backend foundation is healthy and connected to MySQL!", statusInfo));
    }
}

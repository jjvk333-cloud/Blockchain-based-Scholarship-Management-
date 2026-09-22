package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.dto.ScholarshipRequest;
import com.scholarship.scholartrust.dto.ScholarshipResponse;
import com.scholarship.scholartrust.service.ScholarshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scholarships")
@CrossOrigin(origins = "*")
public class ScholarshipController {

    private final ScholarshipService scholarshipService;

    public ScholarshipController(ScholarshipService scholarshipService) {
        this.scholarshipService = scholarshipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getActiveScholarships() {
        List<ScholarshipResponse> list = scholarshipService.getAllActiveScholarships();
        return ResponseEntity.ok(ApiResponse.success("Active scholarships retrieved", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> getScholarshipById(@PathVariable Long id) {
        ScholarshipResponse response = scholarshipService.getScholarshipById(id);
        return ResponseEntity.ok(ApiResponse.success("Scholarship details retrieved", response));
    }

    @GetMapping({"/{id}/check-eligibility", "/{id}/eligibility"})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> checkEligibility(@PathVariable Long id,
                                                                             Authentication authentication) {
        ScholarshipResponse response = scholarshipService.checkEligibility(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Eligibility checked", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> createScholarship(@Valid @RequestBody ScholarshipRequest request,
                                                                              Authentication authentication) {
        ScholarshipResponse response = scholarshipService.createScholarship(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scholarship created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> updateScholarship(@PathVariable Long id,
                                                                              @Valid @RequestBody ScholarshipRequest request) {
        ScholarshipResponse response = scholarshipService.updateScholarship(id, request);
        return ResponseEntity.ok(ApiResponse.success("Scholarship updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateScholarship(@PathVariable Long id) {
        scholarshipService.deactivateScholarship(id);
        return ResponseEntity.ok(ApiResponse.success("Scholarship deactivated successfully"));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getAllScholarshipsForAdmin() {
        List<ScholarshipResponse> list = scholarshipService.getAllScholarshipsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("All scholarships retrieved for admin", list));
    }
}
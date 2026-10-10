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
@RequestMapping("/api/admin/scholarships")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScholarshipController {

    private final ScholarshipService scholarshipService;

    public AdminScholarshipController(ScholarshipService scholarshipService) {
        this.scholarshipService = scholarshipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScholarshipResponse>>> getAllScholarships() {
        List<ScholarshipResponse> list = scholarshipService.getAllScholarshipsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("All scholarships retrieved for admin", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> getScholarshipById(@PathVariable Long id) {
        ScholarshipResponse response = scholarshipService.getScholarshipById(id);
        return ResponseEntity.ok(ApiResponse.success("Scholarship details retrieved", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScholarshipResponse>> createScholarship(
            @Valid @RequestBody ScholarshipRequest request,
            Authentication authentication) {
        ScholarshipResponse response = scholarshipService.createScholarship(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scholarship created successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScholarshipResponse>> updateScholarship(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipRequest request) {
        ScholarshipResponse response = scholarshipService.updateScholarship(id, request);
        return ResponseEntity.ok(ApiResponse.success("Scholarship updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteScholarship(@PathVariable Long id) {
        scholarshipService.deactivateScholarship(id);
        return ResponseEntity.ok(ApiResponse.success("Scholarship deactivated successfully"));
    }
}

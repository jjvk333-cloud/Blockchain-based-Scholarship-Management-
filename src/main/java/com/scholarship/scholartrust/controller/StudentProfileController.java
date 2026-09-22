package com.scholarship.scholartrust.controller;

import com.scholarship.scholartrust.dto.ApiResponse;
import com.scholarship.scholartrust.dto.StudentProfileResponse;
import com.scholarship.scholartrust.dto.StudentProfileUpdateRequest;
import com.scholarship.scholartrust.service.StudentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/student/profile", "/api/students/profile"})
@CrossOrigin(origins = "*")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getProfile(Authentication authentication) {
        StudentProfileResponse profile = studentProfileService.getProfileByEmail(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Student profile retrieved successfully", profile));
    }

    @PutMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> updateProfile(
            @RequestBody StudentProfileUpdateRequest request,
            Authentication authentication) {
        StudentProfileResponse updated = studentProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Student profile updated successfully", updated));
    }
}

package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.ScholarshipRequest;
import com.scholarship.scholartrust.dto.ScholarshipResponse;
import com.scholarship.scholartrust.entity.Scholarship;
import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.repository.ScholarshipRepository;
import com.scholarship.scholartrust.repository.StudentProfileRepository;
import com.scholarship.scholartrust.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScholarshipService {

    private final ScholarshipRepository scholarshipRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public ScholarshipService(ScholarshipRepository scholarshipRepository,
                              UserRepository userRepository,
                              StudentProfileRepository studentProfileRepository) {
        this.scholarshipRepository = scholarshipRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public ScholarshipResponse createScholarship(ScholarshipRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        if (request.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scholarship deadline must be in the future");
        }

        BigDecimal minGpa = request.getMinGpa() != null ? request.getMinGpa() : BigDecimal.ZERO;
        BigDecimal maxIncome = request.getMaxAnnualIncome() != null ? request.getMaxAnnualIncome() : new BigDecimal("99999999.00");

        Scholarship scholarship = new Scholarship(
                request.getTitle(),
                request.getDescription(),
                minGpa,
                maxIncome,
                request.getGrantAmount(),
                request.getDeadline(),
                admin
        );

        Scholarship saved = scholarshipRepository.save(scholarship);
        return mapToResponse(saved);
    }

    @Transactional
    public ScholarshipResponse updateScholarship(Long id, ScholarshipRequest request) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scholarship not found with ID: " + id));

        scholarship.setTitle(request.getTitle());
        scholarship.setDescription(request.getDescription());
        if (request.getMinGpa() != null) scholarship.setMinGpa(request.getMinGpa());
        if (request.getMaxAnnualIncome() != null) scholarship.setMaxAnnualIncome(request.getMaxAnnualIncome());
        if (request.getGrantAmount() != null) scholarship.setGrantAmount(request.getGrantAmount());
        if (request.getDeadline() != null) scholarship.setDeadline(request.getDeadline());

        Scholarship updated = scholarshipRepository.save(scholarship);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getAllActiveScholarships() {
        LocalDateTime now = LocalDateTime.now();
        return scholarshipRepository.findByActiveTrue().stream()
                .filter(s -> s.getDeadline().isAfter(now))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getAllScholarshipsForAdmin() {
        return scholarshipRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse getScholarshipById(Long id) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scholarship not found with ID: " + id));
        return mapToResponse(scholarship);
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse checkEligibility(Long scholarshipId, String studentEmail) {
        Scholarship scholarship = scholarshipRepository.findById(scholarshipId)
                .orElseThrow(() -> new IllegalArgumentException("Scholarship not found with ID: " + scholarshipId));

        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with email: " + studentEmail));

        StudentProfile profile = studentProfileRepository.findByUser(student)
                .orElseThrow(() -> new IllegalStateException("Student profile not completed"));

        ScholarshipResponse response = mapToResponse(scholarship);
        List<String> reasons = new ArrayList<>();
        boolean isEligible = true;

        if (!scholarship.isActive()) {
            isEligible = false;
            reasons.add("Scholarship is currently inactive.");
        }

        if (scholarship.getDeadline().isBefore(LocalDateTime.now())) {
            isEligible = false;
            reasons.add("Application deadline has expired on " + scholarship.getDeadline());
        }

        if (profile.getGpa().compareTo(scholarship.getMinGpa()) < 0) {
            isEligible = false;
            reasons.add(String.format("Your GPA (%.2f) is below the required minimum (%.2f).",
                    profile.getGpa(), scholarship.getMinGpa()));
        }

        if (profile.getAnnualFamilyIncome().compareTo(scholarship.getMaxAnnualIncome()) > 0) {
            isEligible = false;
            reasons.add(String.format("Your annual family income (₹%.2f) exceeds the maximum ceiling (₹%.2f).",
                    profile.getAnnualFamilyIncome(), scholarship.getMaxAnnualIncome()));
        }

        if (isEligible) {
            reasons.add("You fulfill all academic and income eligibility criteria!");
        }

        response.setEligible(isEligible);
        response.setEligibilityReasons(reasons);
        return response;
    }

    @Transactional
    public void deactivateScholarship(Long id) {
        Scholarship scholarship = scholarshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scholarship not found with ID: " + id));
        scholarship.setActive(false);
        scholarshipRepository.save(scholarship);
    }

    private ScholarshipResponse mapToResponse(Scholarship s) {
        ScholarshipResponse dto = new ScholarshipResponse();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        dto.setMinGpa(s.getMinGpa());
        dto.setMaxAnnualIncome(s.getMaxAnnualIncome());
        dto.setGrantAmount(s.getGrantAmount());
        dto.setDeadline(s.getDeadline());
        dto.setActive(s.isActive());
        dto.setCreatedBy(s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : "Admin");
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }
}
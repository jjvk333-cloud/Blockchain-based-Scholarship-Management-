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
import java.util.Collections;
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

    public static class EligibilityResult {
        private final boolean eligible;
        private final List<String> reasons;

        public EligibilityResult(boolean eligible, List<String> reasons) {
            this.eligible = eligible;
            this.reasons = reasons;
        }

        public boolean isEligible() { return eligible; }
        public List<String> getReasons() { return reasons; }
    }

    public EligibilityResult evaluateEligibility(Scholarship scholarship, StudentProfile profile) {
        List<String> reasons = new ArrayList<>();
        boolean isEligible = true;

        if (scholarship == null) {
            return new EligibilityResult(false, Collections.singletonList("Scholarship does not exist."));
        }

        if (!scholarship.isActive()) {
            isEligible = false;
            reasons.add("Scholarship is currently inactive.");
        }

        if (scholarship.getDeadline() != null && scholarship.getDeadline().isBefore(LocalDateTime.now())) {
            isEligible = false;
            reasons.add("Application deadline expired on " + scholarship.getDeadline());
        }

        if (profile == null) {
            return new EligibilityResult(false, Collections.singletonList("Student profile has not been completed. Please set up your academic profile first."));
        }

        // Academic GPA check
        if (scholarship.getMinGpa() != null) {
            if (profile.getGpa() == null) {
                isEligible = false;
                reasons.add(String.format("GPA is not set in your student profile. Required minimum: %.2f.", scholarship.getMinGpa()));
            } else if (profile.getGpa().compareTo(scholarship.getMinGpa()) < 0) {
                isEligible = false;
                reasons.add(String.format("Your GPA (%.2f) is below the required minimum (%.2f).",
                        profile.getGpa(), scholarship.getMinGpa()));
            }
        }

        // Family Income ceiling check
        if (scholarship.getMaxAnnualIncome() != null) {
            if (profile.getAnnualFamilyIncome() == null) {
                isEligible = false;
                reasons.add(String.format("Annual family income is not set in your profile. Maximum allowed ceiling: ₹%.2f.", scholarship.getMaxAnnualIncome()));
            } else if (profile.getAnnualFamilyIncome().compareTo(scholarship.getMaxAnnualIncome()) > 0) {
                isEligible = false;
                reasons.add(String.format("Your annual family income (₹%.2f) exceeds the maximum ceiling (₹%.2f).",
                        profile.getAnnualFamilyIncome(), scholarship.getMaxAnnualIncome()));
            }
        }

        // Wallet address check
        if (profile.getWalletAddress() == null || !profile.getWalletAddress().matches("^0x[a-fA-F0-9]{40}$")) {
            isEligible = false;
            reasons.add("A valid Ethereum wallet address (0x followed by 40 hex characters) is required in your profile.");
        }

        if (isEligible) {
            reasons.add("You fulfill all academic, income, and identity criteria!");
        }

        return new EligibilityResult(isEligible, reasons);
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
                .orElseThrow(() -> new IllegalStateException("Student profile not completed. Please fill in your profile before checking eligibility."));

        ScholarshipResponse response = mapToResponse(scholarship);
        EligibilityResult result = evaluateEligibility(scholarship, profile);

        response.setEligible(result.isEligible());
        response.setEligibilityReasons(result.getReasons());
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
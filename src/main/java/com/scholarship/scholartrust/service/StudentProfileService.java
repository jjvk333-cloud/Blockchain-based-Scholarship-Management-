package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.StudentProfileResponse;
import com.scholarship.scholartrust.dto.StudentProfileUpdateRequest;
import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.repository.StudentProfileRepository;
import com.scholarship.scholartrust.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public StudentProfileService(UserRepository userRepository,
                                 StudentProfileRepository studentProfileRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + email));

        return mapToResponse(user, profile);
    }

    @Transactional
    public StudentProfileResponse updateProfile(String email, StudentProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        StudentProfile profile = studentProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    StudentProfile p = new StudentProfile();
                    p.setUser(user);
                    return p;
                });

        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getDepartment() != null) {
            profile.setDepartment(request.getDepartment());
        }
        if (request.getGpa() != null) {
            if (request.getGpa().doubleValue() < 0.0 || request.getGpa().doubleValue() > 10.0) {
                throw new IllegalArgumentException("GPA must be between 0.0 and 10.0");
            }
            profile.setGpa(request.getGpa());
        }
        if (request.getAnnualFamilyIncome() != null) {
            if (request.getAnnualFamilyIncome().doubleValue() < 0.0) {
                throw new IllegalArgumentException("Annual family income cannot be negative.");
            }
            profile.setAnnualFamilyIncome(request.getAnnualFamilyIncome());
        }
        if (request.getWalletAddress() != null && !request.getWalletAddress().isBlank()) {
            String trimmedWallet = request.getWalletAddress().trim();
            if (!trimmedWallet.matches("^0x[a-fA-F0-9]{40}$")) {
                throw new IllegalArgumentException("Wallet address must be 0x followed by 40 hex characters.");
            }
            profile.setWalletAddress(trimmedWallet);
        }

        StudentProfile saved = studentProfileRepository.save(profile);
        return mapToResponse(user, saved);
    }

    private StudentProfileResponse mapToResponse(User user, StudentProfile profile) {
        StudentProfileResponse res = new StudentProfileResponse();
        res.setId(profile.getId());
        res.setUserId(user.getId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setRollNumber(profile.getRollNumber());
        res.setDepartment(profile.getDepartment());
        res.setGpa(profile.getGpa());
        res.setAnnualFamilyIncome(profile.getAnnualFamilyIncome());
        res.setWalletAddress(profile.getWalletAddress());
        res.setPhone(profile.getPhone());
        return res;
    }
}

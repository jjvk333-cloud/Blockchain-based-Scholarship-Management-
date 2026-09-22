package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.AuthResponse;
import com.scholarship.scholartrust.dto.LoginRequest;
import com.scholarship.scholartrust.dto.RegisterRequest;
import com.scholarship.scholartrust.entity.Role;
import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.repository.StudentProfileRepository;
import com.scholarship.scholartrust.repository.UserRepository;
import com.scholarship.scholartrust.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       StudentProfileRepository studentProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        // Security Hardening: Prevent privilege escalation. Public registration is strictly for students.
        if (request.getRole() != null && request.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Public registration as ROLE_ADMIN is forbidden. Admin accounts are managed by system administrators.");
        }

        if (request.getRollNumber() != null && !request.getRollNumber().isBlank()
                && studentProfileRepository.existsByRollNumber(request.getRollNumber())) {
            throw new IllegalArgumentException("Roll number is already registered: " + request.getRollNumber());
        }
        if (request.getWalletAddress() != null && !request.getWalletAddress().isBlank()
                && !request.getWalletAddress().matches("^0x[a-fA-F0-9]{40}$")) {
            throw new IllegalArgumentException("Wallet address must be 0x followed by 40 hex characters");
        }

        // Always enforce ROLE_STUDENT for public registration
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                Role.ROLE_STUDENT
        );

        User savedUser = userRepository.save(user);

        StudentProfile profile = new StudentProfile(
                savedUser,
                request.getRollNumber(),
                request.getDepartment(),
                request.getGpa(),
                request.getAnnualFamilyIncome(),
                request.getWalletAddress(),
                request.getPhone()
        );
        studentProfileRepository.save(profile);
        String wallet = profile.getWalletAddress();

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                wallet
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("Account is deactivated. Contact administrator.");
        }

        String wallet = null;
        if (user.getRole() == Role.ROLE_STUDENT) {
            wallet = studentProfileRepository.findByUser(user)
                    .map(StudentProfile::getWalletAddress)
                    .orElse(null);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                wallet
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", user.getId());
        profileData.put("email", user.getEmail());
        profileData.put("fullName", user.getFullName());
        profileData.put("role", user.getRole().name());
        profileData.put("createdAt", user.getCreatedAt());

        if (user.getRole() == Role.ROLE_STUDENT) {
            studentProfileRepository.findByUser(user).ifPresent(profile -> {
                profileData.put("rollNumber", profile.getRollNumber());
                profileData.put("department", profile.getDepartment());
                profileData.put("gpa", profile.getGpa());
                profileData.put("annualFamilyIncome", profile.getAnnualFamilyIncome());
                profileData.put("walletAddress", profile.getWalletAddress());
                profileData.put("phone", profile.getPhone());
            });
        }

        return profileData;
    }
}
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, studentProfileRepository, passwordEncoder, jwtUtil);
    }

    @Test
    @DisplayName("Should register student successfully and enforce ROLE_STUDENT")
    void testRegisterStudentSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("student1@college.edu");
        req.setPassword("Password123");
        req.setFullName("Rohan Gupta");
        req.setRollNumber("CS202601");
        req.setDepartment("Computer Science");
        req.setGpa(new BigDecimal("8.5"));
        req.setAnnualFamilyIncome(new BigDecimal("250000"));
        req.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");

        when(userRepository.existsByEmail("student1@college.edu")).thenReturn(false);
        when(studentProfileRepository.existsByRollNumber("CS202601")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("mock_jwt_token");

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("mock_jwt_token", res.getToken());
        assertEquals("student1@college.edu", res.getEmail());
        assertEquals(Role.ROLE_STUDENT, res.getRole());
        assertEquals("0x70997970C51812dc3A010C7d01b50e0d17dc79C8", res.getWalletAddress());

        verify(userRepository, times(1)).save(argThat(u -> u.getRole() == Role.ROLE_STUDENT));
    }

    @Test
    @DisplayName("Should reject public registration attempting to assign ROLE_ADMIN")
    void testRejectPublicAdminRegistration() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("malicious@college.edu");
        req.setPassword("Secret123");
        req.setFullName("Malicious Attacker");
        req.setRole(Role.ROLE_ADMIN);

        when(userRepository.existsByEmail("malicious@college.edu")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("ROLE_ADMIN is forbidden"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testRejectDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@college.edu");
        req.setPassword("Password123");

        when(userRepository.existsByEmail("existing@college.edu")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("Should reject registration with invalid Ethereum wallet format")
    void testRejectInvalidWalletAddress() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("student2@college.edu");
        req.setPassword("Password123");
        req.setRollNumber("CS202602");
        req.setWalletAddress("invalid_wallet_not_hex");

        when(userRepository.existsByEmail("student2@college.edu")).thenReturn(false);
        when(studentProfileRepository.existsByRollNumber("CS202602")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("Wallet address must be 0x followed by 40 hex characters"));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setEmail("student@college.edu");
        req.setPassword("CorrectPassword");

        User user = new User("student@college.edu", "encoded_hash", "Aarav Sharma", Role.ROLE_STUDENT);
        user.setId(1L);

        StudentProfile profile = new StudentProfile();
        profile.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");

        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword", "encoded_hash")).thenReturn(true);
        when(studentProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(jwtUtil.generateToken(1L, "student@college.edu", "ROLE_STUDENT")).thenReturn("jwt_token_123");

        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("jwt_token_123", res.getToken());
        assertEquals("student@college.edu", res.getEmail());
        assertEquals(Role.ROLE_STUDENT, res.getRole());
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void testLoginInvalidPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("student@college.edu");
        req.setPassword("WrongPassword");

        User user = new User("student@college.edu", "encoded_hash", "Aarav Sharma", Role.ROLE_STUDENT);
        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encoded_hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}

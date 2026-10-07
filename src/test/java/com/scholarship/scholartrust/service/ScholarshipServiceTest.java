package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.ScholarshipRequest;
import com.scholarship.scholartrust.dto.ScholarshipResponse;
import com.scholarship.scholartrust.entity.Role;
import com.scholarship.scholartrust.entity.Scholarship;
import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.repository.ScholarshipRepository;
import com.scholarship.scholartrust.repository.StudentProfileRepository;
import com.scholarship.scholartrust.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScholarshipServiceTest {

    @Mock
    private ScholarshipRepository scholarshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    private ScholarshipService scholarshipService;

    @BeforeEach
    void setUp() {
        scholarshipService = new ScholarshipService(scholarshipRepository, userRepository, studentProfileRepository);
    }

    @Test
    @DisplayName("Should create scholarship with valid parameters")
    void testCreateScholarshipSuccess() {
        User admin = new User("admin@college.edu", "pass", "Dr. Ramesh", Role.ROLE_ADMIN);
        ScholarshipRequest req = new ScholarshipRequest();
        req.setTitle("Merit Scholarship 2026");
        req.setDescription("For high-achieving students");
        req.setMinGpa(new BigDecimal("8.0"));
        req.setMaxAnnualIncome(new BigDecimal("300000"));
        req.setGrantAmount(new BigDecimal("50000"));
        req.setDeadline(LocalDateTime.now().plusMonths(2));

        when(userRepository.findByEmail("admin@college.edu")).thenReturn(Optional.of(admin));
        when(scholarshipRepository.save(any(Scholarship.class))).thenAnswer(invocation -> {
            Scholarship s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        ScholarshipResponse res = scholarshipService.createScholarship(req, "admin@college.edu");

        assertNotNull(res);
        assertEquals("Merit Scholarship 2026", res.getTitle());
        assertEquals(new BigDecimal("50000"), res.getGrantAmount());
        assertTrue(res.isActive());
    }

    @Test
    @DisplayName("Should reject scholarship with deadline in the past")
    void testRejectPastDeadline() {
        User admin = new User("admin@college.edu", "pass", "Dr. Ramesh", Role.ROLE_ADMIN);
        ScholarshipRequest req = new ScholarshipRequest();
        req.setTitle("Past Scholarship");
        req.setDeadline(LocalDateTime.now().minusDays(1));

        when(userRepository.findByEmail("admin@college.edu")).thenReturn(Optional.of(admin));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scholarshipService.createScholarship(req, "admin@college.edu"));
        assertTrue(ex.getMessage().contains("must be in the future"));
    }

    @Test
    @DisplayName("Should return eligible true when student satisfies GPA and income criteria")
    void testCheckEligibilitySuccess() {
        Scholarship scholarship = new Scholarship(
                "Merit Scheme",
                "Description",
                new BigDecimal("8.0"),
                new BigDecimal("300000"),
                new BigDecimal("50000"),
                LocalDateTime.now().plusMonths(1),
                null
        );
        scholarship.setId(1L);
        scholarship.setActive(true);

        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        StudentProfile profile = new StudentProfile();
        profile.setGpa(new BigDecimal("8.5"));
        profile.setAnnualFamilyIncome(new BigDecimal("200000"));
        profile.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");

        when(scholarshipRepository.findById(1L)).thenReturn(Optional.of(scholarship));
        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(profile));

        ScholarshipResponse res = scholarshipService.checkEligibility(1L, "student@college.edu");

        assertNotNull(res);
        assertTrue(res.getEligible());
        assertTrue(res.getEligibilityReasons().stream().anyMatch(r -> r.contains("fulfill all academic")));
    }

    @Test
    @DisplayName("Should return eligible false when student GPA is below required minimum")
    void testCheckEligibilityLowGpa() {
        Scholarship scholarship = new Scholarship(
                "Merit Scheme",
                "Description",
                new BigDecimal("8.5"),
                new BigDecimal("300000"),
                new BigDecimal("50000"),
                LocalDateTime.now().plusMonths(1),
                null
        );
        scholarship.setId(1L);
        scholarship.setActive(true);

        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        StudentProfile profile = new StudentProfile();
        profile.setGpa(new BigDecimal("7.2")); // Below 8.5
        profile.setAnnualFamilyIncome(new BigDecimal("200000"));

        when(scholarshipRepository.findById(1L)).thenReturn(Optional.of(scholarship));
        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(profile));

        ScholarshipResponse res = scholarshipService.checkEligibility(1L, "student@college.edu");

        assertNotNull(res);
        assertFalse(res.getEligible());
        assertTrue(res.getEligibilityReasons().stream().anyMatch(r -> r.contains("below the required minimum")));
    }

    @Test
    @DisplayName("Should return eligible false when family income exceeds ceiling")
    void testCheckEligibilityHighIncome() {
        Scholarship scholarship = new Scholarship(
                "Need Scheme",
                "Description",
                new BigDecimal("6.0"),
                new BigDecimal("250000"),
                new BigDecimal("30000"),
                LocalDateTime.now().plusMonths(1),
                null
        );
        scholarship.setId(1L);
        scholarship.setActive(true);

        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        StudentProfile profile = new StudentProfile();
        profile.setGpa(new BigDecimal("7.5"));
        profile.setAnnualFamilyIncome(new BigDecimal("400000")); // Exceeds 250000

        when(scholarshipRepository.findById(1L)).thenReturn(Optional.of(scholarship));
        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(profile));

        ScholarshipResponse res = scholarshipService.checkEligibility(1L, "student@college.edu");

        assertNotNull(res);
        assertFalse(res.getEligible());
        assertTrue(res.getEligibilityReasons().stream().anyMatch(r -> r.contains("exceeds the maximum ceiling")));
    }
}

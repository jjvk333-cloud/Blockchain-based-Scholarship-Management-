package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.ApplicationResponse;
import com.scholarship.scholartrust.dto.DisbursementReceiptResponse;
import com.scholarship.scholartrust.dto.HashVerificationResponse;
import com.scholarship.scholartrust.entity.*;
import com.scholarship.scholartrust.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ScholarshipRepository scholarshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DisbursementRepository disbursementRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private BlockchainService blockchainService;

    @Mock
    private ScholarshipService scholarshipService;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepository,
                scholarshipRepository,
                userRepository,
                studentProfileRepository,
                documentRepository,
                disbursementRepository,
                fileStorageService,
                blockchainService,
                scholarshipService
        );
    }

    @Test
    @DisplayName("Should reject duplicate application by the same student for the same scholarship")
    void testRejectDuplicateApplication() {
        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit", "Desc", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, LocalDateTime.now().plusDays(10), null);
        scholarship.setId(1L);

        MockMultipartFile marksheet = new MockMultipartFile("marksheet", "marks.pdf", "application/pdf", "data".getBytes());

        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(scholarshipRepository.findById(1L)).thenReturn(Optional.of(scholarship));
        when(applicationRepository.existsByStudentAndScholarship(student, scholarship)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> applicationService.submitApplication(1L, marksheet, null, null, "student@college.edu"));
        assertTrue(ex.getMessage().contains("already applied"));
    }

    @Test
    @DisplayName("Should prevent IDOR when non-admin accesses another student's document")
    void testGetDocumentEntityIdorProtection() {
        User studentOwner = new User("owner@college.edu", "pass", "Owner", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit", "Desc", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, LocalDateTime.now().plusDays(10), null);
        Application app = new Application(studentOwner, scholarship);

        Document doc = new Document(DocumentType.MARKSHEET, "marks.pdf", "uploads/documents/marks.pdf", 100L, "hash123");
        doc.setApplication(app);
        doc.setId(10L);

        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        // Attacker attempts to download owner's document
        assertThrows(AccessDeniedException.class,
                () -> applicationService.getDocumentEntity(10L, "attacker@college.edu", false));

        // Owner can access
        assertDoesNotThrow(() -> applicationService.getDocumentEntity(10L, "owner@college.edu", false));

        // Admin can access
        assertDoesNotThrow(() -> applicationService.getDocumentEntity(10L, "attacker@college.edu", true));
    }

    @Test
    @DisplayName("Should prevent IDOR when non-admin verifies another student's application hash")
    void testVerifyApplicationHashIdorProtection() {
        User studentOwner = new User("owner@college.edu", "pass", "Owner", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit", "Desc", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, LocalDateTime.now().plusDays(10), null);
        Application app = new Application(studentOwner, scholarship);
        app.setId(5L);

        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));

        assertThrows(AccessDeniedException.class,
                () -> applicationService.verifyApplicationDocumentIntegrity(5L, "attacker@college.edu", false));
    }

    @Test
    @DisplayName("Should reject disbursement if student does not have a valid Ethereum wallet address")
    void testDisburseScholarshipFailsWithoutWallet() {
        User student = new User("student@college.edu", "pass", "Aarav", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit", "Desc", BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("25000"), LocalDateTime.now().plusDays(10), null);
        Application app = new Application(student, scholarship);
        app.setId(1L);
        app.setStatus(ApplicationStatus.APPROVED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(new StudentProfile())); // Empty wallet

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> applicationService.disburseScholarship(1L, null, null));
        assertTrue(ex.getMessage().contains("does not have a registered valid Ethereum wallet address"));
    }

    @Test
    @DisplayName("Should maintain consistency: never mark DISBURSED if blockchain transaction fails")
    void testDisburseScholarshipFailsOnChainDoesNotMarkDisbursed() {
        User student = new User("student@college.edu", "pass", "Aarav", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit", "Desc", BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("25000"), LocalDateTime.now().plusDays(10), null);
        Application app = new Application(student, scholarship);
        app.setId(1L);
        app.setStatus(ApplicationStatus.APPROVED);

        StudentProfile profile = new StudentProfile();
        profile.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(profile));
        when(blockchainService.disburseScholarshipOnChain(anyLong(), anyString(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Ganache node connection timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.disburseScholarship(1L, null, null));
        assertTrue(ex.getMessage().contains("Blockchain disbursement failed"));
        assertTrue(ex.getMessage().contains("remains APPROVED and was NOT marked as DISBURSED"));

        // Application status must remain APPROVED
        assertEquals(ApplicationStatus.APPROVED, app.getStatus());
        verify(disbursementRepository, never()).save(any(Disbursement.class));
    }

    @Test
    @DisplayName("Should generate valid receipt when application is disbursed")
    void testGetDisbursementReceiptSuccess() {
        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit 2026", "Desc", BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("50000"), LocalDateTime.now().plusDays(10), null);
        Application app = new Application(student, scholarship);
        app.setId(2L);
        app.setStatus(ApplicationStatus.DISBURSED);

        Disbursement disb = new Disbursement(app, "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
                new BigDecimal("50000"), "0xabcdef1234567890", 42L);

        when(applicationRepository.findById(2L)).thenReturn(Optional.of(app));
        when(disbursementRepository.findByApplication(app)).thenReturn(Optional.of(disb));
        when(blockchainService.getContractAddress()).thenReturn("0xD833215cBcc3f914bD1C9ece3EE7BF8B14f841bb");

        DisbursementReceiptResponse receipt = applicationService.getDisbursementReceipt(2L, "student@college.edu", false);

        assertNotNull(receipt);
        assertEquals(2L, receipt.getApplicationId());
        assertEquals("Aarav Sharma", receipt.getStudentName());
        assertEquals(new BigDecimal("50000"), receipt.getDisbursedAmount());
        assertEquals("0xabcdef1234567890", receipt.getBlockchainTxHash());
        assertEquals(42L, receipt.getBlockNumber());
    }
    @Test
    @DisplayName("Should create application snapshot and store personal statement successfully")
    void testSubmitApplicationCreatesSnapshotAndStoresStatement() {
        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        student.setId(10L);
        Scholarship scholarship = new Scholarship("Merit", "Desc", new BigDecimal("8.0"), new BigDecimal("300000"), new BigDecimal("50000"), LocalDateTime.now().plusDays(10), null);
        scholarship.setId(1L);

        StudentProfile profile = new StudentProfile();
        profile.setRollNumber("2026CS101");
        profile.setDepartment("Computer Science");
        profile.setGpa(new BigDecimal("8.5"));
        profile.setAnnualFamilyIncome(new BigDecimal("200000"));
        profile.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");

        MockMultipartFile marksheet = new MockMultipartFile("marksheet", "marks.pdf", "application/pdf", "data".getBytes());

        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(scholarshipRepository.findById(1L)).thenReturn(Optional.of(scholarship));
        when(applicationRepository.existsByStudentAndScholarship(student, scholarship)).thenReturn(false);
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(profile));
        when(scholarshipService.evaluateEligibility(scholarship, profile))
                .thenReturn(new ScholarshipService.EligibilityResult(true, Collections.singletonList("Eligible")));
        when(fileStorageService.calculateSha256(any(org.springframework.web.multipart.MultipartFile.class))).thenReturn("sha256abc");
        when(fileStorageService.storeFile(any(org.springframework.web.multipart.MultipartFile.class), any(), any())).thenReturn("uploads/marks.pdf");
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application app = invocation.getArgument(0);
            app.setId(100L);
            return app;
        });
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse res = applicationService.submitApplication(
                1L, marksheet, null, null, "student@college.edu", "My research interest statement"
        );

        assertNotNull(res);
        assertEquals("My research interest statement", res.getPersonalStatement());
        assertEquals("2026CS101", res.getSubmittedRollNumber());
        assertEquals("Computer Science", res.getSubmittedDepartment());
        assertEquals(new BigDecimal("8.5"), res.getSubmittedGpa());
        assertEquals(new BigDecimal("200000"), res.getSubmittedAnnualIncome());
        assertEquals("0x70997970C51812dc3A010C7d01b50e0d17dc79C8", res.getSubmittedWalletAddress());
    }

    @Test
    @DisplayName("Should reject application when student is ineligible according to unified eligibility evaluator")
    void testSubmitApplicationIneligibleRejected() {
        User student = new User("student@college.edu", "pass", "Aarav Sharma", Role.ROLE_STUDENT);
        Scholarship scholarship = new Scholarship("Merit", "Desc", new BigDecimal("8.0"), new BigDecimal("300000"), new BigDecimal("50000"), LocalDateTime.now().plusDays(10), null);
        scholarship.setId(1L);

        StudentProfile profile = new StudentProfile();
        profile.setRollNumber("2026CS101");
        profile.setDepartment("Computer Science");
        profile.setGpa(new BigDecimal("7.0")); // below min 8.0
        profile.setAnnualFamilyIncome(new BigDecimal("200000"));
        profile.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");

        MockMultipartFile marksheet = new MockMultipartFile("marksheet", "marks.pdf", "application/pdf", "data".getBytes());

        when(userRepository.findByEmail("student@college.edu")).thenReturn(Optional.of(student));
        when(scholarshipRepository.findById(1L)).thenReturn(Optional.of(scholarship));
        when(applicationRepository.existsByStudentAndScholarship(student, scholarship)).thenReturn(false);
        when(studentProfileRepository.findByUser(student)).thenReturn(Optional.of(profile));
        when(scholarshipService.evaluateEligibility(scholarship, profile))
                .thenReturn(new ScholarshipService.EligibilityResult(false, Collections.singletonList("GPA below minimum")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitApplication(1L, marksheet, null, null, "student@college.edu", null));
        assertTrue(ex.getMessage().contains("Eligibility requirements not met"));
    }
}
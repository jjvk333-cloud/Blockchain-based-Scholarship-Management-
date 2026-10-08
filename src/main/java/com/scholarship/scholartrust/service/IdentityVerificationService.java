package com.scholarship.scholartrust.service;

import com.scholarship.scholartrust.dto.IdentityVerificationResponse;
import com.scholarship.scholartrust.entity.StudentIdentityVerification;
import com.scholarship.scholartrust.entity.StudentProfile;
import com.scholarship.scholartrust.entity.User;
import com.scholarship.scholartrust.entity.VerificationStatus;
import com.scholarship.scholartrust.repository.StudentIdentityVerificationRepository;
import com.scholarship.scholartrust.repository.StudentProfileRepository;
import com.scholarship.scholartrust.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IdentityVerificationService {

    private final StudentIdentityVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FileStorageService fileStorageService;

    public IdentityVerificationService(StudentIdentityVerificationRepository verificationRepository,
                                       UserRepository userRepository,
                                       StudentProfileRepository studentProfileRepository,
                                       FileStorageService fileStorageService) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public IdentityVerificationResponse getVerificationStatus(String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + studentEmail));

        StudentIdentityVerification verification = verificationRepository.findByUser(user)
                .orElseGet(() -> {
                    StudentIdentityVerification v = new StudentIdentityVerification(user);
                    return verificationRepository.save(v);
                });

        return mapToResponse(verification);
    }

    @Transactional
    public IdentityVerificationResponse submitIdentityDocuments(String studentEmail,
                                                                String institutionalIdNumber,
                                                                MultipartFile idCardFile) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + studentEmail));

        if (idCardFile == null || idCardFile.isEmpty()) {
            throw new IllegalArgumentException("Institutional ID card file is required.");
        }

        if (institutionalIdNumber == null || institutionalIdNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Institutional ID / Roll Number is required.");
        }

        String hash = fileStorageService.calculateSha256(idCardFile);
        String storedPath = fileStorageService.storeFile(idCardFile);

        StudentIdentityVerification verification = verificationRepository.findByUser(user)
                .orElseGet(() -> new StudentIdentityVerification(user));

        verification.setInstitutionalIdNumber(institutionalIdNumber.trim());
        verification.setIdCardFilePath(storedPath);
        verification.setIdCardSha256(hash);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setRejectionReason(null);
        verification.setVerifiedAt(null);
        verification.setVerifiedByEmail(null);

        // Keep StudentProfile roll number synced if not already present
        studentProfileRepository.findByUser(user).ifPresent(p -> {
            if (p.getRollNumber() == null || p.getRollNumber().isBlank()) {
                p.setRollNumber(institutionalIdNumber.trim());
                studentProfileRepository.save(p);
            }
        });

        StudentIdentityVerification saved = verificationRepository.save(verification);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IdentityVerificationResponse> getAllPendingVerifications() {
        return verificationRepository.findByStatus(VerificationStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IdentityVerificationResponse> getAllVerifications() {
        return verificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IdentityVerificationResponse reviewVerification(Long verificationId,
                                                          boolean approve,
                                                          String rejectionReason,
                                                          String reviewerEmail) {
        StudentIdentityVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found: " + verificationId));

        if (approve) {
            verification.setStatus(VerificationStatus.VERIFIED);
            verification.setRejectionReason(null);
        } else {
            verification.setStatus(VerificationStatus.REJECTED);
            verification.setRejectionReason(rejectionReason != null ? rejectionReason.trim() : "Identity document rejected by administrator.");
        }

        verification.setVerifiedByEmail(reviewerEmail);
        verification.setVerifiedAt(LocalDateTime.now());

        StudentIdentityVerification saved = verificationRepository.save(verification);
        return mapToResponse(saved);
    }

    public boolean isStudentVerified(User user) {
        return verificationRepository.findByUser(user)
                .map(v -> v.getStatus() == VerificationStatus.VERIFIED)
                .orElse(false);
    }

    private IdentityVerificationResponse mapToResponse(StudentIdentityVerification v) {
        IdentityVerificationResponse res = new IdentityVerificationResponse();
        res.setId(v.getId());
        res.setUserId(v.getUser().getId());
        res.setStudentName(v.getUser().getFullName());
        res.setStudentEmail(v.getUser().getEmail());
        res.setInstitutionalIdNumber(v.getInstitutionalIdNumber());
        res.setIdCardFilePath(v.getIdCardFilePath());
        res.setIdCardSha256(v.getIdCardSha256());
        res.setStatus(v.getStatus());
        res.setVerifiedByEmail(v.getVerifiedByEmail());
        res.setVerifiedAt(v.getVerifiedAt());
        res.setRejectionReason(v.getRejectionReason());
        res.setCreatedAt(v.getCreatedAt());
        return res;
    }
}

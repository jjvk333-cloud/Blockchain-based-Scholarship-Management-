-- ============================================================================
-- ScholarTrust: Initial Seed Data for Demo & Evaluation (seed.sql)
-- Default passwords:
--   Admin  (admin@college.edu):   Admin@123   (BCrypt hash)
--   Student(student@college.edu): Password@123 (BCrypt hash)
-- ============================================================================

USE `scholartrust_db`;

-- 1. Seed Administrator and Test Student
INSERT INTO `users` (`id`, `email`, `password`, `full_name`, `role`, `created_at`) VALUES
(1, 'admin@college.edu', '$2a$10$7v27n83d166yZqZ5nLg5UuM.B2Ue7cfl1U3k0tEsh1UheL.K6Vj0y', 'Dr. S. K. Sharma (Dean)', 'ROLE_ADMIN', NOW()),
(2, 'student@college.edu', '$2a$10$97sD9zIeJ5vCkmvH1b9O0eH1kL2.V2e7cfl1U3k0tEsh1UheL.K6Vj0y', 'Aarav Sharma', 'ROLE_STUDENT', NOW())
ON DUPLICATE KEY UPDATE `email` = `email`;

-- 2. Seed Student Profile for Test Student
INSERT INTO `student_profiles` (`id`, `user_id`, `roll_number`, `department`, `gpa`, `annual_family_income`, `wallet_address`, `phone`) VALUES
(1, 2, '2026CS101', 'Computer Science and Engineering', 8.90, 180000.00, '0x70997970C51812dc3A010C7d01b50e0d17dc79C8', '9876543210')
ON DUPLICATE KEY UPDATE `roll_number` = `roll_number`;

-- 3. Seed Verified Identity for Default Student
INSERT INTO `student_identity_verifications` (`id`, `user_id`, `institutional_id_number`, `id_card_file_path`, `id_card_sha256`, `status`, `verified_by_email`, `verified_at`, `created_at`) VALUES
(1, 2, '2026CS101', 'uploads/documents/default_id_card.pdf', 'ccd4ada9899fb66011fbc5b444b5477cfbb7b1ef833ccc5ebdacd0e4835b3f94', 'VERIFIED', 'admin@college.edu', NOW(), NOW())
ON DUPLICATE KEY UPDATE `institutional_id_number` = `institutional_id_number`;

-- 4. Seed Active Scholarships
INSERT INTO `scholarships` (`id`, `title`, `description`, `min_gpa`, `max_annual_income`, `grant_amount`, `deadline`, `active`, `created_at`) VALUES
(1, 'National Merit Academic Scholarship 2026', 'Merit-based scholarship awarded to students demonstrating superior academic distinction in engineering and computer science disciplines.', 8.00, 300000.00, 50000.00, DATE_ADD(NOW(), INTERVAL 90 DAY), TRUE, NOW()),
(2, 'Women in STEM Excellence Grant', 'Special institutional grant empowering female scholars pursuing degrees in STEM fields with demonstrated academic dedication.', 7.50, 450000.00, 40000.00, DATE_ADD(NOW(), INTERVAL 60 DAY), TRUE, NOW()),
(3, 'Need-Based Financial Assistance Scholarship', 'Full assistance grant designated for scholars from economically backward households with consistent academic standing.', 6.50, 200000.00, 30000.00, DATE_ADD(NOW(), INTERVAL 120 DAY), TRUE, NOW()),
(4, 'Postgraduate Research Fellowship', 'Competitive research fellowship covering laboratory expenses, computational equipment, and research conference grants.', 8.50, 600000.00, 75000.00, DATE_ADD(NOW(), INTERVAL 45 DAY), TRUE, NOW())
ON DUPLICATE KEY UPDATE `title` = `title`;

-- ============================================================================
-- ScholarTrust: Clean Database Schema DDL (MySQL 8.0+)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `scholartrust_db` 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE `scholartrust_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Student Profiles Table
DROP TABLE IF EXISTS `student_profiles`;
CREATE TABLE `student_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `roll_number` VARCHAR(50) UNIQUE,
    `department` VARCHAR(100),
    `gpa` DECIMAL(3,2),
    `annual_family_income` DECIMAL(12,2),
    `wallet_address` VARCHAR(42),
    `phone` VARCHAR(20),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_student_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Student Identity Verifications Table (IDENT-01)
DROP TABLE IF EXISTS `student_identity_verifications`;
CREATE TABLE `student_identity_verifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `institutional_id_number` VARCHAR(50),
    `id_card_file_path` VARCHAR(500),
    `id_card_sha256` VARCHAR(64),
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    `verified_by_email` VARCHAR(100),
    `verified_at` DATETIME,
    `rejection_reason` VARCHAR(500),
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_identity_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Scholarships Table
DROP TABLE IF EXISTS `scholarships`;
CREATE TABLE `scholarships` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `min_gpa` DECIMAL(3,2),
    `max_annual_income` DECIMAL(12,2),
    `grant_amount` DECIMAL(12,2) NOT NULL,
    `deadline` DATETIME NOT NULL,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Applications Table (with snapshot & statement)
DROP TABLE IF EXISTS `applications`;
CREATE TABLE `applications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `student_id` BIGINT NOT NULL,
    `scholarship_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `applied_at` DATETIME NOT NULL,
    `blockchain_tx_hash` VARCHAR(66),
    `admin_remarks` VARCHAR(500),
    `personal_statement` TEXT,
    `submitted_roll_number` VARCHAR(50),
    `submitted_department` VARCHAR(100),
    `submitted_gpa` DECIMAL(3,2),
    `submitted_annual_income` DECIMAL(12,2),
    `submitted_wallet_address` VARCHAR(42),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_application_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_application_scholarship` FOREIGN KEY (`scholarship_id`) REFERENCES `scholarships` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Documents Table
DROP TABLE IF EXISTS `documents`;
CREATE TABLE `documents` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT NOT NULL,
    `document_type` VARCHAR(30) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `file_size_bytes` BIGINT,
    `sha256_hash` VARCHAR(64) NOT NULL,
    `uploaded_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_document_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Disbursements Table
DROP TABLE IF EXISTS `disbursements`;
CREATE TABLE `disbursements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT NOT NULL UNIQUE,
    `recipient_wallet` VARCHAR(42) NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `transaction_hash` VARCHAR(66) NOT NULL,
    `block_number` BIGINT,
    `disbursed_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_disbursement_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Audit Events Table (AUDIT-01)
DROP TABLE IF EXISTS `audit_events`;
CREATE TABLE `audit_events` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `application_id` BIGINT,
    `action` VARCHAR(50) NOT NULL,
    `actor_email` VARCHAR(100),
    `actor_role` VARCHAR(30),
    `old_value` VARCHAR(100),
    `new_value` VARCHAR(100),
    `blockchain_tx_hash` VARCHAR(66),
    `details` VARCHAR(1000),
    `timestamp` DATETIME NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Blockchain Transactions Table (CHAIN-06)
DROP TABLE IF EXISTS `blockchain_transactions`;
CREATE TABLE `blockchain_transactions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tx_hash` VARCHAR(66) UNIQUE,
    `action_type` VARCHAR(50) NOT NULL,
    `application_id` BIGINT,
    `block_number` BIGINT,
    `gas_used` BIGINT,
    `execution_latency_ms` BIGINT,
    `from_address` VARCHAR(42),
    `to_contract_address` VARCHAR(42),
    `status` VARCHAR(20),
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

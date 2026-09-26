/*
Database Script Instructions

Execute this script directly in the database.

The script will:

Create a user with the username admin
Set the password as 12345678
Insert 10 sample records into the audit_logs table.
*/


CREATE DATABASE  IF NOT EXISTS `auditvaultdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `auditvaultdb`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: auditvaultdb
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `is_archived` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `event_timestamp` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` varchar(50) DEFAULT NULL,
  `content_hash` varchar(64) NOT NULL,
  `previous_hash` varchar(64) NOT NULL,
  `actor_id` varchar(100) NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `resource_type` varchar(100) NOT NULL,
  `payload` text NOT NULL,
  `redaction_metadata` text,
  `resource_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKne0lhbi4hd8pvk77euwmo94et` (`content_hash`),
  KEY `idx_actor_id` (`actor_id`),
  KEY `idx_resource_type_id` (`resource_type`,`resource_id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_timestamp` (`event_timestamp`),
  KEY `idx_is_archived` (`is_archived`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (_binary '\0','2026-09-26 10:00:00.000000','2026-09-26 10:00:00.000000',1,'ACTIVE','e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855','0000000000000000000000000000000000000000000000000000000000000000','admin','LOGIN','user','{\"ipAddress\": \"192.168.1.100\", \"userAgent\": \"Mozilla/5.0\", \"success\": true}',NULL,'USR-001'),(_binary '\0','2026-09-26 10:15:00.000000','2026-09-26 10:15:00.000000',2,'ACTIVE','2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae','e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855','john.doe','CREATE','invoice','{\"orderId\": \"A-42\", \"amount\": 125.50, \"currency\": \"USD\", \"customer\": \"ACME Corp\"}',NULL,'INV-1001'),(_binary '\0','2026-09-26 10:30:00.000000','2026-09-26 10:30:00.000000',3,'ACTIVE','fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9','2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae','jane.smith','UPDATE','document','{\"changes\": [\"title\", \"content\"], \"previousVersion\": 1, \"newVersion\": 2}',NULL,'DOC-2026-001'),(_binary '\0','2026-09-26 10:45:00.000000','2026-09-26 10:45:00.000000',4,'ACTIVE','b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9','fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9','system','PAYMENT','payment','{\"amount\": 125.50, \"method\": \"credit_card\", \"status\": \"completed\", \"transactionId\": \"TXN-789456\"}',NULL,'PAY-5001'),(_binary '\0','2026-09-26 11:00:00.000000','2026-09-26 11:00:00.000000',5,'ACTIVE','5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9','b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9','admin','LOGOUT','user','{\"sessionId\": \"sess-abc123\", \"duration\": 3600}',NULL,'USR-001'),(_binary '\0','2026-09-26 11:15:00.000000','2026-09-26 11:15:00.000000',6,'ACTIVE','6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b','5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9','analyst','EXPORT','report','{\"format\": \"CSV\", \"recordCount\": 1500, \"filters\": {\"dateRange\": \"2026-07-01 to 2026-09-30\"}}',NULL,'RPT-Q3-2026'),(_binary '\0','2026-09-26 11:30:00.000000','2026-09-26 11:30:00.000000',7,'ACTIVE','d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35','6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b','admin','PERMISSION_CHANGE','role','{\"action\": \"grant\", \"permission\": \"audit:read\", \"targetUser\": \"john.doe\"}',NULL,'ROLE-MANAGER'),(_binary '\0','2026-09-26 11:45:00.000000','2026-09-26 11:45:00.000000',8,'ACTIVE','4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce','d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35','jane.smith','DELETE','document','{\"reason\": \"retention_policy\", \"deletedBy\": \"jane.smith\", \"backup\": true}',NULL,'DOC-2025-050'),(_binary '\0','2026-09-26 12:00:00.000000','2026-09-26 12:00:00.000000',9,'ACTIVE','7902699be42c8a8e46fbbb4501726816b9054bd3ef6a4b7b8a7a2b4b4b4b4b4b','4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce','admin','CONFIG_CHANGE','system','{\"setting\": \"retentionPeriod\", \"oldValue\": \"365\", \"newValue\": \"730\", \"unit\": \"days\"}',NULL,'CFG-AUDIT-001'),(_binary '\0','2026-09-26 12:15:00.000000','2026-09-26 12:15:00.000000',10,'ACTIVE','2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824','7902699be42c8a8e46fbbb4501726816b9054bd3ef6a4b7b8a7a2b4b4b4b4b4b','unknown','AUTH_FAILED','user','{\"reason\": \"invalid_credentials\", \"ipAddress\": \"192.168.1.200\", \"attempts\": 3}',NULL,'USR-999');
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'ADMIN'),(2,'USER');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `role_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`user_id`),
  KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `enabled` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (_binary '',1,'admin','admin@auditvault.com','$2a$10$GoE9ZbIHMzIdGuFAcSpQ4exxnBtLU7xOhl8sbgLp7qtjDCmHPBW7q');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-26 15:27:51

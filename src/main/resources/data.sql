-- Initialize roles and admin user
INSERT IGNORE INTO roles (id, name) VALUES (1, 'ADMIN'), (2, 'USER');

INSERT IGNORE INTO users (id, username, email, password, enabled) VALUES
(1, 'admin', 'admin@auditvault.com', '$2a$10$GoE9ZbIHMzIdGuFAcSpQ4exxnBtLU7xOhl8sbgLp7qtjDCmHPBW7q', true);

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (1, 1);

-- Sample audit log records for testing the audit create API
-- These records demonstrate various event types and scenarios

-- Record 1: User login event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'LOGIN',
    'admin',
    'user',
    'USR-001',
    '{"ipAddress": "192.168.1.100", "userAgent": "Mozilla/5.0", "success": true}',
    '2026-09-26 10:00:00',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    '0000000000000000000000000000000000000000000000000000000000000000',
    '2026-09-26 10:00:00',
    false,
    'ACTIVE'
);

-- Record 2: Invoice creation event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'CREATE',
    'john.doe',
    'invoice',
    'INV-1001',
    '{"orderId": "A-42", "amount": 125.50, "currency": "USD", "customer": "ACME Corp"}',
    '2026-09-26 10:15:00',
    '2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    '2026-09-26 10:15:00',
    false,
    'ACTIVE'
);

-- Record 3: Document update event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'UPDATE',
    'jane.smith',
    'document',
    'DOC-2026-001',
    '{"changes": ["title", "content"], "previousVersion": 1, "newVersion": 2}',
    '2026-09-26 10:30:00',
    'fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9',
    '2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae',
    '2026-09-26 10:30:00',
    false,
    'ACTIVE'
);

-- Record 4: Payment processed event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'PAYMENT',
    'system',
    'payment',
    'PAY-5001',
    '{"amount": 125.50, "method": "credit_card", "status": "completed", "transactionId": "TXN-789456"}',
    '2026-09-26 10:45:00',
    'b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9',
    'fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9',
    '2026-09-26 10:45:00',
    false,
    'ACTIVE'
);

-- Record 5: User logout event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'LOGOUT',
    'admin',
    'user',
    'USR-001',
    '{"sessionId": "sess-abc123", "duration": 3600}',
    '2026-09-26 11:00:00',
    '5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9',
    'b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9',
    '2026-09-26 11:00:00',
    false,
    'ACTIVE'
);

-- Record 6: Data export event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'EXPORT',
    'analyst',
    'report',
    'RPT-Q3-2026',
    '{"format": "CSV", "recordCount": 1500, "filters": {"dateRange": "2026-07-01 to 2026-09-30"}}',
    '2026-09-26 11:15:00',
    '6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b',
    '5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9',
    '2026-09-26 11:15:00',
    false,
    'ACTIVE'
);

-- Record 7: Permission change event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'PERMISSION_CHANGE',
    'admin',
    'role',
    'ROLE-MANAGER',
    '{"action": "grant", "permission": "audit:read", "targetUser": "john.doe"}',
    '2026-09-26 11:30:00',
    'd4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35',
    '6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b',
    '2026-09-26 11:30:00',
    false,
    'ACTIVE'
);

-- Record 8: Record deletion event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'DELETE',
    'jane.smith',
    'document',
    'DOC-2025-050',
    '{"reason": "retention_policy", "deletedBy": "jane.smith", "backup": true}',
    '2026-09-26 11:45:00',
    '4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce',
    'd4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35',
    '2026-09-26 11:45:00',
    false,
    'ACTIVE'
);

-- Record 9: Configuration change event
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'CONFIG_CHANGE',
    'admin',
    'system',
    'CFG-AUDIT-001',
    '{"setting": "retentionPeriod", "oldValue": "365", "newValue": "730", "unit": "days"}',
    '2026-09-26 12:00:00',
    '7902699be42c8a8e46fbbb4501726816b9054bd3ef6a4b7b8a7a2b4b4b4b4b4b',
    '4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce',
    '2026-09-26 12:00:00',
    false,
    'ACTIVE'
);

-- Record 10: Failed authentication attempt
INSERT IGNORE INTO audit_logs (event_type, actor_id, resource_type, resource_id, payload, event_timestamp, content_hash, previous_hash, created_at, is_archived, status) 
VALUES (
    'AUTH_FAILED',
    'unknown',
    'user',
    'USR-999',
    '{"reason": "invalid_credentials", "ipAddress": "192.168.1.200", "attempts": 3}',
    '2026-09-26 12:15:00',
    '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824',
    '7902699be42c8a8e46fbbb4501726816b9054bd3ef6a4b7b8a7a2b4b4b4b4b4b',
    '2026-09-26 12:15:00',
    false,
    'ACTIVE'
);

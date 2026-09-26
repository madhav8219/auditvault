package com.auditvault.auditvault.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AuditLogExceptionTest {

    @Test
    void constructor_setsMessage() {
        AuditLogException exception = new AuditLogException("message");

        assertEquals("message", exception.getMessage());
    }

    @Test
    void constructor_setsMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");

        AuditLogException exception = new AuditLogException("wrapped", cause);

        assertEquals("wrapped", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructor_allowsNullMessage() {
        AuditLogException exception = new AuditLogException(null);

        assertNull(exception.getMessage());
    }
}

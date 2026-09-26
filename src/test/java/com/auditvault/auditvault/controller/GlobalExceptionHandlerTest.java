package com.auditvault.auditvault.controller;

import com.auditvault.auditvault.exception.AuditLogException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    @Test
    void handleAuditLogException_ReturnsBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        AuditLogException exception = new AuditLogException("Test audit log error");

        ResponseEntity<Map<String, Object>> response = handler.handleAuditLogException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertEquals(400, body.get("status"));
        assertEquals("Audit Log Error", body.get("error"));
        assertEquals("Test audit log error", body.get("message"));
    }

    @Test
    void handleValidationException_ReturnsBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        
        List<org.springframework.validation.ObjectError> errors = new ArrayList<>();
        FieldError fieldError = new FieldError("testObject", "fieldName", "must not be empty");
        errors.add(fieldError);
        
        when(bindingResult.getAllErrors()).thenReturn(errors);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("errors"));
        assertEquals(400, body.get("status"));
        assertEquals("Validation Error", body.get("error"));
        assertEquals("Input validation failed", body.get("message"));
    }

    @Test
    void handleValidationException_MultipleErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        
        List<org.springframework.validation.ObjectError> errors = new ArrayList<>();
        errors.add(new FieldError("testObject", "field1", "error1"));
        errors.add(new FieldError("testObject", "field2", "error2"));
        
        when(bindingResult.getAllErrors()).thenReturn(errors);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, String> errorMap = (Map<String, String>) body.get("errors");
        assertEquals(2, errorMap.size());
        assertTrue(errorMap.containsKey("field1"));
        assertTrue(errorMap.containsKey("field2"));
    }

    @Test
    void handleGenericException_ReturnsInternalServerError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception exception = new Exception("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertEquals(500, body.get("status"));
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("An unexpected error occurred. Please try again later.", body.get("message"));
    }

    @Test
    void handleAuditLogException_NullMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        AuditLogException exception = new AuditLogException(null);

        ResponseEntity<Map<String, Object>> response = handler.handleAuditLogException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Audit Log Error", body.get("error"));
    }

    @Test
    void handleValidationException_EmptyErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>());

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, String> errorMap = (Map<String, String>) body.get("errors");
        assertTrue(errorMap.isEmpty());
    }
}

package com.erp.fleet.interfaces.rest.advice;

import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.DocAlreadyExistsException;
import com.erp.fleet.domain.exception.DomainException;
import com.erp.fleet.domain.exception.PlateAlreadyExistsException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Hidden
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler({CustomerNotFoundException.class, VehicleNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(DomainException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler({DocAlreadyExistsException.class, PlateAlreadyExistsException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(DomainException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody("Error interno: " + ex.getClass().getName() + " - " + ex.getMessage()));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of("timestamp", Instant.now().toString(), "error", message);
    }
}

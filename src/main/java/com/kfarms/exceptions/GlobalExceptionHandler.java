package com.kfarms.exceptions;

import com.kfarms.entity.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jdk.dynalink.linker.MethodHandleTransformer;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler  {

    // Validate errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException ex){
        StringBuilder errorMessage = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errorMessage.append(fieldError.getField())
                        .append(": ")
                        .append(fieldError.getDefaultMessage())
                        .append("; ")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Validation failed: " + errorMessage.toString(), null));
    }

    // Entity not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleEntityNotFound(EntityNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // Handle database constraint violations (like unique itemName + category)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String message = "This item already exists in the given category.";
        return ResponseEntity.status(HttpStatus.CONFLICT) // 409 conflict
                .body(new ApiResponse<>(false, message, null));
    }

    // (Optional) Handle Hibernate ConstraintViolationException specifically
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = "Database constraint violated: " + ex.getConstraintName();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, message, null));
    }

    // Access denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, "You are not authorized to perform this action", null));
    }

    // Authentication failed
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> handleBadCredentials(BadCredentialsException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, "Invalid username or password", null));
    }

    // Bad Request - invalid arguments (live type conversion)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgument(IllegalArgumentException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // Generic errors
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntime(RuntimeException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An unexpected error occurred: " + ex.getMessage(), null));
    }

    // Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFound(ResourceNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // Type Mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        String message;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
           Object[] enumConstraints = ex.getRequiredType().getEnumConstants();
           message = String.format(
                   "invalid value '%s' for parameter '%s', Allowed values: %s",
                   ex.getValue(),
                   ex.getName(),
                   Arrays.toString(enumConstraints)
           );
        } else {
            message = String.format(
                    "Invalid value '%s' for parameter '%s'. Expected type: %s",
                    ex.getValue(),
                    ex.getName(),
                    ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, message, null));
    }
}

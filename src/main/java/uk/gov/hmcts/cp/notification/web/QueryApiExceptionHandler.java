package uk.gov.hmcts.cp.notification.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = NotificationQueryController.class)
public class QueryApiExceptionHandler {

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Void> handleInvalidRequestParameter() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}

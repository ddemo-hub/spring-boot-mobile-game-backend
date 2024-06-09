package com.dreamgames.backendengineeringcasestudy.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dreamgames.backendengineeringcasestudy.dto.ErrorDTO;
import com.dreamgames.backendengineeringcasestudy.exception.DatabaseExpection;
import com.dreamgames.backendengineeringcasestudy.exception.IllegalActionException;
import com.dreamgames.backendengineeringcasestudy.exception.RequestTimeoutException;
import com.dreamgames.backendengineeringcasestudy.exception.UnauthorizedException;

/** Global exception handler */
@RestControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDTO> handleUnauthorizedException(UnauthorizedException ex) {
        ErrorDTO responseDTO = ErrorDTO.builder()
                                .status(401)
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DatabaseExpection.class)
    public ResponseEntity<ErrorDTO> handleDatabaseException(DatabaseExpection ex) {
        ErrorDTO responseDTO = ErrorDTO.builder()
                                .status(400)
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalActionException.class)
    public ResponseEntity<ErrorDTO> handleIllegalActionException(IllegalActionException ex) {
        ErrorDTO responseDTO = ErrorDTO.builder()
                                .status(400)
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RequestTimeoutException.class)
    public ResponseEntity<ErrorDTO> handleRequestTimeoutException(RequestTimeoutException ex) {
        ErrorDTO responseDTO = ErrorDTO.builder()
                                .status(408)
                                .error("Request Timeout")
                                .message(ex.getMessage())
                                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorDTO> handleIllegalStateException(IllegalStateException ex) {
        ErrorDTO responseDTO = ErrorDTO.builder()
                                .status(403)
                                .error("Forbidden")
                                .message(ex.getMessage())
                                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
    }

}

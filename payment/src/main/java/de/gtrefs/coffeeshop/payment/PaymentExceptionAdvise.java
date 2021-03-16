package de.gtrefs.coffeeshop.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class PaymentExceptionAdvise {

    private static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";

    @ExceptionHandler(InsufficientFunds.class)
    public final ResponseEntity<ErrorResponse> handleUserNotFoundException(InsufficientFunds ex, WebRequest request) {
        List<String> details = new ArrayList<>();
        details.add(ex.getLocalizedMessage());
        ErrorResponse error = new ErrorResponse(INSUFFICIENT_FUNDS, details);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}

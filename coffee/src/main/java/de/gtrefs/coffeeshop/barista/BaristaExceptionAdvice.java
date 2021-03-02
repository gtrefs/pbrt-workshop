package de.gtrefs.coffeeshop.barista;

import javax.validation.*;
import java.util.*;
import java.util.stream.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

@ControllerAdvice
public class BaristaExceptionAdvice extends ResponseEntityExceptionHandler
{
	private String INCORRECT_REQUEST = "INCORRECT_REQUEST";
	private String BAD_REQUEST = "BAD_REQUEST";
	
	@ExceptionHandler(CoffeeNotMadeHere.class)
	public final ResponseEntity<ErrorResponse> handleUserNotFoundException
						(CoffeeNotMadeHere ex, WebRequest request) {
		List<String> details = new ArrayList<>();
		details.add(ex.getLocalizedMessage());
		ErrorResponse error = new ErrorResponse(INCORRECT_REQUEST, details);
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public final ResponseEntity<ErrorResponse> handleConstraintViolation(
											ConstraintViolationException ex,
											WebRequest request) {
		List<String> details = ex.getConstraintViolations()
									.parallelStream()
									.map(ConstraintViolation::getMessage)
									.collect(Collectors.toList());

		ErrorResponse error = new ErrorResponse(BAD_REQUEST, details);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}
}

package de.gtrefs.coffeeshop.barista;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CoffeeNotMadeHere extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public CoffeeNotMadeHere(String message) {
        super(message);
    }
}

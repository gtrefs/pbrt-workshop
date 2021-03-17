package de.gtrefs.coffeeshop.barista;

import javax.validation.*;
import javax.validation.constraints.*;
import java.util.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE })
@Validated
public class BaristaController {
	private final CoffeeRepository coffees;

	@Autowired
	public BaristaController(CoffeeRepository repository) {
		this.coffees = repository;
	}

	@GetMapping(value = "/coffees")
	public List<Cup> getAllServedCoffees() {
		return coffees.findAll();
	}

	@PostMapping("/coffees")
	Cup orderCoffee(@RequestBody Cup wantedCoffee) {
		return coffees.save(wantedCoffee);
	}
	
	@GetMapping("/coffees/{id}")
	Cup getServedCoffee(@PathVariable @Min(value = 1, message = "Please tell us which coffee you are referring to.") Long id) {
	    return coffees.findById(id)
					  .orElseThrow(() -> new CoffeeNotMadeHere("Sorry. We never made coffee with "+id));
	}

	@PutMapping("/coffees/{id}")
	Cup updateCoffeeOrder(@RequestBody Cup updatedCoffeeCup, @PathVariable Long id) {
		return coffees.findById(id).map(coffeeCup ->
				coffees.save(updateExistingCoffeeOrder(updatedCoffeeCup, coffeeCup))
		).orElseGet(() -> {
			updatedCoffeeCup.setId(id);
			return coffees.save(updatedCoffeeCup);
		});
	}

	private Cup updateExistingCoffeeOrder(@RequestBody Cup updated, Cup existing) {
		existing.setFlavor(updated.getFlavor());
		return existing;
	}

	@DeleteMapping("/coffees/{id}")
	ResponseEntity<?> deleteCoffeeOrder(@PathVariable Long id) {
        return coffees.findById(id).map(coffeeCup -> {
            coffees.delete(coffeeCup);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
	}
}
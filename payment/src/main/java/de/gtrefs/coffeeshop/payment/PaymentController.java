package de.gtrefs.coffeeshop.payment;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE })
@Validated
public class PaymentController {

	private final PaymentService paymentService;

	@Autowired
	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}


	@PostMapping("/charge")
	ResponseEntity<Receipt> orderCoffee(@RequestBody PaymentCharge charge) {
		return ResponseEntity.ok(paymentService.expense(charge));
	}

}
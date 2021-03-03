package de.gtrefs.coffeeshop.order;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import de.gtrefs.coffeeshop.order.OrderService.*;
import de.gtrefs.coffeeshop.order.OrderStatus.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.*;

@RestController
@RequestMapping(value = "/api")
public class OrderController {

	private final static Logger logger = LoggerFactory.getLogger(OrderController.class);

	private final ObjectReader errorReader = new ObjectMapper().readerFor(ErrorResponse.class);
	private final OrderService orderService;

	@Autowired
	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/order")
	Mono<ResponseEntity<OrderStatus>> orderCoffee(@RequestBody Order coffeeOrder) {
		return orderService.orderCoffee(coffeeOrder)
				   .map(successfulOrder -> {
						logger.info("Order successful {}.", successfulOrder);
						return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(successfulOrder);
				   }).onErrorResume(WebClientResponseException.class, e -> {
					   logger.warn("Order not possible {}.", coffeeOrder, e);
					   var orderNotPossible = readOrderNotPossible(e.getResponseBodyAsString());
					   return Mono.just(ResponseEntity
							   .status(e.getStatusCode())
							   .contentType(MediaType.APPLICATION_JSON)
							   .body(orderNotPossible));
				   }).switchIfEmpty(paymentNotPossible());
	}

	private Mono<ResponseEntity<OrderStatus>> paymentNotPossible() {
		var errorResponse = new ErrorResponse(
				"INTERNAL_SERVER_ERROR",
				Collections.singletonList("Something went wrong while paying for your cup.")
		);
		var responseEntity = ResponseEntity.status(500)
										   .contentType(MediaType.APPLICATION_JSON)
										   .body((OrderStatus) new OrderNotPossible(errorResponse));
		return Mono.just(responseEntity).doOnNext(entity -> logger.error("Something went wrong while paying."));
	}

	private OrderNotPossible readOrderNotPossible(String response) {
		if(response == null || response.isEmpty()) return OrderNotPossible.empty();
		try{
			ErrorResponse o = errorReader.readValue(response);
			return new OrderNotPossible(o);
		} catch (JsonProcessingException e) {
			return OrderNotPossible.empty();
		}
	}

}
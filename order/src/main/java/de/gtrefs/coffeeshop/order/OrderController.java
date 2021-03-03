package de.gtrefs.coffeeshop.order;

import com.fasterxml.jackson.databind.*;
import de.gtrefs.coffeeshop.order.OrderStatus.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import static de.gtrefs.coffeeshop.order.OrderStatus.OrderNotPossible.Reason.*;

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

	@GetMapping("/order/{id}")
	public Mono<ResponseEntity<OrderStatus>> orderStatus(@PathVariable Long id){
		return orderService.oderStatus(id).map(orderStatus -> {
			return ResponseEntity.status(200)
								 .contentType(MediaType.APPLICATION_JSON)
					             .body(orderStatus);
		}).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
	}

	@PostMapping("/order")
	public Mono<ResponseEntity<OrderStatus>> orderCoffee(@RequestBody Order coffeeOrder) {
		return orderService.orderCoffee(coffeeOrder)
		   .map(orderStatus -> {
				if(orderStatus.orderNotPossible()){
					logger.warn("Order not possible {}.", coffeeOrder);
					var orderNotPossible = (OrderNotPossible) orderStatus;
					var errorCode = errorCodeByReason(orderNotPossible);
					return ResponseEntity.status(errorCode)
										 .contentType(MediaType.APPLICATION_JSON)
										 .body(orderStatus);
				}
				logger.info("Order successful {}.", orderStatus);
				return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(orderStatus);
		   });
	}

	private int errorCodeByReason(OrderNotPossible orderNotPossible) {
		return orderNotPossible.reason == PAYMENT_NOT_POSSIBLE ? 500 : 400;
	}

}
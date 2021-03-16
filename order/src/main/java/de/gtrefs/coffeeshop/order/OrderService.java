package de.gtrefs.coffeeshop.order;

import javax.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import de.gtrefs.coffeeshop.order.OrderStatus.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.*;

import static de.gtrefs.coffeeshop.order.OrderStatus.OrderNotPossible.Reason.*;

@Service
public class OrderService {

	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

	@Value("${coffeeshop.barista.endpoint}/api/")
	private String baristaServiceEndpoint;

	@Value("${coffeeshop.payment.endpoint}/api/")
	private String paymentServiceEndpoint;

	private final WebClient.Builder webClientBuilder;
	private final Prices prices;
	private final ConcurrentHashMap<Long, OrderStatus> orderRepository;
	private final AtomicLong orderNumberGenerator = new AtomicLong(1);
	private final ObjectReader errorReader = new ObjectMapper().readerFor(ErrorResponse.class);

	private WebClient barista;
	private FallBackBarista fallBackBarista = new FallBackBarista();
	private WebClient paymentProvider;
	private FallbackCash fallbackCash = new FallbackCash();

	@Autowired
	public OrderService(Prices prices, WebClient.Builder webClientBuilder){
		this.webClientBuilder = webClientBuilder;
		this.prices = prices;
		this.orderRepository = new ConcurrentHashMap<>();
	}

	@PostConstruct
	public void connectToBaristaAndPaymentProvider(){
		barista = webClientBuilder.baseUrl(baristaServiceEndpoint).build();
		paymentProvider = webClientBuilder.baseUrl(paymentServiceEndpoint).build();
	}

	public Mono<OrderStatus> orderCoffee(Order order){
		return acceptOrder(order)
				.flatMap(this::makeCoffee)
				.flatMap(this::payForCoffee);
	}

	private Mono<OrderAccepted> acceptOrder(Order order) {
		var orderNumber = orderNumberGenerator.getAndIncrement();
		OrderAccepted orderAccepted = (OrderAccepted) orderRepository.compute(orderNumber, (number, status) -> {
			order.setOrderNumber(orderNumber);
			return new OrderAccepted(order);
		});
		return Mono.just(orderAccepted);
	}

	private Mono<OrderStatus> makeCoffee(OrderAccepted orderAccepted) {
		logger.info("Order accepted, making coffee: {}.", orderAccepted);
		var order = orderAccepted.order;
		return barista.post()
					  .uri(uriBuilder -> uriBuilder.path("coffees").build())
					  .contentType(MediaType.APPLICATION_JSON)
					  .bodyValue(new CupOrder(order.getFlavor()))
					  .retrieve()
					  .bodyToMono(OrderedCup.class)
					  .map(cup -> (OrderStatus) new CoffeeOrdered(order, cup))
					  .timeout(Duration.ofMillis(100))
					  .onErrorResume(TimeoutException.class, e -> {
					  	logger.warn("First Barista is very slow. Asking second Barista to cover.");
					  	return Mono.just(fallBackBarista.makeCoffee(order));
					  })
					  .onErrorResume(WebClientResponseException.class, e -> {
						  logger.warn("First Barista cannot process the order. Let's ask the second Barista.", e);
						  return Mono.just(fallBackOrRejectOrder(order, e));
					  })
					  .doOnNext(status -> orderRepository.put(status.order().getOrderNumber(), status));
	}

	private OrderStatus fallBackOrRejectOrder(Order order, WebClientResponseException response) {
		if(response.getStatusCode().is5xxServerError()){
			return fallBackBarista.makeCoffee(order);
		}
		try{
			var errorMessage = response.getResponseBodyAsString();
			return new OrderNotPossible(order,
										errorReader.readValue(errorMessage),
										OrderNotPossible.Reason.BARISTA_NOT_AVAILABLE);
		} catch (JsonProcessingException e) {
			return OrderNotPossible.empty();
		}
	}

	private Mono<OrderStatus> payForCoffee(OrderStatus orderStatus) {
		if(!orderStatus.coffeeOrdered()){
			return Mono.just(orderStatus);
		}
		CoffeeOrdered ordered = (CoffeeOrdered) orderStatus;
		logger.info("Coffee ordered, paying for coffee: {}.", ordered);
		return Mono.justOrEmpty(paymentCharge(ordered)).flatMap(paymentCharge -> {
			return paymentProvider.post()
				   .uri(uriBuilder -> uriBuilder.path("charge").build())
				   .contentType(MediaType.APPLICATION_JSON)
				   .bodyValue(paymentCharge)
				   .retrieve()
				   .bodyToMono(Receipt.class)
				   .map(receipt -> (OrderStatus) new CoffeePayed(receipt, ordered.cup, ordered.order));
		}).onErrorResume(WebClientRequestException.class, e -> {
			logger.warn("Payment provider could not process payment. We cannot fulfill the order.", e);
			return payByCash(ordered);
		}).onErrorResume(WebClientResponseException.class, e -> {
			return Mono.just(insufficientFunds(ordered.order, e));
		}).switchIfEmpty(paymentNotPossible(ordered.order()))
				   .doOnNext(status -> orderRepository.put(status.order().getOrderNumber(), status));
	}

	private OrderStatus insufficientFunds(Order order, WebClientResponseException response) {
		logger.warn("Insufficient funds: {}", order);
		try{
			var errorMessage = response.getResponseBodyAsString();
			return new OrderNotPossible(order, errorReader.readValue(errorMessage), INSUFFICIENT_FUNDS);
		} catch (JsonProcessingException e) {
			return OrderNotPossible.empty();
		}
	}

	private Mono<? extends OrderStatus> payByCash(CoffeeOrdered ordered) {
		return paymentCharge(ordered)
				.map(paymentCharge -> fallbackCash.payByCash(ordered, paymentCharge))
				.map(Mono::just)
				.orElseGet(Mono::empty);
	}

	private Mono<? extends OrderStatus> paymentNotPossible(Order order) {
		var errorResponse = new ErrorResponse(
				"INTERNAL_SERVER_ERROR",
				Collections.singletonList("Something went wrong while paying for your cup.")
		);
		return Mono.just(new OrderNotPossible(order, errorResponse, PAYMENT_NOT_POSSIBLE))
				   .doOnNext(entity -> logger.error("Something went wrong while paying."));
	}

	private Optional<PaymentCharge> paymentCharge(CoffeeOrdered status) {
		var order = status.order;
		var number = order.getCreditCardNumber();
		return prices.forOrder(order).map(price -> PaymentCharge.of(price, number));
	}

	public Mono<OrderStatus> oderStatus(Long id) {
		OrderStatus orderStatus = orderRepository.get(id);
		return orderStatus == null ? Mono.empty() : Mono.just(orderStatus);
	}

	public static class CupOrder {
		public final String flavor;

		public CupOrder(String flavor) {this.flavor = flavor;}
	}

	public static class PaymentCharge {
		public final BigDecimal price;
		public final String creditCardNumber;

		private PaymentCharge(BigDecimal price, String creditCardNumber) {
			this.price = price;
			this.creditCardNumber = creditCardNumber;
		}

		public static PaymentCharge of(Prices.Price price, String creditCardNumber){
			return new PaymentCharge(price.price, creditCardNumber);
		}
	}
}

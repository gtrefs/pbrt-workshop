package de.gtrefs.coffeeshop.order;

import javax.annotation.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import de.gtrefs.coffeeshop.order.OrderStatus.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.*;

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

	private WebClient barista;
	private WebClient paymentProvider;

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

	private Mono<CoffeeOrdered> makeCoffee(OrderAccepted orderAccepted) {
		logger.info("Order accepted, making coffee: {}.", orderAccepted);
		var order = orderAccepted.order;
		return barista.post()
					  .uri(uriBuilder -> uriBuilder.path("coffees").build())
					  .contentType(MediaType.APPLICATION_JSON)
					  .bodyValue(new CupOrder(order.getFlavor()))
					  .retrieve()
					  .bodyToMono(OrderedCup.class)
					  .map(cup -> (CoffeeOrdered) orderRepository.computeIfPresent(order.getOrderNumber(), (number, status) -> new OrderStatus.CoffeeOrdered(order, cup)));
	}

	private Mono<CoffeePayed> payForCoffee(OrderStatus.CoffeeOrdered ordered) {
		logger.info("Coffee ordered, paying for coffee: {}.", ordered);
		Order order = ordered.order;
		return Mono.justOrEmpty(paymentCharge(ordered)).flatMap(paymentCharge -> {
			return paymentProvider.post()
				   .uri(uriBuilder -> uriBuilder.path("charge").build())
				   .contentType(MediaType.APPLICATION_JSON)
				   .bodyValue(paymentCharge)
				   .retrieve()
				   .bodyToMono(Receipt.class)
				   .map(receipt -> (CoffeePayed) orderRepository.computeIfPresent(order.getOrderNumber(), (number, status) -> new CoffeePayed(receipt, ordered.cup, order)));
		});
	}

	private Optional<PaymentCharge> paymentCharge(CoffeeOrdered status) {
		var order = status.order;
		var number = order.getCreditCardNumber();
		return prices.forOrder(order).map(price -> PaymentCharge.of(price, number));
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

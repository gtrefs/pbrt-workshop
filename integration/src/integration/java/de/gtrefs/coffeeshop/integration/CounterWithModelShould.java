package de.gtrefs.coffeeshop.integration;

import de.gtrefs.coffeeshop.*;
import io.restassured.specification.*;

import net.jqwik.api.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.*;

import static org.hamcrest.Matchers.startsWith;

// Please note, that shrinking is turned off for all the following properties.
// Shrinking helps us to find a minimal or significant less complex example which
// falsifies our properties. However, in a stateful environment shrinking requires
// a reset of the state. Otherwise, a non-deterministic outcome is very likely.
//
// Resetting our application is costly in terms of time. All containers are
// stopped and started again. Sometimes there are easier ways to reset an
// application. For example, Spring Boot offers a way to restart an application
// and we could cleanse our database. Though, spending this effort is not worth
// it most of the times, as there are other ways to determine the root cause.
// For example, logs and metrics.
//
// That is: We trade in determinism for speed.
public class CounterWithModelShould extends CoffeeShop{

	private static CoffeeShopModel model = new CoffeeShopModel("melange|black|espresso|ristretto|cappuccino");

	// An example integration test one could write in order to find out if we can order
	// a latte macchiato. As this coffeeshop is quite new, our baristas are
	// quite untrained and we don't get a latte. But there is more espresso.
	//@Example
	public void not_offer_latte_macchiato(){
		String order = "{\"flavor\": \"Latte Macchiato\", \"creditCardNumber\":  \"123\"}";
		counter.body(order)
				.contentType("application/json")
				.post("/order")
			.then()
				.assertThat()
				.statusCode(400)
			.and()
				.assertThat()
				.body("error.details[0]", startsWith("We don't offer"));
	}

	// A model allows us to model the desired behavior that we want to check.
	// It is the source of truth for our application. Usually, if a tests
	// fails, it is the application which is at fault.
	@Property(shrinking = ShrinkingMode.OFF, tries = 100)
	public void return_successful_orders(@ForAll("order_and_get_orders") ActionSequence<RequestSpecification> actions){
		actions.run(counter);
	}

	@Provide
	private ActionSequenceArbitrary<RequestSpecification> order_and_get_orders(){
		return Arbitraries.sequences(Arbitraries.frequencyOf(Tuple.of(10, orderExistingFlavor()), Tuple.of(1, checkState())));
	}

	// Exercise 2: In the test above we checked that we can get the state
	// of successful orders. Any order which does not contain a supported flavor,
	// should be rejected. The resulting state should be stored in the model.
	@Property(shrinking = ShrinkingMode.OFF, tries = 100)
	public void return_unsuccessful_orders(@ForAll("order_existing_and_not_existing_flavors") ActionSequence<RequestSpecification> actions) {
		actions.run(counter);
	}

	@Provide
	private ActionSequenceArbitrary<RequestSpecification> order_existing_and_not_existing_flavors(){
		return Arbitraries.sequences(Arbitraries.frequencyOf(Tuple.of(15, orderExistingFlavor()),
															 Tuple.of(5, orderRandomFlavor()),
															 Tuple.of(1, checkState())));
	}

	private Arbitrary<Action<RequestSpecification>> checkState() {
		return Arbitraries.longs().between(1, 5000).map(id -> new CheckStatus(model, id));
	}

	private Arbitrary<Action<RequestSpecification>> orderRandomFlavor() {
		var flavors = Arbitraries.strings().ascii().ofMinLength(3).ofMinLength(15);
		var creditCardNumbers = Arbitraries.strings().numeric().ofMinLength(13).ofMaxLength(16);
		return Combinators.combine(flavors, creditCardNumbers).as(Order::new).map(order -> new OrderCoffee(model, order, "Random Flavor"));
	}

	private Arbitrary<Action<RequestSpecification>> orderExistingFlavor() {
		var flavors = Arbitraries.of("Black", "Melange", "Espresso", "Ristretto", "Cappuccino");
		var creditCardNumbers = Arbitraries.strings().numeric().ofMinLength(13).ofMaxLength(16);
		return Combinators.combine(flavors, creditCardNumbers).as(Order::new).map(order -> new OrderCoffee(model, order, "Existing Flavor"));
	}

	// We don't want to run people into significant debt.
	@Property(shrinking = ShrinkingMode.OFF, tries = 20)
	public void not_run_people_into_debt(@ForAll("orders_for_the_same_credit_card") ActionSequence<RequestSpecification> orders){
		orders.run(counter);
	}

	@Provide
	private ActionSequenceArbitrary<RequestSpecification> orders_for_the_same_credit_card(){
		var flavors = Arbitraries.of("Black", "Melange", "Espresso", "Ristretto", "Cappuccino");
		var creditCardNumber = Arbitraries.just("98236587");
		Arbitrary<Action<RequestSpecification>> orders = Combinators.combine(flavors, creditCardNumber)
				.as(Order::new)
				.map(order -> new OrderCoffee(model, order, "Existing"));
		return Arbitraries.sequences(orders);
	}

	public class OrderCoffee implements Action<RequestSpecification> {

		private final CoffeeShopModel model;
		private final Order order;
		private final String statisticsMessage;

		public OrderCoffee(CoffeeShopModel model, Order order, String statisticsMessage) {
			this.model = model;
			this.order = order;
			this.statisticsMessage = statisticsMessage;
		}

		@Override
		public boolean precondition(RequestSpecification state) {
			Statistics.label("Action").collect("Order Coffee: "+statisticsMessage);
			return true;
		}

		@Override
		public RequestSpecification run(RequestSpecification state) {
			var response = state.body(order).post("/order");
			model.order(order).checkPostCondition(response);
			return state;
		}

		@Override
		public String toString() {
			return "OrderCoffee{" +
					"order=" + order +
					'}';
		}
	}

	public class CheckStatus implements Action<RequestSpecification> {

		private final CoffeeShopModel model;
		private Long orderId;

		public CheckStatus(CoffeeShopModel model, Long orderId) {
			this.model = model;
			this.orderId = orderId;
		}

		@Override
		public boolean precondition(RequestSpecification state) {
			Statistics.label("Action").collect("Check order state");
			return true;
		}

		@Override
		public RequestSpecification run(RequestSpecification state) {
			var response = state.body(orderId).get("/order/"+orderId);
			model.checkStatus(orderId).checkPostCondition(response);
			return state;
		}

		@Override
		public String toString() {
			return "CheckStatus{" +
					"orderId=" + orderId +
					'}';
		}
	}
}

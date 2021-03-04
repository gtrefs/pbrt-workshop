package de.gtrefs.coffeeshop.resilience;

import de.gtrefs.coffeeshop.*;
import io.restassured.specification.*;
import org.jetbrains.annotations.*;

import net.jqwik.api.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.*;

// In this section we extend our model to inject faults. One thing to investigate
// would be how the system reacts when a database is down? Lending from chaos
// engineering we would like to test the hypotheses, that our coffeeshop should
// still work, even when our barista is not able to brew our coffee. That is,
// we are still able to process orders.
//
// We can observe our system by comparing the system responses to our model. If
// our system acts different than before, our experiment failed and we need to
// adapt our system to withstand such a failure.
//
public class CounterWithFaultsShould extends CoffeeShopWithFaults {

	private final CoffeeShopModel model = new CoffeeShopModel("melange|black|espresso|ristretto|cappuccino");

	@Property(shrinking = ShrinkingMode.OFF, tries = 100)
	public void return_unsuccessful_orders(@ForAll("order_existing_and_not_existing_flavors") ActionSequence<RequestSpecification> actions) {
		actions.run(counter);
	}

	@Provide
	private ActionSequenceArbitrary<RequestSpecification> order_existing_and_not_existing_flavors(){
		return Arbitraries.sequences(Arbitraries.frequencyOf(Tuple.of(50, orderExistingFlavor()),
															 Tuple.of(10, orderRandomFlavor()),
															 Tuple.of(5, Arbitraries.create(() -> new EnableDatabase(model))),
															 Tuple.of(5, Arbitraries.create(() -> new DisableDatabase(model))),
															 Tuple.of(10, checkState())));
	}

	// Third Exercise: When the payment provider is down, our coffeeshop falls
	// back to cash. Please inject the corresponding faults using the paymentProxy.
	// When you model any action, please keep in mind that there might be some
	// precondition. For example, a disabled payment provider does not need to
	// be enabled again.
	// Hint: Look at the OrderService class in service order
	@Property(shrinking = ShrinkingMode.OFF, tries = 100)
	public void stay_responsive_when_payment_provider_is_down(@ForAll("orders_with_payment_provider_down") ActionSequence<RequestSpecification> actions) {
		actions.run(counter);
	}

	@Provide
	private ActionSequenceArbitrary<RequestSpecification> orders_with_payment_provider_down(){
		return Arbitraries.sequences(Arbitraries.frequencyOf(Tuple.of(50, orderExistingFlavor()),
															 Tuple.of(10, orderRandomFlavor()),
															 Tuple.of(5, enablePaymentProvider()),
															 Tuple.of(5, disablePaymentProvider()),
															 Tuple.of(10, checkState())));
	}

	private Arbitrary<Action<RequestSpecification>> disablePaymentProvider() {
		return todo("Implement action for disabling a payment provider.");
	}

	private Arbitrary<Action<RequestSpecification>> enablePaymentProvider() {
		return todo("Implement action for enabling a payment provider.");
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

	public class DisableDatabase implements Action<RequestSpecification> {

		private final CoffeeShopModel model;

		public DisableDatabase(CoffeeShopModel model) {
			this.model = model;
		}

		@Override
		public boolean precondition(RequestSpecification state) {
			boolean databaseEnabled = model.databaseEnabled();
			if (databaseEnabled) {
				Statistics.label("Fault Injection").collect("Disable Database");
			}
			return databaseEnabled;
		}

		@Override
		public RequestSpecification run(RequestSpecification state) {
			postgresProxy.disable();
			model.disableDatabase();
			return state;
		}

		@Override
		public String toString() {
			return "DisableDatabase{}";
		}
	}

	public class EnableDatabase implements Action<RequestSpecification> {

		private final CoffeeShopModel model;

		public EnableDatabase(CoffeeShopModel model) {
			this.model = model;
		}

		@Override
		public boolean precondition(RequestSpecification state) {
			boolean databaseDisabled = model.isDatabaseDisabled();
			if (databaseDisabled) {
				Statistics.label("Fault Injection").collect("Enable Database");
			}
			return databaseDisabled;
		}

		@Override
		public RequestSpecification run(RequestSpecification state) {
			postgresProxy.enable();
			model.enableDatabase();
			return state;
		}

		@Override
		public String toString() {
			return "EnableDatabase{}";
		}
	}

	private <R> R todo(String todo){
		throw new RuntimeException(todo);
	}

}

package de.gtrefs.coffeeshop.integration;

import io.restassured.specification.*;

import net.jqwik.api.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.*;

public class CounterWithModelShould extends CoffeeShop{

	private CoffeeShopModel model = new CoffeeShopModel("melange|black|espresso|ristretto|cappuccino");

	// In the test above we could see, that there is a lot of state involved.
	// For example, the order number is increasing and for every receipt a new
	// balance is returned. Let's assume we opened a nice coffeeshop and we
	// don't deduct from any card which has more than 10 currency of debt.
	@Property(shrinking = ShrinkingMode.OFF)
	public void not_run_people_into_debt(@ForAll("orders_for_the_same_credit_card") ActionSequence<RequestSpecification> orders){
		orders.run(counter);
	}

	@Provide
	private ActionSequenceArbitrary<RequestSpecification> orders_for_the_same_credit_card(){
		var flavors = Arbitraries.of("Black", "Melange", "Espresso", "Ristretto", "Cappuccino");
		var creditCardNumber = Arbitraries.just("98236587");
		Arbitrary<Action<RequestSpecification>> orders = Combinators.combine(flavors, creditCardNumber)
																 .as(Order::new)
																 .map(order -> new OrderCoffee(model, order));
		return Arbitraries.sequences(orders );
	}

	public class OrderCoffee implements Action<RequestSpecification> {

		private final CoffeeShopModel model;
		private final Order order;

		public OrderCoffee(CoffeeShopModel model, Order order) {
			this.model = model;
			this.order = order;
		}

		@Override
		public boolean precondition(RequestSpecification state) {
			Statistics.label("Action").collect("Order Coffee");
			return true;
		}

		@Override
		public RequestSpecification run(RequestSpecification state) {
			var response = state.body(order).post("/order");
			model.order(order).checkPostCondition(response);
			return state;
		}
	}
}

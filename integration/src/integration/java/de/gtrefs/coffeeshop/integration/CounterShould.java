package de.gtrefs.coffeeshop.integration;

import java.util.*;

import de.gtrefs.coffeeshop.*;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

public class CounterShould extends CoffeeShop {

	// An integration test we would write in order to find out if we can order
	// a latte macchiato. As this coffeeshop is quite new, our baristas are
	// quite untrained and we don't get a latte. But there is more espresso.
	@Example
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
			   .body("error.details[0]", startsWith("We don't offer Latte Macchiato."));
	}

	// If we ask ourselves, what should happen in the case when flavors are ordered
	// that our baristas can make, then we can write some orders (per default 1000).
	@Property
	public void offer_flavors(@ForAll("orders") List<Order> orders){
		orders.forEach(order -> counter.body(order)
								   .post("/order")
							   .then()
								   .assertThat()
								   .statusCode(200)
							   .and()
								   .contentType("application/json"));
	}

	@Provide
	public ListArbitrary<Order> orders(){
		Arbitrary<String> flavors = Arbitraries.of("Black", "Melange", "Espresso", "Ristretto", "Cappuccino");
		Arbitrary<String> creditCardNumbers = Arbitraries.strings().numeric().ofMinLength(13).ofMaxLength(16);
		return Combinators.combine(flavors, creditCardNumbers).as(Order::new).list();
	}

	// In the test above we could see, that there is a lot of state involved.
	// For example, the order number is increasing and for every receipt a new
	// balance is returned. Let's assume we opened a nice coffeeshop and we
	// don't deduct from any card which has more than 10 currency of debt.
	@Property//(shrinking = ShrinkingMode.FULL)
	public void not_run_people_into_debt(@ForAll("orders_for_the_same_credit_card") List<Order> orders){
		orders.forEach(order -> {
			var post = counter.body(order).post("/order");
			var balance = post.body().jsonPath().getDouble("receipt.balance");
			if (balance <= -10) {
				assertThat(post.statusCode()).isEqualTo(400);
			} else {
				assertThat(post.statusCode()).isEqualTo(200);
			}
		});
	}

	@Provide
	public ListArbitrary<Order> orders_for_the_same_credit_card(){
		var flavors = Arbitraries.of("Black", "Melange", "Espresso", "Ristretto", "Cappuccino");
		var creditCardNumber = Arbitraries.just("98236587");
		return Combinators.combine(flavors, creditCardNumber)
						  .as(Order::new)
						  .list().ofMinSize(10).ofMaxSize(12);
	}

	// However, the test should fail even before we run our customers into
	// debt. This means, some orders are prohibited. That is, before
	// you order a coffee you should be able to afford one.
	//
	// Exercise 1: The goal of the first exercise is, that you run some
	// tests against our CoffeeShop. Try to ask our barista for anything but
	// the coffee flavors we offer. The order should always be denied.
	// Next, try some credit card fraud and throw random numbers at our employee
	// at the counter. Are you able to get some coffee for free?

	@Property
	public void deny_order_when_flavors_are_not_known(@ForAll("tasteless_flavors") List<Order> orders){
		orders.forEach(order -> counter.body(order)
								   .post("/order")
							   .then()
								   .assertThat()
								   .statusCode(400)
							   .and()
								   .contentType("application/json"));
	}

	@Provide
	public ListArbitrary<Order> tasteless_flavors(){
		Arbitrary<String> flavors = Arbitraries.strings().all();
		Arbitrary<String> creditCardNumbers = Arbitraries.strings().numeric().ofMinLength(13).ofMaxLength(16);
		return Combinators.combine(flavors, creditCardNumbers).as(Order::new).list();
	}

	@Property
	public void accept_all_credit_cards(@ForAll("creditcard_order") List<Order> orders){
		orders.forEach(order -> counter.body(order)
								   .post("/order")
							   .then()
								   .assertThat()
								   .statusCode(200)
							   .and()
								   .contentType("application/json"));
	}

	@Provide
	public ListArbitrary<Order> creditcard_order(){
		Arbitrary<String> flavors = Arbitraries.of("Black", "Melange", "Espresso", "Ristretto", "Cappuccino");
		Arbitrary<String> creditCardNumbers = Arbitraries.strings().all();
		return Combinators.combine(flavors, creditCardNumbers).as(Order::new).list();
	}

	// Shrinking with these properties won't work because the state is not
	// reset between each try. The easiest, but also most costly way to  we
	// would need to kill all containers and restart the application. This would
	// allow us to shrink an example.

	//@AfterTry
	public void restartCoffeeShop(){
		stop();
		start();
	}

}

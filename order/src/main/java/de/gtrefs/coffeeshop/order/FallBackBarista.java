package de.gtrefs.coffeeshop.order;

import java.util.concurrent.atomic.*;
import java.util.regex.*;

import static de.gtrefs.coffeeshop.order.OrderStatus.OrderNotPossible.Reason.*;
import static java.util.Collections.*;

public class FallBackBarista {

	private AtomicLong ids = new AtomicLong(0);

	private Pattern pattern;

	public FallBackBarista(){
		pattern = Pattern.compile("melange|black|espresso|ristretto|cappuccino");
	}

	public OrderStatus makeCoffee(Order order){
		if(pattern.matcher(order.getFlavor().toLowerCase()).matches()){
			var cup = new OrderStatus.OrderedCup();
			cup.setId(ids.incrementAndGet());
			cup.setFlavor(order.getFlavor());
			return new OrderStatus.CoffeeOrdered(order, cup);
		}
		var error = new ErrorResponse("BAD_REQUEST", singletonList("We don't offer this flavor. " +
																				   "Please pick one of Black Coffee, Melange, " +
																				   "Espresso, Ristretto or Cappuccino."));
		return new OrderStatus.OrderNotPossible(order, error, BARISTA_NOT_AVAILABLE);
	}


}

package de.gtrefs.coffeeshop.order;

import java.util.concurrent.atomic.*;

import de.gtrefs.coffeeshop.order.OrderService.*;
import de.gtrefs.coffeeshop.order.OrderStatus.*;

public class FallbackCash {

	AtomicLong receiptCounter = new AtomicLong(0);

	public CoffeePayed payByCash(CoffeeOrdered ordered, PaymentCharge paymentCharge) {
		var receipt = new OrderStatus.Receipt();
		receipt.setId(receiptCounter.incrementAndGet());
		receipt.setBalance(paymentCharge.price);
		return new CoffeePayed(receipt, ordered.cup, ordered.order);
	}
}

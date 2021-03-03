package de.gtrefs.coffeeshop.order;

import java.math.*;
import java.util.*;

public interface OrderStatus {

	final class OrderNotPossible implements OrderStatus {
		public final ErrorResponse error;

		public OrderNotPossible(ErrorResponse error) {this.error = error;}

		public static OrderNotPossible empty() {
			return new OrderNotPossible(new ErrorResponse("No message.", Collections.emptyList()));
		}

		@Override
		public String toString() {
			return "OrderNotPossible{" +
					"error=" + error +
					'}';
		}
	}

	final class OrderAccepted implements OrderStatus {

		public final Order order;

		public OrderAccepted(Order order){
			this.order = order;
		}

		@Override
		public String toString() {
			return "OrderAccepted{" +
					"order=" + order +
					'}';
		}
	}

	final class CoffeeOrdered implements OrderStatus {
		public final Order order;

		public final OrderedCup cup;

		public CoffeeOrdered(Order order, OrderedCup cup) {
			this.order = order;
			this.cup = cup;
		}

		public static CoffeeOrdered of(Order order, OrderedCup cup){
			return new CoffeeOrdered(order, cup);
		}

		@Override
		public String toString() {
			return "CoffeeOrdered{" +
					"order=" + order +
					", cup=" + cup +
					'}';
		}
	}

	final class CoffeePayed implements OrderStatus {
		public final Receipt receipt;
		public final OrderedCup cup;
		public final Order order;

		public CoffeePayed(Receipt receipt, OrderedCup cup, Order order) {
			this.receipt = receipt;
			this.cup = cup;
			this.order = order;
		}

		@Override
		public String toString() {
			return "CoffeePayed{" +
					"receipt=" + receipt +
					", cup=" + cup +
					", order=" + order +
					'}';
		}
	}

	class OrderedCup {
		private Long id;
		private String flavor;

		public String getFlavor() {
			return flavor;
		}

		public void setFlavor(String flavor) {
			this.flavor = flavor;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "OrderedCup{" +
					"id=" + id +
					", flavor='" + flavor + '\'' +
					'}';
		}
	}

	class Receipt {
		private Long id;
		private BigDecimal balance;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getBalance() {
			return balance;
		}

		public void setBalance(BigDecimal balance) {
			this.balance = balance;
		}

		@Override
		public String toString() {
			return "Receipt{" +
					"id=" + id +
					", balance=" + balance +
					'}';
		}
	}
}

package de.gtrefs.coffeeshop.order;

import java.math.*;
import java.util.*;

public interface OrderStatus {

	default boolean orderNotPossible(){
		return  false;
	}

	default boolean orderAccepted(){
		return false;
	}

	default boolean coffeeOrdered(){
		return false;
	}

	default boolean coffeePayed(){
		return false;
	}

	default Optional<Order> order(){
		return Optional.empty();
	}

	final class OrderNotPossible implements OrderStatus {
		public final ErrorResponse error;
		public final Reason reason;

		public OrderNotPossible(ErrorResponse error, Reason reason) {
			this.error = error;
			this.reason = reason;
		}

		public static OrderNotPossible empty() {
			return new OrderNotPossible(new ErrorResponse("No message.", Collections.emptyList()), Reason.NONE);
		}

		@Override
		public boolean orderNotPossible() {
			return true;
		}

		@Override
		public String toString() {
			return "OrderNotPossible{" +
					"error=" + error +
					'}';
		}

		public enum Reason {
			PAYMENT_NOT_POSSIBLE{
				@Override
				public String toString() {
					return "Payment not possible.";
				}
			}, BARISTA_NOT_AVAILABLE {
				@Override
				public String toString() {
					return "Barista not available.";
				}
			}, NONE {
				@Override
				public String toString() {
					return "No reason.";
				}
			}
		}
	}

	final class OrderAccepted implements OrderStatus {

		public final Order order;

		public OrderAccepted(Order order){
			this.order = order;
		}

		@Override
		public boolean orderAccepted() {
			return true;
		}

		@Override
		public Optional<Order> order() {
			return Optional.of(order);
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
		public boolean coffeeOrdered() {
			return true;
		}

		@Override
		public Optional<Order> order() {
			return Optional.of(order);
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
		public boolean coffeePayed() {
			return true;
		}

		@Override
		public Optional<Order> order() {
			return Optional.of(order);
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

package de.gtrefs.coffeeshop.integration;

import java.math.*;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

public interface OrderStatus {

	final class OrderNotPossible implements OrderStatus {
		public final ErrorResponse error;

		@JsonCreator
		public OrderNotPossible(@JsonProperty("error") ErrorResponse error) {this.error = error;}

		public static OrderNotPossible empty() {
			return new OrderNotPossible(new ErrorResponse("No message.", Collections.emptyList()));
		}

		@Override
		public String toString() {
			return "OrderNotPossible{" +
					"error=" + error +
					'}';
		}

		public static class ErrorResponse {
			public final String message;
			public final List<String> details;

			@JsonCreator
			public ErrorResponse(@JsonProperty("message") String message, @JsonProperty("details") List<String> details) {
				this.message = message;
				this.details = details;
			}

			@Override
			public String toString() {
				return "ErrorResponse{" +
						"message='" + message + '\'' +
						", details=" + details +
						'}';
			}
		}
	}

	final class OrderAccepted implements OrderStatus {

		public final Order order;

		@JsonCreator
		public OrderAccepted(@JsonProperty("order") Order order){
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

		@JsonCreator
		public static CoffeeOrdered of(@JsonProperty("order") Order order,  @JsonProperty("cup") OrderedCup cup){
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

		@JsonCreator
		public CoffeePayed(@JsonProperty("receipt") Receipt receipt,
						   @JsonProperty("cup")OrderedCup cup,
						   @JsonProperty("order") Order order) {
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

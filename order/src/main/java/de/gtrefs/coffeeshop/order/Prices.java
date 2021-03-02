package de.gtrefs.coffeeshop.order;

import java.math.*;
import java.util.*;

public class Prices {

	private final Map<String, BigDecimal> prices;

	private Prices(Map<String, BigDecimal> prices) {
		this.prices = prices;
	}

	public static Prices of(Map<String, BigDecimal> prices) {
		return new Prices(prices);
	}

	public Optional<Price> forOrder(Order order) {
		return Optional.ofNullable(prices.get(order.getFlavor().toLowerCase())).map(Price::new);
	}

	public static class Price {
		public final BigDecimal price;

		private Price(BigDecimal price){
			this.price = price;
		}
	}
}

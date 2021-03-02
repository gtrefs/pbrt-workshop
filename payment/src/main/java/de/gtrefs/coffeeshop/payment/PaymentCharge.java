package de.gtrefs.coffeeshop.payment;

import java.math.*;

public class PaymentCharge {
	private BigDecimal price;
	private String creditCardNumber;

	private PaymentCharge(BigDecimal price, String creditCardNumber) {
		this.price = price;
		this.creditCardNumber = creditCardNumber;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getCreditCardNumber() {
		return creditCardNumber;
	}

	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}
}

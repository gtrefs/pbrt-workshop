package de.gtrefs.coffeeshop;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.*;

public class Order {
	private String flavor;
	private String creditCardNumber;
	@JsonInclude(Include.NON_NULL)
	private Long orderNumber;

	public Order(String flavor, String creditCardNumber){
		this.flavor = flavor;
		this.creditCardNumber = creditCardNumber;
	}

	public Order() {
	}

	public String getCreditCardNumber() {
		return creditCardNumber;
	}

	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}

	public String getFlavor() {
		return flavor;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public Long getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(Long orderNumber) {
		this.orderNumber = orderNumber;
	}

	@Override
	public String toString() {
		return "Order{" +
				"flavor='" + flavor + '\'' +
				", creditCardNumber='" + creditCardNumber + '\'' +
				'}';
	}
}

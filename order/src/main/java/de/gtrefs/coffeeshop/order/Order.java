package de.gtrefs.coffeeshop.order;

import java.io.*;

public class Order implements Serializable {

    private Long orderNumber;

	private String flavor;

	private String creditCardNumber;

	public Order() {
	}

	public Order(Long orderNumber, String flavor, String creditCardNumber){
		this.orderNumber = orderNumber;
		this.flavor = flavor;
		this.creditCardNumber = creditCardNumber;
	}

	public Order(String flavor){
		this.flavor = flavor;
	}

	public Long getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(Long orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getFlavor() {
		return flavor;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public String getCreditCardNumber() {
		return creditCardNumber;
	}

	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}


	@Override
	public String toString() {
		return "Order{" +
				"orderNumber=" + orderNumber +
				", flavor='" + flavor + '\'' +
				", creditCardNumber='" + creditCardNumber + '\'' +
				'}';
	}
}
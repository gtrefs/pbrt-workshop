package de.gtrefs.coffeeshop.integration;

public class Order {
	private String flavor;
	private String creditCardNumber;

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

	@Override
	public String toString() {
		return "Order{" +
				"flavor='" + flavor + '\'' +
				", creditCardNumber='" + creditCardNumber + '\'' +
				'}';
	}
}

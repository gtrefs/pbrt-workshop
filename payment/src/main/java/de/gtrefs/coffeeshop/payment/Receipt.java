package de.gtrefs.coffeeshop.payment;

import java.math.*;

public class Receipt {
	private Long id;
	private BigDecimal balance;

	public Receipt(Long id, BigDecimal price) {
		this.id = id;
		this.balance = price;
	}

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
}

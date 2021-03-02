package de.gtrefs.coffeeshop.payment;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

@Service
public class PaymentService {

	private final ConcurrentHashMap<String, BigDecimal> money;
	private final AtomicLong runningReceiptNumber = new AtomicLong();
	private final Random bigSpender = new Random();

	@Value("${coffeeshop.payment.balance.start:10}")
	private int startBalance = 10;

	public PaymentService(){
		money = new ConcurrentHashMap<>();
	}

	public Receipt expense(PaymentCharge charge){
		BigDecimal newBalance = money.compute(charge.getCreditCardNumber(), (creditCardNumber, balance) -> {
			if(balance == null) return new BigDecimal(startBalance).subtract(charge.getPrice());
			return balance.subtract(charge.getPrice());
		});
		return new Receipt(runningReceiptNumber.incrementAndGet(), newBalance);
	}

}

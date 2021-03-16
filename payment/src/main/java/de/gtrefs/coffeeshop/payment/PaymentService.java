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

	@Value("${coffeeshop.payment.balance.start:10}")
	private int startBalance = 10;

	public PaymentService(){
		money = new ConcurrentHashMap<>();
	}

	public Receipt expense(PaymentCharge charge){
		BigDecimal newBalance = money.compute(charge.getCreditCardNumber(), (creditCardNumber, balance) -> {
			if(balance == null) return new BigDecimal(startBalance).subtract(charge.getPrice());
			BigDecimal computed = balance.subtract(charge.getPrice());
			if(computed.longValue() <= -10L){
				throw new InsufficientFunds("Insufficient funds for credit card: "+creditCardNumber);
			}
			return computed;
		});
		return new Receipt(runningReceiptNumber.incrementAndGet(), newBalance);
	}

}

package de.gtrefs.coffeeshop.order;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.stream.*;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

@Configuration
public class OrderConfiguration {

	@Bean
	public Prices prices(@Value("${coffeeshop.order.prices}") String prices) throws IOException {
		var reader = new ObjectMapper().reader();
		Map<String, String> pricesAsJson = reader.readValue(prices, Map.class);
		var parsedPrices = pricesAsJson
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> new BigDecimal(e.getValue())));
		return Prices.of(parsedPrices);
	}
}

package de.gtrefs.coffeeshop.order;

import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.assertj.core.api.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.web.reactive.function.client.WebClientResponseException.*;

@SpringBootTest
public class PropagateErrorResponse {

	@MockBean
	OrderService service;

	@Autowired
	OrderController controller;

	@Test
	public void badRequestShouldHaveDetails() throws InterruptedException {
		//language=JSON
		var body = "{\"message\": \"BAD_REQUEST\", \"details\": [\"sdfsf\"]}".getBytes();
		var noHeaders = new HttpHeaders();
		int badRequest = 400;
		var await = new CountDownLatch(1);
		Mockito.when(service.orderCoffee(any(Order.class)))
			   .thenReturn(
			   		Mono.error(create(badRequest,"", noHeaders,body,null)));

		controller.orderCoffee(new Order(1L, "Black", "123"))
				  .subscribe(response -> {
					  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
					  assertThat(response.getBody()).isInstanceOf(OrderService.OrderNotPossible.class);
					  assertThat(((OrderService.OrderNotPossible) response.getBody()).error.getMessage()).isEqualTo("BAD_REQUEST");
					  assertThat(((OrderService.OrderNotPossible) response.getBody()).error.getDetails()).isEqualTo(Collections
																															.singletonList("sdfsf"));
					  await.countDown();
				  });
		await.await();

	}

}

package de.gtrefs.coffeeshop.integration;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import de.gtrefs.coffeeshop.*;
import io.restassured.response.*;
import io.vavr.control.Try;

import static de.gtrefs.coffeeshop.integration.CoffeeShopModel.PostCondition.*;
import static org.assertj.core.api.Assertions.*;

// A model of a coffee shop allows us to better represent
// the expected state and behavior of our application.
public class CoffeeShopModel {

	private final Pattern matcherForKnownFlavors;
	private final Map<Long, OrderStatus> orders = new HashMap<>();

	public CoffeeShopModel(String patternForKnownFlavors){
		this.matcherForKnownFlavors = Pattern.compile(patternForKnownFlavors);
	}

	public ModelResponse order(Order modelOrder){
		if(hasKnownFlavor(modelOrder)) {
			return new ModelResponse(has(response -> {
				assertThat(response.getStatusCode()).isEqualTo(200);
				var coffeePayed = response.as(OrderStatus.CoffeePayed.class);
				var orderNumber = coffeePayed.order.getOrderNumber();
				assertThat(orderNumber).isGreaterThan(0L);
				assertThat(coffeePayed.receipt.getBalance()).isGreaterThan(new BigDecimal(-10));
				orders.put(orderNumber, coffeePayed);
			}).or(response -> {
				// TODO: Exercise 2 Verify that the return code is a bad request and save the corresponding state in orders
			}));
		}
		return new ModelResponse(unknownFlavor().andThen(response -> {
			var orderNotPossible = response.as(OrderStatus.OrderNotPossible.class);
			orders.put(orderNotPossible.order.getOrderNumber(), orderNotPossible);
		}));
	}

	private boolean hasKnownFlavor(Order order){
		return matcherForKnownFlavors.matcher(order.getFlavor().toLowerCase()).matches();
	}

	public ModelResponse checkStatus(Long orderId) {
		return new ModelResponse(response -> {
			var modelStatus = orders.get(orderId);
			if(modelStatus == null){
				assertThat(response.getStatusCode()).isEqualTo(404);
			} else {
				assertThat(response.getStatusCode()).isEqualTo(200);
			}
		});
	}

	public static class ModelResponse {

		private final PostCondition postCondition;

		public ModelResponse(PostCondition postCondition) {
			this.postCondition = postCondition;
		}

		public void checkPostCondition(Response apiResponse) {
			postCondition.check(apiResponse);
		}
	}

	interface PostCondition {

		void check(Response response);

		static PostCondition unknownFlavor(){
			return isBadRequest().andThen(response -> {
				String errorMessage = response.body().jsonPath().getString("error.details[0]");
				assertThat(errorMessage).startsWith("We don't offer this flavor");
			});
		}

		static PostCondition has(PostCondition postCondition) {
			return postCondition;
		}

		static PostCondition isBadRequest() {
			return response -> assertThat(response.getStatusCode()).isEqualTo(400);
		}

		static PostCondition isInternalServerError() {
			return response -> assertThat(response.getStatusCode()).isEqualTo(500);
		}

		static PostCondition isNotAnInternalServerError() {
			return response -> assertThat(response.getStatusCode()).isLessThan(500);
		}

		static PostCondition repliedIn(Duration maxResponseTime) {
			return response -> {
				var actualResponseTime = Duration.ofMillis(response.getTime());
				assertThat(actualResponseTime).isLessThanOrEqualTo(maxResponseTime);
			};
		}

		default PostCondition andThen(PostCondition after) {
			Objects.requireNonNull(after);
			return response -> { check(response); after.check(response); };
		}

		default PostCondition or(PostCondition other) {
			Objects.requireNonNull(other);
			return response -> Try.run(() -> this.check(response))
					.recoverWith(Throwable.class, e -> Try.run(() -> other.check(response)))
					.get();
		}
	}
}

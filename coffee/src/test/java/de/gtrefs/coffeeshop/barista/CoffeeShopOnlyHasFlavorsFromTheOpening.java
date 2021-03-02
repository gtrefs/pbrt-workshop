package de.gtrefs.coffeeshop.barista;

import io.restassured.filter.log.*;
import io.restassured.http.*;
import io.restassured.specification.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.boot.web.server.*;

import static io.restassured.RestAssured.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CoffeeShopOnlyHasFlavorsFromTheOpening {

	@LocalServerPort
	private int port;

	@MockBean
	private CoffeeRepository coffees;

	private RequestSpecification coffeeShopApi;

	@BeforeEach
	public void setupCoffeeShopApi(){
		coffeeShopApi = given()
				.filter(new ErrorLoggingFilter())
				.baseUri("http://localhost:"+port+"/api")
				.contentType(ContentType.JSON);
	}

	@Test
	public void espresso_please(){
		var espresso = Cup.of("espresso");
		coffeeShopApi.body(espresso)
					 .post("/coffees")
					 .then()
					 .statusCode(200);
	}
}

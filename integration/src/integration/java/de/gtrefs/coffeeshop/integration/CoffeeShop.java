package de.gtrefs.coffeeshop.integration;

import java.nio.file.*;

import io.restassured.filter.log.*;
import io.restassured.http.*;
import io.restassured.specification.*;
import org.intellij.lang.annotations.*;
import org.slf4j.*;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.*;
import org.testcontainers.containers.wait.strategy.*;
import org.testcontainers.images.builder.*;
import org.testcontainers.utility.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.testcontainers.containers.wait.strategy.Wait.*;

public  class CoffeeShop {

	static Logger logger = LoggerFactory.getLogger(CoffeeShop.class);

	static Network network = Network.newNetwork();

	protected static ImageFromDockerfile coffeeImage = new ImageFromDockerfile("coffee:latest", false)
			.withDockerfile(Paths.get("../Dockerfile-coffee").toAbsolutePath());

	protected static ImageFromDockerfile paymentImage = new ImageFromDockerfile("payment:latest", false)
			.withDockerfile(Paths.get("../Dockerfile-payment").toAbsolutePath());

	protected static ImageFromDockerfile orderImage = new ImageFromDockerfile("order:latest", false)
			.withDockerfile(Paths.get("../Dockerfile-order").toAbsolutePath());

	protected static PostgreSQLContainer postgresqlContainer = (PostgreSQLContainer) new PostgreSQLContainer(DockerImageName.parse("postgres").withTag("9.6.12"))
			.withDatabaseName("coffeeshop_db")
			.withUsername("postgres")
			.withPassword("postgres")
			.withInitScript("coffee.sql")
			.withNetwork(network)
			.withNetworkAliases("db")
			.waitingFor(new AbstractWaitStrategy() {
				@Override
				protected void waitUntilReady() {
					while (!this.waitStrategyTarget.isRunning()){}
				}
			});

	protected static GenericContainer coffeeContainer = new GenericContainer(coffeeImage)
			.withNetwork(network)
			.withNetworkAliases("barista")
			.dependsOn(postgresqlContainer)
			.withEnv("POSTGRES_HOST", "db")
			.withEnv("POSTGRES_PORT", PostgreSQLContainer.POSTGRESQL_PORT.toString())
			.withExposedPorts(8080)
			.waitingFor(forLogMessage(".*Tomcat started.*", 1));

	protected static GenericContainer paymentContainer = new GenericContainer(paymentImage)
			.withNetwork(network)
			.withNetworkAliases("paymentProvider")
			.withExposedPorts(8080)
			.waitingFor(forLogMessage(".*Tomcat started.*", 1));

	protected static GenericContainer orderContainer = new GenericContainer(orderImage)
			.withNetwork(network)
			.dependsOn(coffeeContainer)
			.dependsOn(paymentContainer)
			.withEnv("coffeeshop.barista.endpoint", "http://barista:8080")
			.withEnv("coffeeshop.payment.endpoint", "http://paymentProvider:8080")
			.withExposedPorts(8080)
			.waitingFor(forLogMessage(".*Tomcat started.*", 1));



	static {
		start();
	}

	protected static void start(){
		orderContainer.start();
		coffeeContainer.followOutput(new Slf4jLogConsumer(logger));
		orderContainer.followOutput(new Slf4jLogConsumer(logger));
		paymentContainer.followOutput(new Slf4jLogConsumer(logger));
	}

	protected static void stop(){
		orderContainer.stop();
		paymentContainer.stop();
		coffeeContainer.stop();
		postgresqlContainer.stop();
	}

	protected RequestSpecification counter;

	@BeforeTry
	public void setupCoffeeShopApi(){
		counter = given()
				.filter(new ErrorLoggingFilter())
				.baseUri("http://localhost:"+orderContainer.getMappedPort(8080)+"/api")
				.contentType(ContentType.JSON);
	}

}

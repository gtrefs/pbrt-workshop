package de.gtrefs.coffeeshop.resilience;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

import eu.rekawek.toxiproxy.*;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.*;
import io.restassured.filter.log.*;
import io.restassured.http.*;
import io.restassured.specification.*;
import org.junit.platform.commons.util.*;
import org.slf4j.*;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.*;
import org.testcontainers.containers.wait.strategy.*;
import org.testcontainers.images.builder.*;
import org.testcontainers.utility.*;

import net.jqwik.api.lifecycle.*;

import static io.restassured.RestAssured.*;
import static org.testcontainers.containers.wait.strategy.Wait.*;

public  class CoffeeShopWithFaults {

	static Logger logger = LoggerFactory.getLogger(CoffeeShopWithFaults.class);

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

	// Toxiproxy container, which will be used as a TCP proxy
	static ToxiproxyContainer toxiProxy = new ToxiproxyContainer(DockerImageName.parse("shopify/toxiproxy:2.1.0"))
			.withNetwork(network)
			.dependsOn(postgresqlContainer)
			.withNetworkAliases("toxiproxyPostgres");

	protected static GenericContainer coffeeContainer = new GenericContainer(coffeeImage)
			.withNetwork(network)
			.withNetworkAliases("barista")
			.dependsOn(toxiProxy)
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

	protected static EnableAndDisableProxy postgresProxy;

	static {
		start();
	}

	protected static void start(){
		startCoffeeContainerWithToxic();
		orderContainer.start();
		coffeeContainer.followOutput(new Slf4jLogConsumer(logger));
		orderContainer.followOutput(new Slf4jLogConsumer(logger));
		paymentContainer.followOutput(new Slf4jLogConsumer(logger));
	}

	private static void startCoffeeContainerWithToxic() {
		toxiProxy.start();
		postgresProxy = new EnableAndDisableProxy(toxiProxy, postgresqlContainer, PostgreSQLContainer.POSTGRESQL_PORT);
		coffeeContainer
				.withEnv("POSTGRES_HOST", "toxiproxyPostgres")
				.withEnv("POSTGRES_PORT", postgresProxy.containerProxyPort() + "")
				.start();
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

	protected static class EnableAndDisableProxy {

		private final ToxiproxyContainer.ContainerProxy proxyFromContainers;
		private final Proxy proxyFromToxic;

		EnableAndDisableProxy(ToxiproxyContainer proxyContainer, GenericContainer<?> toBeProxied, int port) {
			proxyFromContainers = proxyContainer.getProxy(toBeProxied, port);
			proxyFromToxic = getToxiProxyFromContainerWrapper(proxyFromContainers);
		}

		private Proxy getToxiProxyFromContainerWrapper(ToxiproxyContainer.ContainerProxy proxy) {
			// Getting the actual proxy by reflection is not what should be done.
			// The ContainerProxy class should provide the possibility to enable or disable the proxy.
			// Lowering the bandwidth keeps the connection alive. This might not be the toxic which is intended with
			// a "cut". See also https://github.com/testcontainers/testcontainers-java/issues/3453
			List<Field> toxicProxyField = ReflectionUtils.findFields(proxy.getClass(),
																	 field -> field.getName().equals("toxi"),
																	 ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);
			return (Proxy) ReflectionUtils.readFieldValues(toxicProxyField, proxy).get(0);
		}

		public void lowerBandwidthToZero(){
			proxyFromContainers.setConnectionCut(true);
		}

		public void allowFullBandwidth(){
			proxyFromContainers.setConnectionCut(false);
		}

		public void disable(){
			try {
				proxyFromToxic.disable();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void enable() {
			try {
				proxyFromToxic.enable();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public ToxicList toxics(){
			return proxyFromToxic.toxics();
		}

		public int containerProxyPort(){
			return proxyFromContainers.getOriginalProxyPort();
		}

		public String containerIpAddress(){
			return proxyFromContainers.getContainerIpAddress();
		}
	}

}

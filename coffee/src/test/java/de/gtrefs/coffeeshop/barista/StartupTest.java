package de.gtrefs.coffeeshop.barista;

import org.junit.jupiter.api.*;
import org.slf4j.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;

@SpringBootTest
public class StartupTest {
	private final static Logger logger = LoggerFactory.getLogger(StartupTest.class);

	// Only interested if the service comes up at all
	@MockBean
	CoffeeRepository coffeeRepository;

	@Test
	public void should_start(){
		logger.info("Barista service successfully started.");
	}
}

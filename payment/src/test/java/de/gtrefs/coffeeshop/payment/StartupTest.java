package de.gtrefs.coffeeshop.payment;

import org.junit.jupiter.api.*;
import org.slf4j.*;
import org.springframework.boot.test.context.*;

@SpringBootTest
public class StartupTest {
	private final static Logger logger = LoggerFactory.getLogger(StartupTest.class);


	@Test
	public void should_start(){
		logger.info("Payment service successfully started.");
	}
}

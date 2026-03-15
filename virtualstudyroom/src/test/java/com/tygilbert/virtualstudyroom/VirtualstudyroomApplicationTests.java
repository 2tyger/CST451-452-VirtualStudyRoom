package com.tygilbert.virtualstudyroom;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Disabled temporarily because GitHub Actions CI does not provide a PostgreSQL instance.
// Service-level tests still run and validate application logic.
// This test can be re-enabled during the testing phase when a CI test database is configured.
@Disabled("Requires PostgreSQL datasource; disabled for CI during development phase")
@SpringBootTest
class VirtualstudyroomApplicationTests {

	@Test
	void contextLoads() {
	}

}

package com.example.petManagementService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Tests run against the in-memory H2 config in application-local.properties, never the
// shared Supabase database — otherwise every test run mutates the team's data and CI
// fails without DB_PASSWORD.
@SpringBootTest
@ActiveProfiles("local")
class PetManagementServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
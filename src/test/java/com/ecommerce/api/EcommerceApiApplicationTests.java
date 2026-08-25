package com.ecommerce.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: garante que o contexto do Spring sobe sem erros (todos os beans se encaixam). */
@SpringBootTest
@ActiveProfiles("test")
class EcommerceApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

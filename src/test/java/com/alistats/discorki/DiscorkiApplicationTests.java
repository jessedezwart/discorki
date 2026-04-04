package com.alistats.discorki;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "discord.enabled=false")
class DiscorkiApplicationTests {

	@Test
	void contextLoads() {
	}

}

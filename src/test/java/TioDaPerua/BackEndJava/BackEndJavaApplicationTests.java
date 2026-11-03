package TioDaPerua.BackEndJava;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"app.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
		"app.admin.bootstrap.enabled=false"
})
class BackEndJavaApplicationTests {

	@Test
	void contextLoads() {
	}

}

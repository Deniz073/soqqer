package nl.quintor.soqqer.config;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("it")
@Import(TestcontainersConfiguration.class)
public abstract class BaseITTest {

    @LocalServerPort
    protected int port;
}

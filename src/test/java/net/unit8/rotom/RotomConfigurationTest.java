package net.unit8.rotom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RotomConfigurationTest {

    @Test
    void defaultBasePathIsEmpty() {
        RotomConfiguration config = new RotomConfiguration();
        assertEquals("", config.getBasePath());
    }

    @Test
    void setBasePathIsRetained() {
        RotomConfiguration config = new RotomConfiguration();
        config.setBasePath("/wiki");
        assertEquals("/wiki", config.getBasePath());
    }

    @Test
    void defaultAuthBackendIsNotNull() {
        RotomConfiguration config = new RotomConfiguration();
        assertNotNull(config.getAuthBackend());
    }

    @Test
    void defaultUnauthEndpointIsNotNull() {
        RotomConfiguration config = new RotomConfiguration();
        assertNotNull(config.getUnauthEndpoint());
    }
}

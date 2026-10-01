package dev.temper.cerebro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"temper.indicators.enabled=false","temper.toxicity.model-dir=","temper.sentiment.model-dir=","temper.foundation.model-dir=","temper.emotion.model-dir=","temper.sarcasm.model-dir="})
@AutoConfigureMockMvc
class HealthControllerTest {
    @Autowired MockMvc mvc;
    @Test void healthUsesStableContract() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("TEMPER"))
            .andExpect(jsonPath("$.analysisMode").value("NONE"));
    }
    @Test void actuatorAvailableWithoutDatabaseOrModels() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}

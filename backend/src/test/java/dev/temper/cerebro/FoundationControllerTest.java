package dev.temper.cerebro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"temper.sentiment.model-dir=","temper.foundation.model-dir="})
@AutoConfigureMockMvc
class FoundationControllerTest {
    @Autowired MockMvc mvc;
    @Test void missingModelIsHonestUnavailableAndBlankInputRejected() throws Exception {
        mvc.perform(post("/api/v1/ai/foundation/classify").contentType("application/json").content("{\"text\":\"A wonderful movie\"}"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/v1/ai/foundation/classify").contentType("application/json").content("{\"text\":\" \"}"))
            .andExpect(status().isBadRequest());
    }
}


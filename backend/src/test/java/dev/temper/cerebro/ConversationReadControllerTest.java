package dev.temper.cerebro;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import dev.temper.cerebro.common.DemoSeed;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"temper.sentiment.model-dir=","temper.foundation.model-dir=","temper.emotion.model-dir=","temper.sarcasm.model-dir="})
@AutoConfigureMockMvc
class ConversationReadControllerTest {
    @Autowired MockMvc mvc;
    @Test void versionedDomainReadsReturnSeededParticipantsAndMessageCountWithoutAnalysis() throws Exception {
        mvc.perform(get("/api/v1/conversations")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(DemoSeed.CONVERSATION_ID.toString())).andExpect(jsonPath("$[0].messageCount").value(7));
        mvc.perform(get("/api/v1/conversations/" + DemoSeed.CONVERSATION_ID)).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("The launch plan")).andExpect(jsonPath("$.participants[0].id").value("alex"))
            .andExpect(jsonPath("$.participants[1].variant").value("female")).andExpect(jsonPath("$.analysisStatus").value("NONE"));
    }
    @Test void missingAndMalformedConversationIdsAreDistinctErrors() throws Exception {
        mvc.perform(get("/api/v1/conversations/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/conversations/not-a-uuid")).andExpect(status().isBadRequest());
    }
}

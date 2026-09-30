package dev.temper.cerebro;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import dev.temper.cerebro.conversation.service.ConversationService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc
class RestApiTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired ConversationService service;
    String create() throws Exception {return json.readTree(mvc.perform(post("/api/v1/conversations").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"REST test\",\"participantIds\":[\"alex\",\"nova\"]}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();}
    @Test void actualDeliveryAnalysisAndAllReadContracts() throws Exception {
        String room=create(),base="/api/v1/conversations/"+room;
        mvc.perform(get(base+"/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.conflictScore").doesNotExist()).andExpect(jsonPath("$.messages").isEmpty());
        var response=mvc.perform(post(base+"/messages").contentType(MediaType.APPLICATION_JSON).content("{\"speakerId\":\"alex\",\"text\":\" Hello \"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.sequence").value(1)).andExpect(jsonPath("$.text").value("Hello")).andReturn();
        String id=json.readTree(response.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/v1/messages/"+id+"/analysis")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ANALYSIS_UNAVAILABLE"));
        mvc.perform(get(base+"/messages")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(post("/api/v1/messages/"+id+"/analyze")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("MOCK")).andExpect(jsonPath("$.evidence[0].source").value("MOCK"));
        mvc.perform(get("/api/v1/messages/"+id+"/analysis")).andExpect(status().isOk()).andExpect(jsonPath("$.messageId").value(id));
        mvc.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$.analysisStatus").value("MOCK"));
        for(String route:List.of("analytics","timeline","insights"))mvc.perform(get(base+"/"+route)).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("MOCK")).andExpect(jsonPath("$.pendingMessages").value(0));
    }
    @Test void invalidRequestsNeverCreateTurnsAndDoNotEchoPrivateText() throws Exception {
        String base="/api/v1/conversations/"+create();
        for(String body:List.of("{\"speakerId\":\"stranger\",\"text\":\"private secret\"}","{\"speakerId\":\"alex\",\"text\":\" \"}","{\"speakerId\":\"alex\",\"text\":\""+"x".repeat(2001)+"\"}","bad-json")) {
            String output=mvc.perform(post(base+"/messages").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andReturn().getResponse().getContentAsString();assertFalse(output.contains("private secret"));
        }
        mvc.perform(get(base+"/messages")).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/v1/conversations").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"test\",\"participantIds\":[\"alex\",\"alex\"]}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/messages/"+UUID.randomUUID()+"/analysis")).andExpect(status().isNotFound());
    }
    @Test void concurrentApplicationSendsAllocateUniqueContiguousSequences() throws Exception {
        var id=UUID.fromString(create());
        try(var pool=Executors.newFixedThreadPool(8)){var futures=new ArrayList<Future<?>>();for(int i=0;i<80;i++)futures.add(pool.submit(()->service.send(id,"alex","Concurrent fictional turn")));for(var f:futures)f.get(10,TimeUnit.SECONDS);}
        var messages=service.messages(id);assertEquals(80,messages.size());for(int i=0;i<80;i++)assertEquals(i+1,messages.get(i).sequence());
    }
    @Test void actualAnalysisReceivesPreviousTurnsWithSpeakerAndStoresOnlyCausalReferences() throws Exception {
        UUID room=UUID.fromString(create());var turns=new ArrayList<dev.temper.cerebro.chat.domain.Message>();for(int i=0;i<8;i++)turns.add(service.send(room,i%2==0?"nova":"alex",i==6?"Fine.":"Fictional history "+i));
        String base="/api/v1/messages/"+turns.get(6).id();
        mvc.perform(get(base+"/context")).andExpect(status().isOk()).andExpect(jsonPath("$.current.text").value("Fine.")).andExpect(jsonPath("$.current.speaker.displayName").value("Nova")).andExpect(jsonPath("$.previous.length()").value(5)).andExpect(jsonPath("$.previous[0].sequence").value(2)).andExpect(jsonPath("$.previous[4].sequence").value(6));
        mvc.perform(post(base+"/analyze")).andExpect(status().isOk()).andExpect(jsonPath("$.contextMessageIds.length()").value(5)).andExpect(jsonPath("$.contextMessageIds[0]").value(turns.get(1).id().toString())).andExpect(jsonPath("$.contextMessageIds[4]").value(turns.get(5).id().toString())).andExpect(jsonPath("$.mode").value("MOCK"));
    }
}

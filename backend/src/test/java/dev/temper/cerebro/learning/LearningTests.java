package dev.temper.cerebro.learning;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

class LearningTests {
    @TempDir Path directory;
    private final ObjectMapper mapper=new ObjectMapper();
    private final String token=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    private EncryptedLearningStore store()throws Exception{return new EncryptedLearningStore(directory,new byte[32],Clock.systemUTC());}
    private ObjectNode body()throws Exception{return (ObjectNode)mapper.readTree("""
        {"consentVersion":1,"adultConfirmed":true,"participantPermissionConfirmed":true,"textReviewed":true,
         "sessionId":"10000000-0000-0000-0000-000000000001","language":"OTHER","appVersion":"0.42.0","modelHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","qualityRating":4,
         "turns":[{"index":0,"role":"LOCAL","text":"How did the fictional meeting go?"},{"index":1,"role":"REMOTE","text":"I am very happy with this fictional project."},{"index":2,"role":"LOCAL","text":"Congratulations. Contact fake@example.invalid"}],
         "analyses":[{"target":1,"context":[0,1,2],"scores":[0.01,0.9,0.01,0.02,0.01,0.01,0.01,0.02],"currentState":"Happy language estimated","direction":"Direction uncertain","trajectory":"UNCERTAIN"}]}
        """);}
    @Test void explicitRatedSessionIsEncryptedAndIdempotent()throws Exception{
        var store=store();var submission=LearningSubmission.parse(body());assertTrue(store.save(token,submission));assertFalse(store.save(token,submission));var records=store.records();assertEquals(1,records.size());assertEquals(4,records.getFirst().path("submission").path("qualityRating").asInt());assertTrue(records.getFirst().toString().contains("[redacted]"));assertFalse(records.getFirst().toString().contains("fake@example.invalid"));
        try(var files=Files.walk(directory)){for(Path file:files.filter(Files::isRegularFile).toList())assertFalse(new String(Files.readAllBytes(file),StandardCharsets.ISO_8859_1).contains("fictional project"));}
    }
    @Test void deletionIsContributorScopedAndWorksAfterConsentWithdrawal()throws Exception{
        var store=store();byte[] other=new byte[32];other[0]=1;String second=Base64.getUrlEncoder().withoutPadding().encodeToString(other);store.save(token,LearningSubmission.parse(body()));store.save(second,LearningSubmission.parse(body()));store.delete(token);assertEquals(1,store.records().size());assertEquals(EncryptedLearningStore.contributor(second),store.records().getFirst().path("contributor").asText());assertThrows(SecurityException.class,()->store.save(token,LearningSubmission.parse(body())));store.delete(token);store.delete(second);assertTrue(store.records().isEmpty());
    }
    @Test void expiredRecordsArePurgedAndTamperingFailsClosed()throws Exception{
        var store=store();store.save(token,LearningSubmission.parse(body()));Path file;try(var files=Files.walk(directory)){file=files.filter(Files::isRegularFile).findFirst().orElseThrow();}byte[] data=Files.readAllBytes(file);data[data.length-1]^=1;Files.write(file,data);assertThrows(Exception.class,store::records);Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.from(Instant.now().minus(Duration.ofDays(91))));store.purge();assertTrue(store.records().isEmpty());
    }
    @Test void noRatingNoPermissionAndInvalidContextAreRejected()throws Exception{
        for(String flag:List.of("adultConfirmed","participantPermissionConfirmed","textReviewed")){var body=body();body.put(flag,false);assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(body));}
        for(int rating:new int[]{0,6}){var body=body();body.put("qualityRating",rating);assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(body));}
        var fractional=body();fractional.put("qualityRating",1.5);assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(fractional));var textRating=body();textRating.put("qualityRating","5");assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(textRating));var textPermission=body();textPermission.put("textReviewed","true");assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(textPermission));var overflow=body();((com.fasterxml.jackson.databind.node.ArrayNode)overflow.path("analyses").get(0).path("context")).set(0,mapper.getNodeFactory().numberNode(4294967296L));assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(overflow));
        var body=body();((ObjectNode)body.path("analyses").get(0)).put("target",2);assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(body));assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(mapper.nullNode()));
    }
    @Test void httpBoundarySubmissionAndDeletionWorkWithoutExposingRecords()throws Exception{
        var store=store();var mvc=MockMvcBuilders.standaloneSetup(new LearningController(store)).addFilters(new LearningBoundaryFilter()).build();
        mvc.perform(post("/api/learning/sessions").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body().toString())).andExpect(status().isCreated()).andExpect(jsonPath("$.stored").value(true)).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/learning/sessions").header("Authorization","Bearer "+token)).andExpect(status().isNotFound());mvc.perform(get("/api/messages")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/learning/contributions").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));assertTrue(store.records().isEmpty());
    }
    @Test void oversizedAndUnauthenticatedInputFailsBeforeStorage()throws Exception{
        var store=store();var mvc=MockMvcBuilders.standaloneSetup(new LearningController(store)).addFilters(new LearningBoundaryFilter()).build();mvc.perform(post("/api/learning/sessions").contentType(MediaType.APPLICATION_JSON).content(body().toString())).andExpect(status().isUnauthorized());mvc.perform(post("/api/learning/sessions").header("Authorization","Bearer "+token).content(new byte[384001])).andExpect(status().isPayloadTooLarge());assertTrue(store.records().isEmpty());
    }
    @Test void quotaAndInvalidTokensAreEnforced()throws Exception{
        var store=store();assertThrows(SecurityException.class,()->store.delete("invalid"));for(int i=0;i<20;i++){var body=body();body.put("sessionId",new UUID(0,i).toString());store.save(token,LearningSubmission.parse(body));}var body=body();body.put("sessionId",new UUID(0,21).toString());assertThrows(IllegalStateException.class,()->store.save(token,LearningSubmission.parse(body)));
    }
    @Test void offlineExportContainsOnlyScopedActiveRecords()throws Exception{
        var store=store();store.save(token,LearningSubmission.parse(body()));Path key=directory.resolve("key.txt"),output=directory.resolve("private-export.jsonl");Files.writeString(key,Base64.getEncoder().encodeToString(new byte[32]));LearningExport.main(new String[]{directory.toString(),key.toString(),output.toString()});assertEquals(1,Files.readAllLines(output).size());assertFalse(Files.readString(output).contains("fake@example.invalid"));assertThrows(IllegalArgumentException.class,()->LearningExport.main(new String[]{directory.toString(),key.toString(),output.toString()}));
    }
    @Test void enabledDeploymentWiresActualControllerAndBoundary()throws Exception{
        Path key=directory.resolve("context-key.txt");Files.writeString(key,Base64.getEncoder().encodeToString(new byte[32]));
        new org.springframework.boot.test.context.runner.WebApplicationContextRunner().withUserConfiguration(dev.temper.cerebro.TemperApplication.class)
            .withPropertyValues("temper.learning.enabled=true","temper.learning.directory="+directory.resolve("records"),"temper.learning.key-file="+key,"temper.store.enabled=false","temper.demo.enabled=false")
            .run(context->{assertNull(context.getStartupFailure());assertNotNull(context.getBean(LearningController.class));var mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(LearningBoundaryFilter.class)).build();mvc.perform(get("/api/messages")).andExpect(status().isNotFound());mvc.perform(post("/api/learning/sessions").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body().toString())).andExpect(status().isCreated());mvc.perform(delete("/api/learning/contributions").header("Authorization","Bearer "+token)).andExpect(status().isOk());});
    }
}

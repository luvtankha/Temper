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
    @Test void duplicateFieldsTrailingDocumentsAndCoercedMetadataAreRejected()throws Exception{
        var store=store();var mvc=MockMvcBuilders.standaloneSetup(new LearningController(store)).addFilters(new LearningBoundaryFilter()).build();
        for(String invalid:List.of(body().toString().replace("\"qualityRating\":4","\"qualityRating\":1,\"qualityRating\":4"),body()+" {}",body()+" null"))mvc.perform(post("/api/learning/sessions").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        var numericVersion=body();numericVersion.put("appVersion",42.3);assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(numericVersion));
        var numericSummary=body();((ObjectNode)numericSummary.path("analyses").get(0)).put("currentState",42);assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(numericSummary));
        var invalidVersion=body();invalidVersion.put("appVersion","...");assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(invalidVersion));assertTrue(store.records().isEmpty());
    }
    @Test void summariesAreRedactedAndControlOnlyTextIsRejected()throws Exception{
        var body=body();var estimate=(ObjectNode)body.path("analyses").get(0);estimate.put("currentState","Contact fake@example.invalid");estimate.put("direction","Visit https://fictional.invalid/path");var submission=LearningSubmission.parse(body);assertFalse(submission.analyses().getFirst().currentState().contains("fake@"));assertEquals("Visit [redacted]",submission.analyses().getFirst().direction());
        var controlOnly=body();((ObjectNode)controlOnly.path("turns").get(0)).put("text","\u0000\u0001");assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(controlOnly));
    }
    @Test void contributorSlotsBoundDeletionFilesAndStillAllowExistingOwnerDeletion()throws Exception{
        var store=new EncryptedLearningStore(directory,new byte[32],Clock.systemUTC(),2);String second=token(1),third=token(2);store.save(token,LearningSubmission.parse(body()));store.delete(second);
        assertThrows(IllegalStateException.class,()->store.save(third,LearningSubmission.parse(body())));assertThrows(IllegalStateException.class,()->store.delete(third));
        var mvc=MockMvcBuilders.standaloneSetup(new LearningController(store)).addFilters(new LearningBoundaryFilter()).build();mvc.perform(delete("/api/learning/contributions").header("Authorization","Bearer "+third)).andExpect(status().isTooManyRequests());
        store.delete(token);store.delete(second);assertThrows(SecurityException.class,()->store.save(token,LearningSubmission.parse(body())));assertTrue(store.records().isEmpty());try(var files=Files.list(directory)){assertEquals(2,files.count());}
    }
    @Test void expiryRemovesEmptyDirectoriesAndUsesAuthenticatedExpiryOnExport()throws Exception{
        var clock=new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));var store=new EncryptedLearningStore(directory,new byte[32],clock,2);store.save(token,LearningSubmission.parse(body()));Path folder=directory.resolve(EncryptedLearningStore.contributor(token)),file=folder.resolve(body().path("sessionId").asText()+".enc");
        clock.now=clock.now.plus(Duration.ofDays(90));Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.from(clock.now));assertTrue(store.records().isEmpty());store.purge();assertFalse(Files.exists(folder));
        store.save(token,LearningSubmission.parse(body()));clock.now=clock.now.plus(Duration.ofDays(90));store.purge();assertFalse(Files.exists(folder));
    }
    @Test void interruptedDeletionCannotExportRevokedRemainders()throws Exception{
        var store=store();store.save(token,LearningSubmission.parse(body()));Path folder=directory.resolve(EncryptedLearningStore.contributor(token)),file=folder.resolve(body().path("sessionId").asText()+".enc");byte[] encrypted=Files.readAllBytes(file);Path blocker=Files.createDirectory(folder.resolve("unfinished-cleanup"));
        assertThrows(SecurityException.class,()->store.delete(token));Files.write(file,encrypted);assertTrue(store.records().isEmpty());assertThrows(SecurityException.class,()->store.save(token,LearningSubmission.parse(body())));Files.delete(blocker);store.delete(token);assertFalse(Files.exists(folder));
    }
    @Test void failedExportDoesNotLeaveAUsablePartialSnapshot()throws Exception{
        var store=store();store.save(token,LearningSubmission.parse(body()));Path folder=directory.resolve(EncryptedLearningStore.contributor(token)),file=folder.resolve(body().path("sessionId").asText()+".enc");byte[] encrypted=Files.readAllBytes(file);encrypted[encrypted.length-1]^=1;Files.write(file,encrypted);
        Path key=directory.resolve("export-key.txt"),output=directory.resolve("failed-export.jsonl");Files.writeString(key,Base64.getEncoder().encodeToString(new byte[32]));assertThrows(Exception.class,()->LearningExport.main(new String[]{directory.toString(),key.toString(),output.toString()}));assertFalse(Files.exists(output));try(var files=Files.list(directory)){assertTrue(files.noneMatch(p->p.getFileName().toString().endsWith(".partial")));}
    }
    @Test void nonCanonicalTokensAndUnsafeInternalSessionIdentityAreRejected()throws Exception{
        var store=store();assertThrows(SecurityException.class,()->store.delete(token.substring(0,42)+"B"));var valid=LearningSubmission.parse(body());var unsafe=new LearningSubmission("../escape",valid.language(),valid.appVersion(),valid.modelHash(),valid.qualityRating(),valid.turns(),valid.analyses());assertThrows(IllegalArgumentException.class,()->store.save(token,unsafe));assertTrue(store.records().isEmpty());
    }
    @Test void arbitraryPrecisionConsentAndTurnIndicesCannotWrapIntoValidValues()throws Exception{
        var consent=body();consent.set("consentVersion",mapper.getNodeFactory().numberNode(new java.math.BigInteger("18446744073709551617")));assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(consent));
        var turn=body();((ObjectNode)turn.path("turns").get(0)).set("index",mapper.getNodeFactory().numberNode(new java.math.BigInteger("18446744073709551616")));assertThrows(IllegalArgumentException.class,()->LearningSubmission.parse(turn));
    }
    private static String token(int value){byte[] data=new byte[32];data[0]=(byte)value;return Base64.getUrlEncoder().withoutPadding().encodeToString(data);}
    private static final class MutableClock extends Clock{
        Instant now;MutableClock(Instant now){this.now=now;}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}
    }
}

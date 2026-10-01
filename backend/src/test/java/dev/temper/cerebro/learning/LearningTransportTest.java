package dev.temper.cerebro.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import javax.net.ssl.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Actual TLS/servlet stack with generated fictional text and ephemeral local-only credentials. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class LearningTransportTest {
    private static final Path directory=createDirectory();
    private static final String password=UUID.randomUUID().toString();
    private static final Path certificate=directory.resolve("localhost.p12"),key=directory.resolve("storage-secret.txt");
    @LocalServerPort int port;

    private static Path createDirectory(){try{return Files.createTempDirectory("temper-fictional-tls-");}catch(Exception error){throw new IllegalStateException(error);}}
    @DynamicPropertySource static void configure(DynamicPropertyRegistry properties)throws Exception{
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);Files.writeString(key,Base64.getEncoder().encodeToString(random));
        String tool=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"keytool.exe":"keytool").toString();
        Process process=new ProcessBuilder(tool,"-genkeypair","-alias","localhost","-keyalg","RSA","-keysize","2048","-validity","2","-dname","CN=localhost","-ext","SAN=dns:localhost,ip:127.0.0.1","-storetype","PKCS12","-keystore",certificate.toString(),"-storepass",password,"-keypass",password,"-noprompt").redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if(!process.waitFor(20,TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("Test TLS certificate generation timed out");}if(process.exitValue()!=0)throw new IllegalStateException("Test TLS certificate generation failed");
        properties.add("server.address",()->"127.0.0.1");properties.add("server.ssl.enabled",()->true);properties.add("server.ssl.key-store",()->certificate.toUri().toString());properties.add("server.ssl.key-store-type",()->"PKCS12");properties.add("server.ssl.key-store-password",()->password);properties.add("server.ssl.key-alias",()->"localhost");
        properties.add("temper.learning.enabled",()->true);properties.add("temper.learning.directory",()->directory.resolve("records").toString());properties.add("temper.learning.key-file",()->key.toString());properties.add("temper.store.enabled",()->false);properties.add("temper.demo.enabled",()->false);
    }
    private HttpClient client()throws Exception{
        KeyStore generated=KeyStore.getInstance("PKCS12");try(var input=Files.newInputStream(certificate)){generated.load(input,password.toCharArray());}
        KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null,null);trust.setCertificateEntry("localhost",generated.getCertificate("localhost"));TrustManagerFactory manager=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());manager.init(trust);SSLContext tls=SSLContext.getInstance("TLS");tls.init(null,manager.getTrustManagers(),null);
        return HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private HttpResponse<String> request(HttpClient client,String method,String path,String token,byte[] body)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("https://localhost:"+port+path)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json");if(token!=null)request.header("Authorization","Bearer "+token);request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body));return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void fictionalSubmissionAndDeletionWorkThroughTlsAndServletFilters()throws Exception{
        HttpClient client=client();byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        byte[] body="""
            {"consentVersion":1,"adultConfirmed":true,"participantPermissionConfirmed":true,"textReviewed":true,
             "sessionId":"10000000-0000-0000-0000-000000000043","language":"OTHER","appVersion":"0.43.0","modelHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","qualityRating":4,
             "turns":[{"index":0,"role":"LOCAL","text":"How was the fictional project?"},{"index":1,"role":"REMOTE","text":"I am happy with this fictional result."},{"index":2,"role":"LOCAL","text":"Congratulations."}],
             "analyses":[{"target":1,"context":[0,1,2],"scores":[0.01,0.9,0.01,0.02,0.01,0.01,0.01,0.02],"currentState":"Happy language estimated","direction":"Direction uncertain","trajectory":"UNCERTAIN"}]}
            """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(200,request(client,"GET","/actuator/health",null,null).statusCode());assertEquals(404,request(client,"GET","/api/messages",null,null).statusCode());assertEquals(401,request(client,"POST","/api/learning/sessions",null,body).statusCode());assertEquals(413,request(client,"POST","/api/learning/sessions",token,new byte[384001]).statusCode());
        var stored=request(client,"POST","/api/learning/sessions",token,body);assertEquals(201,stored.statusCode());assertTrue(new ObjectMapper().readTree(stored.body()).path("stored").asBoolean());assertEquals("no-store",stored.headers().firstValue("Cache-Control").orElseThrow());assertEquals(200,request(client,"POST","/api/learning/sessions",token,body).statusCode());
        var deleted=request(client,"DELETE","/api/learning/contributions",token,null);assertEquals(200,deleted.statusCode());assertTrue(new ObjectMapper().readTree(deleted.body()).path("deleted").asBoolean());assertEquals(401,request(client,"POST","/api/learning/sessions",token,body).statusCode());
        try(var files=Files.walk(directory.resolve("records"))){assertEquals(0,files.filter(path->path.toString().endsWith(".enc")).count());}
    }
    @AfterAll static void removeGeneratedCredentials()throws Exception{
        try(var paths=Files.walk(directory)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
    }
}

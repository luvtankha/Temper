package dev.temper.cerebro.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/** Single-instance encrypted store. No public read/export endpoint or plaintext record files. */
public final class EncryptedLearningStore {
    public static final int RETENTION_DAYS=90;
    public static final int MAX_CONTRIBUTORS=5000;
    public static final int MAX_RECORD_BYTES=400_000;
    private final Path root;private final SecretKeySpec key;private final Clock clock;private final int contributorLimit;private final ObjectMapper mapper=new ObjectMapper();
    public EncryptedLearningStore(Path directory,byte[] encryptionKey,Clock clock)throws Exception{
        this(directory,encryptionKey,clock,MAX_CONTRIBUTORS);
    }
    EncryptedLearningStore(Path directory,byte[] encryptionKey,Clock clock,int contributorLimit)throws Exception{
        if(encryptionKey.length!=32)throw new IllegalArgumentException("A 256-bit storage key is required");if(contributorLimit<1||contributorLimit>MAX_CONTRIBUTORS)throw new IllegalArgumentException("Invalid contributor limit");root=directory.toAbsolutePath().normalize();key=new SecretKeySpec(encryptionKey.clone(),"AES");this.clock=clock;this.contributorLimit=contributorLimit;
        for(Path part=root;part!=null;part=part.getParent())if(Files.isSymbolicLink(part))throw new IllegalArgumentException("Storage must not use symbolic links");
        Files.createDirectories(root);privatePermissions(root,true);purge();
    }
    private static void privatePermissions(Path path,boolean directory)throws Exception{if(path.getFileSystem().supportedFileAttributeViews().contains("posix"))Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(directory?"rwx------":"rw-------"));}
    public static String contributor(String token)throws Exception{if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new SecurityException("Invalid contribution token");byte[] decoded=Base64.getUrlDecoder().decode(token);if(decoded.length!=32||!Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(token))throw new SecurityException("Invalid contribution token");return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));}
    private byte[] aad(String owner,String id){return ("temper-learning-v1:"+owner+":"+id).getBytes(StandardCharsets.US_ASCII);}
    private Path marker(String owner){return root.resolve("revoked-"+owner+".mark");}
    private void reserveContributor(String owner)throws Exception{
        if(Files.exists(root.resolve(owner),LinkOption.NOFOLLOW_LINKS)||Files.exists(marker(owner),LinkOption.NOFOLLOW_LINKS))return;
        Set<String> owners=new HashSet<>();try(var entries=Files.list(root)){for(Path entry:entries.toList()){String name=entry.getFileName().toString();if(name.matches("[a-f0-9]{64}"))owners.add(name);else if(name.matches("revoked-[a-f0-9]{64}\\.mark"))owners.add(name.substring(8,72));if(owners.size()>=contributorLimit)throw new IllegalStateException("Contributor capacity reached");}}
    }
    private static boolean storedRecord(Path file){String name=file.getFileName().toString();return name.endsWith(".enc")||name.matches("pending-.*\\.tmp");}
    public synchronized boolean save(String token,LearningSubmission submission)throws Exception{
        String owner=contributor(token);purge();if(Files.exists(marker(owner),LinkOption.NOFOLLOW_LINKS))throw new SecurityException("Contribution key was deleted");if(submission==null||submission.sessionId()==null||!submission.sessionId().matches("[a-f0-9-]{36}")||!UUID.fromString(submission.sessionId()).toString().equals(submission.sessionId()))throw new IllegalArgumentException("Invalid session identity");Path folder=root.resolve(owner);if(Files.isSymbolicLink(folder)||Files.exists(folder)&&!Files.isDirectory(folder))throw new SecurityException("Invalid storage path");Path file=folder.resolve(submission.sessionId()+".enc");if(Files.isSymbolicLink(file))throw new SecurityException("Invalid storage path");
        if(Files.exists(file))return false;
        reserveContributor(owner);
        if(Files.exists(folder))try(var files=Files.list(folder)){if(files.count()>=20)throw new IllegalStateException("Contribution quota reached");}
        try(var files=Files.walk(root)){if(files.filter(EncryptedLearningStore::storedRecord).count()>=5000)throw new IllegalStateException("Storage capacity reached");}
        Files.createDirectories(folder);privatePermissions(folder,true);
        var record=mapper.createObjectNode();record.put("schemaVersion",1);record.put("contributor",owner);record.put("receivedAt",clock.instant().toEpochMilli());record.put("expiresAt",clock.instant().plus(Duration.ofDays(RETENTION_DAYS)).toEpochMilli());record.set("submission",mapper.valueToTree(submission));
        byte[] plaintext=mapper.writeValueAsBytes(record);if(plaintext.length>MAX_RECORD_BYTES-29)throw new IllegalArgumentException("Invalid record bounds");byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(owner,submission.sessionId()));byte[] encrypted=cipher.doFinal(plaintext);byte[] output=new byte[13+encrypted.length];output[0]=1;System.arraycopy(nonce,0,output,1,12);System.arraycopy(encrypted,0,output,13,encrypted.length);
        Path partial=Files.createTempFile(folder,"pending-",".tmp");try{privatePermissions(partial,false);Files.write(partial,output);Files.move(partial,file,StandardCopyOption.ATOMIC_MOVE);Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.from(clock.instant()));}finally{Files.deleteIfExists(partial);}return true;
    }
    public synchronized void delete(String token)throws Exception{String owner=contributor(token);purge();Path marker=marker(owner);if(Files.isSymbolicLink(marker))throw new SecurityException("Invalid storage path");reserveContributor(owner);if(!Files.exists(marker)){Files.write(marker,new byte[0],StandardOpenOption.CREATE_NEW);privatePermissions(marker,false);Files.setLastModifiedTime(marker,java.nio.file.attribute.FileTime.from(clock.instant()));}Path folder=root.resolve(owner);if(!Files.exists(folder))return;if(Files.isSymbolicLink(folder))throw new SecurityException("Invalid storage path");try(var files=Files.list(folder)){for(Path file:files.toList()){if(Files.isSymbolicLink(file)||!Files.isRegularFile(file))throw new SecurityException("Invalid stored record");Files.delete(file);}}Files.delete(folder);}
    @FunctionalInterface public interface RecordConsumer{void accept(com.fasterxml.jackson.databind.JsonNode record)throws Exception;}
    public synchronized List<com.fasterxml.jackson.databind.JsonNode> records()throws Exception{List<com.fasterxml.jackson.databind.JsonNode> result=new ArrayList<>();forEachRecord(result::add);return result;}
    public synchronized void forEachRecord(RecordConsumer consumer)throws Exception{
        purge();try(var folders=Files.list(root)){for(Path folder:folders.toList()){if(!Files.isDirectory(folder)||!folder.getFileName().toString().matches("[a-f0-9]{64}")||Files.isSymbolicLink(folder)||Files.exists(marker(folder.getFileName().toString()),LinkOption.NOFOLLOW_LINKS))continue;try(var files=Files.list(folder)){for(Path file:files.filter(p->p.getFileName().toString().endsWith(".enc")).toList()){if(Files.isSymbolicLink(file)||Files.size(file)>MAX_RECORD_BYTES)throw new SecurityException("Invalid stored record");String owner=folder.getFileName().toString(),id=file.getFileName().toString().replace(".enc","");byte[] input=Files.readAllBytes(file);if(input.length<29||input[0]!=1)throw new SecurityException("Invalid encrypted record");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(input,1,13)));cipher.updateAAD(aad(owner,id));var record=mapper.readTree(cipher.doFinal(Arrays.copyOfRange(input,13,input.length)));if(!record.path("expiresAt").isIntegralNumber()||!record.path("expiresAt").canConvertToLong())throw new SecurityException("Invalid stored expiry");if(record.path("expiresAt").asLong()<=clock.millis()){Files.delete(file);continue;}consumer.accept(record);}}}}
    }
    public synchronized void purge()throws Exception{
        Instant cutoff=clock.instant().minus(Duration.ofDays(RETENTION_DAYS));try(var files=Files.walk(root)){for(Path file:files.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).toList())if((storedRecord(file)||file.getFileName().toString().matches("revoked-[a-f0-9]{64}\\.mark"))&&!Files.getLastModifiedTime(file).toInstant().isAfter(cutoff))Files.delete(file);}
        try(var folders=Files.list(root)){for(Path folder:folders.toList())if(folder.getFileName().toString().matches("[a-f0-9]{64}")&&Files.isDirectory(folder,LinkOption.NOFOLLOW_LINKS))try(var files=Files.list(folder)){if(files.findAny().isEmpty())Files.delete(folder);}}
    }
}

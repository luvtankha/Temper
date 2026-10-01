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
    private final Path root;private final SecretKeySpec key;private final Clock clock;private final ObjectMapper mapper=new ObjectMapper();
    public EncryptedLearningStore(Path directory,byte[] encryptionKey,Clock clock)throws Exception{
        if(encryptionKey.length!=32)throw new IllegalArgumentException("A 256-bit storage key is required");root=directory.toAbsolutePath().normalize();key=new SecretKeySpec(encryptionKey.clone(),"AES");this.clock=clock;
        for(Path part=root;part!=null;part=part.getParent())if(Files.isSymbolicLink(part))throw new IllegalArgumentException("Storage must not use symbolic links");
        Files.createDirectories(root);privatePermissions(root,true);purge();
    }
    private static void privatePermissions(Path path,boolean directory)throws Exception{if(path.getFileSystem().supportedFileAttributeViews().contains("posix"))Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(directory?"rwx------":"rw-------"));}
    public static String contributor(String token)throws Exception{if(token==null||!token.matches("[A-Za-z0-9_-]{43}")||Base64.getUrlDecoder().decode(token).length!=32)throw new SecurityException("Invalid contribution token");return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));}
    private byte[] aad(String owner,String id){return ("temper-learning-v1:"+owner+":"+id).getBytes(StandardCharsets.US_ASCII);}
    public synchronized boolean save(String token,LearningSubmission submission)throws Exception{
        purge();String owner=contributor(token);if(Files.exists(root.resolve("revoked-"+owner+".mark")))throw new SecurityException("Contribution key was deleted");Path folder=root.resolve(owner);if(Files.isSymbolicLink(folder))throw new SecurityException("Invalid storage path");Path file=folder.resolve(submission.sessionId()+".enc");if(Files.isSymbolicLink(file))throw new SecurityException("Invalid storage path");
        if(Files.exists(file))return false;
        if(Files.exists(folder))try(var files=Files.list(folder)){if(files.count()>=20)throw new IllegalStateException("Contribution quota reached");}
        try(var files=Files.walk(root)){if(files.filter(p->p.getFileName().toString().endsWith(".enc")).count()>=5000)throw new IllegalStateException("Storage capacity reached");}
        Files.createDirectories(folder);privatePermissions(folder,true);
        var record=mapper.createObjectNode();record.put("schemaVersion",1);record.put("contributor",owner);record.put("receivedAt",clock.instant().toEpochMilli());record.put("expiresAt",clock.instant().plus(Duration.ofDays(RETENTION_DAYS)).toEpochMilli());record.set("submission",mapper.valueToTree(submission));
        byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(owner,submission.sessionId()));byte[] encrypted=cipher.doFinal(mapper.writeValueAsBytes(record));byte[] output=new byte[13+encrypted.length];output[0]=1;System.arraycopy(nonce,0,output,1,12);System.arraycopy(encrypted,0,output,13,encrypted.length);
        Path partial=Files.createTempFile(folder,"pending-",".tmp");try{privatePermissions(partial,false);Files.write(partial,output);Files.move(partial,file,StandardCopyOption.ATOMIC_MOVE);Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.from(clock.instant()));}finally{Files.deleteIfExists(partial);}return true;
    }
    public synchronized void delete(String token)throws Exception{String owner=contributor(token);Path marker=root.resolve("revoked-"+owner+".mark");if(Files.isSymbolicLink(marker))throw new SecurityException("Invalid storage path");if(!Files.exists(marker)){Files.write(marker,new byte[0],StandardOpenOption.CREATE_NEW);privatePermissions(marker,false);Files.setLastModifiedTime(marker,java.nio.file.attribute.FileTime.from(clock.instant()));}Path folder=root.resolve(owner);if(!Files.exists(folder))return;if(Files.isSymbolicLink(folder))throw new SecurityException("Invalid storage path");try(var files=Files.list(folder)){for(Path file:files.toList()){if(Files.isSymbolicLink(file)||!Files.isRegularFile(file))throw new SecurityException("Invalid stored record");Files.delete(file);}}Files.delete(folder);}
    @FunctionalInterface public interface RecordConsumer{void accept(com.fasterxml.jackson.databind.JsonNode record)throws Exception;}
    public synchronized List<com.fasterxml.jackson.databind.JsonNode> records()throws Exception{List<com.fasterxml.jackson.databind.JsonNode> result=new ArrayList<>();forEachRecord(result::add);return result;}
    public synchronized void forEachRecord(RecordConsumer consumer)throws Exception{
        purge();try(var folders=Files.list(root)){for(Path folder:folders.toList()){if(!Files.isDirectory(folder)||!folder.getFileName().toString().matches("[a-f0-9]{64}")||Files.isSymbolicLink(folder))continue;try(var files=Files.list(folder)){for(Path file:files.filter(p->p.getFileName().toString().endsWith(".enc")).toList()){if(Files.isSymbolicLink(file)||Files.size(file)>400_000)throw new SecurityException("Invalid stored record");String owner=folder.getFileName().toString(),id=file.getFileName().toString().replace(".enc","");byte[] input=Files.readAllBytes(file);if(input.length<29||input[0]!=1)throw new SecurityException("Invalid encrypted record");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(input,1,13)));cipher.updateAAD(aad(owner,id));consumer.accept(mapper.readTree(cipher.doFinal(Arrays.copyOfRange(input,13,input.length))));}}}}
    }
    public synchronized void purge()throws Exception{
        Instant cutoff=clock.instant().minus(Duration.ofDays(RETENTION_DAYS));try(var files=Files.walk(root)){for(Path file:files.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).toList())if((file.toString().endsWith(".enc")||file.toString().endsWith(".tmp")||file.toString().endsWith(".mark"))&&Files.getLastModifiedTime(file).toInstant().isBefore(cutoff))Files.delete(file);}
    }
}

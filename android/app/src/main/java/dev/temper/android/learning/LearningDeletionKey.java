package dev.temper.android.learning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Durable anonymous deletion key; all instances share the same file lock. */
public final class LearningDeletionKey {
    private static final Object LOCK=new Object();
    private final File file;
    public LearningDeletionKey(File file){this.file=file;}
    public String readOrCreate(boolean create)throws IOException {
        synchronized(LOCK){
            if(file.exists()){
                if(file.length()>60)throw new IOException("Invalid deletion key");
                String token=new String(Files.readAllBytes(file.toPath()),StandardCharsets.US_ASCII).strip();
                if(!token.matches("[A-Za-z0-9_-]{43}"))throw new IOException("Invalid deletion key");
                return token;
            }
            if(!create)return null;
            byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            if(!file.createNewFile())throw new IOException("Deletion key creation failed");
            try(var output=new FileOutputStream(file)){output.write(token.getBytes(StandardCharsets.US_ASCII));output.getFD().sync();}
            catch(IOException unavailable){Files.deleteIfExists(file.toPath());throw unavailable;}
            return token;
        }
    }
    public void forget(String expectedToken)throws IOException {
        synchronized(LOCK){
            if(!Objects.equals(expectedToken,readOrCreate(false)))throw new IOException("Deletion key changed; retry deletion");
            Files.deleteIfExists(file.toPath());
        }
    }
}

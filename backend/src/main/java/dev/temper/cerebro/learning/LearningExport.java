package dev.temper.cerebro.learning;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

/** Offline operator command, never exposed through HTTP. Output is private training data. */
public final class LearningExport {
    public static void main(String[] args)throws Exception{
        if(args.length!=3)throw new IllegalArgumentException("Usage: LearningExport encrypted-directory key-file new-private-jsonl-file");
        var store=new EncryptedLearningStore(Path.of(args[0]),Base64.getDecoder().decode(Files.readString(Path.of(args[1])).strip()),Clock.systemUTC());
        Path output=Path.of(args[2]).toAbsolutePath().normalize();if(Files.exists(output,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("Choose a new export path");
        // Snapshot export is an operator-controlled sensitive artifact; never put it in the repository.
        var attributes=output.getFileSystem().supportedFileAttributeViews().contains("posix")?new java.nio.file.attribute.FileAttribute<?>[]{java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))}:new java.nio.file.attribute.FileAttribute<?>[0];
        Path partial=Files.createTempFile(output.getParent(),"rated-feedback-",".partial",attributes);
        try{try(var writer=Files.newBufferedWriter(partial,StandardCharsets.UTF_8)){store.forEachRecord(record->{((com.fasterxml.jackson.databind.node.ObjectNode)record).put("exportedAt",Clock.systemUTC().millis());writer.write(record.toString());writer.newLine();});}Files.move(partial,output);}finally{Files.deleteIfExists(partial);}
    }
}

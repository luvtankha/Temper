package dev.temper.android.inference;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.function.*;

/** Pinned public weights; the downloader never receives any chat data. */
public final class ModelFiles {
    public static final long SIZE=dev.temper.android.BuildConfig.EMOTION_SIZE;
    public static final String HASH=dev.temper.android.BuildConfig.EMOTION_HASH;
    public static final String URL=dev.temper.android.BuildConfig.EMOTION_URL;
    private ModelFiles(){}
    public static File file(Context context){return new File(context.getNoBackupFilesDir(),HASH.equals("0c1981c5b479674747911c8e2228f0c4ec90bf47bf66e830f7d4fc62be082958")?"emotion-int8.onnx":"emotion-"+HASH.substring(0,16)+".onnx");}
    public static boolean ready(Context context){File file=file(context);return file.isFile()&&file.length()==SIZE;}
    public static void verify(File file)throws Exception{
        if(file.length()!=SIZE)throw new IOException("Model size mismatch");MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(file)){byte[] buffer=new byte[65536];int count;while((count=input.read(buffer))!=-1)digest.update(buffer,0,count);}
        StringBuilder hex=new StringBuilder();for(byte value:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",value&255));if(!HASH.contentEquals(hex))throw new IOException("Model checksum mismatch");
    }
    public static synchronized void download(Context context,IntConsumer progress,BooleanSupplier cancelled)throws Exception{
        File target=file(context),partial=new File(context.getNoBackupFilesDir(),"emotion-int8.partial");if(context.getNoBackupFilesDir().getUsableSpace()<SIZE+16_000_000)throw new IOException("Free up at least "+((SIZE+16_999_999)/1_000_000)+" MB and retry");
        HttpURLConnection connection=(HttpURLConnection)new java.net.URL(URL).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(30000);
        try{
            if(connection.getResponseCode()!=200||!"https".equals(connection.getURL().getProtocol()))throw new IOException("Secure download unavailable");
            try(InputStream input=connection.getInputStream();OutputStream output=new FileOutputStream(partial)){byte[] buffer=new byte[65536];long total=0;int count,last=-1;while((count=input.read(buffer))!=-1){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new IOException("Download cancelled");total+=count;if(total>SIZE)throw new IOException("Model too large");output.write(buffer,0,count);int percent=(int)(total*100/SIZE);if(percent!=last){progress.accept(percent);last=percent;}}}
            verify(partial);Files.move(partial.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{connection.disconnect();Files.deleteIfExists(partial.toPath());}
    }
}

package dev.temper.android.inference;

import java.io.*;

/** Available on every supported Android version, with a hard allocation bound. */
public final class BoundedIo {
    private BoundedIo(){}
    public static byte[] read(InputStream input,int maximum)throws IOException{ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1){if(output.size()+count>maximum)throw new IOException("Response too large");output.write(buffer,0,count);}return output.toByteArray();}
}

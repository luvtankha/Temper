package dev.temper.android;

import android.content.Context;
import dev.temper.android.privacy.*;
import dev.temper.android.accessibility.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class PrivacyChecks {
    static void run(Context context)throws Exception{
        Map<File,byte[]> original=new LinkedHashMap<>();
        for(String name:List.of("overlay-connection.json","whatsapp-layout-metadata.json","whatsapp-parser-report.json")){
            File file=new File(context.getFilesDir(),name);original.put(file,file.exists()?Files.readAllBytes(file.toPath()):null);
        }
        try{
            ConsentStore consent=new ConsentStore(context);consent.accept();consent.pause(false);LiveConsent.accept(context);
            for(File file:original.keySet())Files.write(file.toPath(),"generated test artifact".getBytes(StandardCharsets.UTF_8));
            ProbeState.arm();ParseProbeState.arm(context);LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),3);long generation=LiveCaptureState.generation();
            PrivacyControls.clearInspection(context);
            if(!consent.paused()||!consent.consented()||ProbeState.armed()||ParseProbeState.armed()||LiveCaptureState.current(generation))throw new AssertionError("Clear inspection retained active data or removed base consent");
            if(new File(context.getFilesDir(),"whatsapp-layout-metadata.json").exists()||new File(context.getFilesDir(),"whatsapp-parser-report.json").exists())throw new AssertionError("Inspection files retained");
            if(!new File(context.getFilesDir(),"overlay-connection.json").exists())throw new AssertionError("Inspection cleanup removed connection");
            PrivacyControls.removeConnection(context);
            if(new File(context.getFilesDir(),"overlay-connection.json").exists()||consent.preferences().contains("liveConsentVersion")||!consent.paused())throw new AssertionError("Connection cleanup ineffective");
        }finally{
            for(var saved:original.entrySet())if(saved.getValue()==null)saved.getKey().delete();else{Files.write(saved.getKey().toPath(),saved.getValue());Arrays.fill(saved.getValue(),(byte)0);}
        }
    }
}

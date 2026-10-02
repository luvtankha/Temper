package dev.temper.android;

import android.content.Context;
import dev.temper.android.inference.ModelFiles;
import dev.temper.android.inference.OnDeviceAnalysis;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;

/** Corruption is applied only to public model weights in the isolated QA installation. */
final class ModelRecoveryChecks {
    static void run(Context context)throws Exception{
        if(!context.getPackageName().equals("dev.temper.android.qa"))throw new AssertionError("Recovery fixture requires isolated QA installation");
        File model=ModelFiles.file(context),copy=new File(context.getNoBackupFilesDir(),"model-recovery-public-copy.onnx");
        if(!ModelFiles.ready(context))ModelFiles.download(context,percent->{},()->false);
        ModelFiles.verify(model);Files.copy(model.toPath(),copy.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        try{
            try(RandomAccessFile changed=new RandomAccessFile(model,"rw")){int first=changed.read();changed.seek(0);changed.write(first^255);}
            if(!ModelFiles.ready(context))throw new AssertionError("Fixture must exercise same-size corruption");
            try(OnDeviceAnalysis engine=new OnDeviceAnalysis()){
                try{engine.prepare(context);throw new AssertionError("Corrupt model accepted");}catch(IOException expected){if(!expected.getMessage().contains("checksum"))throw expected;}
            }
            if(ModelFiles.ready(context)||model.exists())throw new AssertionError("Corrupt model blocked setup retry");
            Files.copy(copy.toPath(),model.toPath());
            try(OnDeviceAnalysis engine=new OnDeviceAnalysis()){
                var snapshot=new dev.temper.android.adapters.VisibleConversation(dev.temper.android.adapters.VisibleConversation.Status.AVAILABLE,"a".repeat(64),java.util.List.of(new dev.temper.android.adapters.VisibleConversation.Turn("b".repeat(64),dev.temper.android.adapters.VisibleConversation.Role.REMOTE,"The meeting starts at ten.")),new dev.temper.android.adapters.ScreenObservation.Bounds(0,100,400,150));
                var result=engine.analyze(context,snapshot,()->true);if(!result.summary().available()||result.summary().spectrum()[0]<.7f)throw new AssertionError("Restored model did not produce real neutral inference");
            }
            if(!ModelFiles.ready(context))throw new AssertionError("Valid model did not recover");
        }finally{Files.deleteIfExists(copy.toPath());}
    }
}

package dev.temper.android;

import android.content.Context;
import android.graphics.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import dev.temper.android.character.*;
import dev.temper.android.inference.*;
import dev.temper.android.adapters.*;

/** Own generated fixtures only; this test never obtains an Accessibility root. */
final class ConsumerChecks {
    static void run(Context assets,Context context)throws Exception{
        Bitmap gallery=Bitmap.createBitmap(1024,Avatar.values().length*192,Bitmap.Config.ARGB_8888);Canvas galleryCanvas=new Canvas(gallery);galleryCanvas.drawColor(0xff151019);Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);label.setColor(Color.WHITE);label.setTextSize(14);
        Set<Integer> styles=new HashSet<>();for(Avatar avatar:Avatar.values()){
            CharacterView view=new CharacterView(context);view.setAvatar(avatar);view.layout(0,0,64,88);Set<Integer> poses=new HashSet<>();
            for(Emotion emotion:Emotion.values()){view.setEmotion(emotion,false);Bitmap bitmap=Bitmap.createBitmap(64,88,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));int[] pixels=new int[5632];bitmap.getPixels(pixels,0,64,0,0,64,88);if(!poses.add(Arrays.hashCode(pixels)))throw new AssertionError("Avatar expressions duplicate");if(emotion==Emotion.NEUTRAL&&!styles.add(Arrays.hashCode(pixels)))throw new AssertionError("Avatar styles duplicate");int x=emotion.ordinal()*128,y=avatar.ordinal()*192;galleryCanvas.drawBitmap(bitmap,null,new Rect(x+16,y+12,x+112,y+144),null);galleryCanvas.drawText(avatar.displayName(),x+16,y+164,label);galleryCanvas.drawText(emotion.label(),x+16,y+184,label);bitmap.recycle();}
        }
        try(OutputStream output=new FileOutputStream(new File(context.getFilesDir(),"consumer-avatar-gallery.png"))){gallery.compress(Bitmap.CompressFormat.PNG,100,output);}gallery.recycle();
        AvatarSelection selection=new AvatarSelection(context,avatar->false);Avatar original=selection.selected();try{selection.select(Avatar.NOVA);throw new AssertionError("Unowned avatar selectable");}catch(IllegalStateException expected){}if(selection.selected()!=original)throw new AssertionError("Rejected purchase changed selection");
        RobertaTokenizer tokenizer;try(InputStream compiled=context.getAssets().open("emotion/tokenizer.bin")){tokenizer=new RobertaTokenizer(compiled);}
        try(InputStream input=assets.getAssets().open("roberta-reference.json")){JSONArray cases=new JSONArray(new String(BoundedIo.read(input,500_000),StandardCharsets.UTF_8));for(int i=0;i<cases.length();i++){JSONObject fixture=cases.getJSONObject(i);JSONArray expected=fixture.getJSONArray("ids");long[] ids=tokenizer.encode(fixture.getString("text"),128);if(ids.length!=expected.length())throw new AssertionError("Android tokenizer fixture length "+i);for(int j=0;j<ids.length;j++)if(ids[j]!=expected.getLong(j))throw new AssertionError("Android tokenizer fixture "+i);}}
    }
    static void modelChecks(Context assets,Context context)throws Exception{
        ModelFiles.verify(ModelFiles.file(context));
        try(OnDeviceAnalysis engine=new OnDeviceAnalysis()){
            var bounds=new ScreenObservation.Bounds(0,100,400,150);String[] local={"I am furious and I hate this.","Wonderful! I'm so happy!","Okay."};float[] baseline=null;
            for(int i=0;i<local.length;i++){var snapshot=new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),List.of(new VisibleConversation.Turn("b".repeat(64),VisibleConversation.Role.LOCAL,local[i]),new VisibleConversation.Turn("c".repeat(64),VisibleConversation.Role.REMOTE,"The meeting starts at ten."),new VisibleConversation.Turn("d".repeat(64),VisibleConversation.Role.LOCAL,"Okay, see you then.")),bounds);var result=engine.analyze(context,snapshot,()->true);if(!result.summary().available()||result.summary().spectrum()[0]<.7f)throw new AssertionError("Neutral dummy model fixture failed");if(baseline==null)baseline=result.summary().spectrum();else if(!Arrays.equals(baseline,result.summary().spectrum()))throw new AssertionError("Local emotion contaminated remote result");}
            JSONArray fixtures;try(InputStream input=assets.getAssets().open("overlay-chat-styles.json")){fixtures=new JSONArray(new String(BoundedIo.read(input,100_000),StandardCharsets.UTF_8));}
            JSONArray report=new JSONArray();String[] labels={"neutral","happiness","concern","confusion","sadness","frustration","anger","surprise"};int clear=0;
            for(int i=0;i<fixtures.length();i++){JSONObject fixture=fixtures.getJSONObject(i);JSONArray input=fixture.getJSONArray("turns");List<VisibleConversation.Turn> turns=new ArrayList<>();for(int j=0;j<input.length();j++){JSONObject turn=input.getJSONObject(j);turns.add(new VisibleConversation.Turn(String.format(java.util.Locale.ROOT,"%064x",j+1),VisibleConversation.Role.valueOf(turn.getString("role")),turn.getString("text")));}var result=engine.analyze(context,new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),turns,bounds),()->true);float[] scores=result.summary().spectrum();int max=0;for(int j=1;j<8;j++)if(scores[j]>scores[max])max=j;String id=fixture.getString("id"),expected=fixture.getString("expected");if(!expected.equals("review")&&!id.startsWith("role-control")){clear++;if(!expected.equals(labels[max]))throw new AssertionError("Clear dummy style mismatch: "+id);}report.put(new JSONObject().put("id",id).put("dominant",labels[max]).put("scores",new JSONArray(scores)).put("direction",result.summary().direction()));}
            if(clear!=16)throw new AssertionError("Expected sixteen clear style fixtures");try(OutputStream output=new FileOutputStream(new File(context.getFilesDir(),"consumer-dummy-spectrum.json"))){output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        }
    }
}

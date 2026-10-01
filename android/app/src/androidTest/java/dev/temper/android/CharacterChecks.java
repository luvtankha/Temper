package dev.temper.android;

import android.content.Context;
import android.graphics.*;
import dev.temper.android.character.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

final class CharacterChecks {
    static void run(Context context)throws Exception{
        CharacterView character=new CharacterView(context);character.layout(0,0,64,88);
        List<int[]> pixels=new ArrayList<>();Bitmap gallery=Bitmap.createBitmap(576,672,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(gallery);canvas.drawColor(0xff151019);
        Paint labels=new Paint(Paint.ANTI_ALIAS_FLAG);labels.setColor(Color.WHITE);labels.setTextSize(24);labels.setTextAlign(Paint.Align.CENTER);
        for(Emotion emotion:Emotion.values()){
            character.setEmotion(emotion,false);Bitmap frame=Bitmap.createBitmap(64,88,Bitmap.Config.ARGB_8888);character.draw(new Canvas(frame));int[] data=new int[64*88];frame.getPixels(data,0,64,0,0,64,88);
            for(int[] prior:pixels)if(Arrays.equals(prior,data))throw new AssertionError("Expressions render identically");
            for(int x=0;x<64;x++)if(Color.alpha(data[x])!=0)throw new AssertionError("Drawing clipped at top boundary");
            for(int y=0;y<88;y++)if(Color.alpha(data[y*64])!=0||Color.alpha(data[y*64+63])!=0)throw new AssertionError("Drawing clipped at side boundary");
            if(emotion!=Emotion.NEUTRAL)for(int y=80;y<88;y++)for(int x=0;x<64;x++)if(data[y*64+x]!=pixels.get(0)[y*64+x])throw new AssertionError("Grounding moved between expressions");
            pixels.add(data);int column=emotion.ordinal()%4,row=emotion.ordinal()/4;canvas.drawBitmap(frame,null,new Rect(column*144+8,row*336+12,column*144+136,row*336+188),null);canvas.drawText(emotion.label(),column*144+72,row*336+228,labels);frame.recycle();
        }
        try(FileOutputStream output=new FileOutputStream(new File(context.getFilesDir(),"character-gallery.png"))){gallery.compress(Bitmap.CompressFormat.PNG,100,output);}gallery.recycle();
        character.setEmotion(Emotion.HAPPY,true);if(character.transitioning())throw new AssertionError("Detached view started animation");
    }
}

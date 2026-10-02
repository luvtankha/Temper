package dev.temper.android;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import dev.temper.android.home.*;
import dev.temper.android.character.*;
import dev.temper.android.store.PurchaseStore;
import java.util.*;
import java.io.*;

/** Own-app and generated artwork only. No accessibility roots or system setting mutations. */
final class CarouselUiChecks {
    static void run(Instrumentation test)throws Exception{
        Context context=test.getTargetContext();var prefs=context.getSharedPreferences(AvatarSelection.STORAGE,0);var original=new HashMap<String,Object>(prefs.getAll());
        Activity activity=test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            AvatarHome[] home={null};test.runOnMainSync(()->{home[0]=find(activity.getWindow().getDecorView());if(home[0]==null)throw new AssertionError("No avatar home");});
            test.runOnMainSync(()->home[0].carousel().step(-1));Thread.sleep(400);
            test.runOnMainSync(()->checkSettled(context,home[0]));
            test.runOnMainSync(()->home[0].carousel().step(1));Thread.sleep(400);
            test.runOnMainSync(()->checkSettled(context,home[0]));
            test.runOnMainSync(()->{
                AvatarCarousel carousel=home[0].carousel();float d=context.getResources().getDisplayMetrics().density,x=carousel.getWidth()/2f,y=60*d;long t=android.os.SystemClock.uptimeMillis();
                event(carousel,t,t,MotionEvent.ACTION_DOWN,x,y);event(carousel,t,t+100,MotionEvent.ACTION_MOVE,x-28*d,y);
                if(carousel.position()==Math.round(carousel.position()))throw new AssertionError("Drag jumped between fixed states");
                event(carousel,t,t+600,MotionEvent.ACTION_UP,x-28*d,y);
            });Thread.sleep(400);
            test.runOnMainSync(()->{
                checkSettled(context,home[0]);
                AvatarCarousel strip=home[0].carousel();int before=strip.selectedIndex();long now=android.os.SystemClock.uptimeMillis();float x=strip.getWidth()/2f;
                event(strip,now,now,MotionEvent.ACTION_DOWN,x,20);event(strip,now,now+10,MotionEvent.ACTION_POINTER_DOWN,x,20);event(strip,now,now+20,MotionEvent.ACTION_UP,x+200,20);if(strip.selectedIndex()!=before)throw new AssertionError("Cancelled multi-touch became a selection tap");
                List<Avatar> expanded=new ArrayList<>();for(int i=0;i<51;i++)expanded.add(Avatar.homeCatalog().get(i%5));AvatarCarousel large=new AvatarCarousel(context,expanded,expanded.get(0),a->{});large.measure(exact(390),exact(140));large.layout(0,0,390,140);large.step(50);if(large.catalogSize()!=51||large.getChildCount()!=7||large.selectedIndex()!=50||large.position()!=50)throw new AssertionError("Large catalog not centered/bounded");
                try{
                    for(int width:new int[]{320,390,430})for(float scale:new float[]{1,1.3f}){
                        Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=scale;Context scaled=context.createConfigurationContext(config);AvatarHome sample=new AvatarHome(scaled,Avatar.ASTRA,a->{},on->{},()->{});render(sample,scaled,width,844,"carousel-layout-"+width+"-"+scale+".png");if(sample.getChildAt(0).getWidth()>sample.getWidth())throw new AssertionError("Horizontal layout overflow");
                    }
                    for(Avatar avatar:new Avatar[]{Avatar.ASTRA,Avatar.MIRA,Avatar.KAI})for(boolean on:new boolean[]{false,true}){AvatarHome sample=new AvatarHome(context,avatar,a->{},v->{},()->{});sample.bind(on,false);render(sample,context,390,844,"carousel-"+avatar.id()+"-"+(on?"on":"off")+".png");}
                }catch(IOException error){throw new AssertionError("Generated screenshots failed",error);}
            });
        }finally{test.runOnMainSync(activity::finish);var edit=prefs.edit().clear();for(var e:original.entrySet())if(e.getValue() instanceof String s)edit.putString(e.getKey(),s);edit.commit();}
    }
    private static void checkSettled(Context c,AvatarHome home){AvatarCarousel carousel=home.carousel();if(carousel.snapping()||carousel.position()!=carousel.selectedIndex())throw new AssertionError("Not exactly centered after snap");Avatar expected=Avatar.homeCatalog().get(carousel.selectedIndex());if(home.selectedAvatar()!=expected||new AvatarSelection(c,new PurchaseStore(c)::owned).selected()!=expected)throw new AssertionError("Selection and overlay preference diverged");}
    private static int exact(int n){return View.MeasureSpec.makeMeasureSpec(n,View.MeasureSpec.EXACTLY);}
    private static void event(View view,long start,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,time,action,x,y,0);try{view.dispatchTouchEvent(e);}finally{e.recycle();}}
    private static AvatarHome find(View view){if(view instanceof AvatarHome home)return home;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){AvatarHome found=find(group.getChildAt(i));if(found!=null)return found;}return null;}
    private static void render(AvatarHome view,Context context,int width,int height,String name)throws IOException{float d=context.getResources().getDisplayMetrics().density;int w=Math.round(width*d),h=Math.round(height*d);view.measure(exact(w),exact(h));view.layout(0,0,w,h);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG failed");}finally{bitmap.recycle();}}
}

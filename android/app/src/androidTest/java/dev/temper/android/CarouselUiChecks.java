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
                checkCatalogGeometry(context);
                try{
                    for(int width:new int[]{320,390,430})for(float scale:new float[]{1,1.3f,1.5f,2}){
                        Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=scale;Context scaled=context.createConfigurationContext(config);AvatarHome sample=new AvatarHome(scaled,Avatar.ASTRA,a->{},on->{},()->{});render(sample,scaled,width,844,"carousel-layout-"+width+"-"+scale+".png");if(sample.getChildAt(0).getWidth()>sample.getWidth())throw new AssertionError("Horizontal layout overflow");
                    }
                    for(Avatar avatar:new Avatar[]{Avatar.ASTRA,Avatar.MIRA,Avatar.KAI})for(boolean on:new boolean[]{false,true}){AvatarHome sample=new AvatarHome(context,avatar,a->{},v->{},()->{});sample.bind(on,false);render(sample,context,390,844,"carousel-"+avatar.id()+"-"+(on?"on":"off")+".png");}
                }catch(IOException error){throw new AssertionError("Generated screenshots failed",error);}
            });
            test.runOnMainSync(()->{
                AvatarCarousel carousel=home[0].carousel();carousel.step(-carousel.catalogSize());
            });Thread.sleep(400);
            test.runOnMainSync(()->{
                AvatarCarousel carousel=home[0].carousel();int before=carousel.selectedIndex();carousel.step(1);carousel.step(1);
                if(carousel.catalogSize()>2&&before==0&&android.animation.ValueAnimator.areAnimatorsEnabled()&&!carousel.snapping())throw new AssertionError("Rapid arrows did not animate");
            });Thread.sleep(400);
            test.runOnMainSync(()->{
                AvatarCarousel carousel=home[0].carousel();if(carousel.selectedIndex()!=Math.min(2,carousel.catalogSize()-1))throw new AssertionError("Rapid arrow requests lost a selection");checkSettled(context,home[0]);
                float d=context.getResources().getDisplayMetrics().density,x=carousel.getWidth()/2f;long t=android.os.SystemClock.uptimeMillis();
                event(carousel,t,t,MotionEvent.ACTION_DOWN,x,20*d);event(carousel,t,t+10,MotionEvent.ACTION_MOVE,x+20*d,20*d);event(carousel,t,t+11,MotionEvent.ACTION_CANCEL,x+20*d,20*d);
            });Thread.sleep(400);
            test.runOnMainSync(()->{
                AvatarCarousel carousel=home[0].carousel();int committed=Math.min(2,carousel.catalogSize()-1);if(carousel.selectedIndex()!=committed)throw new AssertionError("Cancelled drag applied fling momentum");checkSettled(context,home[0]);float d=context.getResources().getDisplayMetrics().density,x=carousel.getWidth()/2f;long t=android.os.SystemClock.uptimeMillis();
                // Detach during a partial drag: selection must remain the committed persisted avatar.
                event(carousel,t,t,MotionEvent.ACTION_DOWN,x,20*d);event(carousel,t,t+10,MotionEvent.ACTION_MOVE,x-90*d,20*d);
                ViewGroup parent=(ViewGroup)carousel.getParent();int index=parent.indexOfChild(carousel);ViewGroup.LayoutParams params=carousel.getLayoutParams();parent.removeView(carousel);parent.addView(carousel,index,params);
                if(carousel.position()!=committed||carousel.selectedIndex()!=committed)throw new AssertionError("Detached drag changed selection without persistence");checkSettled(context,home[0]);
            });
        }finally{test.runOnMainSync(activity::finish);var edit=prefs.edit().clear();for(var e:original.entrySet())if(e.getValue() instanceof String s)edit.putString(e.getKey(),s);edit.commit();}
    }
    private static void checkSettled(Context c,AvatarHome home){AvatarCarousel carousel=home.carousel();if(carousel.snapping()||carousel.position()!=carousel.selectedIndex())throw new AssertionError("Not exactly centered after snap");Avatar expected=Avatar.homeCatalog().get(carousel.selectedIndex());if(home.selectedAvatar()!=expected||new AvatarSelection(c,new PurchaseStore(c)::owned).selected()!=expected)throw new AssertionError("Selection and overlay preference diverged");}
    private static void checkCatalogGeometry(Context context){
        for(float density:new float[]{1,1.5f,2,2.625f,3}){
            Configuration config=new Configuration(context.getResources().getConfiguration());config.densityDpi=Math.round(160*density);Context scaled=context.createConfigurationContext(config);
            for(int count:new int[]{1,2,5,10,20,50})for(int width:new int[]{319,320,321,390,391,430,431}){
                List<Avatar> expanded=new ArrayList<>();for(int i=0;i<count;i++)expanded.add(Avatar.homeCatalog().get(i%Avatar.homeCatalog().size()));int[] callbacks={0};AvatarCarousel strip=new AvatarCarousel(scaled,expanded,expanded.get(0),a->callbacks[0]++);strip.measure(exact(width),exact(Math.round(146*density)));strip.layout(0,0,width,strip.getMeasuredHeight());
                for(int index:new int[]{0,count/2,count-1}){strip.step(index-strip.selectedIndex());if(strip.selectedIndex()!=index||strip.position()!=index||strip.getChildCount()!=7)throw new AssertionError("Catalog size changed carousel geometry");boolean centered=false;for(int slot=0;slot<strip.getChildCount();slot++){View child=strip.getChildAt(slot);if(child.getVisibility()==View.VISIBLE&&child.getScaleX()==1){centered=true;if(child.getX()+child.getWidth()/2f!=width/2f)throw new AssertionError("Selected avatar is not exactly at viewport center");}}if(!centered)throw new AssertionError("Selected avatar not rendered");}
                int before=callbacks[0];strip.step(1);strip.step(0);if(callbacks[0]!=before)throw new AssertionError("Unchanged selection emitted duplicate callbacks");
            }
        }
        AvatarHome fallback=new AvatarHome(context,java.util.List.of(Avatar.KAI,Avatar.MIRA),Avatar.ALEX,a->{},on->{},()->{});if(fallback.selectedAvatar()!=Avatar.KAI||fallback.carousel().selectedIndex()!=0)throw new AssertionError("Removed avatar fallback disagrees with carousel");
    }
    private static int exact(int n){return View.MeasureSpec.makeMeasureSpec(n,View.MeasureSpec.EXACTLY);}
    private static void event(View view,long start,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,time,action,x,y,0);try{view.dispatchTouchEvent(e);}finally{e.recycle();}}
    private static AvatarHome find(View view){if(view instanceof AvatarHome home)return home;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){AvatarHome found=find(group.getChildAt(i));if(found!=null)return found;}return null;}
    private static void render(AvatarHome view,Context context,int width,int height,String name)throws IOException{float d=context.getResources().getDisplayMetrics().density;int w=Math.round(width*d),h=Math.round(height*d);view.measure(exact(w),exact(h));view.layout(0,0,w,h);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));try(OutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG failed");}finally{bitmap.recycle();}}
}

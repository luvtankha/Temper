package dev.temper.android.overlay;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import dev.temper.android.MainActivity;
import dev.temper.android.character.*;
import dev.temper.android.store.PurchaseStore;
import dev.temper.android.analytics.*;

/** Explicitly started floating companion; never inspects any other application's content. */
public final class FloatingOverlayService extends Service {
    private static FloatingOverlayService instance;
    private static boolean appVisible,analysisVisible,checkoutVisible;
    private WindowManager windows;
    private CharacterView character;
    private AnalyticsPanel panel;
    private WindowManager.LayoutParams position;
    private boolean attached,popup,locked;
    private DragGesture gesture;
    private long outsideDismissedAt;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable check=()->{if(instance==this){refresh();main.postDelayed(this.check,60000);}};
    private final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){locked=Intent.ACTION_SCREEN_OFF.equals(intent.getAction())||((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked();refresh();}};
    public static boolean running(){return instance!=null;}
    public static boolean visible(){return instance!=null&&instance.attached;}
    public static void appVisible(boolean value){appVisible=value;if(instance!=null)instance.refresh();}
    public static void analysisVisible(boolean value){analysisVisible=value;if(instance!=null)instance.refresh();}
    public static void changed(){if(instance!=null)instance.refresh();}
    public static void checkoutVisible(boolean value){checkoutVisible=value;if(instance!=null)instance.refresh();}
    public static void stop(Context context){context.stopService(new Intent(context,FloatingOverlayService.class));}
    public static void start(Context context){context.startForegroundService(new Intent(context,FloatingOverlayService.class));}
    @Override public void onCreate(){
        super.onCreate();instance=this;windows=(WindowManager)getSystemService(WINDOW_SERVICE);character=new CharacterView(this);panel=new AnalyticsPanel(this);
        gesture=new DragGesture(ViewConfiguration.get(this).getScaledTouchSlop());
        panel.bind(new OverlaySummary("Open a supported chat for analysis","Automatic estimates unavailable here",new float[8],false));
        panel.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_OUTSIDE){outsideDismissedAt=SystemClock.elapsedRealtime();dismiss();return true;}return false;});
        character.setOnClickListener(view->{if(popup)dismiss();else if(SystemClock.elapsedRealtime()-outsideDismissedAt>350)showPanel();});
        character.setOnTouchListener((view,event)->{
            if(!attached||position==null){gesture.cancel();return false;}
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN -> {gesture.begin(event.getRawX(),event.getRawY(),position.x,position.y);return true;}
                case MotionEvent.ACTION_MOVE -> {if(event.getPointerCount()!=1){cancelGesture();return true;}moveGesture(event);return true;}
                case MotionEvent.ACTION_UP -> {moveGesture(event);DragGesture.Finish finished=gesture.finish();if(finished==DragGesture.Finish.DRAG)savePosition();else if(finished==DragGesture.Finish.TAP)view.performClick();return true;}
                case MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN,MotionEvent.ACTION_POINTER_UP -> {cancelGesture();return true;}
                default -> {return true;}
            }
        });
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_USER_PRESENT);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,filter);
        NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);notifications.createNotificationChannel(new NotificationChannel("companion","Companion controls",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,FloatingOverlayService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"companion").setSmallIcon(dev.temper.android.R.drawable.ic_notification).setContentTitle("TEMPER companion is active").setContentText("Drag to move • tap for estimates • Stop to hide").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(41,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(41,notification);
        main.post(check);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){if(intent!=null&&"STOP".equals(intent.getAction())){new dev.temper.android.privacy.ConsentStore(this).pause(true);stopSelf();}else refresh();return START_NOT_STICKY;}
    private Rect display(){if(Build.VERSION.SDK_INT>=30)return windows.getMaximumWindowMetrics().getBounds();android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(metrics);return new Rect(0,0,metrics.widthPixels,metrics.heightPixels);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private WindowManager.LayoutParams params(int width,int height){var params=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.LEFT;params.setTitle("TEMPER floating companion");if(Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);return params;}
    private void refresh(){
        if(!Settings.canDrawOverlays(this)){stopSelf();return;}
        if(appVisible||analysisVisible||checkoutVisible||locked||((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()){hide();return;}
        character.setAvatar(new AvatarSelection(this,new PurchaseStore(this)::owned).selected());
        if(attached)return;
        Rect bounds=display();var prefs=getSharedPreferences("temper_floating",0);var point=FloatingPlacement.place(prefs.getFloat("x",0),prefs.getFloat("y",.87f),bounds.width(),bounds.height(),dp(64),dp(88),dp(32),dp(32));
        position=params(dp(64),dp(88));position.x=point.x();position.y=point.y();
        try{windows.addView(character,position);attached=true;}catch(RuntimeException denied){stopSelf();}
    }
    private void move(float x,float y){Rect bounds=display();int maxX=Math.max(1,bounds.width()-dp(64)),maxY=Math.max(1,bounds.height()-dp(88)-dp(32));var point=FloatingPlacement.place(x/maxX,y/maxY,bounds.width(),bounds.height(),dp(64),dp(88),dp(32),dp(32));position.x=point.x();position.y=point.y();try{windows.updateViewLayout(character,position);}catch(RuntimeException gone){hide();}}
    private void moveGesture(MotionEvent event){var next=gesture.move(event.getRawX(),event.getRawY());if(next!=null){dismiss();move(next.x(),next.y());}}
    private void cancelGesture(){if(gesture!=null&&gesture.cancel()&&position!=null)savePosition();}
    private void savePosition(){Rect bounds=display();getSharedPreferences("temper_floating",0).edit().putFloat("x",position.x/(float)Math.max(1,bounds.width()-dp(64))).putFloat("y",position.y/(float)Math.max(1,bounds.height()-dp(88)-dp(32))).apply();}
    private void showPanel(){
        if(!attached)return;Rect bounds=display();int width=Math.min(dp(280),bounds.width());panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int height=panel.getMeasuredHeight();if(height>bounds.height()-dp(64))return;
        var params=params(width,height);params.flags|=WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;params.x=Math.min(position.x,Math.max(0,bounds.width()-width));params.y=Math.max(dp(32),Math.min(bounds.height()-dp(32)-height,position.y-height-dp(4)));
        try{windows.addView(panel,params);popup=true;}catch(RuntimeException denied){dismiss();}
    }
    private void dismiss(){if(popup){try{windows.removeViewImmediate(panel);}catch(RuntimeException ignored){}popup=false;}}
    private void hide(){cancelGesture();dismiss();if(attached){try{windows.removeViewImmediate(character);}catch(RuntimeException ignored){}attached=false;}}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);hide();refresh();}
    @Override public void onDestroy(){main.removeCallbacksAndMessages(null);unregisterReceiver(screen);hide();if(instance==this)instance=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}

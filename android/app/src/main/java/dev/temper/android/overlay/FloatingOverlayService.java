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
import dev.temper.android.privacy.PowerStore;
import dev.temper.android.privacy.ConsentStore;
import dev.temper.android.adapters.ScreenObservation.Bounds;

/** Explicitly started floating companion; never inspects any other application's content. */
public final class FloatingOverlayService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static volatile FloatingOverlayService instance;
    private static volatile boolean appVisible,analysisVisible,checkoutVisible;
    private static volatile boolean foregroundScreenAllowed=true;
    private WindowManager windows;
    private CharacterView character;
    private AnalyticsPanel panel;
    private WindowManager.LayoutParams position;
    private boolean attached,popup,locked,receiverRegistered,foreground,failed;
    private AppOpsManager appOps;
    private int keyboardBottom;
    private Bounds popupViewport;
    private DragGesture gesture;
    private long outsideDismissedAt;
    private ConsentStore consent;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable check=()->{if(instance==this&&!failed){refresh();main.postDelayed(this.check,60000);}};
    private final AppOpsManager.OnOpChangedListener overlayPermission=(op,packageName)->{if(getPackageName().equals(packageName))requestRefresh();};
    private final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){locked=Intent.ACTION_SCREEN_OFF.equals(intent.getAction())||isLocked();refresh();}};
    public static boolean running(){return instance!=null;}
    public static boolean visible(){return instance!=null&&instance.attached;}
    public static void appVisible(boolean value){appVisible=value;changed();}
    public static void analysisVisible(boolean value){analysisVisible=value;changed();}
    public static void changed(){FloatingOverlayService service=instance;if(service!=null)service.requestRefresh();}
    public static void checkoutVisible(boolean value){checkoutVisible=value;changed();}
    /** Package/window metadata only; never requests other-app content. */
    public static void foregroundScreenAllowed(boolean value){foregroundScreenAllowed=value;changed();}
    public static void stop(Context context){new PowerStore(context).setEnabled(false);context.stopService(new Intent(context,FloatingOverlayService.class));}
    public static void start(Context context){if(!new PowerStore(context).enabled())throw new IllegalStateException("Turn TEMPER ON first");try{context.startForegroundService(new Intent(context,FloatingOverlayService.class));}catch(RuntimeException denied){new PowerStore(context).setEnabled(false);throw denied;}}
    @Override public void onCreate(){
        super.onCreate();
        try{
        // Enter the foreground before decoding artwork or attaching windows. A failed
        // foreground start must never advertise a running owner to Accessibility.
        NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);notifications.createNotificationChannel(new NotificationChannel("companion","Companion controls",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,FloatingOverlayService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"companion").setSmallIcon(dev.temper.android.R.drawable.ic_notification).setContentTitle("TEMPER is ON").setContentText("Drag to move • tap for estimates • OFF to hide").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"OFF",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(41,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(41,notification);
        foreground=true;
        consent=new ConsentStore(this);consent.preferences().registerOnSharedPreferenceChangeListener(this);windows=(WindowManager)getSystemService(WINDOW_SERVICE);character=new CharacterView(this);panel=new AnalyticsPanel(this);
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
        if(Build.VERSION.SDK_INT>=30)character.setOnApplyWindowInsetsListener((view,insets)->{
            int bottom=insets.isVisible(WindowInsets.Type.ime())?insets.getInsets(WindowInsets.Type.ime()).bottom:0;
            if(keyboardBottom!=bottom){keyboardBottom=bottom;main.post(this::refresh);}
            return insets;
        });
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_USER_PRESENT);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,filter);
        receiverRegistered=true;
        appOps=(AppOpsManager)getSystemService(APP_OPS_SERVICE);appOps.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,getPackageName(),overlayPermission);
        instance=this;
        dev.temper.android.accessibility.TemperAccessibilityService.refreshPowerState();
        main.post(check);
        }catch(RuntimeException unavailable){fail();}
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){if(failed){stopSelf();return START_NOT_STICKY;}if(intent!=null&&"STOP".equals(intent.getAction())){new PowerStore(this).requestOff();hide();stopSelf();}else refresh();return START_NOT_STICKY;}
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key){if(!new PowerStore(this).enabled()){hide();stopSelf();}}
    @Override public void onTaskRemoved(Intent rootIntent){new PowerStore(this).setEnabled(false);hide();stopSelf();super.onTaskRemoved(rootIntent);}
    private boolean isLocked(){KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);return keyguard!=null&&keyguard.isKeyguardLocked();}
    private void requestRefresh(){if(Looper.myLooper()==Looper.getMainLooper())refresh();else main.post(this::refresh);}
    private void fail(){if(failed)return;failed=true;new PowerStore(this).setEnabled(false);hide();if(instance==this)instance=null;dev.temper.android.accessibility.TemperAccessibilityService.refreshPowerState();stopSelf();android.widget.Toast.makeText(this,"TEMPER is OFF. Overlay unavailable; check setup and retry.",android.widget.Toast.LENGTH_LONG).show();}
    private Bounds display(){
        if(Build.VERSION.SDK_INT>=30){var metrics=windows.getMaximumWindowMetrics();Rect bounds=metrics.getBounds();var bars=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());return new Bounds(bounds.left+bars.left,bounds.top+bars.top,bounds.right-bars.right,bounds.bottom-bars.bottom);}
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(metrics);
        int top=systemDimension("status_bar_height"),bottom=systemDimension("navigation_bar_height");
        return new Bounds(0,top,metrics.widthPixels,metrics.heightPixels-bottom);
    }
    private int systemDimension(String name){int id=getResources().getIdentifier(name,"dimen","android");return id==0?dp(32):getResources().getDimensionPixelSize(id);}
    private Bounds usableDisplay(){Bounds bounds=display();if(keyboardBottom<=0||Build.VERSION.SDK_INT<30)return bounds;Rect full=windows.getMaximumWindowMetrics().getBounds();return new Bounds(bounds.left(),bounds.top(),bounds.right(),Math.min(bounds.bottom(),full.bottom-keyboardBottom));}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private WindowManager.LayoutParams params(int width,int height){var params=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.LEFT;params.setTitle("TEMPER floating companion");if(Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);return params;}
    private void refresh(){
        if(failed||!foreground||character==null)return;
        try{
        if(!new PowerStore(this).enabled()){hide();stopSelf();return;}
        if(!Settings.canDrawOverlays(this)||!dev.temper.android.inference.ModelFiles.ready(this)){new PowerStore(this).setEnabled(false);hide();stopSelf();return;}
        if(appVisible||analysisVisible||checkoutVisible||!foregroundScreenAllowed||locked||isLocked()){hide();return;}
        character.setAvatar(new AvatarSelection(this,new PurchaseStore(this)::owned).selected());
        Bounds bounds=display(),usable=usableDisplay();var prefs=getSharedPreferences("temper_floating",0);float x=prefs.getFloat("x",0),y=prefs.getFloat("y",.87f);
        if(!Float.isFinite(x)||!Float.isFinite(y)){prefs.edit().clear().apply();x=0;y=.87f;}
        var preferred=FloatingPlacement.place(x,y,bounds,dp(64),dp(88));var point=gesture.dragging()?FloatingPlacement.move(position.x,position.y,usable,dp(64),dp(88)):FloatingPlacement.move(preferred.x(),preferred.y(),usable,dp(64),dp(88));
        if(attached){if(position.x!=point.x()||position.y!=point.y()){position.x=point.x();position.y=point.y();try{windows.updateViewLayout(character,position);}catch(RuntimeException denied){fail();return;}}if(popup&&!usable.equals(popupViewport)){dismiss();showPanel();}return;}
        position=params(dp(64),dp(88));position.x=point.x();position.y=point.y();try{windows.addView(character,position);attached=true;}catch(RuntimeException denied){fail();}
        }catch(IllegalArgumentException insufficientRoom){hide();}catch(RuntimeException denied){fail();}
    }
    private void move(float x,float y){try{var point=FloatingPlacement.move(x,y,usableDisplay(),dp(64),dp(88));position.x=point.x();position.y=point.y();windows.updateViewLayout(character,position);}catch(RuntimeException gone){fail();}}
    private void moveGesture(MotionEvent event){var next=gesture.move(event.getRawX(),event.getRawY());if(next!=null){dismiss();move(next.x(),next.y());}}
    private void cancelGesture(){if(gesture!=null&&gesture.cancel()&&position!=null)savePosition();}
    private void savePosition(){try{Bounds bounds=display();getSharedPreferences("temper_floating",0).edit().putFloat("x",(position.x-bounds.left())/(float)Math.max(1,bounds.right()-bounds.left()-dp(64))).putFloat("y",(position.y-bounds.top())/(float)Math.max(1,bounds.bottom()-bounds.top()-dp(88))).apply();}catch(RuntimeException unavailable){/* A removed display must not crash teardown. */}}
    private void showPanel(){
        if(!attached||popup)return;
        try{Bounds bounds=usableDisplay();int width=Math.min(dp(280),bounds.right()-bounds.left());panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(bounds.bottom()-bounds.top(),View.MeasureSpec.AT_MOST));int height=panel.getMeasuredHeight();
        Bounds target=PopupPlacement.place(bounds,new Bounds(position.x,position.y,position.x+dp(64),position.y+dp(88)),width,height,dp(4));
        var params=params(width,height);params.flags|=WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;params.x=target.left();params.y=target.top();windows.addView(panel,params);popup=true;popupViewport=bounds;
        }catch(RuntimeException denied){dismiss();}
    }
    private void dismiss(){if(popup){try{windows.removeViewImmediate(panel);}catch(RuntimeException ignored){}popup=false;}popupViewport=null;}
    private void hide(){cancelGesture();dismiss();if(attached){try{windows.removeViewImmediate(character);}catch(RuntimeException ignored){}attached=false;}keyboardBottom=0;}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);keyboardBottom=0;hide();refresh();}
    @Override public void onDestroy(){main.removeCallbacksAndMessages(null);if(consent!=null)consent.preferences().unregisterOnSharedPreferenceChangeListener(this);if(receiverRegistered){try{unregisterReceiver(screen);}catch(RuntimeException unavailable){}receiverRegistered=false;}if(appOps!=null){try{appOps.stopWatchingMode(overlayPermission);}catch(RuntimeException unavailable){}}if(instance==this)instance=null;new PowerStore(this).setEnabled(false);hide();analysisVisible=false;dev.temper.android.accessibility.TemperAccessibilityService.refreshPowerState();if(foreground)stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}

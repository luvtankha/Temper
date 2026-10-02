package dev.temper.android.home;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import dev.temper.android.character.*;
import java.util.List;
import java.util.function.Consumer;

/** Focused responsive home. Selection is cosmetic; the one switch delegates to existing setup. */
public final class AvatarHome extends ScrollView {
    private final CharacterView hero,mini;
    private final TextView name,subtitle,status,pill;
    private final MasterToggle toggle;
    private final Stage stage;
    private final AvatarCarousel carousel;
    private boolean enabled;
    private Avatar avatar;
    public AvatarHome(Context context,Avatar initial,Consumer<Avatar> select,Consumer<Boolean> power,Runnable settings){
        this(context,Avatar.homeCatalog(),initial,select,power,settings);
    }
    public AvatarHome(Context context,List<Avatar> catalog,Avatar initial,Consumer<Avatar> select,Consumer<Boolean> power,Runnable settings){
        super(context);if(catalog==null||catalog.isEmpty())throw new IllegalArgumentException("Empty catalog");initial=catalog.contains(initial)?initial:catalog.get(0);avatar=initial;setFillViewport(true);setClipToPadding(false);setBackgroundColor(HomeTokens.BACKGROUND);
        LinearLayout column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(20),dp(16),dp(20),dp(20));addView(column,new ScrollView.LayoutParams(-1,-2));
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);column.addView(header,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout branding=new LinearLayout(context);branding.setOrientation(LinearLayout.VERTICAL);header.addView(branding,new LinearLayout.LayoutParams(0,-2,1));branding.addView(label("TEMPER",23,HomeTokens.TEXT,true));branding.addView(label("Choose your avatar",13,HomeTokens.SECONDARY,false));
        pill=label("Avatar OFF",11,HomeTokens.INACTIVE,false);pill.setPadding(dp(11),dp(8),dp(11),dp(8));header.addView(pill);
        IconButton setup=new IconButton(context,0);setup.setContentDescription("Settings");setup.setOnClickListener(v->settings.run());header.addView(setup,new LinearLayout.LayoutParams(dp(48),dp(48)));
        carousel=new AvatarCarousel(context,catalog,initial,a->{if(a==avatar)return;select.accept(a);updateAvatar(a,true);});column.addView(carousel,new LinearLayout.LayoutParams(-1,dp(124)));
        LinearLayout caption=new LinearLayout(context);caption.setGravity(Gravity.CENTER_VERTICAL);column.addView(caption,new LinearLayout.LayoutParams(-1,dp(48)));
        IconButton prev=new IconButton(context,-1);prev.setContentDescription("Previous avatar");prev.setOnClickListener(v->carousel.step(-1));caption.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView hint=label("Swipe to explore",12,HomeTokens.MUTED,false);hint.setGravity(Gravity.CENTER);caption.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        IconButton next=new IconButton(context,1);next.setContentDescription("Next avatar");next.setOnClickListener(v->carousel.step(1));caption.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));
        stage=new Stage(context);hero=new CharacterView(context);hero.setAvatar(initial);hero.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);FrameLayout.LayoutParams art=new FrameLayout.LayoutParams(dp(168),dp(210),Gravity.CENTER);stage.addView(hero,art);column.addView(stage,new LinearLayout.LayoutParams(-1,dp(216)));
        name=label(initial.displayName(),27,HomeTokens.TEXT,true);name.setGravity(Gravity.CENTER);column.addView(name,new LinearLayout.LayoutParams(-1,-2));
        subtitle=label(initial.description(),14,HomeTokens.SECONDARY,false);subtitle.setGravity(Gravity.CENTER);column.addView(subtitle,new LinearLayout.LayoutParams(-1,-2));
        TextView action=label("Show Temper avatar on chat apps",14,HomeTokens.TEXT,false);action.setGravity(Gravity.CENTER);LinearLayout.LayoutParams actionSize=new LinearLayout.LayoutParams(-1,-2);actionSize.topMargin=dp(20);column.addView(action,actionSize);
        toggle=new MasterToggle(context);toggle.setOnClickListener(v->power.accept(enabled));LinearLayout.LayoutParams switchSize=new LinearLayout.LayoutParams(dp(112),dp(60));switchSize.gravity=Gravity.CENTER_HORIZONTAL;column.addView(toggle,switchSize);
        status=label("Currently disabled",12,HomeTokens.MUTED,false);status.setGravity(Gravity.CENTER);column.addView(status,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout info=new LinearLayout(context);info.setPadding(dp(16),dp(14),dp(12),dp(14));info.setGravity(Gravity.CENTER_VERTICAL);info.setBackground(surface(HomeTokens.SURFACE,HomeTokens.BORDER,16));LinearLayout.LayoutParams infoSize=new LinearLayout.LayoutParams(-1,-2);infoSize.topMargin=dp(20);column.addView(info,infoSize);
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);info.addView(copy,new LinearLayout.LayoutParams(0,-2,1));copy.addView(label("Overlay preview",13,HomeTokens.TEXT,true));TextView explanation=label("When enabled, the selected avatar appears as a small floating character over supported chat apps. Tapping it opens Temper analytics.",12,HomeTokens.SECONDARY,false);explanation.setPadding(0,dp(5),dp(8),0);copy.addView(explanation);
        mini=new CharacterView(context);mini.setAvatar(initial);mini.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);info.addView(mini,new LinearLayout.LayoutParams(dp(42),dp(56)));
        bind(false,false);
    }
    public AvatarCarousel carousel(){return carousel;}
    public Avatar selectedAvatar(){return avatar;}
    public void bind(boolean on,boolean pending){enabled=on;toggle.bind(on,pending);pill.setText(on?"Avatar ON":"Avatar OFF");pill.setTextColor(on?HomeTokens.ACTIVE:HomeTokens.INACTIVE);pill.setBackground(surface(on?0xff16322d:0xff35222d,on?0xff286453:0xff684050,18));status.setText(pending?"Waiting for overlay…":on?"Currently active":"Currently disabled");status.setTextColor(on?HomeTokens.ACTIVE:HomeTokens.MUTED);applyIllumination();}
    private void applyIllumination(){ColorMatrix saturation=new ColorMatrix();saturation.setSaturation(enabled?1:.45f);Paint layer=new Paint();layer.setColorFilter(new ColorMatrixColorFilter(saturation));hero.setLayerType(View.LAYER_TYPE_HARDWARE,layer);hero.setAlpha(enabled?1:.8f);stage.accent=avatar.shirt();stage.active=enabled;stage.invalidate();}
    private void updateAvatar(Avatar value,boolean animate){avatar=value;name.setText(value.displayName());subtitle.setText(value.description());mini.setAvatar(value);hero.animate().cancel();if(animate&&hero.isAttachedToWindow()&&android.animation.ValueAnimator.areAnimatorsEnabled()){hero.animate().alpha(0).setDuration(90).withEndAction(()->{hero.setAvatar(value);hero.animate().alpha(enabled?1:.8f).setDuration(180).start();}).start();}else{hero.setAvatar(value);hero.setAlpha(enabled?1:.8f);}stage.accent=value.shirt();stage.invalidate();}
    private TextView label(String text,int sp,int color,boolean bold){TextView v=new TextView(getContext());v.setText(text);v.setTextSize(sp);v.setTextColor(color);v.setFontFeatureSettings("kern");if(bold)v.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));v.setIncludeFontPadding(false);return v;}
    private GradientDrawable surface(int color,int border,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));b.setStroke(dp(1),border);return b;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onDetachedFromWindow(){hero.animate().cancel();hero.setAvatar(avatar);hero.setAlpha(enabled?1:.8f);super.onDetachedFromWindow();}
    private final class Stage extends FrameLayout {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int accent;boolean active;
        private RadialGradient glow;private float glowX,glowY;private int glowColor;
        private void prepareGlow(float x,float y,float radius,int color){if(glow==null||glowX!=x||glowY!=y||glowColor!=color){glowX=x;glowY=y;glowColor=color;glow=new RadialGradient(x,y,radius,new int[]{color,0},null,Shader.TileMode.CLAMP);}}
        Stage(Context c){super(c);setWillNotDraw(false);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);hero.setTranslationX(getWidth()/2f-(hero.getLeft()+hero.getWidth()/2f));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()*.44f,r=dp(115);int rgb=accent&0xffffff;prepareGlow(x,y,r,(active?0x42000000:0x18000000)|rgb);p.setShader(glow);c.drawCircle(x,y,r,p);p.setShader(null);p.setColor(active?0xff2c2c43:0xff232532);c.drawOval(x-dp(82),getHeight()-dp(17),x+dp(82),getHeight()-dp(4),p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(active?0xff685487:HomeTokens.BORDER);c.drawArc(x-dp(82),getHeight()-dp(17),x+dp(82),getHeight()-dp(4),0,180,false,p);p.setStyle(Paint.Style.FILL);}
    }
    private final class IconButton extends android.widget.ImageButton {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int kind;
        IconButton(Context c,int kind){super(c);this.kind=kind;setBackground(null);setPadding(0,0,0,0);}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f;p.setStyle(Paint.Style.FILL);p.setColor(isPressed()?HomeTokens.ELEVATED:HomeTokens.SURFACE);c.drawCircle(x,y,dp(17),p);p.setStyle(Paint.Style.STROKE);p.setColor(HomeTokens.SECONDARY);p.setStrokeWidth(dp(1.6f));p.setStrokeCap(Paint.Cap.ROUND);if(kind==0){c.drawCircle(x,y,dp(5),p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(x+(float)Math.cos(a)*dp(9),y+(float)Math.sin(a)*dp(9),x+(float)Math.cos(a)*dp(11),y+(float)Math.sin(a)*dp(11),p);}}else{c.drawLine(x-kind*dp(3),y-dp(5),x+kind*dp(3),y,p);c.drawLine(x+kind*dp(3),y,x-kind*dp(3),y+dp(5),p);}p.setStyle(Paint.Style.FILL);if(isFocused()){p.setStyle(Paint.Style.STROKE);p.setColor(HomeTokens.ACCENT);c.drawCircle(x,y,dp(20),p);p.setStyle(Paint.Style.FILL);}}
    }
}

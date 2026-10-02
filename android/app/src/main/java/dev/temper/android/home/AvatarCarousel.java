package dev.temper.android.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import dev.temper.android.character.*;
import java.util.List;
import java.util.function.Consumer;

/** Continuous center-lock strip. Seven reusable vector views, regardless of catalog size. */
public final class AvatarCarousel extends android.view.ViewGroup {
    private final List<Avatar> catalog;
    private final CharacterView[] slots=new CharacterView[7];
    private final int[] bound=new int[7];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Consumer<Avatar> selected;
    private float position,downX,downY,start, stride;
    private int activePointer=-1;
    private boolean dragging,vertical,cancelledInteraction;
    private VelocityTracker velocity;
    private ValueAnimator snap;
    private final int slop;
    public AvatarCarousel(Context context,List<Avatar> catalog,Avatar initial,Consumer<Avatar> selected){
        super(context);if(catalog==null||catalog.isEmpty())throw new IllegalArgumentException("Empty catalog");this.catalog=List.copyOf(catalog);this.selected=selected;position=Math.max(0,catalog.indexOf(initial));slop=ViewConfiguration.get(context).getScaledTouchSlop();setWillNotDraw(false);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        for(int i=0;i<slots.length;i++){bound[i]=-1;slots[i]=new CharacterView(context);slots[i].setPortrait(true);slots[i].setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(slots[i]);}updateDescription();
    }
    public int selectedIndex(){return CarouselMath.nearest(position,catalog.size());}
    public float position(){return position;}
    public int catalogSize(){return catalog.size();}
    public void step(int delta){settle(CarouselMath.nearest(position,catalog.size())+delta);}
    public boolean snapping(){return snap!=null&&snap.isRunning();}
    public void setSelectionListener(Consumer<Avatar> listener){selected=listener;}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);setMeasuredDimension(width,resolveSize(Math.round(dp(146)),heightSpec));stride=Math.max(dp(54),Math.min(dp(72),width/5.1f));int w=Math.round(dp(88)),h=Math.round(dp(110));for(var slot:slots)slot.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){layoutSlots();}
    private void layoutSlots(){
        int first=CarouselMath.first(position,catalog.size()),last=CarouselMath.last(position,catalog.size());
        for(int s=0;s<slots.length;s++){int index=first+s;CharacterView view=slots[s];if(index>last){view.setVisibility(INVISIBLE);continue;}view.setVisibility(VISIBLE);if(bound[s]!=index){view.setAvatar(catalog.get(index));bound[s]=index;}
            float distance=index-position,scale=CarouselMath.scale(distance),cx=getWidth()/2f+distance*stride;int w=view.getMeasuredWidth(),h=view.getMeasuredHeight();int x=Math.round(cx-w/2f),y=Math.round(dp(12));view.layout(x,y,x+w,y+h);view.setPivotX(w/2f);view.setPivotY(h/2f);view.setScaleX(scale);view.setScaleY(scale);view.setAlpha(CarouselMath.opacity(distance));view.setTranslationZ(dp(4*(3-Math.min(3,Math.abs(distance)))));
        }
        invalidate();
    }
    @Override protected void onDraw(Canvas c){super.onDraw(c);paint.setColor(HomeTokens.BORDER);c.drawLine(getWidth()/2f-dp(20),getHeight()-dp(8),getWidth()/2f+dp(20),getHeight()-dp(8),paint);paint.setColor(HomeTokens.ACCENT);c.drawCircle(getWidth()/2f,getHeight()-dp(8),dp(3),paint);}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){return true;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(stride<=0)return false;
        if(cancelledInteraction&&event.getActionMasked()!=MotionEvent.ACTION_DOWN){if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)cancelledInteraction=false;return true;}
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN->{cancelSnap();cancelledInteraction=false;activePointer=event.getPointerId(0);downX=event.getX();downY=event.getY();start=position;dragging=vertical=false;if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();velocity.addMovement(event);return true;}
            case MotionEvent.ACTION_MOVE->{int p=event.findPointerIndex(activePointer);if(p<0){finishGesture(true);return true;}float dx=event.getX(p)-downX,dy=event.getY(p)-downY;if(!dragging&&!vertical&&Math.max(Math.abs(dx),Math.abs(dy))>slop){vertical=Math.abs(dy)>Math.abs(dx);dragging=!vertical;if(dragging)getParent().requestDisallowInterceptTouchEvent(true);}if(velocity!=null)velocity.addMovement(event);if(dragging){position=CarouselMath.clamp(start-dx/stride,catalog.size());layoutSlots();}return true;}
            case MotionEvent.ACTION_POINTER_DOWN->{finishGesture(true);cancelledInteraction=true;return true;}
            case MotionEvent.ACTION_POINTER_UP,MotionEvent.ACTION_CANCEL->{finishGesture(true);return true;}
            case MotionEvent.ACTION_UP->{if(velocity!=null)velocity.addMovement(event);if(!dragging&&!vertical){float offset=(event.getX()-getWidth()/2f)/stride;settle(Math.round(position+offset));performClick();finishGesture(false);}else finishGesture(true);return true;}
            default->{return true;}
        }
    }
    private void finishGesture(boolean settle){float speed=0;if(velocity!=null){velocity.computeCurrentVelocity(1000);if(dragging&&activePointer>=0)speed=velocity.getXVelocity(activePointer);velocity.recycle();velocity=null;}activePointer=-1;dragging=false;getParent().requestDisallowInterceptTouchEvent(false);if(settle)settle(CarouselMath.release(position,speed,stride,catalog.size()));}
    private void settle(int destination){
        cancelSnap();int target=CarouselMath.nearest(destination,catalog.size());if(position==target||!isAttachedToWindow()||!ValueAnimator.areAnimatorsEnabled()){position=target;layoutSlots();notifySelection();return;}
        snap=ValueAnimator.ofFloat(position,target);snap.setDuration(HomeTokens.SNAP_MS);snap.setInterpolator(new PathInterpolator(.2f,0,.2f,1));snap.addUpdateListener(a->{position=(float)a.getAnimatedValue();layoutSlots();});snap.addListener(new android.animation.AnimatorListenerAdapter(){private boolean cancelled;@Override public void onAnimationCancel(android.animation.Animator a){cancelled=true;}@Override public void onAnimationEnd(android.animation.Animator a){if(!cancelled){position=target;layoutSlots();notifySelection();}}});snap.start();
    }
    private void notifySelection(){updateDescription();if(selected!=null)selected.accept(catalog.get(selectedIndex()));announceForAccessibility(catalog.get(selectedIndex()).displayName()+" selected");}
    private void updateDescription(){setContentDescription("Avatar carousel. "+catalog.get(selectedIndex()).displayName()+", "+(selectedIndex()+1)+" of "+catalog.size()+". Swipe left or right to choose.");}
    private void cancelSnap(){if(snap!=null){snap.cancel();snap=null;}}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setScrollable(catalog.size()>1);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,0,catalog.size()-1,selectedIndex()));if(selectedIndex()>0)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);if(selectedIndex()<catalog.size()-1)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD){step(1);return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){step(-1);return true;}return super.performAccessibilityAction(action,args);}
    @Override public boolean onKeyDown(int code,KeyEvent event){if(code==KeyEvent.KEYCODE_DPAD_RIGHT){step(1);return true;}if(code==KeyEvent.KEYCODE_DPAD_LEFT){step(-1);return true;}return super.onKeyDown(code,event);}
    @Override protected void onDetachedFromWindow(){cancelSnap();if(velocity!=null){velocity.recycle();velocity=null;}activePointer=-1;position=selectedIndex();super.onDetachedFromWindow();}
}

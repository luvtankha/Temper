package dev.temper.android;
import dev.temper.android.home.CarouselMath;
import org.junit.Test;
import static org.junit.Assert.*;

public class CarouselTests {
    @Test public void centeringAndRenderingStayBoundedForLargeCatalogs(){for(int count:new int[]{1,5,10,20,51,10_000})for(float position:new float[]{-5,0,.49f,.51f,2.4f,18.9f,count-1,count+5}){int center=CarouselMath.nearest(position,count),first=CarouselMath.first(position,count),last=CarouselMath.last(position,count);assertTrue(center>=0&&center<count);assertTrue(first<=center&&last>=center);assertTrue(last-first+1<=7);assertEquals(1,CarouselMath.scale(0),0);assertEquals(1,CarouselMath.opacity(0),0);}}
    @Test public void releaseUsesBoundedVelocityAndNearestCenter(){assertEquals(2,CarouselMath.release(2.49f,0,72,5));assertEquals(3,CarouselMath.release(2.51f,0,72,5));assertEquals(0,CarouselMath.release(.2f,100000,72,5));assertEquals(4,CarouselMath.release(3.8f,-100000,72,5));assertEquals(0,CarouselMath.nearest(Float.NaN,5));}
    @Test public void ownedLegacyArtRemainsUsableWithoutExposingUnownedProducts(){assertEquals(5,dev.temper.android.character.Avatar.homeCatalog(a->false).size());var catalog=dev.temper.android.character.Avatar.homeCatalog(a->a==dev.temper.android.character.Avatar.NOVA);assertEquals(6,catalog.size());assertTrue(catalog.contains(dev.temper.android.character.Avatar.NOVA));assertFalse(catalog.contains(dev.temper.android.character.Avatar.ORBIT));}
}

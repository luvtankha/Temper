package dev.temper.android;

import dev.temper.android.accessibility.AutomaticCapturePolicy;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutomaticCaptureTests {
    @Test public void automaticProcessingRequiresAllFourIndependentGates(){
        for(int mask=0;mask<16;mask++)assertEquals("Automatic eligibility mask "+mask,mask==15,
                AutomaticCapturePolicy.ready((mask&1)!=0,(mask&2)!=0,(mask&4)!=0,(mask&8)!=0));
    }
}

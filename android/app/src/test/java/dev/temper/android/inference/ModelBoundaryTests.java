package dev.temper.android.inference;

import dev.temper.android.adapters.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelBoundaryTests {
    private JSONObject metadata()throws Exception{
        try(var input=new FileInputStream("src/main/assets/emotion/context-model.json")){
            return new JSONObject(new String(input.readAllBytes(),StandardCharsets.UTF_8));
        }
    }
    private ConversationContextModel model(JSONObject metadata)throws Exception{
        return new ConversationContextModel(new ByteArrayInputStream(metadata.toString().getBytes(StandardCharsets.UTF_8)));
    }
    @Test public void handleNormalizationCannotRejectABoundedVisibleMessage()throws Exception{
        String raw="@a ".repeat(333)+"x";assertEquals(1000,raw.length());
        String normalized=OnDeviceAnalysis.normalizedInput(raw);
        assertTrue(normalized.length()<=1000);
        try(var input=new FileInputStream("src/main/assets/emotion/tokenizer.bin")){
            long[] tokens=new RobertaTokenizer(input).encode(normalized,128);
            assertEquals(128,tokens.length);assertEquals(0,tokens[0]);assertEquals(2,tokens[tokens.length-1]);
        }
        String emojiBoundary=OnDeviceAnalysis.normalizedInput("x".repeat(993)+" @a😀");
        assertEquals(999,emojiBoundary.length());assertFalse(Character.isHighSurrogate(emojiBoundary.charAt(emojiBoundary.length()-1)));
    }
    @Test public void directionMetadataRejectsCorruptLabelsBiasThresholdAndModelIdentity()throws Exception{
        JSONObject valid=metadata();assertNotNull(model(valid));
        JSONObject bad=metadata();bad.getJSONArray("labels").put(0,"HAPPY");assertThrows(IllegalArgumentException.class,()->model(bad));
        JSONObject duplicate=metadata();duplicate.getJSONArray("labels").put(0,"STABLE");assertThrows(IllegalArgumentException.class,()->model(duplicate));
        JSONObject bias=metadata();bias.getJSONArray("bias").put(0,"NaN");assertThrows(IllegalArgumentException.class,()->model(bias));
        JSONObject threshold=metadata().put("minimum",-1);assertThrows(IllegalArgumentException.class,()->model(threshold));
        JSONObject margin=metadata().put("margin","Infinity");assertThrows(IllegalArgumentException.class,()->model(margin));
        JSONObject identity=metadata().put("baseModelSha256","unknown");assertThrows(IllegalArgumentException.class,()->model(identity));
    }
    @Test public void directionRejectsInvalidFeaturesAndPriorSpectrum()throws Exception{
        ConversationContextModel model=model(metadata());
        double[] features=new double[116];features[0]=Double.NaN;
        assertThrows(IllegalArgumentException.class,()->model.probabilities(features));
        features[0]=Double.POSITIVE_INFINITY;assertThrows(IllegalArgumentException.class,()->model.probabilities(features));
        JSONObject oversized=metadata();oversized.getJSONArray("weights").getJSONArray(0).put(0,Double.MAX_VALUE);
        ConversationContextModel overflowing=model(oversized);features[0]=2;
        assertThrows(IllegalArgumentException.class,()->overflowing.probabilities(features));
        var snapshot=new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),List.of(new VisibleConversation.Turn("b".repeat(64),VisibleConversation.Role.REMOTE,"Fictional incoming message")),new ScreenObservation.Bounds(0,100,400,150));
        float[] prior=new float[8];prior[0]=Float.NaN;
        assertThrows(IllegalArgumentException.class,()->model.features(snapshot,new float[8],prior));
    }
    @Test public void canonicalGoEmotionIndicesAndIndependentScoresRemainValid(){
        int[] labels={27,17,14,6,25,3,2,26};
        for(int index=0;index<labels.length;index++){
            float[] logits=new float[28];Arrays.fill(logits,-10);logits[labels[index]]=10;
            float[] values=EmotionEstimate.spectrum(logits);
            assertTrue(values[index]>.99f);for(int other=0;other<8;other++)if(other!=index)assertTrue(values[other]<.001f);
        }
        float[] logits=new float[28];Arrays.fill(logits,-10);logits[14]=10;logits[19]=10;logits[17]=10;
        float[] values=EmotionEstimate.spectrum(logits);assertTrue(values[1]>.99f&&values[2]>.99f);
        logits[0]=Float.NaN;assertThrows(IllegalArgumentException.class,()->EmotionEstimate.spectrum(logits));
        logits[0]=Float.POSITIVE_INFINITY;assertThrows(IllegalArgumentException.class,()->EmotionEstimate.spectrum(logits));
    }
}

package dev.temper.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import ai.onnxruntime.*;
import dev.temper.android.inference.*;

public class TokenizerModelTests {
    private RobertaTokenizer tokenizer()throws Exception{try(InputStream vocab=new FileInputStream("src/main/assets/emotion/vocab.json");InputStream merges=new FileInputStream("src/main/assets/emotion/merges.txt")){return new RobertaTokenizer(vocab,merges);}}
    @Test public void bpeMatchesIndependentHuggingFaceReferences()throws Exception{
        RobertaTokenizer tokenizer=tokenizer();try(InputStream input=getClass().getResourceAsStream("/roberta-reference.json")){JSONArray fixtures=new JSONArray(new String(input.readAllBytes(),StandardCharsets.UTF_8));assertTrue(fixtures.length()>=30);for(int i=0;i<fixtures.length();i++){JSONObject fixture=fixtures.getJSONObject(i);JSONArray raw=fixture.getJSONArray("ids");long[] ids=new long[raw.length()];for(int j=0;j<ids.length;j++)ids[j]=raw.getLong(j);assertArrayEquals("Tokenizer fixture "+i,ids,tokenizer.encode(fixture.getString("text"),128));}}
    }
    @Test public void int8ModelProducesRealEmotionScores()throws Exception{
        String path=System.getenv("TEMPER_TEST_MODEL");Assume.assumeTrue(path!=null&&new File(path).isFile());RobertaTokenizer tokenizer=tokenizer();OrtEnvironment environment=OrtEnvironment.getEnvironment();
        try(var options=new OrtSession.SessionOptions();var session=environment.createSession(path,options)){
            String[] texts={"The meeting starts at ten.","I am furious with you!","I got the job! I'm so happy!","I feel so sad and lonely.","Wait, I am confused. What does this mean?"};int[] expected={0,6,1,4,3};
            for(int i=0;i<texts.length;i++){long[] ids=tokenizer.encode(texts[i],128),mask=new long[ids.length];Arrays.fill(mask,1);try(var input=OnnxTensor.createTensor(environment,new long[][]{ids});var attention=OnnxTensor.createTensor(environment,new long[][]{mask});var result=session.run(Map.of("input_ids",input,"attention_mask",attention))){float[] spectrum=EmotionEstimate.spectrum(((float[][])result.get(0).getValue())[0]);int max=0;for(int j=1;j<8;j++)if(spectrum[j]>spectrum[max])max=j;assertEquals("Generated model fixture "+i,expected[i],max);assertTrue(spectrum[max]>.3);}}
        }
    }
}

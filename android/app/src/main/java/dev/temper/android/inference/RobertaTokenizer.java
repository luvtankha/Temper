package dev.temper.android.inference;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import org.json.JSONObject;

/** RoBERTa byte-level BPE, no native tokenizer or text persistence. */
public final class RobertaTokenizer {
    private static final String SPACE="\\p{Z}\\t\\n\\r\\f\\u000B\\u0085";
    private static final Pattern WORDS=Pattern.compile("'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^"+SPACE+"\\p{L}\\p{N}]+|["+SPACE+"]+(?![^"+SPACE+"])|["+SPACE+"]+");
    private static final Pattern SPECIAL=Pattern.compile("<s>|</s>|<unk>|<pad>|<mask>");
    private final Map<String,Integer> vocab=new HashMap<>(),ranks=new HashMap<>();
    private final char[] bytes=new char[256];
    public RobertaTokenizer(InputStream vocabulary,InputStream merges)throws Exception{
        JSONObject json=new JSONObject(new String(BoundedIo.read(vocabulary,2_000_000),StandardCharsets.UTF_8));for(Iterator<String> keys=json.keys();keys.hasNext();){String key=keys.next();vocab.put(key,json.getInt(key));}
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(merges,StandardCharsets.UTF_8))){String line;int rank=0;while((line=reader.readLine())!=null)if(!line.startsWith("#")&&!line.isBlank())ranks.put(line,rank++);}
        int extra=0;for(int i=0;i<256;i++){boolean visible=i>=33&&i<=126||i>=161&&i<=172||i>=174;bytes[i]=(char)(visible?i:256+extra++);}
    }
    public long[] encode(String text,int limit){
        if(text==null||text.isBlank()||text.length()>1000||limit<4||limit>512)throw new IllegalArgumentException("Invalid model input");
        List<Long> result=new ArrayList<>();result.add(0L);Matcher special=SPECIAL.matcher(text);int start=0;
        while(special.find()){
            String before=text.substring(start,special.start());if("<mask>".equals(special.group()))before=before.replaceFirst("["+SPACE+"]+$","");
            tokenize(before,result,limit);if(result.size()<limit-1)result.add((long)vocab.get(special.group()));start=special.end();
        }
        tokenize(text.substring(start),result,limit);result.add(2L);return result.stream().mapToLong(Long::longValue).toArray();
    }
    private void tokenize(String text,List<Long> result,int limit){
        Matcher words=WORDS.matcher(text);while(words.find()&&result.size()<limit-1){
            byte[] utf8=words.group().getBytes(StandardCharsets.UTF_8);List<String> pieces=new ArrayList<>(utf8.length);for(byte value:utf8)pieces.add(String.valueOf(bytes[value&255]));
            while(pieces.size()>1){int best=Integer.MAX_VALUE;String pair=null;for(int i=0;i<pieces.size()-1;i++){String next=pieces.get(i)+" "+pieces.get(i+1);Integer rank=ranks.get(next);if(rank!=null&&rank<best){best=rank;pair=next;}}if(pair==null)break;
                List<String> merged=new ArrayList<>();for(int i=0;i<pieces.size();i++){if(i+1<pieces.size()&&(pieces.get(i)+" "+pieces.get(i+1)).equals(pair))merged.add(pieces.get(i)+pieces.get(++i));else merged.add(pieces.get(i));}pieces=merged;
            }
            for(String piece:pieces){if(result.size()>=limit-1)break;Integer id=vocab.get(piece);if(id==null)throw new IllegalStateException("Invalid vocabulary");result.add((long)id);}
        }
    }
}

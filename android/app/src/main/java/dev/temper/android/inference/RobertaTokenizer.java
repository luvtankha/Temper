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
    private record Merge(int rank,int token){}
    private record Pair(int left,int right,int leftToken,int rightToken,Merge merge){}
    private long[] mergeKeys;
    private int[] mergeRanks,mergeTokens;
    private final Map<String,Integer> specials=new HashMap<>();
    private final int[] byteTokens=new int[256];
    private static long key(int left,int right){return ((long)left<<32)|(right&0xffffffffL);}
    public RobertaTokenizer(InputStream vocabulary,InputStream input)throws Exception{
        JSONObject json=new JSONObject(new String(BoundedIo.read(vocabulary,2_000_000),StandardCharsets.UTF_8));
        int extra=0;for(int i=0;i<256;i++){boolean visible=i>=33&&i<=126||i>=161&&i<=172||i>=174;byteTokens[i]=json.getInt(String.valueOf((char)(visible?i:256+extra++)));}
        for(String token:List.of("<s>","</s>","<unk>","<pad>","<mask>"))specials.put(token,json.getInt(token));
        Map<Long,Merge> merges=new TreeMap<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){String line;int rank=0;while((line=reader.readLine())!=null)if(!line.startsWith("#")&&!line.isBlank()){int separator=line.indexOf(' ');String left=line.substring(0,separator),right=line.substring(separator+1);merges.put(key(json.getInt(left),json.getInt(right)),new Merge(rank++,json.getInt(left+right)));}}
        mergeKeys=new long[merges.size()];mergeRanks=new int[merges.size()];mergeTokens=new int[merges.size()];int index=0;for(var entry:merges.entrySet()){mergeKeys[index]=entry.getKey();mergeRanks[index]=entry.getValue().rank();mergeTokens[index++]=entry.getValue().token();}
    }
    /** Compact tables compiled from the pinned vocab/merges; no JSON object graph at startup. */
    public RobertaTokenizer(InputStream compiled)throws IOException{
        DataInputStream input=new DataInputStream(new BufferedInputStream(compiled));
        if(input.readInt()!=0x54425031)throw new IOException("Invalid tokenizer");int count=input.readInt();if(count!=49992)throw new IOException("Invalid merge count");
        for(int i=0;i<256;i++)byteTokens[i]=input.readInt();
        for(String token:List.of("<s>","</s>","<unk>","<pad>","<mask>"))specials.put(token,input.readInt());
        mergeKeys=new long[count];mergeRanks=new int[count];mergeTokens=new int[count];
        for(int i=0;i<count;i++){mergeKeys[i]=input.readLong();mergeRanks[i]=input.readInt();mergeTokens[i]=input.readInt();if(i>0&&mergeKeys[i]<=mergeKeys[i-1])throw new IOException("Unsorted merges");}
        if(input.read()!=-1)throw new IOException("Trailing tokenizer data");
    }
    public long[] encode(String text,int limit){
        if(text==null||text.isBlank()||text.length()>1000||limit<4||limit>512)throw new IllegalArgumentException("Invalid model input");
        long[] result=new long[limit];result[0]=0;int count=1;Matcher special=SPECIAL.matcher(text);int start=0;
        while(count<limit-1&&special.find()){
            String before=text.substring(start,special.start());if("<mask>".equals(special.group()))before=before.replaceFirst("["+SPACE+"]+$","");
            count=tokenize(before,result,count);if(count<limit-1)result[count++]=specials.get(special.group());start=special.end();
        }
        count=tokenize(text.substring(start),result,count);result[count++]=2;return Arrays.copyOf(result,count);
    }
    private void offer(PriorityQueue<Pair> queue,int left,int[] ids,int[] next){
        if(left<0||next[left]<0)return;int right=next[left];int found=Arrays.binarySearch(mergeKeys,key(ids[left],ids[right]));if(found>=0)queue.add(new Pair(left,right,ids[left],ids[right],new Merge(mergeRanks[found],mergeTokens[found])));
    }
    private int tokenize(String text,long[] result,int count){
        Matcher words=WORDS.matcher(text);while(count<result.length-1&&words.find()){
            byte[] utf8=words.group().getBytes(StandardCharsets.UTF_8);int n=utf8.length;int[] ids=new int[n],next=new int[n],previous=new int[n];boolean[] alive=new boolean[n];
            for(int i=0;i<n;i++){ids[i]=byteTokens[utf8[i]&255];next[i]=i==n-1?-1:i+1;previous[i]=i-1;alive[i]=true;}
            PriorityQueue<Pair> queue=new PriorityQueue<>(Comparator.<Pair>comparingInt(p->p.merge().rank()).thenComparingInt(Pair::left));
            for(int i=0;i<n-1;i++)offer(queue,i,ids,next);
            while(!queue.isEmpty()){
                Pair pair=queue.remove();int left=pair.left(),right=pair.right();
                if(!alive[left]||!alive[right]||next[left]!=right||ids[left]!=pair.leftToken()||ids[right]!=pair.rightToken())continue;
                ids[left]=pair.merge().token();next[left]=next[right];alive[right]=false;if(next[left]>=0)previous[next[left]]=left;
                offer(queue,previous[left],ids,next);offer(queue,left,ids,next);
            }
            for(int i=0;i>=0&&count<result.length-1;i=next[i])result[count++]=ids[i];
        }
        return count;
    }
}

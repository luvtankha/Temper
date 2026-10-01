package dev.temper.android.auth;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Device-only demo account; not remote API authentication. Sensitive state is Keystore-encrypted. */
public final class DeviceAccountStore {
    private static final int ITERATIONS=210_000;
    private final SharedPreferences preferences;private final String alias;
    public DeviceAccountStore(Context context){this(context,"temper_device_account");}
    public DeviceAccountStore(Context context,String storage){preferences=context.getSharedPreferences(storage,Context.MODE_PRIVATE);alias="dev.temper.auth."+storage;}
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(alias))return (SecretKey)store.getKey(alias,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey();
    }
    private String encode(byte[] value){return Base64.encodeToString(value,Base64.NO_WRAP);}
    private byte[] decode(String value){return Base64.decode(value,Base64.NO_WRAP);}
    private JSONObject read() throws Exception {
        String blob=preferences.getString("encrypted",null);if(blob==null)return new JSONObject();
        String[] parts=blob.split(":",-1);if(parts.length!=2)throw new GeneralSecurityException("Invalid account state");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,decode(parts[0])));cipher.updateAAD(alias.getBytes(StandardCharsets.UTF_8));
        return new JSONObject(new String(cipher.doFinal(decode(parts[1])),StandardCharsets.UTF_8));
    }
    private void write(JSONObject state) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());cipher.updateAAD(alias.getBytes(StandardCharsets.UTF_8));
        String blob=encode(cipher.getIV())+":"+encode(cipher.doFinal(state.toString().getBytes(StandardCharsets.UTF_8)));
        if(!preferences.edit().putString("encrypted",blob).commit())throw new IllegalStateException("Account storage unavailable");
    }
    private byte[] hash(char[] password,byte[] salt) throws Exception {PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    private String username(String name){if(name==null||!name.trim().matches("[A-Za-z0-9_.-]{3,40}"))throw new IllegalArgumentException("Use 3–40 letters, digits, dots, dashes or underscores");return name.trim().toLowerCase(Locale.ROOT);}
    public synchronized void create(String name,char[] password) throws Exception {
        try{String user=username(name);if(password==null||password.length<10||password.length>128)throw new IllegalArgumentException("Use a password of 10–128 characters");
            JSONObject state=read();if(state.has("username"))throw new IllegalArgumentException("A device account already exists; sign in instead");
            byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);state.put("username",user).put("salt",encode(salt)).put("hash",encode(hash(password,salt))).put("session",user);write(state);
        }finally{if(password!=null)Arrays.fill(password,'\0');}
    }
    public synchronized void signIn(String name,char[] password) throws Exception {
        try{String user=username(name);if(password==null||password.length>128)throw new IllegalArgumentException("Invalid credentials");JSONObject state=read();
            if(!state.has("username")||!user.equals(state.getString("username"))||!MessageDigest.isEqual(hash(password,decode(state.getString("salt"))),decode(state.getString("hash"))))throw new IllegalArgumentException("Invalid credentials");
            state.put("session",user);write(state);
        }finally{if(password!=null)Arrays.fill(password,'\0');}
    }
    public synchronized String session() throws Exception {JSONObject state=read();String user=state.optString("session",null);return user!=null&&user.equals(state.optString("username",null))?user:null;}
    public synchronized void signOut() throws Exception {JSONObject state=read();state.remove("session");write(state);}
}

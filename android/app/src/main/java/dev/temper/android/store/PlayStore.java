package dev.temper.android.store;

import android.app.Activity;
import android.os.*;
import com.android.billingclient.api.*;
import dev.temper.android.BuildConfig;
import dev.temper.android.character.Avatar;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Non-consumable cosmetics. Prices and purchase UI come exclusively from Google Play. */
public final class PlayStore implements AutoCloseable {
    private final Activity activity;
    private final BillingClient billing;
    private final PurchaseStore ownership;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<String,ProductDetails> products=new HashMap<>();
    private final Set<String> verifying=new HashSet<>();
    private final Runnable changed;
    private boolean closed;
    private String status="Connecting to Google Play…";
    public PlayStore(Activity activity,Runnable changed){
        this.activity=activity;this.changed=changed;ownership=new PurchaseStore(activity);
        billing=BillingClient.newBuilder(activity).setListener((result,purchases)->{
            dev.temper.android.overlay.FloatingOverlayService.checkoutVisible(false);
            if(closed)return;
            if(result.getResponseCode()==BillingClient.BillingResponseCode.OK&&purchases!=null)purchases.forEach(this::verify);
            else if(result.getResponseCode()==BillingClient.BillingResponseCode.USER_CANCELED)update("Purchase cancelled");
            else if(result.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)restore();
            else update("Purchase unavailable. Please retry in Google Play.");
        }).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).enableAutoServiceReconnection().build();
        billing.startConnection(new BillingClientStateListener(){
            @Override public void onBillingSetupFinished(BillingResult result){if(closed)return;if(result.getResponseCode()==0){loadProducts();restore();}else update("Google Play is unavailable on this installation");}
            @Override public void onBillingServiceDisconnected(){if(!closed)update("Google Play disconnected. Reopen the shop to retry.");}
        });
    }
    public boolean configured(){try{return new URI(BuildConfig.STORE_URL).getScheme().equals("https")&&!BuildConfig.ENTITLEMENT_KEY.isBlank();}catch(Exception invalid){return false;}}
    public String status(){return configured()?status:"Premium avatars open after the Play store launch";}
    public boolean owned(Avatar avatar){return ownership.owned(avatar);}
    private void update(String value){if(closed)return;status=value;changed.run();}
    private void loadProducts(){
        List<QueryProductDetailsParams.Product> request=new ArrayList<>();for(Avatar avatar:Avatar.values())if(!avatar.free())request.add(QueryProductDetailsParams.Product.newBuilder().setProductId(avatar.productId()).setProductType(BillingClient.ProductType.INAPP).build());
        billing.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(request).build(),(result,response)->{
            if(closed)return;products.clear();if(result.getResponseCode()==0)for(ProductDetails product:response.getProductDetailsList())products.put(product.getProductId(),product);
            update(products.isEmpty()?"No premium products available yet":"One-time upgrades • no subscription");
        });
    }
    private ProductDetails.OneTimePurchaseOfferDetails offer(Avatar avatar){ProductDetails product=products.get(avatar.productId());if(product==null)return null;var offers=product.getOneTimePurchaseOfferDetailsList();return offers!=null&&offers.size()==1?offers.get(0):null;}
    public String price(Avatar avatar){var offer=offer(avatar);return offer==null?null:offer.getFormattedPrice();}
    public boolean canBuy(Avatar avatar){return configured()&&billing.isReady()&&!owned(avatar)&&offer(avatar)!=null;}
    public void buy(Avatar avatar){
        if(!canBuy(avatar)){update("This avatar is not available for purchase yet");return;}
        var item=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(products.get(avatar.productId())).setOfferToken(offer(avatar).getOfferToken()).build();
        dev.temper.android.overlay.FloatingOverlayService.checkoutVisible(true);
        var result=billing.launchBillingFlow(activity,BillingFlowParams.newBuilder().setProductDetailsParamsList(List.of(item)).build());if(result.getResponseCode()!=0){dev.temper.android.overlay.FloatingOverlayService.checkoutVisible(false);update("Purchase could not start. Please retry.");}
    }
    public void restore(){
        if(closed||!billing.isReady())return;
        billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),(result,purchases)->{
            if(closed)return;if(result.getResponseCode()!=0){update("Restore unavailable. Connect to Google Play and retry.");return;}
            Set<String> present=new HashSet<>();for(Purchase purchase:purchases){present.addAll(purchase.getProducts());verify(purchase);}
            for(Avatar avatar:Avatar.values())if(!avatar.free()&&!present.contains(avatar.productId()))ownership.remove(avatar.productId());
            changed.run();
        });
    }
    private void verify(Purchase purchase){
        if(purchase.getPurchaseState()==Purchase.PurchaseState.PENDING){update("Payment pending. The avatar unlocks after payment completes.");return;}
        if(purchase.getPurchaseState()!=Purchase.PurchaseState.PURCHASED||!configured())return;
        for(String product:purchase.getProducts())if(Avatar.fromProduct(product).isPresent()&&verifying.add(product)){
            update("Verifying purchase…");
            worker.execute(()->{
                String envelope=null;int responseCode=0;
                try{
                    HttpURLConnection connection=(HttpURLConnection)new URL(BuildConfig.STORE_URL+"/api/store/verify").openConnection();
                    try{
                        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(15000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");
                        byte[] body=new JSONObject().put("productId",product).put("purchaseData",purchase.getOriginalJson()).put("signature",purchase.getSignature()).toString().getBytes(StandardCharsets.UTF_8);
                        connection.setFixedLengthStreamingMode(body.length);try(OutputStream output=connection.getOutputStream()){output.write(body);}finally{Arrays.fill(body,(byte)0);}
                        responseCode=connection.getResponseCode();if(responseCode==200)try(InputStream input=connection.getInputStream()){byte[] bytes=dev.temper.android.inference.BoundedIo.read(input,8192);envelope=new String(bytes,StandardCharsets.UTF_8);}
                    }finally{connection.disconnect();}
                }catch(Exception unavailable){}
                String verified=envelope;int code=responseCode;
                main.post(()->{if(closed)return;verifying.remove(product);try{if(verified==null){if(code==403)ownership.remove(product);update("Verification unavailable. Tap Restore purchases to retry.");return;}ownership.save(product,verified);update("Avatar unlocked • select it below");}catch(IllegalArgumentException invalid){update("Purchase verification failed. Restore to retry.");}});
            });
        }
    }
    @Override public void close(){closed=true;billing.endConnection();worker.shutdownNow();}
}

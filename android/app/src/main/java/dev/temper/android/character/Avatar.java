package dev.temper.android.character;

import java.util.*;

/** Original code-drawn companions. Prices are supplied by the store, never this catalog. */
public enum Avatar {
    ALEX("alex","Alex","A calm companion for every conversation",null,0xffffc894,0xff503334,0xff795f89,0xff332839),
    NOVA("nova","Nova","Warmth, style and expressive long hair","temper_avatar_nova",0xffd99163,0xff291b25,0xff26212e,0xff17151f),
    ORBIT("orbit","Orbit","A curious little fox with a bright personality","temper_avatar_orbit",0xffffc078,0xffdb7036,0xff3a857d,0xff244e54),
    LUMA("luma","Luma","A gentle robot with a luminous face","temper_avatar_luma",0xffd2e8ff,0xff607891,0xff7787ca,0xff394d78),
    STUDIO_NOVA("studio_nova","Nova","Calm and observant",null,0xffffddcf,0xffe8dce8,0xff986479,0xff453347,dev.temper.android.R.drawable.avatar_studio_nova),
    KAI("kai","Kai","Focused and adaptive",null,0xffffd4bc,0xff292334,0xff9175eb,0xff342840,dev.temper.android.R.drawable.avatar_kai),
    ASTRA("astra","Astra","Thoughtful and grounded",null,0xffffddc9,0xff638e8b,0xff68bcae,0xff263f44,dev.temper.android.R.drawable.avatar_astra),
    MIRA("mira","Mira","Analytical and intuitive",null,0xffffd6c7,0xff30232f,0xffd97390,0xff462736,dev.temper.android.R.drawable.avatar_mira),
    VOLT("volt","Volt","Energetic and expressive",null,0xffffdac2,0xff4c80c9,0xff71b5ea,0xff263751,dev.temper.android.R.drawable.avatar_volt);
    private final String id,name,description,product;
    private final int skin,hair,shirt,shoes,artResource;
    Avatar(String id,String name,String description,String product,int skin,int hair,int shirt,int shoes){this(id,name,description,product,skin,hair,shirt,shoes,0);}
    Avatar(String id,String name,String description,String product,int skin,int hair,int shirt,int shoes,int artResource){this.artResource=artResource;this.id=id;this.name=name;this.description=description;this.product=product;this.skin=skin;this.hair=hair;this.shirt=shirt;this.shoes=shoes;}
    public String id(){return id;}public String displayName(){return name;}public String description(){return description;}public String productId(){return product;}public boolean free(){return product==null;}
    public int skin(){return skin;}public int hair(){return hair;}public int shirt(){return shirt;}public int shoes(){return shoes;}
    /** The home is catalog driven. Historical paid companions keep their entitlement rules. */
    public static List<Avatar> homeCatalog(){return List.of(STUDIO_NOVA,KAI,ASTRA,MIRA,VOLT);}
    /** Already-owned legacy artwork remains usable, without shop or locked-card UI. */
    public static List<Avatar> homeCatalog(java.util.function.Predicate<Avatar> owned){List<Avatar> catalog=new ArrayList<>(homeCatalog());for(Avatar avatar:values())if(!avatar.free()&&owned.test(avatar))catalog.add(avatar);return List.copyOf(catalog);}
    public boolean studio(){return artResource!=0;}
    /** Any Android Drawable resource can supply the body, including PNG/WebP or vectors. */
    public int artResource(){return artResource;}
    public static Avatar fromId(String id){return Arrays.stream(values()).filter(a->a.id.equals(id)).findFirst().orElse(ALEX);}
    public static Optional<Avatar> fromProduct(String id){return Arrays.stream(values()).filter(a->Objects.equals(a.product,id)&&!a.free()).findFirst();}
}

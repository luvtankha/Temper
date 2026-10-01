package dev.temper.android.character;

import java.util.*;

/** Original code-drawn companions. Prices are supplied by the store, never this catalog. */
public enum Avatar {
    ALEX("alex","Alex","A calm companion for every conversation",null,0xffffc894,0xff503334,0xff795f89,0xff332839),
    NOVA("nova","Nova","Warmth, style and expressive long hair","temper_avatar_nova",0xffd99163,0xff291b25,0xff26212e,0xff17151f),
    ORBIT("orbit","Orbit","A curious little fox with a bright personality","temper_avatar_orbit",0xffffc078,0xffdb7036,0xff3a857d,0xff244e54),
    LUMA("luma","Luma","A gentle robot with a luminous face","temper_avatar_luma",0xffd2e8ff,0xff607891,0xff7787ca,0xff394d78);
    private final String id,name,description,product;
    private final int skin,hair,shirt,shoes;
    Avatar(String id,String name,String description,String product,int skin,int hair,int shirt,int shoes){this.id=id;this.name=name;this.description=description;this.product=product;this.skin=skin;this.hair=hair;this.shirt=shirt;this.shoes=shoes;}
    public String id(){return id;}public String displayName(){return name;}public String description(){return description;}public String productId(){return product;}public boolean free(){return product==null;}
    public int skin(){return skin;}public int hair(){return hair;}public int shirt(){return shirt;}public int shoes(){return shoes;}
    public static Avatar fromId(String id){return Arrays.stream(values()).filter(a->a.id.equals(id)).findFirst().orElse(ALEX);}
    public static Optional<Avatar> fromProduct(String id){return Arrays.stream(values()).filter(a->Objects.equals(a.product,id)&&!a.free()).findFirst();}
}

package dev.temper.android.character;

/** Presentation states; these are estimates, not claims about a person's feelings. */
public enum Emotion {
    NEUTRAL("Neutral", 0,0,0,0,1,2,0),
    HAPPY("Happy", -1,-2,0,-1,.35f,6,3),
    CONCERNED("Concerned", 0,-3,3,0,.85f,-2,0),
    CONFUSED("Confused", 5,-4,-1,2,.8f,-1,0),
    SAD("Sad", -4,-2,3,2,.55f,-5,0),
    FRUSTRATED("Frustrated", 0,2,-3,-1,.45f,0,1),
    ANGRY("Angry", -2,3,-4,-2,.75f,-3,4),
    SURPRISED("Surprised", 0,-5,0,-1,1.35f,0,7);

    private final String label;
    private final float[] pose;
    Emotion(String label,float tilt,float browHeight,float browSlope,float shoulder,float eyes,float smile,float mouthOpen){
        this.label=label;pose=new float[]{tilt,browHeight,browSlope,shoulder,eyes,smile,mouthOpen};
    }
    public String label(){return label;}
    public float[] pose(){return pose.clone();}
}

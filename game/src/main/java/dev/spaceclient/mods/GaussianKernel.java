package dev.spaceclient.mods;

/** Symmetric, normalized 17-tap kernel. Computed once per draw, not per pixel. */
public final class GaussianKernel {
    private GaussianKernel(){}
    public static float[] weights(int strength){
        if(strength<1||strength>8)throw new IllegalArgumentException("Blur radius");
        float[] weights=new float[9];double sigma=strength/2.0,total=0;
        for(int i=0;i<weights.length;i++){weights[i]=(float)Math.exp(-i*i/(2*sigma*sigma));total+=weights[i]*(i==0?1:2);}
        for(int i=0;i<weights.length;i++)weights[i]/=total;
        return weights;
    }
}

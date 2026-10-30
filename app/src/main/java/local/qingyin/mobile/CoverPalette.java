package local.qingyin.mobile;
import android.graphics.Bitmap;
import android.graphics.Color;
/** Representative cover colors with a minimum area requirement for colored accents. */
final class CoverPalette {
    private CoverPalette(){}
    static int extract(Bitmap image){
        double[] count=new double[512],red=new double[512],green=new double[512],blue=new double[512];
        double[] hueWeight=new double[24],hueCount=new double[24],hueR=new double[24],hueG=new double[24],hueB=new double[24];
        float[] hsv=new float[3];double chromaCount=0,samples=0;
        double total=0,totalR=0,totalG=0,totalB=0;
        int width=image.getWidth(),height=image.getHeight();
        for(int y=0;y<28;y++)for(int x=0;x<28;x++){
            int color=image.getPixel(Math.min(width-1,(int)((x+.5)*width/28)),Math.min(height-1,(int)((y+.5)*height/28)));
            if(Color.alpha(color)<128)continue;
            samples++;
            int r=Color.red(color),g=Color.green(color),b=Color.blue(color),max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
            double weight=max<24?.45:min>235?.55:1;
            int bucket=(r>>5)*64+(g>>5)*8+(b>>5);
            count[bucket]+=weight;red[bucket]+=r*weight;green[bucket]+=g*weight;blue[bucket]+=b*weight;
            total+=weight;totalR+=r*weight;totalG+=g*weight;totalB+=b*weight;
            Color.colorToHSV(color,hsv);
            if(hsv[1]>=.18f&&hsv[2]>=.14f&&hsv[2]<=.98f){
                int hue=Math.min(23,(int)(hsv[0]/15));
                double strength=.55+.45*hsv[1];
                hueWeight[hue]+=strength;hueCount[hue]++;chromaCount++;
                hueR[hue]+=r*strength;hueG[hue]+=g*strength;hueB[hue]+=b*strength;
            }
        }
        if(total==0)return 0xff50515C;
        int chosen=0;for(int i=1;i<512;i++)if(count[i]>count[chosen])chosen=i;
        int base=Color.rgb((int)(red[chosen]/count[chosen]*.74+totalR/total*.26),
            (int)(green[chosen]/count[chosen]*.74+totalG/total*.26),(int)(blue[chosen]/count[chosen]*.74+totalB/total*.26));
        if(chromaCount<samples*.10)return base;
        int family=0;double best=-1;
        for(int i=0;i<24;i++){double score=hueWeight[(i+23)%24]+hueWeight[i]+hueWeight[(i+1)%24];if(score>best){best=score;family=i;}}
        double mass=0,weights=0,r=0,g=0,b=0;
        for(int offset=-1;offset<=1;offset++){int bin=(family+offset+24)%24;mass+=hueCount[bin];weights+=hueWeight[bin];r+=hueR[bin];g+=hueG[bin];b+=hueB[bin];}
        if(mass<samples*.06||weights==0)return base;
        // Sparse accents tint the neutral base lightly; broad color areas receive more weight.
        double mix=Math.max(.28,Math.min(.88,(mass/samples-.055)/.20));
        return Color.rgb((int)(Color.red(base)*(1-mix)+r/weights*mix),
            (int)(Color.green(base)*(1-mix)+g/weights*mix),(int)(Color.blue(base)*(1-mix)+b/weights*mix));
    }
}

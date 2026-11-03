package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import dev.spaceclient.hud.HudIconState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;

/** Own GLSL 1.20 separable blur. No replacement of Minecraft's active shader. */
public final class InventoryBlurRenderer {
    private static Framebuffer a,b;private static int program,w,h;private static boolean failed;
    private InventoryBlurRenderer(){}
    public static boolean supported(){return !failed&&GLContext.getCapabilities().OpenGL20;}
    private static int shader(int type,String source){int s=GL20.glCreateShader(type);GL20.glShaderSource(s,source);GL20.glCompileShader(s);if(GL20.glGetShaderi(s,GL20.GL_COMPILE_STATUS)==0){GL20.glDeleteShader(s);throw new IllegalStateException("Blur shader compile failed");}return s;}
    private static void init(){
        if(program!=0)return;int vs=0,fs=0;try{vs=shader(GL20.GL_VERTEX_SHADER,"#version 120\nvarying vec2 uv;void main(){uv=gl_MultiTexCoord0.xy;gl_Position=gl_Vertex;}");
            fs=shader(GL20.GL_FRAGMENT_SHADER,"#version 120\nuniform sampler2D image;uniform vec2 stepSize;uniform float weights[9];varying vec2 uv;void main(){vec4 c=texture2D(image,uv)*weights[0];for(int i=1;i<=8;i++){vec2 offset=stepSize*float(i);c+=(texture2D(image,uv+offset)+texture2D(image,uv-offset))*weights[i];}gl_FragColor=c;}");program=GL20.glCreateProgram();GL20.glAttachShader(program,vs);GL20.glAttachShader(program,fs);GL20.glLinkProgram(program);if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException("Blur shader link failed");
        }finally{if(vs!=0)GL20.glDeleteShader(vs);if(fs!=0)GL20.glDeleteShader(fs);}
    }
    private static void quad(){GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2f(0,0);GL11.glVertex2f(-1,-1);GL11.glTexCoord2f(1,0);GL11.glVertex2f(1,-1);GL11.glTexCoord2f(1,1);GL11.glVertex2f(1,1);GL11.glTexCoord2f(0,1);GL11.glVertex2f(-1,1);GL11.glEnd();}
    private static void pass(Framebuffer source,Framebuffer target,float dx,float dy){target.bind(true);GL11.glBindTexture(GL11.GL_TEXTURE_2D,source.colorAttachment);GL20.glUniform2f(GL20.glGetUniformLocation(program,"stepSize"),dx,dy);quad();}
    public static boolean draw(){
        MinecraftClient c=MinecraftClient.getInstance();ModSettings s=SpaceClient.mods;if(s==null||!s.inventoryBlur()||c.world==null||!(c.currentScreen instanceof HandledScreen)||!supported())return false;
        Framebuffer main=c.getFramebuffer();if(main==null||main.fbo<0)return false;
        int previousProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),previousActive=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try(HudIconState state=new HudIconState()){
            init();if(a==null||w!=c.width||h!=c.height){if(a!=null){a.delete();b.delete();}w=c.width;h=c.height;a=new Framebuffer(w,h,false);b=new Framebuffer(w,h,false);a.setTexFilter(GL11.GL_LINEAR);b.setTexFilter(GL11.GL_LINEAR);}
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_CULL_FACE);
            GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"image"),0);
            java.nio.FloatBuffer weights=BufferUtils.createFloatBuffer(9);weights.put(GaussianKernel.weights(s.blurStrength())).flip();GL20.glUniform1(GL20.glGetUniformLocation(program,"weights"),weights);
            pass(main,a,1f/w,0);pass(a,b,0,1f/h);
            main.bind(true);GL11.glBindTexture(GL11.GL_TEXTURE_2D,b.colorAttachment);GL20.glUniform2f(GL20.glGetUniformLocation(program,"stepSize"),0,0);quad();return true;
        }catch(RuntimeException e){failed=true;release();return false;}
        finally{GL20.glUseProgram(previousProgram);main.bind(true);GL13.glActiveTexture(previousActive);GL11.glPopAttrib();}
    }
    public static void release(){if(a!=null){a.delete();b.delete();a=b=null;}if(program!=0){GL20.glDeleteProgram(program);program=0;}}
}

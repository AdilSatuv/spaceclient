package dev.spaceclient.mods;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;
import net.minecraft.scoreboard.*;

public final class ScoreboardRenderer extends DrawableHelper {
    private ScoreboardRenderer(){}
    public static void draw(ScoreboardObjective objective,Window window){
        MinecraftClient c=MinecraftClient.getInstance();ModSettings s=SpaceClient.mods;
        Scoreboard board=objective.getScoreboard();List<String> lines=new ArrayList<String>();
        for(ScoreboardPlayerScore score:board.getAllPlayerScores(objective))if(score.getPlayerName()!=null&&!score.getPlayerName().startsWith("#"))
            lines.add(Team.decorateName(board.getPlayerTeam(score.getPlayerName()),score.getPlayerName())+(s.scoreboardNumbers()?" \u00a7c"+score.getScore():""));
        if(lines.size()>15)lines=new ArrayList<String>(lines.subList(lines.size()-15,lines.size()));
        int w=c.textRenderer.getStringWidth(objective.getDisplayName())+8;for(String line:lines)w=Math.max(w,c.textRenderer.getStringWidth(line)+8);
        float scale=s.scoreboardScale()/100f;scale=Math.min(scale,Math.min(window.getWidth()/(float)w,window.getHeight()/(float)((lines.size()+1)*10+4)));
        float x=Math.max(0,Math.min(window.getWidth()-w*scale,window.getWidth()-w*scale-4+s.scoreboardX()));
        float y=Math.max(0,Math.min(window.getHeight()-(lines.size()+1)*10*scale,window.getHeight()/2f-(lines.size()+1)*5*scale+s.scoreboardY()));
        GlStateManager.pushMatrix();try{GlStateManager.translate(x,y,0);GlStateManager.scale(scale,scale,1);
            if(s.scoreboardBackground())fill(0,-2,w,(lines.size()+1)*10+2,0xB5101114);
            c.textRenderer.drawWithShadow(objective.getDisplayName(),(w-c.textRenderer.getStringWidth(objective.getDisplayName()))/2f,0,0xE7BF79);
            for(int i=0;i<lines.size();i++)c.textRenderer.drawWithShadow(lines.get(lines.size()-1-i),4,(i+1)*10,0xFFFFFF);
        }finally{GlStateManager.popMatrix();}
    }
}

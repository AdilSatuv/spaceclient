package dev.spaceclient.menu;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.MinecraftClient;

/** Presentation-only phrase catalog. Unknown/new phrases retain their original English. */
public final class UiTranslations {
    private static final Map<String,String> RU=load();
    private UiTranslations(){ }
    private static Map<String,String> load(){
        Map<String,String> map=new HashMap<>();
        try(InputStream in=UiTranslations.class.getResourceAsStream("/assets/spaceclient/lang/ui_ru.tsv")){
            if(in!=null)try(BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){int tab=line.indexOf('\t');if(tab>0&&tab<line.length()-1)map.put(line.substring(0,tab),line.substring(tab+1));}}
        }catch(IOException ignored){ }
        return Collections.unmodifiableMap(map);
    }
    public static String text(MinecraftClient c,String text){return translate(MenuStrings.isRussian(c),text);}
    public static String translate(boolean russian,String text){
        if(text==null)return "";if(!russian)return text;
        String direct=RU.get(text);if(direct!=null)return direct;
        if(text.startsWith("Profile: "))return text.replace("Profile: ","Профиль: ").replace("HUD visibility: On","HUD: вкл").replace("HUD visibility: Off","HUD: выкл");
        return text;
    }
}

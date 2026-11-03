package dev.spaceclient.mods;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Local markers keyed by a hashed world/server identity and dimension. */
public final class WaypointStore {
    public static final class Point {
        public final String context,name;public final int dimension,x,y,z;public final VisualPalette color;public final boolean visible;
        public Point(String context,String name,int dimension,int x,int y,int z,VisualPalette color,boolean visible){
            if(context==null||!context.matches("[a-f0-9]{64}")||!ModSettings.validText(name)||name.trim().isEmpty()||name.length()>32||color==null||Math.abs((long)x)>30_000_000||Math.abs((long)z)>30_000_000||y<0||y>255)throw new IllegalArgumentException("Invalid marker");
            this.context=context;this.name=name;this.dimension=dimension;this.x=x;this.y=y;this.z=z;this.color=color;this.visible=visible;
        }
        public Point visible(boolean v){return new Point(context,name,dimension,x,y,z,color,v);}
        public Point color(VisualPalette v){return new Point(context,name,dimension,x,y,z,v,visible);}
    }
    private final Path file;private List<Point> points=new ArrayList<Point>();private boolean readOnly;private String error="";
    private WaypointStore(Path file){this.file=file;}
    public static WaypointStore open(Path file){
        WaypointStore s=new WaypointStore(file);if(!Files.exists(file))return s;
        try{if(Files.size(file)>262144)throw new IOException();Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}
            if(!"1".equals(p.getProperty("schemaVersion")))throw new IOException();int count=Integer.parseInt(p.getProperty("count"));if(count<0||count>512||p.size()!=2+count*8)throw new IOException();
            for(int i=0;i<count;i++){String k="point."+i+".";String show=p.getProperty(k+"visible");if(!"true".equals(show)&&!"false".equals(show))throw new IOException();s.points.add(new Point(p.getProperty(k+"context"),p.getProperty(k+"name"),Integer.parseInt(p.getProperty(k+"dimension")),Integer.parseInt(p.getProperty(k+"x")),Integer.parseInt(p.getProperty(k+"y")),Integer.parseInt(p.getProperty(k+"z")),VisualPalette.valueOf(p.getProperty(k+"color", "")),Boolean.parseBoolean(show)));}
        }catch(IOException|IllegalArgumentException e){s.points.clear();s.readOnly=true;s.error="Markers unreadable; original kept (read-only).";}return s;
    }
    public List<Point> all(){return Collections.unmodifiableList(points);}
    public List<Point> current(String context,int dimension){List<Point> r=new ArrayList<Point>();for(Point p:points)if(p.context.equals(context)&&p.dimension==dimension)r.add(p);return r;}
    public boolean readOnly(){return readOnly;}public String error(){return error;}
    public boolean add(Point p){List<Point> next=new ArrayList<Point>(points);if(next.size()>=512||current(p.context,p.dimension).size()>=64){error="Marker limit reached";return false;}next.add(p);return save(next);}
    public boolean replace(Point old,Point replacement){List<Point> next=new ArrayList<Point>(points);int i=next.indexOf(old);if(i<0)return false;if(replacement==null)next.remove(i);else next.set(i,replacement);return save(next);}
    private boolean save(List<Point> next){
        if(readOnly)return false;Path tmp=null;
        try{Properties p=new Properties();p.setProperty("schemaVersion","1");p.setProperty("count",Integer.toString(next.size()));for(int i=0;i<next.size();i++){Point v=next.get(i);String k="point."+i+".";p.setProperty(k+"context",v.context);p.setProperty(k+"name",v.name);p.setProperty(k+"dimension",Integer.toString(v.dimension));p.setProperty(k+"x",Integer.toString(v.x));p.setProperty(k+"y",Integer.toString(v.y));p.setProperty(k+"z",Integer.toString(v.z));p.setProperty(k+"color",v.color.name());p.setProperty(k+"visible",Boolean.toString(v.visible));}
            Files.createDirectories(file.toAbsolutePath().getParent());tmp=Files.createTempFile(file.toAbsolutePath().getParent(),"space-markers-",".tmp");try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Space local waypoints");}
            try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}points=next;error="";return true;
        }catch(IOException e){error="Could not save markers; change reverted.";return false;}finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}
    }
}

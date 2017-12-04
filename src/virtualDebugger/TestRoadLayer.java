package virtualDebugger;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.view.AreaLayer;
import rescuecore2.worldmodel.EntityID;
import sos.module.complex.police.PoliceTools;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.List;

public class TestRoadLayer extends AreaLayer<Road> {
    private static final Color ROAD_EDGE_COLOUR;
    private static final Color ROAD_SHAPE_COLOUR;
    private static final Stroke WALL_STROKE;
    private static final Stroke ENTRANCE_STROKE;
    private static final Color INFERNO = new Color(160, 52, 52, 128);
    private static final Color SEVERE_DAMAGE = new Color(80, 60, 140, 128);

    int i = 1;


    public TestRoadLayer() {
        super(Road.class);

//        info = deserialzeAddress("info.ser");

    }

//    public Info deserialzeAddress(String filename) {
//        Info info = null;
//        FileInputStream fin = null;
//        ObjectInputStream ois = null;
//        try {
//            fin = new FileInputStream(filename);
//            ois = new ObjectInputStream(fin);
//            info = (Info) ois.readObject();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            if (fin != null) {
//                try {
//                    fin.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (ois != null) {
//                try {
//                    ois.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return info;
//    }


    public String getName() {
        return "Roads";
    }

    protected void paintShape(Road r, Polygon shape, Graphics2D g) {
        g.setColor(ROAD_SHAPE_COLOUR);
        g.fill(shape);
//        paintCulsters(r,shape,g);
//        paintMST(r,shape,g);
    }

//    private void paintMST(Road r, Polygon shape, Graphics2D g) {
//
//        for (Map.Entry<EntityID, Map<EntityID, java.util.List<EntityID>>> entry: PoliceTools.pathsBetweenCenters.entrySet()){
//            for (Map.Entry<EntityID, List<EntityID>> entry2: entry.getValue().entrySet()){
//                for (EntityID entityID: entry2.getValue()){
////                    System.out.println("$$$ "+entityID.getValue());
//                    if (entityID.getValue() == r.getID().getValue()){
//                        g.setColor(SEVERE_DAMAGE);
//                        g.fill(shape);
//                    }
//                }
//            }
//            if (entry.getKey().getValue() == r.getID().getValue()){
//                g.setColor(INFERNO);
//                g.fill(shape);
//            }
////            System.out.println("### "+entry.getKey().getValue());
//
//        }
//
//    }

    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(ROAD_EDGE_COLOUR);
        g.setStroke(e.isPassable()?ENTRANCE_STROKE:WALL_STROKE);
        g.drawLine(t.xToScreen((double)e.getStartX()), t.yToScreen((double)e.getStartY()), t.xToScreen((double)e.getEndX()), t.yToScreen((double)e.getEndY()));
    }

    static {
        ROAD_EDGE_COLOUR = Color.GRAY.darker();
        ROAD_SHAPE_COLOUR = new Color(185, 185, 185);
        WALL_STROKE = new BasicStroke(2.0F, 0, 0);
        ENTRANCE_STROKE = new BasicStroke(1.0F, 0, 0);
    }
}

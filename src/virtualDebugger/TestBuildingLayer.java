package virtualDebugger;

/**
 * Created by kasra on 11/19/17.
 */

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.view.AreaLayer;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class TestBuildingLayer extends AreaLayer<Building> {
    private static final Color HEATING = new Color(176, 176, 56, 128);
    private static final Color BURNING = new Color(204, 122, 50, 128);
    private static final Color INFERNO = new Color(160, 52, 52, 128);
    private static final Color WATER_DAMAGE = new Color(50, 120, 130, 128);
    private static final Color MINOR_DAMAGE = new Color(100, 140, 210, 128);
    private static final Color MODERATE_DAMAGE = new Color(100, 70, 190, 128);
    private static final Color SEVERE_DAMAGE = new Color(80, 60, 140, 128);
    private static final Color BURNT_OUT = new Color(0, 0, 0, 255);
    private static final Color OUTLINE_COLOUR;
    private static final Color ENTRANCE;
    private static final Stroke WALL_STROKE;
    private static final Stroke ENTRANCE_STROKE;
    int i = 1;

    public TestBuildingLayer() {
        super(Building.class);
        System.out.println("getting back the info");

//        info = deserialzeAddress("info.ser");
    }

    public String getName() {
        return "Building shapes";
    }

    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(OUTLINE_COLOUR);
        g.setStroke(e.isPassable()?ENTRANCE_STROKE:WALL_STROKE);
        g.drawLine(t.xToScreen((double)e.getStartX()), t.yToScreen((double)e.getStartY()), t.xToScreen((double)e.getEndX()), t.yToScreen((double)e.getEndY()));
    }

    protected void paintShape(Building b, Polygon shape, Graphics2D g) {
        this.drawBrokenness(b, shape, g);
        this.paintCulsters(b,shape,g);
//        this.drawFieryness(b, shape, g);
    }

    private void paintCulsters(Building b, Polygon shape, Graphics2D g) {


//        for (EntityID entityID: Info.centerIDs)
//            if (entityID.getValue() == b.getID().getValue()){
//                g.setColor(INFERNO);
//                g.fill(shape);
//                System.out.println(i++);
//            }

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

//    private void drawFieryness(Building b, Polygon shape, Graphics2D g) {
//        if(b.isFierynessDefined()) {
//            switch(null.$SwitchMap$rescuecore2$standard$entities$StandardEntityConstants$Fieryness[b.getFierynessEnum().ordinal()]) {
//                case 1:
//                    return;
//                case 2:
//                    g.setColor(HEATING);
//                    break;
//                case 3:
//                    g.setColor(BURNING);
//                    break;
//                case 4:
//                    g.setColor(INFERNO);
//                    break;
//                case 5:
//                    g.setColor(WATER_DAMAGE);
//                    break;
//                case 6:
//                    g.setColor(MINOR_DAMAGE);
//                    break;
//                case 7:
//                    g.setColor(MODERATE_DAMAGE);
//                    break;
//                case 8:
//                    g.setColor(SEVERE_DAMAGE);
//                    break;
//                case 9:
//                    g.setColor(BURNT_OUT);
//                    break;
//                default:
//                    throw new IllegalArgumentException("Don\'t know how to render fieryness " + b.getFierynessEnum());
//            }
//
//            g.fill(shape);
//        }
//    }

    private void drawBrokenness(Building b, Shape shape, Graphics2D g) {
        if(b.isBrokennessDefined()) {
            int brokenness = b.getBrokenness();
            int colour = Math.max(0, 135 - brokenness / 2);
            g.setColor(new Color(colour, colour, colour));
            g.fill(shape);
        }
    }

    static {
        OUTLINE_COLOUR = Color.GRAY.darker();
        ENTRANCE = new Color(120, 120, 120);
        WALL_STROKE = new BasicStroke(2.0F, 0, 0);
        ENTRANCE_STROKE = new BasicStroke(1.0F, 0, 0);
    }
}

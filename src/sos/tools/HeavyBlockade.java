package sos.tools;

import adf.agent.info.WorldInfo;
import org.apache.log4j.Logger;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Line2D;
import java.util.ArrayList;

/**
 * Created by kasra on 7/11/17.
 */
public class HeavyBlockade {

    private ArrayList<EntityID> blockades;
    private WorldInfo worldInfo;
    Logger logger;
    public HeavyBlockade(WorldInfo worldInfo, EntityID entityID){
        blockades = new ArrayList<>();
        this.worldInfo = worldInfo;
        blockades.add(entityID);
        logger = Logger.getLogger("SOSRoadDetector");
    }

    public int belongsToThis(Blockade other){
        for (EntityID entityID: blockades){
            if (entityID.getValue() == other.getID().getValue())
                return -1;
        }
        for (EntityID entityID: blockades){
            Blockade blockade = (Blockade) worldInfo.getEntity(entityID);
            if (intersect(blockade, other)){
                blockades.add(other.getID());
                return 1;
            }
        }
        return 0;
    }

    public ArrayList<EntityID> getBlockades(){
        return blockades;
    }

    public boolean isBesideHeavyBlockade(HeavyBlockade heavyBlockade){

        double minDist1 = Integer.MAX_VALUE;
        for (EntityID blockadeID: blockades){
            Blockade blockade = (Blockade) worldInfo.getEntity(blockadeID);
            if (!blockade.isApexesDefined())
                continue;
            int[] apexes = blockade.getApexes();
            Line2D blockadeSide = new Line2D.Double();
            for (int i = 0; i < apexes.length; i += 2){

                if (i != apexes.length - 2){
                    blockadeSide.setLine(apexes[i],apexes[i+1],apexes[i+2],apexes[i+3]);}
                else{
                    blockadeSide.setLine(apexes[i],apexes[i+1],apexes[0],apexes[1]);
                }

                for (EntityID blockadeID2: heavyBlockade.getBlockades()){
                    Blockade blockade2 = (Blockade) worldInfo.getEntity(blockadeID2);
                    if (!blockade2.isApexesDefined())
                        continue;
                    int[] apexes2 = blockade2.getApexes();
                    Line2D blockadeSide2 = new Line2D.Double();

                    for (int j = 0; j < apexes2.length; j += 2){

                        double d = distance(apexes2[j],apexes2[j+1],apexes[i],apexes[i+1]);
                        if (d < minDist1){
                            minDist1 = d;
                        }
                        if (j != apexes2.length - 2){
                            blockadeSide2.setLine(apexes2[j],apexes2[j+1],apexes2[j+2],apexes2[j+3]);
                        }else{
                            blockadeSide2.setLine(apexes2[j],apexes2[j+1],apexes2[0],apexes2[1]);
                        }

                        double dd;
                        dd = blockadeSide2.ptLineDist(apexes[i],apexes[i+1]);
                        if (dd < minDist1 && distIsValid(apexes[i],apexes[i+1], blockadeSide2.getX1(), blockadeSide2.getY1(), blockadeSide2.getX2(), blockadeSide2.getY2())){
                            minDist1 = dd;
                        }

                        dd = blockadeSide.ptLineDist(apexes2[j],apexes2[j+1]);
                        if (dd < minDist1 && distIsValid(apexes2[j],apexes2[j+1], blockadeSide.getX1(), blockadeSide.getY1(), blockadeSide.getX2(), blockadeSide.getY2())){
                            minDist1 = dd;
                        }

                    }


                }
            }

        }
//        if(worldInfo.getPosition((Blockade) worldInfo.getEntity(blockades.get(0))).getID().getValue() == 36008)
//            System.out.println("isBesideHeavyBlockade : " + minDist1);

        if (minDist1 < 1190)
            return true;

        return false;

    }

    private boolean distIsValid(double x1, double y1, double x2, double y2, double x3, double y3) {

        double a = distance(x1, y1, x2, y2);
        double b = distance(x1, y1, x3, y3);
        double c = distance(x2, y2, x3, y3);
        double angle1 = Math.toDegrees(Math.acos((c*c + b*b - a*a)/(2*b*c)));
        double angle2 = Math.toDegrees(Math.acos((c*c + a*a - b*b)/(2*a*c)));
        if (angle1 > 90 || angle2 > 90){
//            logger.debug("\n" + "inValidLine *** : " + x1 + " " + y1 + " " + x2 + " " +  y2 + " " +  x3 + " " +  y3 + "\n");
            return false;
        }
        return true;

    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = (x1 - x2);
        double dy = (y1 - y2);
        return Math.hypot(dx, dy);
    }

    public boolean isBesideLine(Line2D line2D){
        double minDist1 = Integer.MAX_VALUE;
        for (EntityID blockadeID: blockades){
            Blockade blockade = (Blockade) worldInfo.getEntity(blockadeID);
            if (!blockade.isApexesDefined())
                continue;
            int[] apexes = blockade.getApexes();
            for (int i = 0; i < apexes.length; i += 2){
                double d = line2D.ptLineDist(apexes[i],apexes[i+1]);
                if (d < minDist1){
                    minDist1 = d;
                }
            }
        }

//        if(worldInfo.getPosition((Blockade) worldInfo.getEntity(blockades.get(0))).getID().getValue() == 36008)
//            System.out.println("isBesideLine : " + minDist1);
        if (minDist1 < 1000)
            return true;
        return false;
    }

    private boolean intersect(Blockade blockade, Blockade another) {
        if(blockade.isApexesDefined() && another.isApexesDefined()) {
            int[] apexes0 = blockade.getApexes();
            int[] apexes1 = another.getApexes();
            for (int i = 0; i < (apexes0.length - 2); i += 2) {
                for (int j = 0; j < (apexes1.length - 2); j += 2) {
                    if (Line2D.linesIntersect(
                            apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                            apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3]
                    )) {
                        return true;
                    }
                }
            }
            for (int i = 0; i < (apexes0.length - 2); i += 2) {
                if (Line2D.linesIntersect(
                        apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1]
                )) {
                    return true;
                }
            }
            for (int j = 0; j < (apexes1.length - 2); j += 2) {
                if (Line2D.linesIntersect(
                        apexes0[apexes0.length - 2], apexes0[apexes0.length - 1], apexes0[0], apexes0[1],
                        apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3]
                )) {
                    return true;
                }
            }
        }else
            System.out.println("avval apex difined bude alan mishe nabashe akhe???");
        return false;
    }

    private boolean isInside(double pX, double pY, int[] apex) {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for(int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if(flag > 0) {
            return angle;
        }
        if(flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }
}

package sos.module.complex.police;

import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import org.apache.log4j.Logger;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kasra on 11/2/17.
 */
public class PoliceTools {

    private Logger logger;
    public PathPlanning pathPlanning;
    public WorldInfo worldInfo;
    public Map<EntityID, AtomicInteger> roadIndex;
    private boolean[][] matrix;
    private List<EntityID>[][] tempPaths;
    public Map<EntityID, Map<EntityID, List<EntityID>>> pathsBetweenCenters;

    public PoliceTools(PathPlanning pathPlanning, Logger logger, WorldInfo worldInfo){
        this.pathPlanning = pathPlanning;
        this.logger = logger;
        this.worldInfo = worldInfo;
        roadIndex = new LazyMap<EntityID, AtomicInteger>() {
            @Override
            public AtomicInteger createValue() {
                return new AtomicInteger(0);
            }
        };
    }

    public Map<EntityID, Map<EntityID, List<EntityID>>> prime( List<EntityID> nodes, boolean calcForClusterCenters){

        logger.debug(">> prime...\n\n");

        int numberOfNodes = nodes.size();
        matrix = new boolean[numberOfNodes][numberOfNodes];
        double[][] weights = calcWeights(numberOfNodes, nodes);

        int from = 0;
        Boolean[] isChecked = new Boolean[numberOfNodes];
        int[] nearest = new int[numberOfNodes];
        double[] distance = new double[numberOfNodes];
        for (int i = 0; i < numberOfNodes; i++) {
            nearest[i] = from;
            distance[i] = weights[i][from];
            isChecked[i] = false;
        }
        isChecked[from] = true;

        int sizeOfTree = 0;
        while(sizeOfTree < numberOfNodes - 1){

            double min = 99999999;
            int vNear = from;

            for (int i = 0; i < numberOfNodes; i++) {
                if(!isChecked[i]){
                    if(distance[i] < min){
                        min = distance[i];
                        vNear = i;
                    }
                }
            }

            if (vNear == from){
                logger.error("broblem in finding MST ");
                break;
            }


            isChecked[vNear] = true;
            matrix[vNear][nearest[vNear]] = true;
            matrix[nearest[vNear]][vNear] = true;
            sizeOfTree++;
            logger.debug("chosen Edge between : " + getIDFromIndex(vNear) + " & " + getIDFromIndex(nearest[vNear]));

            for (int i = 0; i < numberOfNodes; i++) {
                if(!isChecked[i])
                    if(weights[vNear][i] < distance[i]){
                        distance[i] = weights[vNear][i];
                        nearest[i] = vNear;
                    }
            }

        }

        if (calcForClusterCenters) {
            return saveCalculatedPaths(true);
        }else
            return saveCalculatedPaths(false);

    }

    private Map<EntityID, Map<EntityID, List<EntityID>>> saveCalculatedPaths(boolean save) {

        Map<EntityID, Map<EntityID, List<EntityID>>> tempPathsBetweenCenters = new LazyMap<EntityID, Map<EntityID, List<EntityID>>>() {
            @Override
            public Map<EntityID, List<EntityID>> createValue() {
                return new HashMap<>();
            }
        };

        for (int i = 0; i < matrix.length; i++){
            for (int j = 0; j < matrix.length; j++){
                if (matrix[i][j]){
                    EntityID from = getIDFromIndex(i);
                    EntityID to = getIDFromIndex(j);
                    tempPathsBetweenCenters.get(from).put(to, tempPaths[i][j]);
                }
            }
        }

        tempPaths = null;

        if (save)
            pathsBetweenCenters = tempPathsBetweenCenters;

        return tempPathsBetweenCenters;
    }

    private double[][] calcWeights(int numberOfNodes, List<EntityID> nodes){

        logger.debug(">> calcWeights...\n\n");

        double[][] weights = new double[numberOfNodes][numberOfNodes];
        tempPaths = new ArrayList[numberOfNodes][numberOfNodes];
        int z = 0;
        for (EntityID id: nodes){
            roadIndex.get(id).set(z);
            z++;
        }

        for (int i = 0; i<numberOfNodes; i++)
            for (int j = 0; j<numberOfNodes; j++)
                weights[i][j] = -1;
        for (EntityID id1: nodes){
            for (EntityID id2: nodes){
                int index1 = getIndex(id1);
                int index2 = getIndex(id2);
                if (weights[index1][index2] != -1 || index1 == index2)
                    continue;
                pathPlanning.setFrom(id1);
                ArrayList<EntityID> someArr = new ArrayList<>();
                someArr.add(id2);
                pathPlanning.setDestination(someArr);
                List<EntityID> path = pathPlanning.calc().getResult();
                if (path == null || path.size() <= 0){
                    logger.debug("could find no path from " + id1.getValue() + " to " + id2.getValue());
                    continue;
                }
                double d = lengthOfPath(path);
                logger.debug("length between " + id1.getValue() + " & " + id2.getValue() + " = " + d);
                weights[index1][index2] = d;
                weights[index2][index1] = weights[index1][index2];
                tempPaths[index1][index2] = path;
//                if (paths[1][3] != null){
//                    logger.debug("paths[1][3].get(0) "+index1+" = " + paths[1][3].get(0) + " paths[1][3].get(path.size() - 1) " +index2 +" = "+ paths[1][3].get(paths[1][3].size() - 1));
//                    if (paths[3][1] != null)
//                        logger.debug("paths[3][1].get(0) "+index1+" = " + paths[3][1].get(0) + " paths[3][1].get(paths[3][1].size() - 1) " +index2 +" = "+ paths[3][1].get(paths[3][1].size() - 1));
//                }
                List<EntityID> path2 = new ArrayList<>(path.size());
                for(int i = path.size()-1; i >= 0; i--){
                    path2.add(path.get(i));
                }

                tempPaths[index2][index1] = path2;
                logger.debug(" reverse from "+index2+" = " + path.get(0) + " to " +index1 +" = "+ path.get(path.size() - 1));
//                if (paths[1][3] != null)
//                    logger.debug("paths[1][3].get(0) "+getIndex(id1)+" = " + paths[1][3].get(0) + " paths[1][3].get(path.size() - 1) " +getIndex(id2) +" = "+ paths[1][3].get(paths[1][3].size() - 1));

            }
        }

        return weights;

    }

    private EntityID getIDFromIndex(int index){

        for (Map.Entry<EntityID, AtomicInteger> entry: roadIndex.entrySet())
            if (entry.getValue().intValue() == index)
                return entry.getKey();

        return null;

    }

    private int getIndex(EntityID id) {
        return roadIndex.get(id).intValue();
    }

    private double lengthOfPath(List<EntityID> roads){
        double length = 0;
        for (int i = 0;i < roads.size() - 1; i++){
            length += getDistance2(roads.get(i), roads.get(i+1));
        }
        return length;
    }

    private double getDistance2(EntityID r1, EntityID r2){

        Area road1 = (Area) worldInfo.getEntity(r1);
        Area road2 = (Area) worldInfo.getEntity(r2);
        int[] apx1 = road1.getApexList();
        int[] apx2 = road2.getApexList();
        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
        for (int i = 0; i<apx1.length; i++){
            if (i % 2 != 0)
                y1 += apx1[i];
            else
                x1 += apx1[i];
        }
        x1 = x1/(apx1.length / 2);
        y1 = y1/(apx1.length / 2);

        for (int i = 0; i<apx2.length; i++){
            if (i % 2 != 0)
                y2 += apx2[i];
            else
                x2 += apx2[i];
        }
        x2 = x2/(apx2.length / 2);
        y2 = y2/(apx2.length / 2);

        return distance(x1,y1,x2,y2);

    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = (x1 - x2);
        double dy = (y1 - y2);
        return Math.hypot(dx, dy);
    }

}

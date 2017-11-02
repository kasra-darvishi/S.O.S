package sos.module.algorithm;

/**
 * Created by kasra on 6/4/17.
 */

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sos.tools.HeavyBlockade;

import java.awt.geom.Line2D;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SOSPathPlanning_Police extends PathPlanning {

    private Map<EntityID, Set<EntityID>> graph;

    private EntityID from;
    private Collection<EntityID> targets;
    private List<EntityID> result;
    private int numberOfNodes;
    //    private int[][] lengthWeight;
    private Map<EntityID, AtomicInteger> roadIndex;
    private Map<EntityID, ArrayList<HeavyBlockade>> heavyBlockadesMap;
    private Logger logger;
    private boolean noPath = false;
    private Collection<StandardEntity> nodes;

    private Map<Integer, Map<Integer, Integer>> lengthWeight;

    public SOSPathPlanning_Police(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        PropertyConfigurator.configure("log4j.properties");
        logger = Logger.getLogger("SOSPathPlanning");
        nodes = worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT, StandardEntityURN.REFUGE, StandardEntityURN.BUILDING, StandardEntityURN.FIRE_STATION, StandardEntityURN.GAS_STATION, StandardEntityURN.POLICE_OFFICE, StandardEntityURN.AMBULANCE_TEAM);
        numberOfNodes = nodes.size();
//        lengthWeight = new int[numberOfNodes+1][numberOfNodes+1];
        lengthWeight = new LazyMap<Integer, Map<Integer, Integer>>() {
            @Override
            public Map<Integer, Integer> createValue() {
                return new HashMap<>();
            }
        };

        roadIndex = new LazyMap<EntityID, AtomicInteger>() {
            @Override
            public AtomicInteger createValue() {
                return new AtomicInteger(0);
            }
        };
        heavyBlockadesMap = new LazyMap<EntityID, ArrayList<HeavyBlockade>>(){
            @Override
            public ArrayList<HeavyBlockade> createValue() {
                return new ArrayList<HeavyBlockade>();
            }
        };

        prepareGraph();

    }

    private void prepareGraph() {

        //initialize

        //setting road index
        int i = 1;
        for (StandardEntity standardEntity: nodes){
//            if (i == 0)
//                System.out.println("avvalin entity : " + standardEntity.getID().getValue());
            roadIndex.get(standardEntity.getID()).set(i);
            lengthWeight.get(i);
//            if (i == 0)
//                System.out.println("id e avvalin index : " + getIDFromIndex(0));
//            if (id.getValue() == 33130)
//                logger.debug("!!!!!!!!!!!! " + i + " --- " + roadIndex.get(id) + " " + roadIndex.get(id).intValue() + " id: "+id.getValue());
            i++;
        }

        //fill initial weight lengthWeight
        for (StandardEntity standardEntity: nodes){
            if (!(standardEntity instanceof Area))
                continue;
            Area area = (Area) standardEntity;
            for (Edge edge: area.getEdges())
                if (edge.isPassable()){
                    EntityID neghbourID = edge.getNeighbour();
                    Double dist = getDistance2(standardEntity.getID(), neghbourID);
                    int index1 = roadIndex.get(standardEntity.getID()).intValue();
                    int index2 = roadIndex.get(neghbourID).intValue();
                    lengthWeight.get(index1).put(index2, dist.intValue());
                }
        }

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

    @Override
    public List<EntityID> getResult() {
        return this.result;
    }

    @Override
    public PathPlanning setFrom(EntityID id) {
        this.from = id;
        logger.debug("from " + id.getValue());
        return this;
    }

    @Override
    public PathPlanning setDestination(Collection<EntityID> targets) {
        this.targets = targets;
        logger.debug("to ");
        for (EntityID id: targets)
            logger.debug(" " + id.getValue());
        logger.debug("");
        return this;
    }

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        return this;
    }

    @Override
    public PathPlanning precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
//        prepareGraph();

        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
//        prepareGraph();
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
//        prepareGraph();
        return this;
    }

    @Override
    public PathPlanning calc() {

        noPath = false;
        updateWeightGraph();

        int initialNodeIndex = getIndex(from);
//        if (initialNodeIndex == -1){
//            logger.error("could not find initialNodeIndex");
//            this.result = null;
//            return this;
//        }
//        EntityID firstTarget = targets.toArray(new EntityID[targets.size()])[0];
        ArrayList<Integer> targetsIndexes = new ArrayList<>();
        for (EntityID entityID: targets){
            int i = getIndex(entityID);
            targetsIndexes.add(i);
        }

        ArrayList<EntityID> middleRoads = new ArrayList<>();
        ArrayList<EntityID> finalPath = new ArrayList<>();
        finalPath.add(from);
        EntityID temp = getIDFromIndex(initialNodeIndex);

        if (temp == null){
            logger.error("problem in finding ID of initial node index");
            this.result = null;
            return this;
        }

        dijkstra(numberOfNodes, middleRoads, initialNodeIndex, targetsIndexes);
        if (noPath){
            result = null;
            return this;
        }
        int f = 1;
        for (EntityID i :middleRoads){
            finalPath.add(i);
            logger.debug(this.agentInfo.getID().getValue() + " -> "+ " middle road #" + f + " : " + i.getValue());
            f++;
        }

        for (EntityID i :finalPath){
            logger.debug(this.agentInfo.getID().getValue() + " -> "+ " road #" + f + " : " + i.getValue());
            f++;
        }

        this.result = finalPath;

        return this;

    }

    private void updateWeightGraph() {

        if (agentInfo.getTime() == 0)
            return;

        //TODO
//        heavyBlockadesMap.clear();

        for (EntityID entityID: worldInfo.getChanged().getChangedEntities()){

            StandardEntity entity = worldInfo.getEntity(entityID);
            if (entity instanceof Blockade){
                StandardEntity standardEntity = worldInfo.getPosition((Blockade) entity);
                if (!(standardEntity instanceof Road)){
                    logger.error("A blockade is not in Road " + entity.getID());
                    continue;
                }
                EntityID id;
                try {
                    id = standardEntity.getID();
                }catch (NullPointerException e){
                    logger.debug("blockade was not position defined : " + entity.getID().getValue());
                    continue;
                }
                Road blockadePosition = (Road)standardEntity;
                int belongsToAHeavyB = 0;
                ArrayList<HeavyBlockade> arrayList = heavyBlockadesMap.get(id);
                for (HeavyBlockade heavyBlockade: arrayList){
                    int u = heavyBlockade.belongsToThis((Blockade) entity);
                    if (u == 1){
                        belongsToAHeavyB = 1;
                        break;
                    }else if (u == -1){
                        belongsToAHeavyB = -1;
                        break;
                    }
                }
                //if its repetitive
                if (belongsToAHeavyB == -1)
                    continue;
                else if(belongsToAHeavyB == 0){
                    arrayList.add(new HeavyBlockade(worldInfo, entity.getID()));
                }

                boolean normalRoad = isNormal(blockadePosition);
                Line2D lineOfEdge1 = new Line2D.Double();
                Line2D lineOfEdge2 = new Line2D.Double();
                boolean firstEdgeChecked = false;
                int numberOfImpassableEdges = 0;

                for (Edge edge: blockadePosition.getEdges()) {

                    if (edge.isPassable()){

                        for (HeavyBlockade heavyBlockade: arrayList){
                            double minDist1 = Integer.MAX_VALUE;
                            double minDist2 = Integer.MAX_VALUE;
                            for (EntityID blockadeID: heavyBlockade.getBlockades()){
                                Blockade blockade = (Blockade) worldInfo.getEntity(blockadeID);
                                if (!blockade.isApexesDefined())
                                    continue;
                                int[] apexes = blockade.getApexes();
                                Line2D blockadeSide = new Line2D.Double();
//                                logger.debug("blockade: " + blockade.getID().getValue() + " edge between: " + road.getID().getValue() +" & "+ edge.getNeighbour().getValue() + " agentID: " + agentInfo.getID().getValue() + " cycle: "+agentInfo.getTime());
                                for (int i = 0; i < apexes.length; i += 2){

                                    double d = distance(apexes[i],apexes[i+1],edge.getStartX(),edge.getStartY());
                                    if (d < minDist1){
//                                        logger.debug("min dist1: " + d);
//                                        logger.debug(apexes[i]+" "+apexes[i+1]+" "+edge.getStartX()+" "+edge.getStartY());
                                        minDist1 = d;}
                                    d = distance(apexes[i],apexes[i+1],edge.getEndX(),edge.getEndY());
                                    if (d < minDist2){
//                                        logger.debug("min dist2: " + d);
//                                        logger.debug(apexes[i]+" "+apexes[i+1]+" "+edge.getEndX()+" "+edge.getEndY());
                                        minDist2 = d;}
                                    if (i != apexes.length - 2){
//                                        logger.debug("side: "+apexes[i]+" "+apexes[i+1]+" "+apexes[i+2]+" "+apexes[i+3]);
                                        blockadeSide.setLine(apexes[i],apexes[i+1],apexes[i+2],apexes[i+3]);}
                                    else{
//                                        logger.debug("side: "+apexes[i]+" "+apexes[i+1]+" "+apexes[0]+" "+apexes[1]);
                                        blockadeSide.setLine(apexes[i],apexes[i+1],apexes[0],apexes[1]);}

                                    double dd;
                                    dd = blockadeSide.ptLineDist((double)edge.getStartX(), (double)edge.getStartY());
                                    if (dd < minDist1 && distIsValid(edge.getStartX(), edge.getStartY(), blockadeSide.getX1(), blockadeSide.getY1(), blockadeSide.getX2(), blockadeSide.getY2())){
//                                        logger.debug("min dist3: " + dd);
                                        minDist1 = dd;}
                                    dd = blockadeSide.ptLineDist((double)edge.getEndX(), (double)edge.getEndY());
                                    if (dd < minDist2 && distIsValid(edge.getEndX(), edge.getEndY(), blockadeSide.getX1(), blockadeSide.getY1(), blockadeSide.getX2(), blockadeSide.getY2())){
//                                        logger.debug("min dist4: " + dd + "\n");
                                        minDist2 = dd;}

                                }
//                                logger.debug("min dist1: " + minDist1);
//                                logger.debug("min dist2: " + minDist2);
//                                logger.debug("min dist3: " + minDist3);
//                                logger.debug("min dist4: " + minDist4 + " \n");

//                                logger.debug("a passableEdge between : " + road.getID().getValue() +" & "+ edge.getNeighbour().getValue() +" min dist 1: " + minDist1 + " mindist2 "+ minDist2);
                            }

                            if (minDist1 < 1190 && minDist2 < 1190){
                                logger.debug("a blocked passableEdge between : " + blockadePosition.getID().getValue() +" & "+ edge.getNeighbour().getValue());
                                int index1 = roadIndex.get(blockadePosition.getID()).intValue();
                                int index2 = roadIndex.get(edge.getNeighbour()).intValue();

                                break;
                            }
                        }

                    }else if (normalRoad){
                        logger.debug("this is a normal road : " + blockadePosition.getID().getValue());
                        if (!firstEdgeChecked) {
                            lineOfEdge1.setLine(edge.getStart().getX(), edge.getStart().getY(), edge.getEnd().getX(), edge.getEnd().getY());
                            firstEdgeChecked = true;
                        }else{
                            lineOfEdge2.setLine(edge.getStart().getX(),edge.getStart().getY(),edge.getEnd().getX(),edge.getEnd().getY());
                        }
                    }

                    if (!edge.isPassable())
                        numberOfImpassableEdges++;

                }

                if (normalRoad){
                    //TODO check this ID for being blocked again
                    for (HeavyBlockade heavyBlockade: arrayList){
                        if (heavyBlockade.isBesideLine(lineOfEdge1)){
//                            if(blockadePosition.getID().getValue() == 6234)
//                                System.out.println("36008 isBesideLine(lineOfEdge1)");
                            if (heavyBlockade.isBesideLine(lineOfEdge2)){
//                                if(blockadePosition.getID().getValue() == 6234)
//                                    System.out.println("36008 isBesideLine(lineOfEdge2)");
                                logger.debug("normal road is blaocked : " + blockadePosition.getID().getValue());
                                for (Edge edge1: blockadePosition.getEdges()){
                                    if (edge1.isPassable()){
                                        int index1 = roadIndex.get(blockadePosition.getID()).intValue();
                                        int index2 = roadIndex.get(edge1.getNeighbour()).intValue();
                                    }
                                }
                            }
                            for (HeavyBlockade heavyBlockade2: arrayList){
                                if (heavyBlockade.equals(heavyBlockade2))
                                    continue;

                                if (heavyBlockade2.isBesideLine(lineOfEdge2)){
//                                    if(blockadePosition.getID().getValue() == 6234)
//                                        System.out.println("36008 heavyBlockade2.isBesideLine(lineOfEdge2)");
                                    if (heavyBlockade.isBesideHeavyBlockade(heavyBlockade2)){
//                                        if(blockadePosition.getID().getValue() == 6234)
//                                            System.out.println("36008 heavyBlockade.isBesideHeavyBlockade(heavyBlockade2)");
                                        logger.debug("normal road is blaocked with two HeavyBlockades : " + blockadePosition.getID().getValue());
                                        String s = " ";
                                        for (EntityID entityID1: heavyBlockade.getBlockades()){
                                            s += " " + entityID1.getValue();
                                        }
                                        logger.debug("HeavyB 1 : " + s);
                                        s = " ";
                                        for (EntityID entityID1: heavyBlockade2.getBlockades()){
                                            s += " " + entityID1.getValue();
                                        }
                                        logger.debug("HeavyB 2 : " + s);
                                        for (Edge edge1: blockadePosition.getEdges()){
                                            if (edge1.isPassable()){
                                                int index1 = roadIndex.get(blockadePosition.getID()).intValue();
                                                int index2 = roadIndex.get(edge1.getNeighbour()).intValue();
                                            }
                                        }
                                    }
                                }
                            }


                        }
                    }
                }else if (numberOfImpassableEdges > 1){
                    logger.debug("is blaocked because of being unknown : " + blockadePosition.getID().getValue());
                    for (Edge edge1: blockadePosition.getEdges()){
                        if (edge1.isPassable()){
                            int index1 = roadIndex.get(blockadePosition.getID()).intValue();
                            int index2 = roadIndex.get(edge1.getNeighbour()).intValue();
                        }
                    }
                }

            }

        }

//        for (int i = 1; i< numberOfNodes+1; i++)
//            for (int j = 1; j< numberOfNodes+1; j++) {
//                lastUpdateTime[i][j] += 1;
//                if (lastUpdateTime[i][j] > 10){
//                    lastUpdateTime[i][j] = 0;
//
//                    //t.println("455....");
//                    logger.debug("now between " + getIDFromIndex(i) + " to " + getIDFromIndex(j) + " is refreshed!");
//                }
//            }

    }

    private boolean distIsValid(double x1, double y1, double x2, double y2, double x3, double y3) {

        double a = distance(x1, y1, x2, y2);
        double b = distance(x1, y1, x3, y3);
        double c = distance(x2, y2, x3, y3);
        double angle1 = Math.toDegrees(Math.acos((c*c + b*b - a*a)/(2*b*c)));
        double angle2 = Math.toDegrees(Math.acos((c*c + a*a - b*b)/(2*a*c)));
        return !(angle1 > 90 || angle2 > 90);

    }

    private boolean isNormal(Road road) {
        int i = 0;
        for (Edge edge: road.getEdges()){
            if (!edge.isPassable())
                i++;
        }
        return i == 2;
    }

    private int getIndex(EntityID id) {
        if (!roadIndex.containsKey(id))
            System.out.println("has not the index of this ID");
        int index = roadIndex.get(id).intValue();
        if (index == 0){
            logger.debug("index e sefr: " + id.getValue());
        }
        return index;
    }

//    private int getIndex(EntityID id) {
//
//        StandardEntity standardEntity = worldInfo.getEntity(id);
//        if (standardEntity instanceof Road){
//            return roadIndex.get(id).intValue();
//        }else {
//
//            EntityID entryRoad = null;
//            double min = Integer.MAX_VALUE;
//            if (!(standardEntity instanceof Area))
//                System.out.println("its not an area????????????????????????????????? " + id.getValue());
//            if (((Area)standardEntity).getNeighbours() == null)
//                System.out.println("no neighbour????????????????????????????????? " + id.getValue());
//            for (EntityID entityID: ((Area)standardEntity).getNeighbours()){
//                //TODO CHOSE THE ENTRY ROAD BASED ON BEING OPEN OR CLOSED
//                double dist = getDistance2(id, entityID);
//                if (worldInfo.getEntity(entityID) instanceof Road && dist < min){
//                    min = dist;
//                    entryRoad = entityID;
//                }
////                System.out.println("road neigh size : " + ((Area)worldInfo.getEntity(id)).getNeighbours().size());
//            }
//            if (entryRoad != null){
//                someMap.put(entryRoad, id);
//                return roadIndex.get(entryRoad).intValue();
//            }else{
//                for (Edge edge: ((Area)standardEntity).getEdges())
//                    if (edge.isPassable()){
//                        someMap.put(edge.getNeighbour(), id);
//                        return getIndex(edge.getNeighbour(), id);
//                    }
//            }
//
//        }
//        return -1;
//
//    }

//    private int getIndex(EntityID id, EntityID previousID) {
//
//        StandardEntity standardEntity = worldInfo.getEntity(id);
//        if (standardEntity instanceof Road){
//            return roadIndex.get(id).intValue();
//        }else {
//
//            EntityID entryRoad = null;
//            int min = Integer.MAX_VALUE;
//            for (EntityID entityID: ((Area)standardEntity).getNeighbours()){
//                //TODO CHOSE THE ENTRY ROAD BASED ON BEING OPEN OR CLOSED
//                if (worldInfo.getEntity(entityID) instanceof Road && getDistance2(id, entityID) < min)
//                    entryRoad = entityID;
////                System.out.println("road neigh size : " + ((Area)worldInfo.getEntity(id)).getNeighbours().size());
//            }
//            if (entryRoad != null){
//                someMap.put(entryRoad, id);
//                return roadIndex.get(entryRoad).intValue();
//            }else{
//                for (Edge edge: ((Area)standardEntity).getEdges())
//                    if (edge.isPassable() && edge.getNeighbour().getValue() != previousID.getValue()){
//                        someMap.put(edge.getNeighbour(), id);
//                        return getIndex(edge.getNeighbour(), id);
//                    }
//            }
//
//        }
//        return -1;
//
//    }


    private void dijkstra(int n, ArrayList<EntityID> nodes, int from, ArrayList<Integer> destinations){

        Boolean[] isChecked = new Boolean[n+1];
        int[] nearest = new int[n+1];
        double[] distance = new double[n+1];
        for (int i = 1; i < n+1; i++) {

            nearest[i] = from;
//            distance[i] = lengthWeight[i][from];
            Integer j = lengthWeight.get(i).get(from);
            if (j != null)
                distance[i] = j;
            else
                distance[i] = 99999999;
            isChecked[i] = false;

        }
        isChecked[from] = true;

        boolean shouldBreak = false;
        while(!shouldBreak){

            double min = 99999999;
            int vNear = from;

            for (int i = 1; i < n+1; i++) {
                if(!isChecked[i]){
                    if(distance[i] < min){
                        min = distance[i];
                        vNear = i;
                    }
                }
            }
            //TODO
            //HANDLE IT!
            if(vNear == from){
                shouldBreak = true;
                noPath = true;
                System.out.println("597....");
                logger.debug("could find no path from " + getIDFromIndex(from).getValue() + " to " + targets.toArray(new EntityID[targets.size()])[0].getValue() + " and others");
            }

            isChecked[vNear] = true;
//            logger.debug(getIDFromIndex(vNear).getValue() + " " + getIDFromIndex(nearest[vNear]).getValue() + " " + lengthWeight[vNear][nearest[vNear]] +"&"+ lengthWeight[nearest[vNear]][vNear]+" " + distance[vNear]);
            if(destinations.contains(vNear)){

                ArrayList<Integer> path = new ArrayList<>();
                path.add(vNear);
                int temp = nearest[vNear];
                while(temp != from){
                    path.add(temp);
                    temp = nearest[temp];
                }
                for (int i = path.size() - 1; i >= 0; i--){
                    EntityID id = getIDFromIndex(path.get(i));
                    try {
                        id.getValue();
                    }catch (NullPointerException nnnn){
                        System.out.println("id ro nadade buuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuddd i : " + i  + " path.get(i): " + path.get(i) + " getIDFromIndex(0) " + getIDFromIndex(0));
                    }
                    if (id == null)
                        System.out.println("id null buuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuud....");
                    nodes.add(id);
                }

                shouldBreak = true;
            }
            //TODO in ezafe kar anjam mide inja vaqti ke karesh tamum shode
            double d;

            for (int i = 1; i < n+1; i++) {
                if(!isChecked[i]){
                    Integer j = lengthWeight.get(vNear).get(i);
                    d = distance[vNear] + (j == null ? 99999999 : j) ;
                    if(d < distance[i]){
//                        logger.debug("distance[vNear] " +distance[vNear] +" lengthWeight[vNear][i] " +lengthWeight[vNear][i]+" distance[i] "+ distance[i] + " i : " + getIDFromIndex(i).getValue());
                        distance[i] = d;
                        nearest[i] = vNear;
                    }
                }
            }

        }

//  for (int i = 0; i < nodes.size(); i++) {
//      Integer get = nodes.get(i);
//      System.out.println(get);
//  }
    }

    private EntityID getIDFromIndex(int index){

        for (Map.Entry<EntityID, AtomicInteger> entry: roadIndex.entrySet())
            if (entry.getValue().intValue() == index)
                return entry.getKey();

        System.out.println("couldnt find road id from index " + index);
        return null;

    }

    private double distance(double x1, double y1, double x2, double y2) {
        double dx = (x1 - x2);
        double dy = (y1 - y2);
        return Math.hypot(dx, dy);
    }

}

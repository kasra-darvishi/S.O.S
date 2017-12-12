package sos.module.complex.police;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by kasra on 10/19/17.
 */
public class UrgentTargets extends PO_AbstractState {

    private Collection<StandardEntity> clusterEntities;
    private ArrayList<EntityID> fbList, atList, pfList;
    private Map<EntityID,PFInfo> fbMap;
    private Map<EntityID,PFInfo> atMap;


    public UrgentTargets(WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, SOSPathPlanning_Police pp, Logger logger, PoliceClustering clustering, ArrayList<EntityID> isReached, PoliceTools policeTools) {
        super(worldInfo, agentInfo, scenarioInfo, pp, logger, clustering, isReached, policeTools);
        clusterEntities = clustering.getClusterEntities(clustering.getClusterIndex(agentInfo.getID()));
        fbList = new ArrayList<>();
        atList = new ArrayList<>();
        fbList.addAll(worldInfo.getEntityIDsOfType(StandardEntityURN.FIRE_BRIGADE));
        atList.addAll(worldInfo.getEntityIDsOfType(StandardEntityURN.AMBULANCE_TEAM));
        for (EntityID entityID: worldInfo.getEntityIDsOfType(StandardEntityURN.FIRE_BRIGADE))
            fbList.add(entityID);
        for (EntityID entityID: worldInfo.getEntityIDsOfType(StandardEntityURN.AMBULANCE_TEAM))
            atList.add(entityID);
        pfList = new ArrayList<>();
        fbMap = new LazyMap<EntityID, PFInfo>() {
            @Override
            public PFInfo createValue() {
                return new PFInfo();
            }
        };
        atMap = new LazyMap<EntityID, PFInfo>() {
            @Override
            public PFInfo createValue() {
                return new PFInfo();
            }
        };
        for (StandardEntity se: worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)){
            fbMap.get(se.getID()).intialize(se.getID());
        }
        for (StandardEntity se: worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)){
            atMap.get(se.getID()).intialize(se.getID());
        }
        result = null;
    }

    @Override
    public EntityID check() {

        logger.debug("\n\n... URGENT AGENTS STATE ... " + agentInfo.getID().getValue() + " clock: " + agentInfo.getTime());

        logger.debug("68 " + fbList.size() + " " + agentInfo.getTime());
        updateInfo();

        if (agentInfo.getTime() <= 1)
            return null;

        if (result != null){
            StandardEntity se = worldInfo.getEntity(result);
            if (se instanceof FireBrigade){
                PFInfo info = fbMap.get(result);
                if (info.canMove){
                    logger.debug("I " + agentInfo.getID().getValue() + " made free fireBrigade " + info.agentID.getValue());
                    fbList.remove(info.agentID);
                    result = null;
                }else
                    return result;
            }else if (se instanceof AmbulanceTeam){
                PFInfo info = atMap.get(result);
                if (info.canMove){
                    logger.debug("I " + agentInfo.getID().getValue() + " made free ambulance " + info.agentID.getValue());
                    atList.remove(info.agentID);
                    result = null;
                }else
                    return result;
            }
        }

        if (policeTools.probableFutureTarget == null){
            ArrayList<EntityID> removeList = new ArrayList<>();
            for (EntityID entityID: fbList){
                logger.debug("chekicng " + entityID.getValue());
                StandardEntity standardEntity = worldInfo.getPosition(entityID);
                if (clusterEntities.contains(standardEntity)){
                    StandardEntity position = worldInfo.getPosition(standardEntity.getID());
                    if (position != null && position instanceof Road){
                        Road road = (Road) position;
                        if (road.isBlockadesDefined() && road.getBlockades().size() == 0){
                            removeList.add(entityID);
                            System.out.println("i cum here...");
                            continue;
                        }
                        logger.debug("fire agent is probably stuck: " + entityID.getValue());
                        result = entityID;
                        break;
                    }else if(position != null && position instanceof Building){
                        Road road = findEntrance();
                    }
                }else {
                    removeList.add(entityID);
                    System.out.println("or here");
                }
            }
            fbList.removeAll(removeList);
            removeList = new ArrayList<>();
            if (result != null)
                return result;
            for (EntityID entityID: atList){
                logger.debug("chekicng " + entityID.getValue());
                StandardEntity standardEntity = worldInfo.getPosition(entityID);
                if (clusterEntities.contains(standardEntity)){
                    StandardEntity position = worldInfo.getPosition(standardEntity.getID());
                    if (position != null && position instanceof Road){
                        Road road = (Road) position;
                        if (road.isBlockadesDefined() && road.getBlockades().size() == 0){
                            removeList.add(entityID);
                            continue;
                        }
                        logger.debug("ambulance agent is probably stuck: " + entityID.getValue());
                        result = entityID;
                        break;
                    }else if(position != null && position instanceof Building) {
                        Road road = findEntrance();
                    }
                }else {
                    removeList.add(entityID);
                }
            }
            atList.removeAll(removeList);
            return result;
        }else {
            logger.debug("there is a future target: " + agentInfo.getID().getValue());
            double length1, length2;
            List<EntityID> path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(policeTools.probableFutureTarget).calc().getResult();
            if (path == null || path.size() == 0){
                logger.debug("what the hell! (108)");
                return null;
            }
            length1 = lengthOfPath(path);
            ArrayList<EntityID> removeList = new ArrayList<>();
            for (EntityID entityID: fbList){
                logger.debug("chekicng " + entityID.getValue());
                StandardEntity standardEntity = worldInfo.getEntity(entityID);
                StandardEntity position = worldInfo.getPosition(standardEntity.getID());
                Road road = null;
                if (position != null && position instanceof Road){
                    road = (Road) position;
                }else if(position != null && position instanceof Building){
//                    road = findEntrance();
                    continue;
                }else
                    continue;
                if (road.isBlockadesDefined() && road.getBlockades().size() == 0){
                    removeList.add(entityID);
                    logger.debug("agent removed from list " + entityID.getValue());
                    continue;
                }
                logger.debug("agent is probably stuck: " + entityID.getValue());
                length2 = 0;
                EntityID positionOfID = worldInfo.getPosition(entityID).getID();
                List<EntityID> path1 = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(positionOfID).calc().getResult();
                List<EntityID> path2 = pathPlanning.setFrom(positionOfID).setDestination(policeTools.probableFutureTarget).calc().getResult();
                if (path1 == null || path1.size() == 0 || path2 == null || path2.size() == 0){
                    logger.debug("what the hell! (135)");
                    return null;
                }
                length2 = lengthOfPath(path1) + lengthOfPath(path2);
                if (length2 < length1*1.5){
                    logger.debug("agent was close to my path: " + entityID.getValue() + " im " + agentInfo.getID().getValue());
                    result = entityID;
                    break;
                }else
                    logger.debug("agent was far from my path: " + entityID.getValue() + " im " + agentInfo.getID().getValue());
            }
            fbList.removeAll(removeList);
            if (result != null)
                return result;
            removeList = new ArrayList<>();
            for (EntityID entityID: atList){
                logger.debug("chekicng " + entityID.getValue());
                StandardEntity standardEntity = worldInfo.getEntity(entityID);
                StandardEntity position = worldInfo.getPosition(standardEntity.getID());
                Road road = null;
                if (position != null && position instanceof Road){
                    road = (Road) position;
                }else if(position != null && position instanceof Building){
//                    road = findEntrance();
                    continue;
                }else
                    continue;
                if (road.isBlockadesDefined() && road.getBlockades().size() == 0){
                    removeList.add(entityID);
                    logger.debug("agent removed from list " + entityID.getValue());
                    continue;
                }
                logger.debug("ambulance agent is probably stuck: " + entityID.getValue());
                List<EntityID> path1 = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(entityID).calc().getResult();
                List<EntityID> path2 = pathPlanning.setFrom(entityID).setDestination(policeTools.probableFutureTarget).calc().getResult();
                if (path1 == null || path1.size() == 0 || path2 == null || path2.size() == 0){
                    logger.debug("what the hell! (135)");
                    return null;
                }
                length2 = lengthOfPath(path1) + lengthOfPath(path2);
                if (length2 < length1*1.3){
                    logger.debug("agent was close to my path: " + entityID.getValue() + " im " + agentInfo.getID().getValue());
                    result = entityID;
                    break;
                }else
                    logger.debug("agent was far from my path: " + entityID.getValue() + " im " + agentInfo.getID().getValue());
            }
            atList.removeAll(removeList);
            return result;

        }

    }

    private Road findEntrance() {
        return null;
    }

    private void updateInfo(){
        logger.debug("changed agents: ");
        for (EntityID entityID: worldInfo.getChanged().getChangedEntities()){
            StandardEntity standardEntity = worldInfo.getEntity(entityID);
            if (standardEntity instanceof FireBrigade){
                logger.debug("FB: " + entityID.getValue());
//                if (fbMap.containsKey(standardEntity.getID()))
//                    logger.debug("map had this id: " + standardEntity.getID().getValue());
//                else
//                    logger.debug("how the hell ...........................");
                PFInfo info = fbMap.get(standardEntity.getID());
                info.update();
                if(info.canMove){
                    fbList.remove(info.agentID);
                }
            }else if (standardEntity instanceof AmbulanceTeam){
                logger.debug("AT: " + entityID.getValue());
                PFInfo info = atMap.get(standardEntity.getID());
                info.update();
                if(info.canMove){
                    atList.remove(info.agentID);
                }
            }
        }
    }

    private class PFInfo{
        EntityID agentID;
        StandardEntity position;
        boolean canMove = false;
        int lastUpdate = 0;
        rescuecore2.misc.Pair<Integer,Integer> location;
        public void intialize(EntityID agentID){
            this.agentID = agentID;
            position = worldInfo.getPosition(agentID);
            location = worldInfo.getLocation(agentID);
        }
        public void update(){
            StandardEntity se = worldInfo.getPosition(agentID);
            EntityID newPosition = null;
            try {
                logger.debug("have i seen this agent? " + agentID.getValue() + " im " + agentInfo.getID().getValue());
                newPosition = se.getID();
            }catch (Exception e){
                logger.debug("agent " + agentID.getValue() + " is not position defined " + agentInfo.getTime());
            }
            if (newPosition == null){
                return;
            }
            lastUpdate = agentInfo.getTime();
            rescuecore2.misc.Pair<Integer,Integer> newLocation = worldInfo.getLocation(agentID);
            if (newPosition.getValue() != position.getID().getValue() && newLocation != null && distance((double) location.first(),(double)location.second(),(double)newLocation.first(),(double)newLocation.second()) > 10000){
                canMove = true;
                logger.debug("this agent can move: " + agentID.getValue() + " preloc: " + position.getID().getValue() + " newloc " + newPosition.getValue());
                logger.debug(" X: " + location.first() +" Y: " + location.second() + " newX: " + newLocation.first() + " newY: " + newLocation.second());
            }

        }
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

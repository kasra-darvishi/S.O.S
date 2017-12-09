package sos.module.complex.police;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.*;

/**
 * Created by kasra on 10/19/17.
 */
public class OpenMST extends PO_AbstractState {

    private boolean precomputed = /*false*/ true;
//    private List<EntityID> nodes;
    private List<EntityID>[][] paths;
    private Map<EntityID, Map<EntityID, List<EntityID>>>    pathsBetweenCenters;
    private EntityID myCenter;
    private List<EntityID> conectedCenters, centers;
    private EntityID entityOfWorkingCenter = null;
    private EntityID currentWorkingCenter = null;
    private boolean goingCenter = false;
    private Collection<EntityID> myClusterEntities;
    private boolean allCentersConnected = false;
    private ArrayList<Pair<EntityID,Boolean>> chosenCenters;


    public OpenMST(WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, SOSPathPlanning_Police pp, Logger logger, PoliceClustering clustering, ArrayList<EntityID> isReached, PoliceTools policeTools) {

        super(worldInfo, agentInfo, scenarioInfo, pp, logger, clustering, isReached, policeTools);
        if (/*scenarioInfo.getMode() == ScenarioInfo.Mode.NON_PRECOMPUTE*/ false){
            logger.debug("there was no precompute! ");
            precomputed = false;
        }else {

            /**
             * MST of agent cluster
             */



            /**
             * MST of all clusters
             */
            pathsBetweenCenters = policeTools.pathsBetweenCenters;
            myCenter = clustering.centerMap.get(agentInfo.getID());
            myClusterEntities = clustering.getClusterEntityIDs(clustering.getClusterIndex(agentInfo.getID()));
            conectedCenters = new ArrayList<>();
            chosenCenters = new ArrayList<>();
            System.out.println("\n............. " + agentInfo.getID().getValue() + " ..............");
            System.out.println("my center: " + myCenter);
            //it may use so much time
            //check for large number of nodes
            chooseCenters();
            goToCenter();

        }

    }

    //chooses centers to connect to its center
    private void chooseCenters() {
//        printMST();
        centers = new ArrayList<>();
        for (Map.Entry<EntityID, Map<EntityID, List<EntityID>>> entry: pathsBetweenCenters.entrySet()) {
            if (entry.getKey().getValue() == myCenter.getValue()){
                for (Map.Entry<EntityID, List<EntityID>> entry2 : entry.getValue().entrySet()) {
                    centers.add(entry2.getKey());
                }
            }
        }


        if (centers.size() == 0){
            logger.debug("i have no center to connect to mine " + agentInfo.getID().getValue());
            return;
        }

        if (centers.size() == 1){
            logger.debug("my size");
            if (pathsBetweenCenters.get(centers.get(0)).entrySet().size() == 1) {
                chosenCenters.add(new Pair<EntityID, Boolean>(centers.get(0), false));
                logger.debug("connect straight to other center");
            }else{
                chosenCenters.add(new Pair<EntityID, Boolean>(centers.get(0), true));
                logger.debug("connect until your cluster");
            }
        }else {
            ArrayList<Map.Entry<EntityID, Map<EntityID, List<EntityID>>>> toRemove = new ArrayList<>();
            for (Map.Entry<EntityID, Map<EntityID, List<EntityID>>> entry : pathsBetweenCenters.entrySet()) {
                if (entry.getValue().entrySet().size() == 1){
                    toRemove.add(entry);
                }
            }
            pathsBetweenCenters.entrySet().removeAll(toRemove);
            for (Map.Entry<EntityID, Map<EntityID, List<EntityID>>> entry : pathsBetweenCenters.entrySet()){
                ArrayList<Map.Entry<EntityID, List<EntityID>>> toRemove2 = new ArrayList<>();
                for (Map.Entry<EntityID, List<EntityID>> entry2: entry.getValue().entrySet()){
                    for (Map.Entry<EntityID, Map<EntityID, List<EntityID>>> r: toRemove){
                        if (entry2.getKey().getValue() == r.getKey().getValue())
                            toRemove2.add(entry2);
                    }
                }
                entry.getValue().entrySet().removeAll(toRemove2);
            }
            chooseCenters();
        }

    }

    @Override
    public EntityID check() {

        logger.debug("\n\n... OPEN MST STATE ... " + agentInfo.getID().getValue() + " clock: " + agentInfo.getTime());
        result = null;

        if (!precomputed || allCentersConnected){
            return null;
        }
        if (goingCenter){
            if (iveReachedCenter()){
                goingCenter = false;
                logger.debug("returned to my center: " + myCenter.getValue() + " agent: " + agentInfo.getID().getValue());
            }else {
                logger.debug("still going to my center: "+ myCenter.getValue() + " agent: " + agentInfo.getID().getValue());
                goToCenter();
                logger.debug("entity to be cleared: " + result+ " agent: " + agentInfo.getID().getValue());
                policeTools.probableFutureTarget = result;
                return result;
            }
        }else if (entityOfWorkingCenter != null){
            if (agentInfo.getPosition().getValue() != entityOfWorkingCenter.getValue()){
                logger.debug("still connecting to other center: "+ entityOfWorkingCenter.getValue() + " agent: " + agentInfo.getID().getValue());
                result = entityOfWorkingCenter;
                logger.debug("entity to be cleared: " + result+ " agent: " + agentInfo.getID().getValue());
                policeTools.probableFutureTarget = result;
                return result;
            }else {
                centers.remove(currentWorkingCenter);
                logger.debug("center: " + myCenter.getValue() + " connected to: " + currentWorkingCenter.getValue() + " by: " + agentInfo.getID().getValue());
                entityOfWorkingCenter = null;
                currentWorkingCenter = null;
                allCentersConnected = true;
                logger.debug("clearde my share of MST: " + agentInfo.getID().getValue() + " cycle: " + agentInfo.getTime());
                policeTools.probableFutureTarget = null;
                return null;
            }
        }

        for (Pair<EntityID,Boolean> pair: chosenCenters){
            logger.debug("a center: " + pair.getFirst().getValue());
            if (conectedCenters.contains(pair.getFirst()))
                continue;
            connectTo(pair.getFirst(), pair.getSecond());
            logger.debug("entity to be cleared: " + result+ " agent: " + agentInfo.getID().getValue());
            policeTools.probableFutureTarget = result;
            return result;
        }

        policeTools.probableFutureTarget = null;
        return null;
    }

    private boolean iveReachedCenter(){

        if (agentInfo.getPosition().getValue() == myCenter.getValue()){
            return true;
        }else {
            for (EntityID entityID: ((Area)worldInfo.getEntity(myCenter)).getNeighbours()){
                if (agentInfo.getPosition().getValue() == entityID.getValue()){
                    logger.debug("reached the neighbour of the my center!!");
                    return true;
                }
            }
        }

        return false;
    }

    private void connectTo(EntityID centerID, boolean goStraight) {

        if (goStraight){
            result = centerID;
            entityOfWorkingCenter = centerID;
            currentWorkingCenter = centerID;
            logger.debug("entity to be connected from MST is center it self "+ agentInfo.getID().getValue());
            return;
        }

        List<EntityID> path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(centerID).calc().getResult();
        if (path == null || path.size() == 0){
            logger.debug("could find no path!! " + agentInfo.getPosition().getValue() + " to " + centerID.getValue());
            result = null;
            return;
        }
        long t = System.currentTimeMillis();
        int i = 0;
        for (EntityID id: path){
            if (myClusterEntities.contains(id)){
                i++;
                continue;
            }
            entityOfWorkingCenter = id;
            currentWorkingCenter = centerID;
            logger.debug("entity to be connected from MST: " + id.getValue()+ " agent: " + agentInfo.getID().getValue());
            if (i == 0)
                logger.debug("how the fuck is this possible?!! #117");
            break;
        }
        logger.debug("it took " + (System.currentTimeMillis() - t) + " ...");
        result = path.get(path.size() - 1);

    }

    private void goToCenter() {

//        List<EntityID> path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(myCenter).calc().getResult();
//        if (path == null || path.size() == 0){
//            logger.debug("could find no path!! " + agentInfo.getPosition().getValue() + " to " + myCenter.getValue());
//            result = null;
//            return;
//        }
        goingCenter = true;
//        result = path.get(path.size() - 1);

        result = myCenter;
    }

    private void printMST(){
        System.out.println("\nnew mst\n");
        for (Map.Entry<EntityID, Map<EntityID, List<EntityID>>> entry: pathsBetweenCenters.entrySet()) {
            System.out.println("\n### " + entry.getKey().getValue());
            for (Map.Entry<EntityID, List<EntityID>> entry2 : entry.getValue().entrySet()) {
                System.out.println("   " + entry2.getKey().getValue());
            }
        }
    }

}

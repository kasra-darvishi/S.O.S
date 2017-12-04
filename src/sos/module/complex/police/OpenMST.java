package sos.module.complex.police;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import org.apache.log4j.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by kasra on 10/19/17.
 */
public class OpenMST extends PO_AbstractState {

    private boolean precomputed = /*false*/ true;
//    private List<EntityID> nodes;
    private List<EntityID>[][] paths;
    private Map<EntityID, Map<EntityID, List<EntityID>>> pathsBetweenCenters;
    private EntityID myCenter;
    private List<EntityID> conectedCenters, centers;
    private EntityID entityOfWorkingCenter = null;
    private EntityID currentWorkingCenter = null;
    private boolean goingCenter = false;
    private Collection<EntityID> myClusterEntities;
    private boolean allCentersConnected = false;


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
            centers = new ArrayList<>();
            for (Map.Entry<EntityID, Map<EntityID, java.util.List<EntityID>>> entry: pathsBetweenCenters.entrySet()) {
                if (entry.getKey().getValue() == agentInfo.getID().getValue()){
                    for (Map.Entry<EntityID, List<EntityID>> entry2 : entry.getValue().entrySet()) {
                        centers.add(entry2.getKey());
                    }
                }
            }

            goToCenter();

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
                logger.debug("entity to be cleared: " + result);
                return result;
            }
        }else if (entityOfWorkingCenter != null){
            if (agentInfo.getPosition().getValue() != entityOfWorkingCenter.getValue()){
                logger.debug("still connecting to other center: "+ entityOfWorkingCenter.getValue() + " agent: " + agentInfo.getID().getValue());
                List<EntityID> path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(entityOfWorkingCenter).calc().getResult();
                if (path == null || path.size() == 0){
                    logger.debug("could find no path!! " + agentInfo.getPosition().getValue() + " to " + entityOfWorkingCenter.getValue());
                }
                result = path.get(path.size() - 1);
                logger.debug("entity to be cleared: " + result);
                return result;
            }else {
                centers.remove(currentWorkingCenter);
                logger.debug("center: " + myCenter.getValue() + " connected to: " + currentWorkingCenter.getValue() + " by: " + agentInfo.getID().getValue());
                entityOfWorkingCenter = null;
                currentWorkingCenter = null;
                if (centers.size() != 0){
                    goToCenter();
                    logger.debug("entity to be cleared: " + result);
                    return result;
                }else {
                    allCentersConnected = true;
                    return null;
                }
            }
        }
        for (EntityID centerID: centers){
            if (conectedCenters.contains(centerID))
                continue;
            connectTo(centerID);
            logger.debug("entity to be cleared: " + result);
            return result;
        }


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

    private void connectTo(EntityID centerID) {

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
            logger.debug("entity to be connected from MST: " + id.getValue());
            if (i == 0)
                logger.debug("how the fuck is this possible?!! #117");
            break;
        }
        logger.debug("it took " + (System.currentTimeMillis() - t) + " ...");
        result = path.get(path.size() - 1);

    }

    private void goToCenter() {

        List<EntityID> path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(myCenter).calc().getResult();
        if (path == null || path.size() == 0){
            logger.debug("could find no path!! " + agentInfo.getPosition().getValue() + " to " + myCenter.getValue());
            result = null;
            return;
        }
        goingCenter = true;
        result = path.get(path.size() - 1);

    }

}

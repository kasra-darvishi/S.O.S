package sos.module.complex.police;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import org.apache.log4j.Logger;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kasra on 10/19/17.
 */
public abstract class PO_AbstractState {

    protected WorldInfo worldInfo;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;
    protected SOSPathPlanning_Police pathPlanning;
    protected EntityID result = null;
    protected Logger logger;
    protected Clustering clustering;
    protected ArrayList<EntityID> isReached;

    public PO_AbstractState(WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, SOSPathPlanning_Police pp, Logger logger, Clustering clustering, ArrayList<EntityID> isReached){
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.pathPlanning = pp;
        this.logger = logger;
        this.clustering = clustering;
        this.isReached = isReached;
    }

    public abstract EntityID check();

    public boolean isReachable(StandardEntity standardEntity, boolean targetIsMSTNode){

        if (standardEntity instanceof Human){
            Human human = (Human) standardEntity;
            Area area = (Area) worldInfo.getPosition(human);
            if (area != null && area.isBlockadesDefined()) {
                for (EntityID blockadeID: area.getBlockades()){
                    Blockade blockade = (Blockade) worldInfo.getEntity(blockadeID);
                    rescuecore2.misc.Pair<Integer,Integer> loc = worldInfo.getLocation(human);
                    try {
                        if (blockade.getShape().contains(loc.first(),loc.second())){
                            logger.debug("Human stuck in blockades: " + ((Human) standardEntity).getID().getValue());
                            return false;
                        }
                    }catch (Exception e){

                    }
                }
            }
        }

        if (standardEntity instanceof Refuge || targetIsMSTNode)
            if (isReached.contains(standardEntity.getID()))
                return true;
            else
                return false;

        pathPlanning.setFrom(agentInfo.getPosition());
        pathPlanning.setDestination(standardEntity.getID());
        List<EntityID> path = pathPlanning.calc().getResult();
        if (path != null && path.size() > 0){
            for (EntityID id: path){
                if (!((Area)worldInfo.getEntity(id)).isBlockadesDefined()){
                    return false;
                }
            }
            return true;
        }else
            return false;


    }

}

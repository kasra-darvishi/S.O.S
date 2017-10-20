package sos.module.complex.police;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import org.apache.log4j.Logger;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by kasra on 10/19/17.
 */
public class UrgentTargets extends PO_AbstractState{

    private ArrayList<StandardEntity> refuges;
    private Collection<StandardEntity> allClusterEntities;
    private boolean allRefsAreOpen = false;

    public UrgentTargets(WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, SOSPathPlanning_Police pp, Logger logger, Clustering clustering, ArrayList<EntityID> isReached) {
        super(worldInfo, agentInfo, scenarioInfo, pp, logger, clustering, isReached);
        refuges = new ArrayList<>();
        allClusterEntities = clustering.getClusterEntities(clustering.getClusterIndex(agentInfo.getID()));
        for (StandardEntity standardEntity: allClusterEntities){
            if (standardEntity instanceof Refuge)
                refuges.add(standardEntity);
        }
    }

    @Override
    public EntityID check() {

        if (!allRefsAreOpen){
            ArrayList<StandardEntity> removeList = new ArrayList<>();
            EntityID target = null;
            for (StandardEntity ref: refuges){
                if (!isReachable(ref, false)){
                    logger.debug("refuge selected: " + ref.getID().getValue());
                    target = ref.getID();
                    break;
                }else {
                    logger.debug("ref was reachable: " + ref.getID().getValue());
                    removeList.add(ref);
                }
            }
            refuges.removeAll(removeList);
            if (target != null)
                return target;
            else{
                logger.debug("all refs in my cluster are open! ID: " + agentInfo.getID().getValue() + " cluster index: " + clustering.getClusterIndex(agentInfo.getID()));
                allRefsAreOpen = true;
            }
        }

        return null;
    }

}

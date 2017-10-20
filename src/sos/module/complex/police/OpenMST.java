package sos.module.complex.police;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import org.apache.log4j.Logger;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.ArrayList;

/**
 * Created by kasra on 10/19/17.
 */
public class OpenMST extends PO_AbstractState {


    public OpenMST(WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, SOSPathPlanning_Police pp, Logger logger, Clustering clustering, ArrayList<EntityID> isReached) {

        super(worldInfo, agentInfo, scenarioInfo, pp, logger, clustering, isReached);

    }

    @Override
    public EntityID check() {
        return null;
    }
}

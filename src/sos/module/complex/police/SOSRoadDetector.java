package sos.module.complex.police;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.RoadDetector;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sos.module.algorithm.SOSPathPlanning_Police;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * Created by kasra on 10/19/17.
 */
public class SOSRoadDetector extends RoadDetector
{
    private ArrayList<PO_AbstractState> states;

    private SOSPathPlanning_Police pathPlanning;

    private Set<EntityID> targetAreas;
    private Set<EntityID> priorityRoads;
    private EntityID result;
    private PoliceClustering clustering;

    ArrayList<EntityID> isReached;
    private List<EntityID> centerIDs;

    private Logger logger;
    private PoliceTools policeTools;
    private int numberOfPF;


    public SOSRoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);

        PropertyConfigurator.configure("log4j.properties");
        logger = Logger.getLogger("SOSRoadDetector");
        isReached = new ArrayList<>();
        numberOfPF = worldInfo.getEntitiesOfType(POLICE_FORCE).size();

        switch (scenarioInfo.getMode())
        {
            case PRECOMPUTATION_PHASE:
                clustering = moduleManager.getModule("", "");
                this.pathPlanning = /*(SOSPathPlanning_Police)*/ moduleManager.getModule("sos.module.algorithm.SOSPathPlanning", "SampleRoadDetector.PathPlanning");
                break;
            case PRECOMPUTED:
                clustering = moduleManager.getModule("", "");
                this.pathPlanning = /*(SOSPathPlanning_Police)*/ moduleManager.getModule("sos.module.algorithm.SOSPathPlanning", "SampleRoadDetector.PathPlanning");
                break;
            case NON_PRECOMPUTE:
                clustering = moduleManager.getModule("", "");
                this.pathPlanning = /*(SOSPathPlanning_Police)*/ moduleManager.getModule("sos.module.algorithm.SOSPathPlanning", "SampleRoadDetector.PathPlanning");
                break;
        }
        registerModule(this.pathPlanning);
        this.result = null;

        policeTools = new PoliceTools(pathPlanning, logger, worldInfo);


        states = new ArrayList<>();
    }

    @Override
    public RoadDetector calc()
    {

        EntityID target;

        for (PO_AbstractState state: states){
            target = state.check();
            if (target != null){
                result = target;
                return this;
            }
        }

        return this;
    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public RoadDetector precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        pathPlanning.precompute(precomputeData);
        clustering.precompute(precomputeData);

        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }

        states.add(new UrgentTargets(worldInfo, agentInfo, scenarioInfo, pathPlanning,logger, clustering, isReached, policeTools));
        states.add(new OpenMST(worldInfo, agentInfo, scenarioInfo, pathPlanning,logger, clustering, isReached, policeTools));

        centerIDs = precomputeData.getEntityIDList("sample.clustering.centers");

        //TODO kasra >> this could be done only once in the precompute phase. so change if it takes shorter time!
        //calculates MST on center of clusters
        findMST();

        return this;
    }

    private void findMST() {
        policeTools.prime(centerIDs.size(), centerIDs, true);
    }

    @Override
    public RoadDetector preparate()
    {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }

        states.add(new UrgentTargets(worldInfo, agentInfo, scenarioInfo, pathPlanning,logger, clustering, isReached, policeTools));
        states.add(new OpenMST(worldInfo, agentInfo, scenarioInfo, pathPlanning,logger, clustering, isReached, policeTools));

        return this;
    }

    @Override
    public RoadDetector updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        if (this.result != null)
        {
            if (this.agentInfo.getPosition().equals(this.result))
            {
                StandardEntity entity = this.worldInfo.getEntity(this.result);
                if (entity instanceof Building)
                {
                    this.result = null;
                }
                else if (entity instanceof Road)
                {
                    Road road = (Road) entity;
                    if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
                    {
                        this.targetAreas.remove(this.result);
                        this.result = null;
                    }
                }
            }
        }
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        for (CommunicationMessage message : messageManager.getReceivedMessageList())
        {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if (messageClass == MessageAmbulanceTeam.class)
            {
                this.reflectMessage((MessageAmbulanceTeam) message);
            }
            else if (messageClass == MessageFireBrigade.class)
            {
                this.reflectMessage((MessageFireBrigade) message);
            }
            else if (messageClass == MessageRoad.class)
            {
                this.reflectMessage((MessageRoad) message, changedEntities);
            }
            else if (messageClass == MessagePoliceForce.class)
            {
                this.reflectMessage((MessagePoliceForce) message);
            }
            else if (messageClass == CommandPolice.class)
            {
                this.reflectMessage((CommandPolice) message);
            }
        }
        for (EntityID id : this.worldInfo.getChanged().getChangedEntities())
        {
            StandardEntity entity = this.worldInfo.getEntity(id);
            if (entity instanceof Road)
            {
                Road road = (Road) entity;
                if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
                {
                    this.targetAreas.remove(id);
                }
            }
        }
        return this;
    }

    private void reflectMessage(MessageRoad messageRoad, Collection<EntityID> changedEntities)
    {
        if (messageRoad.isBlockadeDefined() && !changedEntities.contains(messageRoad.getBlockadeID()))
        {
            MessageUtil.reflectMessage(this.worldInfo, messageRoad);
        }
        if (messageRoad.isPassable())
        {
            this.targetAreas.remove(messageRoad.getRoadID());
        }
    }

    private void reflectMessage(MessageAmbulanceTeam messageAmbulanceTeam)
    {
        if (messageAmbulanceTeam.getPosition() == null)
        {
            return;
        }
        if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_RESCUE)
        {
            StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
            if (position != null && position instanceof Building)
            {
                this.targetAreas.removeAll(((Building) position).getNeighbours());
            }
        }
        else if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_LOAD)
        {
            StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
            if (position != null && position instanceof Building)
            {
                this.targetAreas.removeAll(((Building) position).getNeighbours());
            }
        }
        else if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_MOVE)
        {
            if (messageAmbulanceTeam.getTargetID() == null)
            {
                return;
            }
            StandardEntity target = this.worldInfo.getEntity(messageAmbulanceTeam.getTargetID());
            if (target instanceof Building)
            {
                for (EntityID id : ((Building) target).getNeighbours())
                {
                    StandardEntity neighbour = this.worldInfo.getEntity(id);
                    if (neighbour instanceof Road)
                    {
                        this.priorityRoads.add(id);
                    }
                }
            }
            else if (target instanceof Human)
            {
                Human human = (Human) target;
                if (human.isPositionDefined())
                {
                    StandardEntity position = this.worldInfo.getPosition(human);
                    if (position instanceof Building)
                    {
                        for (EntityID id : ((Building) position).getNeighbours())
                        {
                            StandardEntity neighbour = this.worldInfo.getEntity(id);
                            if (neighbour instanceof Road)
                            {
                                this.priorityRoads.add(id);
                            }
                        }
                    }
                }
            }
        }
    }

    private void reflectMessage(MessageFireBrigade messageFireBrigade)
    {
        if (messageFireBrigade.getTargetID() == null)
        {
            return;
        }
        if (messageFireBrigade.getAction() == MessageFireBrigade.ACTION_REFILL)
        {
            StandardEntity target = this.worldInfo.getEntity(messageFireBrigade.getTargetID());
            if (target instanceof Building)
            {
                for (EntityID id : ((Building) target).getNeighbours())
                {
                    StandardEntity neighbour = this.worldInfo.getEntity(id);
                    if (neighbour instanceof Road)
                    {
                        this.priorityRoads.add(id);
                    }
                }
            }
            else if (target.getStandardURN() == HYDRANT)
            {
                this.priorityRoads.add(target.getID());
                this.targetAreas.add(target.getID());
            }
        }
    }

    private void reflectMessage(MessagePoliceForce messagePoliceForce)
    {
        if (messagePoliceForce.getAction() == MessagePoliceForce.ACTION_CLEAR)
        {
            if (messagePoliceForce.getAgentID().getValue() != this.agentInfo.getID().getValue())
            {
                if (messagePoliceForce.isTargetDefined())
                {
                    EntityID targetID = messagePoliceForce.getTargetID();
                    if (targetID == null)
                    {
                        return;
                    }
                    StandardEntity entity = this.worldInfo.getEntity(targetID);
                    if (entity == null)
                    {
                        return;
                    }

                    if (entity instanceof Area)
                    {
                        this.targetAreas.remove(targetID);
                        if (this.result != null && this.result.getValue() == targetID.getValue())
                        {
                            if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue())
                            {
                                this.result = null;
                            }
                        }
                    }
                    else if (entity.getStandardURN() == BLOCKADE)
                    {
                        EntityID position = ((Blockade) entity).getPosition();
                        this.targetAreas.remove(position);
                        if (this.result != null && this.result.getValue() == position.getValue())
                        {
                            if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue())
                            {
                                this.result = null;
                            }
                        }
                    }

                }
            }
        }
    }

    private void reflectMessage(CommandPolice commandPolice)
    {
        boolean flag = false;
        if (commandPolice.isToIDDefined() && this.agentInfo.getID().getValue() == commandPolice.getToID().getValue())
        {
            flag = true;
        }
        else if (commandPolice.isBroadcast())
        {
            flag = true;
        }
        if (flag && commandPolice.getAction() == CommandPolice.ACTION_CLEAR)
        {
            if (commandPolice.getTargetID() == null)
            {
                return;
            }
            StandardEntity target = this.worldInfo.getEntity(commandPolice.getTargetID());
            if (target instanceof Area)
            {
                this.priorityRoads.add(target.getID());
                this.targetAreas.add(target.getID());
            }
            else if (target.getStandardURN() == BLOCKADE)
            {
                Blockade blockade = (Blockade) target;
                if (blockade.isPositionDefined())
                {
                    this.priorityRoads.add(blockade.getPosition());
                    this.targetAreas.add(blockade.getPosition());
                }
            }
        }
    }
}
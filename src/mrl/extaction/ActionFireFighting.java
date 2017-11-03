package mrl.extaction;


import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.fire.ActionExtinguish;
import adf.agent.action.fire.ActionRefill;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import com.mrl.debugger.remote.VDClient;
import mrl.complex.firebrigade.FireBrigadeUtilities;
import mrl.complex.firebrigade.MrlFireBrigadeWorld;
import mrl.util.Util;
import mrl.viewer.MrlPersonalData;
import mrl.world.MrlWorldHelper;
import mrl.world.entity.MrlBuilding;
import mrl.world.entity.MrlRoad;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.*;

import static rescuecore2.misc.Handy.objectsToIDs;
import static rescuecore2.standard.entities.StandardEntityURN.HYDRANT;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

public class ActionFireFighting extends ExtAction {
    private PathPlanning pathPlanning;

    private int maxExtinguishDistance;
    private int maxExtinguishPower;
    private int thresholdRest;
    private int kernelTime;
    private int refillCompleted;
    private int refillRequest;
    private boolean refillFlag;
    private ExtAction actionExtMove;
    private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;
//    private MrlFireClustering clustering;
    private MrlFireBrigadeWorld worldHelper;
    private MessageManager messageManager;
    private EntityID target;

//    private Map<StandardEntity, Set<EntityID>> observableAreas;
//    private Map<StandardEntity, Set<EntityID>> visibleFromAreas;

    public ActionFireFighting(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance();
        this.maxExtinguishPower = scenarioInfo.getFireExtinguishMaxSum();
        this.thresholdRest = developData.getInteger("ActionFireFighting.rest", 100);
        int maxWater = scenarioInfo.getFireTankMaximum();
        this.refillCompleted = (maxWater / 10) * developData.getInteger("ActionFireFighting.refill.completed", 10);
        this.refillRequest = this.maxExtinguishPower * developData.getInteger("ActionFireFighting.refill.request", 1);
        this.refillFlag = false;

        this.target = null;
//        observableAreas = new HashMap<>();
//        visibleFromAreas = new HashMap<>();
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
//                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
//                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
//                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
        }

        this.worldHelper = (MrlFireBrigadeWorld) MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
//        this.clustering.precompute(precomputeData);
        this.actionExtMove.precompute(precomputeData);

//        fillProperties();
//        fillVisibleSets();
//        ProcessAreaVisibility.process((MrlFireBrigadeWorld) worldHelper, worldInfo, scenarioInfo, true);

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }

        fillProperties();
//        fillVisibleSets();
//        ProcessAreaVisibility.process((MrlFireBrigadeWorld) worldHelper, worldInfo, scenarioInfo, false);


        this.worldHelper.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
//        this.clustering.resume(precomputeData);
        this.actionExtMove.resume(precomputeData);

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }

//        fillVisibleSets();
//        ProcessAreaVisibility.process((MrlFireBrigadeWorld) worldHelper, worldInfo, scenarioInfo, false);

        this.worldHelper.preparate();
        this.pathPlanning.preparate();
//        this.clustering.preparate();
        this.actionExtMove.preparate();
        fillProperties();

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager) {
        this.messageManager = messageManager;
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.worldHelper.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        this.actionExtMove.updateInfo(messageManager);
//        this.clustering.updateInfo(messageManager);

        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;
        if (target != null) {
            StandardEntity entity = this.worldInfo.getEntity(target);
            if (entity instanceof Building) {
                this.target = target;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;
        FireBrigade agent = (FireBrigade) this.agentInfo.me();

        this.refillFlag = this.needRefill(agent, this.refillFlag);
        if (this.refillFlag) {
            this.result = this.calcRefill(agent, this.pathPlanning, this.target);
            if (this.result != null) {
                return this;
            }
        }

        if (this.needRest(agent)) {
            this.result = this.calcRefugeAction(agent, this.pathPlanning, this.target, false);
            if (this.result != null) {
                return this;
            }
        }

        if (this.target == null) {
            return this;
        }
        this.result = this.calcExtinguish(agent, this.pathPlanning, this.target);
        return this;
    }

    private void fillProperties() {
        for (MrlBuilding mrlBuilding : worldHelper.getMrlBuildings()) {
            Set<EntityID> extinguishableFromAreas = FireBrigadeUtilities.findAreaIDsInExtinguishRange(worldInfo, scenarioInfo, mrlBuilding.getID());
            List<MrlBuilding> buildingsInExtinguishRange = new ArrayList<MrlBuilding>();
            for (EntityID next : extinguishableFromAreas) {
                if (worldInfo.getEntity(next) instanceof Building) {
                    buildingsInExtinguishRange.add(worldHelper.getMrlBuilding(next));
                }
            }
            mrlBuilding.setExtinguishableFromAreas(extinguishableFromAreas);
//            extinguishableFromAreasMap.put(mrlBuilding.getID(), extinguishableFromAreas);
            mrlBuilding.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
//            buildingsInExtinguishRangeMap.put(mrlBuilding.getID(), buildingsInExtinguishRange);
        }
        for (MrlRoad mrlRoad : worldHelper.getMrlRoads()) {
            List<MrlBuilding> buildingsInExtinguishRange = FireBrigadeUtilities.findBuildingsInExtinguishRangeOf(worldHelper, worldInfo, scenarioInfo, mrlRoad.getID());
            mrlRoad.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
//            buildingsInExtinguishRangeMap.put(mrlRoad.getID(), buildingsInExtinguishRange);
        }

//        MrlPersonalData.VIEWER_DATA.setExtinguishData(extinguishableFromAreasMap, buildingsInExtinguishRangeMap);
    }


    private Action calcExtinguish(FireBrigade agent, PathPlanning pathPlanning, EntityID target) {
        EntityID agentPosition = agent.getPosition();
        StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getPosition(agent));
//        if (StandardEntityURN.REFUGE == positionEntity.getStandardURN()) {
//            Action action = this.getMoveAction(pathPlanning, agentPosition, target);
//            if (action != null) {
//                return action;
//            }
//        }

        List<StandardEntity> neighbourBuilding = new ArrayList<>();
        StandardEntity entity = this.worldInfo.getEntity(target);
        if (entity instanceof Building) {
            if (this.worldInfo.getDistance(positionEntity, entity) < this.maxExtinguishDistance) {
                neighbourBuilding.add(entity);
            }
        }

        if (neighbourBuilding.size() > 0) {
            neighbourBuilding.sort(new DistanceSorter(this.worldInfo, agent));

            int waterPower;
            if (worldHelper != null) {
                MrlBuilding targetMrlBuilding = worldHelper.getMrlBuilding(target);
                waterPower = FireBrigadeUtilities.calculateWaterPower(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), targetMrlBuilding);
                targetMrlBuilding.increaseWaterQuantity(waterPower);
            } else {
                waterPower = FireBrigadeUtilities.calculateWaterPowerNotEstimated(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), (Building) worldInfo.getEntity(target));
            }

//            if (messageManager != null) {
//                sendWaterMessage(target, waterPower);
//            }

            return new ActionExtinguish(neighbourBuilding.get(0).getID(), waterPower);
        }

        StandardEntity bestLocation = chooseBestLocationToStandForExtinguishingFire(target);
        if (bestLocation == null) {
            return null;
        }
        List<EntityID> movePlan = pathPlanning.setFrom(agentPosition).setDestination(bestLocation.getID()).calc().getResult();
        if (movePlan == null || movePlan.isEmpty()) {
            Collection<StandardEntity> inRange = worldInfo.getObjectsInRange(bestLocation, maxExtinguishDistance / 3);
            int counter = 0;
            for (StandardEntity e : inRange) {
                if (e instanceof Area && worldInfo.getDistance(target, e.getID()) < maxExtinguishDistance) {

                    movePlan = pathPlanning.setFrom(agentPosition).setDestination(e.getID()).calc().getResult();
                    counter++;
                    if (movePlan != null && !movePlan.isEmpty()) {
                        return getMoveAction(movePlan);
                    }
                    if (counter > 3) {
                        lastTryToExtinguish();
                    }
                }
            }
        }
        return this.getMoveAction(pathPlanning, agentPosition, bestLocation.getID());
    }

    private void sendWaterMessage(EntityID target, int waterPower) {
        if (waterPower == scenarioInfo.getFireExtinguishMaxSum()) {
            messageManager.addMessage(new MessageFireBrigade(true, (FireBrigade) agentInfo.me(), 23, target));
        } else {
            messageManager.addMessage(new MessageFireBrigade(true, (FireBrigade) agentInfo.me(), 24, target));
        }
    }


    private Action lastTryToExtinguish() {

        if (MrlPersonalData.DEBUG_MODE) {
//            System.out.println(agentInfo.me() + "in lastTryToExtinguish");
        }
        Set<StandardEntity> buildingsInMyExtinguishRange = getBuildingsInMyExtinguishRange();
        List<StandardEntity> fieryBuildingsInMyExtinguishRange = new ArrayList<>();
        for (StandardEntity entity : buildingsInMyExtinguishRange) {
            Building building = (Building) entity;
            if (building.isOnFire()) {
                fieryBuildingsInMyExtinguishRange.add(entity);
            }
        }
        StandardEntity tempTarget = findNearest(fieryBuildingsInMyExtinguishRange, agentInfo.getPositionArea());
        if (tempTarget != null) {
//            int waterPower = FireBrigadeUtilities.calculateWaterPower(world, tempTarget);
//            ((MrlFireBrigade) platoonAgent).getFireBrigadeMessageHelper().sendWaterMessage(tempTarget.getID(), waterPower);
//            tempTarget.increaseWaterQuantity(waterPower);
//            platoonAgent.sendExtinguishAct(world.getTime(), tempTarget.getID(), waterPower);
            if (MrlPersonalData.DEBUG_MODE) {
                System.out.println(agentInfo.me() + "target in lastTryToExtinguish " + tempTarget.getID());
            }


            int waterPower;
            if (worldHelper != null) {
                MrlBuilding targetMrlBuilding = worldHelper.getMrlBuilding(tempTarget.getID());
                waterPower = FireBrigadeUtilities.calculateWaterPower(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), targetMrlBuilding);
                targetMrlBuilding.increaseWaterQuantity(waterPower);
            } else {
                waterPower = FireBrigadeUtilities.calculateWaterPowerNotEstimated(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), (Building) worldInfo.getEntity(target));
            }


//            if (messageManager != null) {
//                sendWaterMessage(target, waterPower);
//            }

            return new ActionExtinguish(tempTarget.getID(), waterPower);
        }
        return null;
    }

    public Set<StandardEntity> getBuildingsInMyExtinguishRange() {
        Set<StandardEntity> result = new HashSet<>();
        int maxExtinguishDistance = this.maxExtinguishDistance - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : worldInfo.getObjectsInRange(agentInfo.getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                if (worldInfo.getDistance(next.getID(), agentInfo.getID()) < maxExtinguishDistance) {
                    result.add(next);
                }
            }
        }
        return result;
    }

    public StandardEntity findNearest(List<StandardEntity> buildings, StandardEntity basePosition) {
        StandardEntity result = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (StandardEntity next : buildings) {
            double dist = worldInfo.getDistance(next.getID(), basePosition.getID());
            if (dist < minDistance) {
                result = next;
                minDistance = dist;
            }
        }
        return result;

    }

//    private List<EntityID> getForbiddenLocations(MrlFireBrigadeWorld world, FireBrigadeTarget target) {
//        List<EntityID> forbiddenLocations = new ArrayList<EntityID>();
//
//        if (target.getCluster() != null) {
//            forbiddenLocations.addAll(objectsToIDs(target.getCluster().getAllEntities()));
//        }
//        forbiddenLocations.addAll(world.getBurningBuildings());
//        //if i am nth smallest FB, i should move over there
//        //to force FBs to create a ring around fire
//        int n = 3;
//        if (world.isMapMedium()) n = 10;
//        if (world.isMapHuge()) n = 15;
//        for (FireBrigade next : world.getFireBrigadeList()) {
//            if (world.getSelf().getID().getValue() < next.getID().getValue() && world.getDistance(world.getSelf().getID(), next.getID()) < world.getViewDistance() && --n <= 0) {
//                MrlRoad roadOfNearFB = world.getMrlRoad(next.getPosition());
//                MrlBuilding buildingOfNearFB = world.getMrlBuilding(next.getPosition());
//                if (roadOfNearFB != null) {
//                    forbiddenLocations.addAll(roadOfNearFB.getObservableAreas());
//                }
//                if (buildingOfNearFB != null) {
//                    forbiddenLocations.addAll(buildingOfNearFB.getObservableAreas());
//                }
//            }
//        }
//
//        MrlPersonalData.VIEWER_DATA.setForbiddenLocations(world.getSelf().getID(), forbiddenLocations);
//        return forbiddenLocations;
//    }

    private StandardEntity chooseBestLocationToStandForExtinguishingFire(EntityID target) {
        double minDistance = Integer.MAX_VALUE;
        Set<EntityID> forbiddenLocationIDs = getForbiddenLocations();
        List<StandardEntity> possibleAreas = new ArrayList<StandardEntity>();
        StandardEntity targetToExtinguish = null;
        double dis;
        Set<EntityID> extinguishableFromAreas = worldHelper.getMrlBuilding(target).getExtinguishableFromAreas();
//        visibleFromAreas.get(worldInfo.getEntity(target));
//        List<EntityID> extinguishableFromAreas = worldHelper.getMrlBuilding(target).getExtinguishableFromAreas();
//        Collection<EntityID> extinguishableFromAreas = worldInfo.getObjectIDsInRange(target, scenarioInfo.getFireExtinguishMaxDistance());
        for (EntityID next : extinguishableFromAreas) {
            StandardEntity entity = worldInfo.getEntity(next);
            possibleAreas.add(entity);
        }
//        MrlPersonalData.VIEWER_DATA.setBestPlaceToStand(agentInfo.getID(), worldInfo.getEntities(extinguishableFromAreas));

        List<StandardEntity> forbiddenLocations = worldHelper.getEntities(forbiddenLocationIDs);
        possibleAreas.removeAll(forbiddenLocations);
        if (possibleAreas.isEmpty()) {
            for (EntityID next : extinguishableFromAreas) {
                possibleAreas.add(worldInfo.getEntity(next));
            }
        }


        //fist search for a road to stand there
        for (StandardEntity entity : possibleAreas) {
            if (entity instanceof Road) {
                dis = worldInfo.getDistance(agentInfo.me(), entity);
                if (dis < minDistance) {
                    minDistance = dis;
                    targetToExtinguish = entity;
                }
            }
        }
        //if there is no road to stand, search for a no fiery building to go
        if (targetToExtinguish == null) {
            for (StandardEntity entity : possibleAreas) {
                if (entity instanceof Building) {
                    Building building = (Building) entity;
                    dis = worldInfo.getDistance(agentInfo.me(), entity);
                    if (dis < minDistance && (!building.isFierynessDefined() || (building.isFierynessDefined() && (building.getFieryness() >= 4 || building.getFieryness() <= 1)))) {
                        minDistance = dis;
                        targetToExtinguish = entity;
                    }
                }
            }
        }
        return targetToExtinguish;
    }


    private Set<EntityID> getForbiddenLocations() {
        Set<EntityID> forbiddenLocations = new HashSet<>();

        Collection<StandardEntity> clusterEntities = this.worldHelper.getFireClustering().getClusterEntities(this.worldHelper.getFireClustering().getMyClusterIndex());

        if (clusterEntities != null) {
            forbiddenLocations.addAll(objectsToIDs(clusterEntities));
        }

        //TODO: @MRL change this line to add cluster on-fire buildings
        forbiddenLocations.addAll(worldInfo.getFireBuildingIDs());
        //if i am nth smallest FB, i should move over there
        //to force FBs to create a ring around fire
        int n = 3;
        if (worldHelper.isMapMedium()) n = 10;
        if (worldHelper.isMapHuge()) n = 15;
        for (StandardEntity next : worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
            if (agentInfo.getID().getValue() < next.getID().getValue() && worldInfo.getDistance(agentInfo.getID(), next.getID()) < scenarioInfo.getPerceptionLosMaxDistance() && --n <= 0) {
                StandardEntity position = worldInfo.getPosition(next.getID());
                if (position != null) {
                    MrlBuilding buildingOfNearFB = worldHelper.getMrlBuilding(position.getID());
                    MrlRoad roadOfNearFB = worldHelper.getMrlRoad(position.getID());
                    if (roadOfNearFB != null) {
                        forbiddenLocations.addAll(roadOfNearFB.getObservableAreas());
                    }
                    if (buildingOfNearFB != null) {
                        forbiddenLocations.addAll(buildingOfNearFB.getObservableAreas());
                    }
//                    if (observableAreas.get(position) != null) {
//                        forbiddenLocations.addAll(observableAreas.get(position));
//                    } else {
//                        if (MrlPersonalData.DEBUG_MODE) {
//                            System.out.println(agentInfo.getTime() + " " + agentInfo.getID() + "observableAreas.get(position) is null for " + position);
//                        }
//                    }
                }
            }
        }


        if (MrlPersonalData.DEBUG_MODE) {
            List<Integer> elementList = Util.fetchIdValueFormElementIds(forbiddenLocations);
            VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlForbiddenAreaLayer", (Serializable) elementList);
        }

//        MrlPersonalData.VIEWER_DATA.setForbiddenLocations(world.getSelf().getID(), forbiddenLocations);
        return forbiddenLocations;
    }

//    private void fillVisibleSets() {
//        MrlLineOfSightPerception lineOfSightPerception = new MrlLineOfSightPerception(worldInfo, scenarioInfo);
//
//
//        Collection<StandardEntity> allAreaEntities = worldInfo.getEntitiesOfType(
//                StandardEntityURN.ROAD,
//                StandardEntityURN.HYDRANT,
//                StandardEntityURN.BUILDING,
//                StandardEntityURN.REFUGE,
//                StandardEntityURN.AMBULANCE_CENTRE,
//                StandardEntityURN.FIRE_STATION,
//                StandardEntityURN.POLICE_OFFICE
//        );
//
//        for (StandardEntity road : allAreaEntities) {
//            observableAreas.put(road, lineOfSightPerception.getVisibleAreas(road.getID()));
//        }
//
//        for (StandardEntity road1 : allAreaEntities) {
//            for (StandardEntity road2 : allAreaEntities) {
//                if (road1.equals(road2)) continue;
//                if (observableAreas.get(road2).contains(road1.getID())) {
//                    Set<EntityID> entityIDSet = visibleFromAreas.get(road1);
//                    if (entityIDSet == null) {
//                        entityIDSet = new HashSet<>();
//                    }
//                    entityIDSet.add(road2.getID());
//                    visibleFromAreas.put(road1, entityIDSet);
//                }
//            }
//
//            // to write to file for precompute
////            visibleFrom.put(road1.getID().getValue(), Util.EIDListToIntegerList(road1.getVisibleFrom()));
//        }
//    }


    private Action getMoveAction(PathPlanning pathPlanning, EntityID from, EntityID target) {
        pathPlanning.setFrom(from);
        pathPlanning.setDestination(target);
        List<EntityID> path = pathPlanning.calc().getResult();
        return getMoveAction(path);
    }

    private Action getMoveAction(List<EntityID> path) {
        if (path != null && path.size() > 0) {
            StandardEntity entity = this.worldInfo.getEntity(path.get(path.size() - 1));
            if (entity instanceof Building) {
                if (entity.getStandardURN() != StandardEntityURN.REFUGE) {
                    path.remove(path.size() - 1);
                }
            }
            if (!path.isEmpty()) {
                ActionMove moveAction = (ActionMove) actionExtMove.setTarget(path.get(path.size() - 1)).calc().getAction();
                if (moveAction != null) {
                    return moveAction;
                }
            }
            return null;
        }
        return null;
    }

    private boolean needRefill(FireBrigade agent, boolean refillFlag) {
        if (refillFlag) {
            StandardEntityURN positionURN = Objects.requireNonNull(this.worldInfo.getPosition(agent)).getStandardURN();
            return !(positionURN == REFUGE || positionURN == HYDRANT) || agent.getWater() < this.refillCompleted;
        }
        return agent.getWater() <= this.refillRequest;
    }

    private boolean needRest(Human agent) {
        int hp = agent.getHP();
        int damage = agent.getDamage();
        if (hp == 0 || damage == 0) {
            return false;
        }
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
        if (this.kernelTime == -1) {
            try {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            } catch (NoSuchConfigOptionException e) {
                this.kernelTime = -1;
            }
        }
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
    }

    private Action calcRefill(FireBrigade agent, PathPlanning pathPlanning, EntityID target) {
        StandardEntityURN positionURN = Objects.requireNonNull(this.worldInfo.getPosition(agent)).getStandardURN();
        if (positionURN == REFUGE) {
            return new ActionRefill();
        }
        Action action = this.calcRefugeAction(agent, pathPlanning, target, true);
        if (action != null) {
            return action;
        }
        action = this.calcHydrantAction(agent, pathPlanning, target);
        if (action != null) {
            if (positionURN == HYDRANT && action.getClass().equals(ActionMove.class)) {
                pathPlanning.setFrom(agent.getPosition());
                pathPlanning.setDestination(target);
                double currentDistance = pathPlanning.calc().getDistance();
                List<EntityID> path = ((ActionMove) action).getPath();
                pathPlanning.setFrom(path.get(path.size() - 1));
                pathPlanning.setDestination(target);
                double newHydrantDistance = pathPlanning.calc().getDistance();
                if (currentDistance <= newHydrantDistance) {
                    return new ActionRefill();
                }
            }
            return action;
        }
        return null;
    }

    private Action calcRefugeAction(Human human, PathPlanning pathPlanning, EntityID target, boolean isRefill) {
        return this.calcSupplyAction(
                human,
                pathPlanning,
                this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE),
                target,
                isRefill
        );
    }

    private Action calcHydrantAction(Human human, PathPlanning pathPlanning, EntityID target) {
        Collection<EntityID> hydrants = this.worldInfo.getEntityIDsOfType(HYDRANT);
        hydrants.remove(human.getPosition());
        return this.calcSupplyAction(
                human,
                pathPlanning,
                hydrants,
                target,
                true
        );
    }

    private Action calcSupplyAction(Human human, PathPlanning pathPlanning, Collection<EntityID> supplyPositions, EntityID target, boolean isRefill) {
        EntityID position = human.getPosition();
        int size = supplyPositions.size();
        if (supplyPositions.contains(position)) {
            return isRefill ? new ActionRefill() : new ActionRest();
        }
        List<EntityID> firstResult = null;
        while (supplyPositions.size() > 0) {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(supplyPositions);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                if (firstResult == null) {
                    firstResult = new ArrayList<>(path);
                    if (target == null) {
                        break;
                    }
                }
                EntityID supplyPositionID = path.get(path.size() - 1);
                pathPlanning.setFrom(supplyPositionID);
                pathPlanning.setDestination(target);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0) {
                    ActionMove moveAction = (ActionMove) actionExtMove.setTarget(path.get(path.size() - 1)).calc().getAction();
                    if (moveAction != null) {
                        return moveAction;
                    }
                }
                supplyPositions.remove(supplyPositionID);
                //remove failed
                if (size == supplyPositions.size()) {
                    break;
                }
                size = supplyPositions.size();
            } else {
                break;
            }
        }

        if (firstResult != null && !firstResult.isEmpty()) {
            ActionMove moveAction = (ActionMove) actionExtMove.setTarget(firstResult.get(firstResult.size() - 1)).calc().getAction();
            if (moveAction != null) {
                return moveAction;
            }
        } else {
            return null;
        }
//        return firstResult != null ? new ActionMove(firstResult) : null;
        return null;
    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }

}


package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.impl.aim.AimDuplicateLook;
import ac.grim.grimac.checks.impl.aim.AimModulo360;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.impl.badpackets.*;
import ac.grim.grimac.checks.impl.breaking.*;
import ac.grim.grimac.checks.impl.chat.ChatA;
import ac.grim.grimac.checks.impl.chat.ChatB;
import ac.grim.grimac.checks.impl.chat.ChatC;
import ac.grim.grimac.checks.impl.chat.ChatD;
import ac.grim.grimac.checks.impl.combat.*;
import ac.grim.grimac.checks.impl.crash.*;
import ac.grim.grimac.checks.impl.elytra.*;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
import ac.grim.grimac.checks.impl.exploit.ExploitB;
import ac.grim.grimac.checks.impl.groundspoof.NoFall;
import ac.grim.grimac.checks.impl.misc.ClientBrand;
import ac.grim.grimac.checks.impl.misc.GhostBlockMitigation;
import ac.grim.grimac.checks.impl.misc.Post;
import ac.grim.grimac.checks.impl.misc.TransactionOrder;
import ac.grim.grimac.checks.impl.movement.NoSlow;
import ac.grim.grimac.checks.impl.movement.PredictionRunner;
import ac.grim.grimac.checks.impl.movement.SetbackBlocker;
import ac.grim.grimac.checks.impl.movement.VehiclePredictionRunner;
import ac.grim.grimac.checks.impl.multiactions.*;
import ac.grim.grimac.checks.impl.packetorder.*;
import ac.grim.grimac.checks.impl.prediction.DebugHandler;
import ac.grim.grimac.checks.impl.prediction.GroundSpoof;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.checks.impl.prediction.Phase;
import ac.grim.grimac.checks.impl.scaffolding.*;
import ac.grim.grimac.checks.impl.sprint.*;
import ac.grim.grimac.checks.impl.timer.*;
import ac.grim.grimac.checks.impl.vehicle.*;
import ac.grim.grimac.checks.impl.velocity.ExplosionHandler;
import ac.grim.grimac.checks.impl.velocity.KnockbackHandler;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.events.packets.PacketChangeGameState;
import ac.grim.grimac.events.packets.PacketEntityReplication;
import ac.grim.grimac.events.packets.PacketPlayerAbilities;
import ac.grim.grimac.events.packets.PacketWorldBorder;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.GhostBlockDetector;
import ac.grim.grimac.predictionengine.SneakingEstimator;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.latency.CompensatedCameraEntity;
import ac.grim.grimac.utils.latency.CompensatedCooldown;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import ac.grim.grimac.utils.latency.CompensatedInventory;
import ac.grim.grimac.utils.team.TeamHandler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.Getter;

import ac.grim.grimac.checks.CheckCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckManager {
    private static final AtomicBoolean initedAtomic = new AtomicBoolean(false);
    private static boolean inited;

    private static final Map<Class<? extends AbstractCheck>, CheckCategory> CATEGORY_MAP;

    static {
        Map<Class<? extends AbstractCheck>, CheckCategory> map = new HashMap<>();

        // MOVEMENT
        map.put(OffsetHandler.class, CheckCategory.MOVEMENT);
        map.put(NoSlow.class, CheckCategory.MOVEMENT);
        map.put(Phase.class, CheckCategory.MOVEMENT);
        map.put(GroundSpoof.class, CheckCategory.MOVEMENT);
        map.put(NoFall.class, CheckCategory.MOVEMENT);

        // ELYTRA
        map.put(ElytraA.class, CheckCategory.ELYTRA);
        map.put(ElytraB.class, CheckCategory.ELYTRA);
        map.put(ElytraC.class, CheckCategory.ELYTRA);
        map.put(ElytraD.class, CheckCategory.ELYTRA);
        map.put(ElytraE.class, CheckCategory.ELYTRA);
        map.put(ElytraF.class, CheckCategory.ELYTRA);
        map.put(ElytraG.class, CheckCategory.ELYTRA);
        map.put(ElytraH.class, CheckCategory.ELYTRA);
        map.put(ElytraI.class, CheckCategory.ELYTRA);

        // COMBAT
        map.put(Reach.class, CheckCategory.COMBAT);
        map.put(Hitboxes.class, CheckCategory.COMBAT);
        map.put(MultiInteractA.class, CheckCategory.COMBAT);
        map.put(MultiInteractB.class, CheckCategory.COMBAT);
        map.put(AimModulo360.class, CheckCategory.COMBAT);
        map.put(AimDuplicateLook.class, CheckCategory.COMBAT);
        map.put(KnockbackHandler.class, CheckCategory.COMBAT);
        map.put(ExplosionHandler.class, CheckCategory.COMBAT);

        // VEHICLE
        map.put(VehicleA.class, CheckCategory.VEHICLE);
        map.put(VehicleB.class, CheckCategory.VEHICLE);
        map.put(VehicleC.class, CheckCategory.VEHICLE);
        map.put(VehicleD.class, CheckCategory.VEHICLE);
        map.put(VehicleE.class, CheckCategory.VEHICLE);
        map.put(VehicleF.class, CheckCategory.VEHICLE);
        map.put(VehicleTimer.class, CheckCategory.VEHICLE);

        // TIMER
        map.put(Timer.class, CheckCategory.TIMER);
        map.put(TickTimer.class, CheckCategory.TIMER);
        map.put(TimerLimit.class, CheckCategory.TIMER);
        map.put(NegativeTimer.class, CheckCategory.TIMER);

        // BADPACKETS
        map.put(BadPacketsA.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsB.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsC.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsD.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsE.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsF.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsG.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsH.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsI.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsJ.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsK.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsL.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsM.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsN.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsO.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsP.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsQ.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsR.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsS.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsT.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsU.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsV.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsW.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsX.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsY.class, CheckCategory.BADPACKETS);
        map.put(BadPacketsZ.class, CheckCategory.BADPACKETS);

        // BREAKING
        map.put(AirLiquidBreak.class, CheckCategory.BREAKING);
        map.put(WrongBreak.class, CheckCategory.BREAKING);
        map.put(RotationBreak.class, CheckCategory.BREAKING);
        map.put(FastBreak.class, CheckCategory.BREAKING);
        map.put(MultiBreak.class, CheckCategory.BREAKING);
        map.put(NoSwingBreak.class, CheckCategory.BREAKING);
        map.put(FarBreak.class, CheckCategory.BREAKING);
        map.put(InvalidBreak.class, CheckCategory.BREAKING);
        map.put(PositionBreakA.class, CheckCategory.BREAKING);
        map.put(PositionBreakB.class, CheckCategory.BREAKING);

        // PLACING
        map.put(InvalidPlaceA.class, CheckCategory.PLACING);
        map.put(InvalidPlaceB.class, CheckCategory.PLACING);
        map.put(AirLiquidPlace.class, CheckCategory.PLACING);
        map.put(MultiPlace.class, CheckCategory.PLACING);
        map.put(FarPlace.class, CheckCategory.PLACING);
        map.put(FabricatedPlace.class, CheckCategory.PLACING);
        map.put(PositionPlace.class, CheckCategory.PLACING);
        map.put(RotationPlace.class, CheckCategory.PLACING);
        map.put(DuplicateRotPlace.class, CheckCategory.PLACING);

        // PACKET_ORDER
        map.put(PacketOrderA.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderB.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderC.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderD.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderE.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderF.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderG.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderH.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderI.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderJ.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderK.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderL.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderM.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderN.class, CheckCategory.PACKET_ORDER);
        map.put(PacketOrderO.class, CheckCategory.PACKET_ORDER);

        // MULTI_ACTIONS
        map.put(MultiActionsA.class, CheckCategory.MULTI_ACTIONS);
        map.put(MultiActionsB.class, CheckCategory.MULTI_ACTIONS);
        map.put(MultiActionsC.class, CheckCategory.MULTI_ACTIONS);
        map.put(MultiActionsD.class, CheckCategory.MULTI_ACTIONS);
        map.put(MultiActionsE.class, CheckCategory.MULTI_ACTIONS);
        map.put(MultiActionsF.class, CheckCategory.MULTI_ACTIONS);
        map.put(MultiActionsG.class, CheckCategory.MULTI_ACTIONS);

        // CRASH (includes exploit checks)
        map.put(CrashA.class, CheckCategory.CRASH);
        map.put(CrashB.class, CheckCategory.CRASH);
        map.put(CrashC.class, CheckCategory.CRASH);
        map.put(CrashD.class, CheckCategory.CRASH);
        map.put(CrashE.class, CheckCategory.CRASH);
        map.put(CrashF.class, CheckCategory.CRASH);
        map.put(CrashG.class, CheckCategory.CRASH);
        map.put(CrashH.class, CheckCategory.CRASH);
        map.put(CrashI.class, CheckCategory.CRASH);
        map.put(ExploitA.class, CheckCategory.CRASH);
        map.put(ExploitB.class, CheckCategory.CRASH);

        // SPRINT
        map.put(SprintA.class, CheckCategory.SPRINT);
        map.put(SprintB.class, CheckCategory.SPRINT);
        map.put(SprintC.class, CheckCategory.SPRINT);
        map.put(SprintD.class, CheckCategory.SPRINT);
        map.put(SprintE.class, CheckCategory.SPRINT);
        map.put(SprintF.class, CheckCategory.SPRINT);
        map.put(SprintG.class, CheckCategory.SPRINT);

        // CHAT
        map.put(ChatA.class, CheckCategory.CHAT);
        map.put(ChatB.class, CheckCategory.CHAT);
        map.put(ChatC.class, CheckCategory.CHAT);
        map.put(ChatD.class, CheckCategory.CHAT);

        CATEGORY_MAP = Collections.unmodifiableMap(map);
    }

    public static CheckCategory getCategory(Class<? extends AbstractCheck> checkClass) {
        return CATEGORY_MAP.get(checkClass);
    }

    public static Map<Class<? extends AbstractCheck>, CheckCategory> getCategoryMap() {
        return CATEGORY_MAP;
    }

    /**
     * Get all checks in a given category from this player's check manager.
     */
    public List<AbstractCheck> getChecksByCategory(CheckCategory category) {
        List<AbstractCheck> result = new ArrayList<>();
        for (Map.Entry<Class<? extends AbstractCheck>, CheckCategory> entry : CATEGORY_MAP.entrySet()) {
            if (entry.getValue() == category) {
                AbstractCheck check = allChecks.get(entry.getKey());
                if (check != null) result.add(check);
            }
        }
        return result;
    }

    public final ClassToInstanceMap<AbstractCheck> allChecks;
    private final ClassToInstanceMap<PacketCheck> packetChecks;
    private final ClassToInstanceMap<PositionCheck> positionChecks;
    private final ClassToInstanceMap<RotationCheck> rotationChecks;
    private final ClassToInstanceMap<VehicleCheck> vehicleChecks;
    private final ClassToInstanceMap<PacketCheck> prePredictionChecks;
    private final ClassToInstanceMap<BlockBreakCheck> blockBreakChecks;
    private final ClassToInstanceMap<BlockPlaceCheck> blockPlaceChecks;
    private final ClassToInstanceMap<PostPredictionCheck> postPredictionChecks;
    @Getter
    private final PacketEntityReplication packetEntityReplication;

    private final List<PacketCheck> packetChecksValues;
    private final List<PositionCheck> positionChecksValues;
    private final List<RotationCheck> rotationChecksValues;
    private final List<VehicleCheck> vehicleChecksValues;
    private final List<PacketCheck> prePredictionChecksValues;
    private final List<BlockBreakCheck> blockBreakChecksValues;
    private final List<BlockPlaceCheck> blockPlaceChecksValues;
    private final List<PostPredictionCheck> postPredictionChecksValues;

    public CheckManager(GrimPlayer player) {
        packetEntityReplication = new PacketEntityReplication(player);

        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(CompensatedCameraEntity.class, player.cameraEntity)
                .put(PacketOrderProcessor.class, player.packetOrderProcessor)
                .put(Reach.class, new Reach(player))
                .put(PacketEntityReplication.class, packetEntityReplication)
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(CompensatedInventory.class, player.inventory)
                .put(PacketPlayerAbilities.class, new PacketPlayerAbilities(player))
                .put(PacketWorldBorder.class, new PacketWorldBorder(player))
                .put(ActionManager.class, player.actionManager)
                .put(TeamHandler.class, new TeamHandler(player))
                .put(ClientBrand.class, new ClientBrand(player))
                .put(NoFall.class, new NoFall(player))
                .put(ChatA.class, new ChatA(player))
                .put(ChatB.class, new ChatB(player))
                .put(ChatC.class, new ChatC(player))
                .put(ChatD.class, new ChatD(player))
                .put(ExploitA.class, new ExploitA(player))
                .put(ExploitB.class, new ExploitB(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsI.class, new BadPacketsI(player))
                .put(BadPacketsJ.class, new BadPacketsJ(player))
                .put(BadPacketsK.class, new BadPacketsK(player))
                .put(BadPacketsL.class, new BadPacketsL(player))
                .put(BadPacketsM.class, new BadPacketsM(player))
                .put(BadPacketsO.class, new BadPacketsO(player))
                .put(BadPacketsP.class, new BadPacketsP(player))
                .put(BadPacketsQ.class, new BadPacketsQ(player))
                .put(BadPacketsR.class, new BadPacketsR(player))
                .put(BadPacketsS.class, new BadPacketsS(player))
                .put(BadPacketsT.class, new BadPacketsT(player))
                .put(BadPacketsU.class, new BadPacketsU(player))
                .put(BadPacketsV.class, new BadPacketsV(player))
                .put(BadPacketsY.class, new BadPacketsY(player))
                .put(BadPacketsZ.class, new BadPacketsZ(player))
                .put(SelfInteract.class, new SelfInteract(player))
                .put(MultiActionsA.class, new MultiActionsA(player))
                .put(MultiActionsC.class, new MultiActionsC(player))
                .put(MultiActionsD.class, new MultiActionsD(player))
                .put(MultiActionsE.class, new MultiActionsE(player))
                .put(PacketOrderB.class, new PacketOrderB(player))
                .put(PacketOrderC.class, new PacketOrderC(player))
                .put(PacketOrderD.class, new PacketOrderD(player))
                .put(PacketOrderO.class, new PacketOrderO(player))
//                .put(PacketOrderP.class, new PacketOrderP(player))
                .put(SprintA.class, new SprintA(player))
                .put(VehicleA.class, new VehicleA(player))
                .put(VehicleB.class, new VehicleB(player))
                .put(VehicleD.class, new VehicleD(player))
                .put(VehicleE.class, new VehicleE(player))
                .put(VehicleF.class, new VehicleF(player))
                .put(CrashB.class, new CrashB(player))
                .put(CrashD.class, new CrashD(player))
                .put(CrashE.class, new CrashE(player))
                .put(CrashF.class, new CrashF(player))
                .put(CrashH.class, new CrashH(player))
                .put(CrashI.class, new CrashI(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();

        positionChecks = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .build();
        rotationChecks = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimProcessor.class, new AimProcessor(player))
                .put(AimModulo360.class, new AimModulo360(player))
                .put(AimDuplicateLook.class, new AimDuplicateLook(player))
                .build();
        vehicleChecks = new ImmutableClassToInstanceMap.Builder<VehicleCheck>()
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .build();

        postPredictionChecks = new ImmutableClassToInstanceMap.Builder<PostPredictionCheck>()
                .put(NegativeTimer.class, new NegativeTimer(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(GhostBlockDetector.class, new GhostBlockDetector(player))
                .put(Phase.class, new Phase(player))
                .put(Post.class, new Post(player))
                .put(PacketOrderA.class, new PacketOrderA(player))
                .put(PacketOrderE.class, new PacketOrderE(player))
                .put(PacketOrderF.class, new PacketOrderF(player))
                .put(PacketOrderG.class, new PacketOrderG(player))
                .put(PacketOrderH.class, new PacketOrderH(player))
                .put(PacketOrderI.class, new PacketOrderI(player))
                .put(PacketOrderJ.class, new PacketOrderJ(player))
                .put(PacketOrderK.class, new PacketOrderK(player))
                .put(PacketOrderL.class, new PacketOrderL(player))
                .put(PacketOrderM.class, new PacketOrderM(player))
                .put(GroundSpoof.class, new GroundSpoof(player))
                .put(OffsetHandler.class, new OffsetHandler(player))
                .put(SuperDebug.class, new SuperDebug(player))
                .put(DebugHandler.class, new DebugHandler(player))
                .put(BadPacketsX.class, new BadPacketsX(player))
                .put(NoSlow.class, new NoSlow(player))
                .put(SprintB.class, new SprintB(player))
                .put(SprintC.class, new SprintC(player))
                .put(SprintD.class, new SprintD(player))
                .put(SprintE.class, new SprintE(player))
                .put(SprintF.class, new SprintF(player))
                .put(SprintG.class, new SprintG(player))
                .put(MultiInteractA.class, new MultiInteractA(player))
                .put(MultiInteractB.class, new MultiInteractB(player))
                .put(ElytraA.class, new ElytraA(player))
                .put(ElytraB.class, new ElytraB(player))
                .put(ElytraC.class, new ElytraC(player))
                .put(ElytraD.class, new ElytraD(player))
                .put(ElytraE.class, new ElytraE(player))
                .put(ElytraF.class, new ElytraF(player))
                .put(ElytraG.class, new ElytraG(player))
                .put(ElytraH.class, new ElytraH(player))
                .put(ElytraI.class, new ElytraI(player))
                .put(SetbackTeleportUtil.class, new SetbackTeleportUtil(player)) // Avoid teleporting to new position, update safe pos last
                .put(CompensatedFireworks.class, player.fireworks)
                .put(SneakingEstimator.class, new SneakingEstimator(player))
                .put(LastInstanceManager.class, player.lastInstanceManager)
                .build();

        blockPlaceChecks = new ImmutableClassToInstanceMap.Builder<BlockPlaceCheck>()
                .put(InvalidPlaceA.class, new InvalidPlaceA(player))
                .put(InvalidPlaceB.class, new InvalidPlaceB(player))
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(MultiPlace.class, new MultiPlace(player))
                .put(MultiActionsF.class, new MultiActionsF(player))
                .put(MultiActionsG.class, new MultiActionsG(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(CrashG.class, new CrashG(player))
                .put(FarPlace.class, new FarPlace(player))
                .put(FabricatedPlace.class, new FabricatedPlace(player))
                .put(PositionPlace.class, new PositionPlace(player))
                .put(RotationPlace.class, new RotationPlace(player))
                .put(PacketOrderN.class, new PacketOrderN(player))
                .put(DuplicateRotPlace.class, new DuplicateRotPlace(player))
                .put(GhostBlockMitigation.class, new GhostBlockMitigation(player))
                .build();

        prePredictionChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Timer.class, new Timer(player))
                .put(TickTimer.class, new TickTimer(player))
                .put(TimerLimit.class, new TimerLimit(player))
                .put(CrashA.class, new CrashA(player))
                .put(CrashC.class, new CrashC(player))
                .put(VehicleTimer.class, new VehicleTimer(player))
                .build();

        blockBreakChecks = new ImmutableClassToInstanceMap.Builder<BlockBreakCheck>()
                .put(AirLiquidBreak.class, new AirLiquidBreak(player))
                .put(WrongBreak.class, new WrongBreak(player))
                .put(RotationBreak.class, new RotationBreak(player))
                .put(FastBreak.class, new FastBreak(player))
                .put(MultiBreak.class, new MultiBreak(player))
                .put(NoSwingBreak.class, new NoSwingBreak(player))
                .put(FarBreak.class, new FarBreak(player))
                .put(InvalidBreak.class, new InvalidBreak(player))
                .put(PositionBreakA.class, new PositionBreakA(player))
                .put(PositionBreakB.class, new PositionBreakB(player))
                .put(MultiActionsB.class, new MultiActionsB(player))
                .build();

        // All checks that have no listeners, generally invoked by other code to flag
        // TODO migrate more checks to here
        ClassToInstanceMap<AbstractCheck> noneModules = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                // BadPacketsN/W + VehicleC + TransactionOrder are packet checks with no listener
                .put(BadPacketsN.class, new BadPacketsN(player))
                .put(BadPacketsW.class, new BadPacketsW(player))
                .put(TransactionOrder.class, new TransactionOrder(player))
                .put(VehicleC.class, new VehicleC(player))
                .put(Hitboxes.class, new Hitboxes(player)) // Hitboxes is invoked by Reach
                .build();

        allChecks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .putAll(packetChecks)
                .putAll(positionChecks)
                .putAll(rotationChecks)
                .putAll(vehicleChecks)
                .putAll(postPredictionChecks)
                .putAll(blockPlaceChecks)
                .putAll(prePredictionChecks)
                .putAll(blockBreakChecks)
                .putAll(noneModules)
                .build();

        packetChecksValues = new ArrayList<>(packetChecks.values());
        positionChecksValues = new ArrayList<>(positionChecks.values());
        rotationChecksValues = new ArrayList<>(rotationChecks.values());
        vehicleChecksValues = new ArrayList<>(vehicleChecks.values());
        prePredictionChecksValues = new ArrayList<>(prePredictionChecks.values());
        blockBreakChecksValues = new ArrayList<>(blockBreakChecks.values());
        blockPlaceChecksValues = new ArrayList<>(blockPlaceChecks.values());
        postPredictionChecksValues = new ArrayList<>(postPredictionChecks.values());

        init();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractCheck> T getCheck(Class<T> check) {
        return (T) allChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PositionCheck> T getPositionCheck(Class<T> check) {
        return (T) positionChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends RotationCheck> T getRotationCheck(Class<T> check) {
        return (T) rotationChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockPlaceCheck> T getBlockPlaceCheck(Class<T> check) {
        return (T) blockPlaceChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPrePredictionCheck(Class<T> check) {
        return (T) prePredictionChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PostPredictionCheck> T getPostPredictionCheck(Class<T> check) {
        return (T) postPredictionChecks.get(check);
    }

    public void onPrePredictionReceivePacket(final PacketReceiveEvent packet) {
        for (PacketCheck check : prePredictionChecksValues) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetChecksValues) {
            check.onPacketReceive(packet);
        }
        for (PostPredictionCheck check : postPredictionChecksValues) {
            check.onPacketReceive(packet);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPacketReceive(packet);
        }
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : prePredictionChecksValues) {
            check.onPacketSend(packet);
        }
        for (PacketCheck check : packetChecksValues) {
            check.onPacketSend(packet);
        }
        for (PostPredictionCheck check : postPredictionChecksValues) {
            check.onPacketSend(packet);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPacketSend(packet);
        }
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPacketSend(packet);
        }
    }

    public void onPositionUpdate(final PositionUpdate position) {
        for (PositionCheck check : positionChecksValues) {
            check.onPositionUpdate(position);
        }
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        for (RotationCheck check : rotationChecksValues) {
            check.process(rotation);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.process(rotation);
        }
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        for (VehicleCheck check : vehicleChecksValues) {
            check.process(update);
        }
    }

    public void onPredictionFinish(final PredictionComplete complete) {
        for (PostPredictionCheck check : postPredictionChecksValues) {
            check.onPredictionComplete(complete);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPredictionComplete(complete);
        }
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPredictionComplete(complete);
        }
    }

    public void onBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onBlockPlace(place);
        }
    }

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPostFlyingBlockPlace(place);
        }
    }

    public void onBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onBlockBreak(blockBreak);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onBlockBreak(blockBreak);
        }
    }

    public void onPostFlyingBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
    }

    public ExplosionHandler getExplosionHandler() {
        return getPostPredictionCheck(ExplosionHandler.class);
    }

    public NoFall getNoFall() {
        return getPacketCheck(NoFall.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return getPostPredictionCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return getPositionCheck(CompensatedCooldown.class);
    }

    public NoSlow getNoSlow() {
        return getPostPredictionCheck(NoSlow.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return getPostPredictionCheck(SetbackTeleportUtil.class);
    }

    public DebugHandler getDebugHandler() {
        return getPostPredictionCheck(DebugHandler.class);
    }

    private void init() {
        if (inited || initedAtomic.getAndSet(true)) return;
        inited = true;

        final String[] permissions = {
                "grim.exempt.",
                "grim.nosetback.",
                "grim.nomodifypacket.",
        };

        for (final AbstractCheck check : allChecks.values()) {
            if (check.getConfigName() == null) continue;
            final String id = check.getConfigName().toLowerCase();
            for (String permissionName : permissions) {
                permissionName += id;
                GrimAPI.INSTANCE.getPermissionManager().registerPermission(permissionName, PermissionDefaultValue.FALSE);
            }
        }
    }
}

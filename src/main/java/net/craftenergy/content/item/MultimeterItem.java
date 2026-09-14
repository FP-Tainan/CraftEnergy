package net.craftenergy.content.item;

import net.craftenergy.api.MultimeterReadable;
import net.craftenergy.content.block.CableBlock;
import net.craftenergy.fabric.EnergyNetworkManager;
import net.craftenergy.grid.EnergyNetwork;
import net.craftenergy.network.MultimeterReadingPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Multímetro: segurado na mão ou no slot do escudo, mede o cabo ou o bloco que o jogador está
 * olhando (até 8 blocos) e mostra as leituras flutuando sobre ele. Cabos mostram tensão (MV),
 * corrente (RA), potência (CW) e temperatura; outros mods entram implementando
 * {@link MultimeterReadable} no block entity.
 */
public class MultimeterItem extends Item {
    private static final double RANGE = 8.0;
    private static final int UPDATE_TICKS = 5;
    /** Temperatura do cabo mostrada no multímetro: frio a 20 CCº, queima a 220 CCº. */
    public static final double CABLE_AMBIENT = 20.0;
    public static final double CABLE_BURN = 220.0;
    public static final String CABLE_TEMPERATURE = "CCº";

    public MultimeterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(MultimeterItem::tick);
    }

    public static boolean holds(net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem().getItem() instanceof MultimeterItem || player.getOffhandItem().getItem() instanceof MultimeterItem;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % UPDATE_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!holds(player)) continue;
            HitResult hit = player.pick(RANGE, 1.0F, false);
            MultimeterReadingPayload reading = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                    ? measure(player.level(), blockHit.getBlockPos()) : MultimeterReadingPayload.empty();
            ServerPlayNetworking.send(player, reading);
        }
    }

    /** Lê o bloco: cabo (rede do Craft Energy) ou block entity que implementa {@link MultimeterReadable}. */
    public static MultimeterReadingPayload measure(ServerLevel level, BlockPos pos) {
        List<Double> values = new ArrayList<>();
        List<String> units = new ArrayList<>();
        if (level.getBlockState(pos).getBlock() instanceof CableBlock) {
            double voltage = 0;
            double current = 0;
            for (EnergyNetwork<Long> network : EnergyNetworkManager.get(level).networksAt(pos)) {
                voltage = Math.max(voltage, network.voltage());
                current += network.conductorCurrent(pos.asLong());
            }
            MultimeterReadable.electric(values, units, voltage, current * voltage);
            // aquecimento do cabo: 0 (frio, 20 CCº) até 1 (queima, 220 CCº)
            values.add(CABLE_AMBIENT + EnergyNetworkManager.get(level).heatAt(pos) * (CABLE_BURN - CABLE_AMBIENT));
            units.add(CABLE_TEMPERATURE);
        } else if (level.getBlockEntity(pos) instanceof MultimeterReadable readable) {
            readable.multimeterReading(values, units);
        }
        return values.isEmpty() ? MultimeterReadingPayload.empty() : new MultimeterReadingPayload(pos.immutable(), values, units);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatableWithFallback("tooltip.craftenergy.multimeter",
                "Hold it (or put it in the shield slot) and look at cables and machines").withStyle(ChatFormatting.GRAY));
    }
}

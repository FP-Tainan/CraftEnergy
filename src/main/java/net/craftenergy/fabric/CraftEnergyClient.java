package net.craftenergy.fabric;

import net.craftenergy.client.MultimeterClient;
import net.fabricmc.api.ClientModInitializer;

public final class CraftEnergyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MultimeterClient.init();
    }
}

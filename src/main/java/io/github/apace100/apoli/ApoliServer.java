package io.github.apace100.apoli;

import io.github.apace100.apoli.networking.ModPacketsS2C;
import io.github.apace100.apoli.power.factory.condition.EntityConditionsClient;
import io.github.apace100.apoli.power.factory.condition.EntityConditionsServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class ApoliServer implements DedicatedServerModInitializer {

	@Override
	public void onInitializeServer() {

		EntityConditionsServer.register();

	}
}

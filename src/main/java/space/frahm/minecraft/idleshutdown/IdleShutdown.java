package space.frahm.minecraft.idleshutdown;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class IdleShutdown implements ModInitializer {
	public static final String MOD_ID = "IdleShutdown";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static IdleShutdownConfig config;
	private static ScheduledExecutorService scheduler;
	private static ScheduledFuture<?> shutdownTask;
	private static boolean initialized = false;


	@Override
	public void onInitialize() {
		config = IdleShutdownConfig.createAndLoad(LOGGER);

		LOGGER.info(
			"IdleShutdown initialized with {} minute idle shutdown",
			config.getMinutesUntilShutdown()
		);

		// We just need to initialize our timer on server start, then we basically
		// deactivate this listener.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (initialized) return;
			if (server.getCurrentPlayerCount() <= 0) {
				LOGGER.info("Starting shutdown timer for {} minutes", config.getMinutesUntilShutdown());
				startShutdownTimer(server);
			}
			initialized = true;
		});


		// Register join event listener
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			LOGGER.info("Player {} joined the server. Deactivating shutdown.", player.getName().getString());
			cancelShutdown();
		});

		// Register leave event listener
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			// Minus one to discount the currently disconnecting player
			int playerCount = server.getCurrentPlayerCount() - 1;
			ServerPlayerEntity player = handler.getPlayer();
			LOGGER.info(
				"Player {} left the server, {} still connected.",
				player.getName().getString(),
				playerCount
			);
			if (playerCount <= 0) {
				LOGGER.info("Starting shutdown timer for {} minutes", config.getMinutesUntilShutdown());
				startShutdownTimer(server);
			}
		});
	}

	public static void startShutdownTimer(MinecraftServer server) {
		scheduler = Executors.newScheduledThreadPool(1);

		// Schedule the actual shutdown
		shutdownTask = scheduler.schedule(() -> {
			LOGGER.info("Server idle timeout reached. Shutting down.");
			server.execute(() -> server.stop(false));
			scheduler.shutdown();
		}, config.getMinutesUntilShutdown(), TimeUnit.MINUTES);
	}

	public static void cancelShutdown() {
		if (shutdownTask != null) {
			shutdownTask.cancel(false);
			scheduler.shutdown();
			LOGGER.info("Shutdown timer cancelled");
		}
	}
}

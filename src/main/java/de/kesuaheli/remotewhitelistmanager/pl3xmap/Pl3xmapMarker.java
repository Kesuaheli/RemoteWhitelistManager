package de.kesuaheli.remotewhitelistmanager.pl3xmap;

import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.markers.layer.CustomLayer;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Pl3xmapMarker {
	public static String MARKER_LAYER_BASES = "bases";
	public static Supplier<String> MARKER_LAYER_BASES_LABEL = () -> "Bases";

	public static SimpleLayer getBases() {
		World world = getOverworld();
		SimpleLayer layer = (SimpleLayer) world.getLayerRegistry().get(MARKER_LAYER_BASES);
		if (layer == null) {
			layer = new CustomLayer(MARKER_LAYER_BASES, world, MARKER_LAYER_BASES_LABEL);
			world.getLayerRegistry().register(layer);
		}
		return layer;
	}

	public static @NotNull World getOverworld() {
		return getWorld("overworld");
	}

	public static @NotNull World getNether() {
		return getWorld("the_nether");
	}

	public static @NotNull World getEnd() {
		return getWorld("the_end");
	}

	private static @NotNull World getWorld(final String name) {
		World world = Pl3xMap.api().getWorldRegistry().get("minecraft:"+name);
		if (world == null) {
			throw new RuntimeException("failed to get vanilla "+name+" from pl3xmap world registry.");
		}
		return world;
	}
}

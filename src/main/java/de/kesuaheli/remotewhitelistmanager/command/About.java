package de.kesuaheli.remotewhitelistmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

import static de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager.LOGGER;

public class About extends LiteralArgumentBuilder<ServerCommandSource> {
	public About() {
		super("about");
		for (AboutType type : AboutType.values()) {
			then(CommandManager.literal(type.asString())
					.then(CommandManager.argument("name", StringArgumentType.greedyString())
							.executes(ctx -> this.execute(ctx, type))
					)
			);
		}
	}

	private int execute(CommandContext<ServerCommandSource> ctx, AboutType typeArgument) {
		String nameArgument = ctx.getArgument("name", String.class);
		LOGGER.info("About executed type={}, name={}", typeArgument, nameArgument);
		return 0;
	}

	enum AboutType implements StringIdentifiable {
		MINECRAFT,
		DISCORD,
		NAME,
		;

		@Override
		public String asString() {
			return this.toString().toLowerCase();
		}
	}
}

package de.kesuaheli.remotewhitelistmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager;
import de.kesuaheli.remotewhitelistmanager.database.WhitelistEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager.LOGGER;

public class About extends LiteralArgumentBuilder<CommandSourceStack> {

	public About() {
		super("about");
		for (AboutType type : AboutType.values()) {
			then(Commands.literal(type.getSerializedName())
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.executes(ctx -> this.execute(ctx, type))
					)
			);
		}
	}

	private int execute(CommandContext<CommandSourceStack> ctx, AboutType typeArgument) {
		String nameArgument = ctx.getArgument("name", String.class);
		LOGGER.info("About executed type={}, name={}", typeArgument, nameArgument);
		switch (typeArgument) {
			case NAME -> {
				List<WhitelistEntry> entries = WhitelistEntry.getByNameMatch(nameArgument);
				for (WhitelistEntry entry : entries) {
					LOGGER.info(entry.toString());
				}
			}
			default -> {
				ctx.getSource().sendSystemMessage(Component.literal("this type is not supported for now."));
			}
		}
		return 0;
	}

	enum AboutType implements StringRepresentable {
		MINECRAFT,
		DISCORD,
		NAME,
		;

		@Override
		public @NonNull String getSerializedName() {
			return this.toString().toLowerCase();
		}
	}
}

package de.kesuaheli.remotewhitelistmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.kesuaheli.remotewhitelistmanager.database.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.kesuaheli.remotewhitelistmanager.RemoteWhitelistmanager.LOGGER;

public class About extends LiteralArgumentBuilder<CommandSourceStack> {

	private static final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();

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
		ServerPlayer player = ctx.getSource().getPlayer();
		List<DialogBody> body = new ArrayList<>();

		String nameArgument = ctx.getArgument("name", String.class);

		asyncExecutor.submit(() -> {
			switch (typeArgument) {
				case NAME -> {
					List<User> entries = User.getByNameMatch(nameArgument);
					for (User user : entries) {
						if (player == null) {
							LOGGER.info(user.toString());
						}
						body.add(new PlainMessage(user.format(), PlainMessage.DEFAULT_WIDTH*2));
					}
				}
				default -> {
					ctx.getSource().sendSystemMessage(Component.literal("this type is not supported for now."));
					return;
				}
			}
			if (player == null) return;

			if (body.isEmpty()) {
				body.add(new PlainMessage(Component.literal("No user found with '").withColor(TextColor.RED)
						.append(Component.literal(nameArgument).withStyle(style -> style.withItalic(true).withColor(TextColor.AQUA)))
						.append("' anywhere in the name :c\n\n")
						.append(Component.literal("Maybe try less letters? (Uppercase/Lowercase is ignored)").withColor(TextColor.GRAY))
						, PlainMessage.DEFAULT_WIDTH));
			}
			Dialog dialog = new NoticeDialog(
					new CommonDialogData(
							Component.literal("Player Info"),
							Optional.empty(),
							true,
							false,
							DialogAction.CLOSE,
							body,
							new ArrayList<>()
					),
					NoticeDialog.DEFAULT_ACTION
			);
			player.openDialog(Holder.direct(dialog));

		});
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

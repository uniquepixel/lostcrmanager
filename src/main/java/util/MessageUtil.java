package util;

import java.awt.Color;

import lostcrmanager.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class MessageUtil {

	public enum EmbedType {
		INFO, SUCCESS, ERROR
	}

	public static String footer = "CR Manager | Made by Pixel | v" + Bot.VERSION;

	public static MessageEmbed buildEmbed(String title, String description, EmbedType type, Field... fields) {
		EmbedBuilder embedreply = new EmbedBuilder();
		embedreply.setTitle(title);
		embedreply.setDescription(description);
		for (int i = 0; i < fields.length; i++) {
			embedreply.addField(fields[i]);
		}
		embedreply.setFooter(footer);
		switch (type) {
		case INFO:
			embedreply.setColor(Color.CYAN);
			break;
		case SUCCESS:
			embedreply.setColor(Color.GREEN);
			break;
		case ERROR:
			embedreply.setColor(Color.RED);
			break;
		}
		return embedreply.build();
	}

	public static String unformat(String s) {
		return s.replaceAll("_", "\\_").replaceAll("\\*", "\\\\*").replaceAll("~", "\\~").replaceAll("`", "\\`")
				.replaceAll("\\|", "\\\\|").replaceAll(">", "\\>").replaceAll("-", "\\-").replaceAll("#", "\\#");
	}
	
	public static void sendUserPingHidden(MessageChannelUnion channel, String uuid) {
		channel.sendMessage(".").queue(sentMessage -> {
			new Thread(() -> {
				try {
					Thread.sleep(100);
					sentMessage.editMessage("<@" + uuid + ">").queue();
					Thread.sleep(100);
					sentMessage.delete().queue();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}).start();
		});
	}

}

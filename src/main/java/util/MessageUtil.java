package util;

import java.awt.Color;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class MessageUtil {

	public enum EmbedType {
		INFO, SUCCESS, ERROR
	}

	public static String footer = "CR Manager | Made by Pixel";

	public static MessageEmbed buildEmbed(String title, String description, EmbedType type) {
		EmbedBuilder embedreply = new EmbedBuilder();
		embedreply.setTitle(title);
		embedreply.setDescription(description);
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
		return s.replaceAll("_", "\\_").replaceAll("\\*", "\\\\*").replaceAll("~", "\\~")
				.replaceAll("`", "\\`").replaceAll("\\|", "\\\\|").replaceAll(">", "\\>")
				.replaceAll("-", "\\-").replaceAll("#", "\\#");
	}

}

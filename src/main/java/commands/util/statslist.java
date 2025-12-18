package commands.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import datautil.DBManager;
import datawrapper.Clan;
import datawrapper.Player;
import datawrapper.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.MessageUtil;

public class statslist extends ListenerAdapter {

	// Available stat fields
	private static final String[] AVAILABLE_FIELDS = { "Wins", "Trophies", "STRTrophies", "PoLTrophies",
			"PoLLeagueNumber", "Kickpoints", "CWFame" };

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (!event.getName().equals("statslist"))
			return;
		event.deferReply().queue();
		String title = "Stats Liste";

		OptionMapping clanOption = event.getOption("clan");
		OptionMapping displayFieldsOption = event.getOption("display_fields");
		OptionMapping sortFieldsOption = event.getOption("sort_fields");
		OptionMapping rolesSortingOption = event.getOption("roles_sorting");

		if (clanOption == null) {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Der Parameter Clan ist erforderlich!", MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		String clanInput = clanOption.getAsString();
		String rolesSortingValue = rolesSortingOption != null ? rolesSortingOption.getAsString() : "false";
		boolean rolesSorting = "true".equalsIgnoreCase(rolesSortingValue) || "clans".equalsIgnoreCase(rolesSortingValue);
		boolean clanSorting = "clans".equalsIgnoreCase(rolesSortingValue);

		// Parse display fields - now required
		List<String> displayFields = new ArrayList<>();
		if (displayFieldsOption != null) {
			String fieldsInput = displayFieldsOption.getAsString();
			displayFields = parseFields(fieldsInput);
			if (displayFields.isEmpty()) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Keine gültigen Anzeigefelder angegeben!", MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
		} else {
			event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
					"Der Parameter display_fields ist erforderlich!", MessageUtil.EmbedType.ERROR)).queue();
			return;
		}

		// Parse sort fields - default to alphabetically by name
		List<String> sortFields = new ArrayList<>();
		if (sortFieldsOption != null) {
			String sortInput = sortFieldsOption.getAsString();
			sortFields = parseFields(sortInput);
			if (sortFields.isEmpty()) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Keine gültigen Sortierfelder angegeben!", MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
		}
		// If no sort fields specified, default is alphabetical (empty list means sort by name)

		// Get clans to process
		ArrayList<String> clanTags = new ArrayList<>();
		if (clanInput.equalsIgnoreCase("Alle Clans")) {
			// Check permissions
			User userExecuted = new User(event.getUser().getId());
			boolean hasPermission = false;
			for (String clantag : DBManager.getAllClans()) {
				Player.RoleType role = userExecuted.getClanRoles().get(clantag);
				if (role == Player.RoleType.ADMIN || role == Player.RoleType.LEADER
						|| role == Player.RoleType.COLEADER) {
					hasPermission = true;
					break;
				}
			}
			if (!hasPermission) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Du musst mindestens Vize-Anführer eines Clans sein, um diesen Befehl mit 'Alle Clans' ausführen zu können.",
						MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
			clanTags = DBManager.getAllClans();
		} else {
			Clan c = new Clan(clanInput);
			if (!c.ExistsDB()) {
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title, "Dieser Clan existiert nicht.",
						MessageUtil.EmbedType.ERROR)).queue();
				return;
			}
			clanTags.add(clanInput);
		}

		final ArrayList<String> clans = clanTags;
		final List<String> finalDisplayFields = displayFields;
		final List<String> finalSortFields = sortFields;
		final boolean finalRolesSorting = rolesSorting;
		final boolean finalClanSorting = clanSorting;

		Thread thread = new Thread(() -> {
			try {
				String content = generateStatsList(clans, finalDisplayFields, finalSortFields, finalRolesSorting,
						finalClanSorting, event, title);

				ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
				String description = "Hier die Stats-Liste";
				if (clans.size() == 1) {
					Clan c = new Clan(clans.get(0));
					description += " für " + c.getInfoStringDB();
				} else {
					description += " für alle Clans";
				}
				description += ".";
				event.getHook().editOriginal(inputStream, "StatsList.txt")
						.setEmbeds(MessageUtil.buildEmbed(title, description, MessageUtil.EmbedType.INFO)).queue();
			} catch (Exception e) {
				e.printStackTrace();
				event.getHook().editOriginalEmbeds(MessageUtil.buildEmbed(title,
						"Ein Fehler ist aufgetreten: " + e.getMessage(), MessageUtil.EmbedType.ERROR)).queue();
			}
		});
		thread.start();
	}

	private String generateStatsList(ArrayList<String> clanTags, List<String> displayFields, List<String> sortFields,
			boolean rolesSorting, boolean clanSorting, SlashCommandInteractionEvent event, String title) {

		ArrayList<Player> allPlayers = new ArrayList<>();
		String content = "";

		// Load all players
		for (int a = 0; a < clanTags.size(); a++) {
			String clantag = clanTags.get(a);
			Clan c = new Clan(clantag);
			ArrayList<Player> playerListClan = c.getPlayersDB();
			for (int i = 0; i < playerListClan.size(); i++) {
				event.getHook()
						.editOriginalEmbeds(MessageUtil.buildEmbed(title,
								"Lade Spieler " + (i + 1) + " / " + playerListClan.size()
										+ (clanTags.size() > 1 ? " aus Clan " + (a + 1) + " / " + clanTags.size() : "")
										+ " von Datenbank in den Cache...",
								MessageUtil.EmbedType.LOADING))
						.queue();
				allPlayers.add(playerListClan.get(i));
			}
		}

		// Sort players
		if (rolesSorting && clanSorting && clanTags.size() > 1) {
			// When roles_sorting with "clans" is enabled and "Alle Clans", organize by clan and role
			allPlayers = sortPlayersByRolesAndFields(allPlayers, sortFields);

			// Group by clan with role-based sections
			content = generateRoleSortedContent(allPlayers, clanTags, displayFields, event, title);
		} else if (rolesSorting) {
			// Single clan with role sorting OR "Alle Clans" with role sorting but no clan grouping
			allPlayers = sortPlayersByRolesAndFields(allPlayers, sortFields);
			if (clanTags.size() > 1) {
				// All clans with role sorting - show all players in one sorted list by role
				content = generateAllClansContent(allPlayers, displayFields, event, title);
			} else {
				content = generateSingleClanContent(allPlayers, clanTags.get(0), displayFields, true, event, title);
			}
		} else {
			// No role sorting - just sort by specified fields (or alphabetically if no fields)
			allPlayers = sortPlayersByFields(allPlayers, sortFields);
			if (clanTags.size() > 1) {
				// All clans without role sorting - show all players in one sorted list
				content = generateAllClansContent(allPlayers, displayFields, event, title);
			} else {
				content = generateSingleClanContent(allPlayers, clanTags.get(0), displayFields, false, event, title);
			}
		}

		return content;
	}

	private String generateRoleSortedContent(ArrayList<Player> allPlayers, ArrayList<String> clanTags,
			List<String> displayFields, SlashCommandInteractionEvent event, String title) {
		StringBuilder content = new StringBuilder();

		// Group players by clan
		for (String clanTag : clanTags) {
			Clan clan = new Clan(clanTag);
			content.append(clan.getInfoStringDB()).append("\n\n");

			// Filter players for this clan
			ArrayList<Player> clanPlayers = allPlayers.stream().filter(p -> {
				Clan playerClan = p.getClanDB();
				return playerClan != null && playerClan.getTag().equals(clanTag);
			}).collect(Collectors.toCollection(ArrayList::new));

			// Group by role
			ArrayList<Player> admins = new ArrayList<>();
			ArrayList<Player> leaders = new ArrayList<>();
			ArrayList<Player> coleaders = new ArrayList<>();
			ArrayList<Player> marked = new ArrayList<>();
			ArrayList<Player> regular = new ArrayList<>();

			for (Player p : clanPlayers) {
				if (p.isHiddenColeader()) {
					continue;
				}
				if (p.getRole() == Player.RoleType.ADMIN) {
					admins.add(p);
				} else if (p.getRole() == Player.RoleType.LEADER) {
					leaders.add(p);
				} else if (p.getRole() == Player.RoleType.COLEADER) {
					coleaders.add(p);
				} else if (p.isMarked()) {
					marked.add(p);
				} else {
					regular.add(p);
				}
			}

			int counter = 1;

			// Add leaders/coleaders section
			boolean leadersExist = !admins.isEmpty() || !leaders.isEmpty() || !coleaders.isEmpty();
			if (leadersExist) {
				for (Player p : admins) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				for (Player p : leaders) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				for (Player p : coleaders) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				content.append("----------------------------\n\n");
			}

			// Add marked section
			if (!marked.isEmpty()) {
				for (Player p : marked) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				content.append("----------------------------\n\n");
			}

			// Add regular members
			for (Player p : regular) {
				content.append("#").append(counter++).append(" ");
				content.append(formatPlayerLine(p, displayFields));
			}

			content.append("-----------------------------------------------------------\n\n");
		}

		return content.toString();
	}

	private String generateSingleClanContent(ArrayList<Player> players, String clanTag, List<String> displayFields,
			boolean rolesSorting, SlashCommandInteractionEvent event, String title) {
		StringBuilder content = new StringBuilder();
		Clan clan = new Clan(clanTag);
		content.append(clan.getInfoStringDB()).append("\n\n");

		// Filter out hidden coleaders
		players = players.stream().filter(p -> !p.isHiddenColeader())
				.collect(Collectors.toCollection(ArrayList::new));

		if (rolesSorting) {
			// Group by role
			ArrayList<Player> admins = new ArrayList<>();
			ArrayList<Player> leaders = new ArrayList<>();
			ArrayList<Player> coleaders = new ArrayList<>();
			ArrayList<Player> marked = new ArrayList<>();
			ArrayList<Player> regular = new ArrayList<>();

			for (Player p : players) {
				if (p.getRole() == Player.RoleType.ADMIN) {
					admins.add(p);
				} else if (p.getRole() == Player.RoleType.LEADER) {
					leaders.add(p);
				} else if (p.getRole() == Player.RoleType.COLEADER) {
					coleaders.add(p);
				} else if (p.isMarked()) {
					marked.add(p);
				} else {
					regular.add(p);
				}
			}

			int counter = 1;

			// Add leaders/coleaders section
			boolean leadersExist = !admins.isEmpty() || !leaders.isEmpty() || !coleaders.isEmpty();
			if (leadersExist) {
				for (Player p : admins) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				for (Player p : leaders) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				for (Player p : coleaders) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				content.append("----------------------------\n\n");
			}

			// Add marked section
			if (!marked.isEmpty()) {
				for (Player p : marked) {
					content.append("#").append(counter++).append(" ");
					content.append(formatPlayerLine(p, displayFields));
				}
				content.append("----------------------------\n\n");
			}

			// Add regular members
			for (Player p : regular) {
				content.append("#").append(counter++).append(" ");
				content.append(formatPlayerLine(p, displayFields));
			}
		} else {
			// No role sorting - just list in order
			int counter = 1;
			for (Player p : players) {
				content.append("#").append(counter++).append(" ");
				content.append(formatPlayerLine(p, displayFields));
			}
		}

		content.append("-----------------------------------------------------------\n\n");

		return content.toString();
	}

	private String generateAllClansContent(ArrayList<Player> players, List<String> displayFields,
			SlashCommandInteractionEvent event, String title) {
		StringBuilder content = new StringBuilder();
		content.append("Alle Clans - Sortiert nach gewählten Kriterien\n\n");

		// Filter out hidden coleaders
		players = players.stream().filter(p -> !p.isHiddenColeader())
				.collect(Collectors.toCollection(ArrayList::new));

		int counter = 1;
		for (Player p : players) {
			content.append("#").append(counter++).append(" ");
			content.append(formatPlayerLine(p, displayFields));
		}

		content.append("-----------------------------------------------------------\n\n");

		return content.toString();
	}

	private String formatPlayerLine(Player p, List<String> displayFields) {
		boolean isMarked = p.isMarked();

		// Status
		String status;
		if (p.getClanDB() == null) {
			status = "[Warteschlange]";
		} else {
			String role = "";
			if (p.getRole() == Player.RoleType.ADMIN) {
				role = "Admin";
			} else if (p.getRole() == Player.RoleType.LEADER) {
				role = "Anführer";
			} else if (p.getRole() == Player.RoleType.COLEADER) {
				role = "Vize-Anführer";
			} else if (p.getRole() == Player.RoleType.ELDER) {
				role = "Ältester";
			} else if (p.getRole() == Player.RoleType.MEMBER) {
				role = "Mitglied";
			}
			status = "[" + role + " " + p.getClanDB().getNameDB() + "]";
		}

		String noteInfo = "";
		if (isMarked) {
			status += " [MARKIERT]";
			String note = p.getNote();
			if (note != null && !note.trim().isEmpty()) {
				noteInfo = " Notiz: " + note + "\n";
			}
		}

		// Build field output
		StringBuilder fields = new StringBuilder();
		for (String field : displayFields) {
			Object value = getFieldValue(p, field);
			fields.append(" ").append(getFieldDisplayName(field)).append(": ").append(value).append("\n");
		}

		return String.format("%s (%s) %s\n%s%s\n", p.getNameDB(), p.getTag(), status, noteInfo, fields.toString());
	}

	private Object getFieldValue(Player p, String field) {
		switch (field) {
		case "Wins":
			Integer wins = p.getWinsAPI();
			return wins != null ? wins : 0;
		case "Trophies":
			Integer trophies = p.getTrophies();
			return trophies != null ? trophies : 0;
		case "STRTrophies":
			Integer strTrophies = p.getSTRTrophies();
			return strTrophies != null ? strTrophies : 0;
		case "PoLTrophies":
			Integer polTrophies = p.getPoLTrophies();
			return polTrophies != null ? polTrophies : 0;
		case "PoLLeagueNumber":
			Integer leagueNumber = p.getPoLLeagueNumber();
			return leagueNumber != null ? leagueNumber : 0;
		case "Kickpoints":
			return p.getTotalKickpoints();
		case "CWFame":
			Integer cwFame = p.getCWFame();
			return cwFame != null ? cwFame : 0;
		default:
			return "N/A";
		}
	}

	private String getFieldDisplayName(String field) {
		switch (field) {
		case "Wins":
			return "Wins";
		case "Trophies":
			return "Trophäen";
		case "STRTrophies":
			return "Seasonal-Trophy-Road-Trophäen";
		case "PoLTrophies":
			return "PathOfLegend-Trophäen";
		case "PoLLeagueNumber":
			return "LeagueNumber";
		case "Kickpoints":
			return "Kickpunkte";
		case "CWFame":
			return "CW-Fame";
		default:
			return field;
		}
	}

	private ArrayList<Player> sortPlayersByRolesAndFields(ArrayList<Player> players, List<String> sortFields) {
		Comparator<Player> comparator = Comparator
				// First by role (Admin > Leader > CoLeader > others)
				.comparingInt((Player p) -> {
					if (p.getRole() == Player.RoleType.ADMIN)
						return 0;
					if (p.getRole() == Player.RoleType.LEADER)
						return 1;
					if (p.getRole() == Player.RoleType.COLEADER)
						return 2;
					return 3;
				})
				// Then by marked status (marked first within same role)
				.thenComparing(Comparator.comparing(Player::isMarked).reversed());

		// Add sort fields or default to alphabetical
		if (sortFields.isEmpty()) {
			// Default: sort alphabetically by name
			comparator = comparator.thenComparing(Player::getNameDB, String.CASE_INSENSITIVE_ORDER);
		} else {
			for (String field : sortFields) {
				comparator = comparator.thenComparing(getFieldComparator(field).reversed());
			}
		}

		return players.stream().sorted(comparator).collect(Collectors.toCollection(ArrayList::new));
	}

	private ArrayList<Player> sortPlayersByFields(ArrayList<Player> players, List<String> sortFields) {
		Comparator<Player> comparator = null;

		if (sortFields.isEmpty()) {
			// Default: sort alphabetically by name
			comparator = Comparator.comparing(Player::getNameDB, String.CASE_INSENSITIVE_ORDER);
		} else {
			for (int i = 0; i < sortFields.size(); i++) {
				String field = sortFields.get(i);
				Comparator<Player> fieldComparator = getFieldComparator(field).reversed();
				if (i == 0) {
					comparator = fieldComparator;
				} else {
					comparator = comparator.thenComparing(fieldComparator);
				}
			}
		}

		return players.stream().sorted(comparator).collect(Collectors.toCollection(ArrayList::new));
	}

	private Comparator<Player> getFieldComparator(String field) {
		switch (field) {
		case "Wins":
			return Comparator.comparingInt((Player p) -> {
				Integer wins = p.getWinsAPI();
				return wins != null ? wins : 0;
			});
		case "Trophies":
			return Comparator.comparingInt((Player p) -> {
				Integer trophies = p.getTrophies();
				return trophies != null ? trophies : 0;
			});
		case "STRTrophies":
			return Comparator.comparingInt((Player p) -> {
				Integer strTrophies = p.getSTRTrophies();
				return strTrophies != null ? strTrophies : 0;
			});
		case "PoLTrophies":
			return Comparator.comparingInt((Player p) -> {
				Integer polTrophies = p.getPoLTrophies();
				return polTrophies != null ? polTrophies : 0;
			});
		case "PoLLeagueNumber":
			return Comparator.comparingInt((Player p) -> {
				Integer leagueNumber = p.getPoLLeagueNumber();
				return leagueNumber != null ? leagueNumber : 0;
			});
		case "Kickpoints":
			return Comparator.comparingLong(Player::getTotalKickpoints);
		case "CWFame":
			return Comparator.comparingInt((Player p) -> {
				Integer cwFame = p.getCWFame();
				return cwFame != null ? cwFame : 0;
			});
		default:
			return Comparator.comparingInt(p -> 0);
		}
	}

	private List<String> parseFields(String input) {
		List<String> fields = new ArrayList<>();
		if (input == null || input.trim().isEmpty()) {
			return fields;
		}

		String[] parts = input.split(",");
		for (String part : parts) {
			String trimmed = part.trim();
			// Check if it's a valid field
			for (String availableField : AVAILABLE_FIELDS) {
				if (availableField.equalsIgnoreCase(trimmed)) {
					fields.add(availableField);
					break;
				}
			}
		}

		return fields;
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals("statslist"))
			return;

		String focused = event.getFocusedOption().getName();
		String input = event.getFocusedOption().getValue();

		if (focused.equals("clan")) {
			List<Command.Choice> choices = new ArrayList<>();

			// Check if user has permission for "Alle Clans"
			User userExecuted = new User(event.getUser().getId());
			boolean hasPermission = false;
			for (String clantag : DBManager.getAllClans()) {
				Player.RoleType role = userExecuted.getClanRoles().get(clantag);
				if (role == Player.RoleType.ADMIN || role == Player.RoleType.LEADER
						|| role == Player.RoleType.COLEADER) {
					hasPermission = true;
					break;
				}
			}

			if (hasPermission && "Alle Clans".toLowerCase().contains(input.toLowerCase())) {
				choices.add(new Command.Choice("Alle Clans", "Alle Clans"));
			}

			// Add individual clans
			choices.addAll(DBManager.getClansAutocomplete(input));

			// Limit to 25 choices
			if (choices.size() > 25) {
				choices = choices.subList(0, 25);
			}

			event.replyChoices(choices).queue();
		} else if (focused.equals("display_fields") || focused.equals("sort_fields")) {
			// Handle comma-separated autocomplete
			List<Command.Choice> choices = getFieldAutocomplete(input);
			event.replyChoices(choices).queue();
		} else if (focused.equals("roles_sorting")) {
			List<Command.Choice> choices = new ArrayList<>();
			if ("true".toLowerCase().contains(input.toLowerCase())) {
				choices.add(new Command.Choice("true", "true"));
			}
			if ("clans".toLowerCase().contains(input.toLowerCase())) {
				choices.add(new Command.Choice("clans", "clans"));
			}
			event.replyChoices(choices).queue();
		}
	}

	private List<Command.Choice> getFieldAutocomplete(String input) {
		List<Command.Choice> choices = new ArrayList<>();

		// Split by comma and get the last part
		String[] parts = input.split(",");
		String prefix = "";
		String lastPart = "";

		if (parts.length > 0) {
			lastPart = parts[parts.length - 1].trim();
			// Build prefix from all parts except the last one
			if (parts.length > 1) {
				StringBuilder prefixBuilder = new StringBuilder();
				for (int i = 0; i < parts.length - 1; i++) {
					if (i > 0)
						prefixBuilder.append(",");
					prefixBuilder.append(parts[i].trim());
				}
				prefix = prefixBuilder.toString() + ",";
			}
		}

		// Get already selected fields to avoid duplicates
		List<String> alreadySelected = new ArrayList<>();
		for (int i = 0; i < parts.length - 1; i++) {
			String trimmed = parts[i].trim();
			for (String field : AVAILABLE_FIELDS) {
				if (field.equalsIgnoreCase(trimmed)) {
					alreadySelected.add(field);
					break;
				}
			}
		}

		// Autocomplete the last part
		for (String field : AVAILABLE_FIELDS) {
			if (!alreadySelected.contains(field) && field.toLowerCase().startsWith(lastPart.toLowerCase())) {
				String displayValue = prefix.isEmpty() ? field : prefix + field;
				choices.add(new Command.Choice(displayValue, displayValue));
				if (choices.size() >= 25) {
					break;
				}
			}
		}

		// If no matches with startsWith, try contains
		if (choices.isEmpty()) {
			for (String field : AVAILABLE_FIELDS) {
				if (!alreadySelected.contains(field) && field.toLowerCase().contains(lastPart.toLowerCase())) {
					String displayValue = prefix.isEmpty() ? field : prefix + field;
					choices.add(new Command.Choice(displayValue, displayValue));
					if (choices.size() >= 25) {
						break;
					}
				}
			}
		}

		return choices;
	}
}

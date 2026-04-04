package com.alistats.discorki.discord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alistats.discorki.config.DiscordConfigProperties;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Component
@ConditionalOnProperty(name = "discord.enabled", havingValue = "true", matchIfMissing = true)
public class JDAInitializer implements CommandLineRunner {
        @Autowired
        private DiscordConfigProperties discordConfigProperties;
        private final SlashCommandController slashCommandListener;

        public JDAInitializer(SlashCommandController slashCommandListener) {
                this.slashCommandListener = slashCommandListener;
        }

        @Override
        public void run(String... args) {
                JDA jda = JDABuilder.createDefault(discordConfigProperties.getToken())
                                .build();

                jda.addEventListener(slashCommandListener);

                // add slash commands
                jda.updateCommands().addCommands(
                                Commands.slash("add", "Add a user to Discorki")
                                                .addOption(OptionType.USER, "discord-username", "The user to add", true)
                                                .addOption(OptionType.STRING, "riot-id",
                                                                "Riot ID of the account (e.g. PlayerName#EUW)", true)
                                                .setGuildOnly(true),
                                Commands.slash("remove", "Remove a user from Discorki")
                                                .addOption(OptionType.USER, "discord-username", "The user to remove",
                                                                true)
                                                .setGuildOnly(true),
                                Commands.slash("list", "List all users in Discorki")
                                                .setGuildOnly(true),
                                Commands.slash("leaderboard", "Get ranked leaderboard for all summoners in this server")
                                                .addOption(OptionType.BOOLEAN, "force-update", "Force update the leaderboard",
                                                                false)
                                                .setGuildOnly(true),
                                Commands.slash("unlink", "Unlink a summoner from a user")
                                                .addOption(OptionType.USER, "discord-username",
                                                                "The user to unlink a summoner from", true)
                                                .addOption(OptionType.STRING, "riot-id",
                                                                "Riot ID of the summoner to unlink (e.g. PlayerName#EUW)",
                                                                true)
                                                .setGuildOnly(true),
                                Commands.slash("channel", "Set the default channel for Discorki to post in")
                                                .addOption(OptionType.CHANNEL, "channel", "The channel to post in",
                                                                true)
                                                .setGuildOnly(true),
                                Commands.slash("debug", "Discorki developers only - Get debug info")
                                                .setGuildOnly(true),
                                Commands.slash("aram", "Generate aram teams")
                                                .addOption(OptionType.USER, "other-captain",
                                                                "Other team captain (you're the first)", true)
                                                .addOption(OptionType.STRING, "other-players",
                                                                "The rest of the players. Comma seperated!", true)
                                                .addOption(OptionType.INTEGER, "champion-amount",
                                                                "Amount of random champions each person gets", false)
                                                .addOptions(
                                                                new OptionData(OptionType.STRING, "champion-class",
                                                                                "Champion class to use", false)
                                                                                .addChoice("Assassin", "Assassin")
                                                                                .addChoice("Fighter", "Fighter")
                                                                                .addChoice("Mage", "Mage")
                                                                                .addChoice("Marksman", "Marksman")
                                                                                .addChoice("Support", "Support")
                                                                                .addChoice("Tank", "Tank"))
                                                .setGuildOnly(true),
                                Commands.slash("latest", "Get latest match for a summoner")
                                                .addOption(OptionType.STRING, "summoner",
                                                                "Riot ID of the summoner (e.g. PlayerName#EUW)",
                                                                true),
                                Commands.slash("help", "Show information about commands"))
                                .queue();

                JDASingleton.setJDA(jda);
        }
}

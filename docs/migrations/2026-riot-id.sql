-- Carries linked accounts over from the summoner name era to Riot IDs (MariaDB).
--
-- Riot retired summoner names, summoner ids and account ids, so accounts are
-- now stored by PUUID in new tables. Start the new version once so Hibernate
-- creates them, then run this script. The old tables are left untouched.
--
-- Accounts are copied with their old summoner name and without a tag line.
-- The Riot ID is filled in after the next finished game of each account.
-- Rank history is not copied, run /leaderboard with force-update afterwards
-- to fetch current ranks.

INSERT IGNORE INTO riot_accounts (puuid, game_name, tag_line, profile_icon_id, revision_date, level)
SELECT puuid, name, NULL, profile_icon_id, revision_date, level
FROM summoners
WHERE puuid IS NOT NULL;

INSERT IGNORE INTO player_riot_account (player_id, riot_account_puuid)
SELECT ps.player_id, s.puuid
FROM player_summoner ps
JOIN summoners s ON s.account_id = ps.summoner_id
WHERE s.puuid IS NOT NULL;

# 🚀 Discorki

## Summary

Discorki is a Discord bot that tracks your games and sends out achievements for you and your friends. Did someone just promote? Did someone just hard int? Discorki will let you know.

In addition, the bot offers features such as generating teams and champions with the ARAM command, the ability to subscribe to a friend's game to know when they're available, leaderboards and more!

The bot is currently in closed beta. Open beta is expected to start early april!

As soon as the bot is live, you can add it via [discorki.nl](https://discorki.nl).

## Install - Windows/Linux/Mac OS

### Prerequisites

+ [Java JDK 21](https://adoptium.net/temurin/releases/?version=21)
+ A [MariaDB](https://mariadb.org/download) database (or configure something else in the `application.properties`)

### Step by Step

1. Clone the repo

```Bash
git clone https://github.com/freshcoders/discorki
```

2. Set up the application settings at `src/main/resources/application.properties`

Example:

```Bash
# database settings
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/discorki
spring.datasource.username=username
spring.datasource.password=password
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# spring misc settings
spring.jpa.open-in-view=false
spring.output.ansi.enabled=always

# riot settings
riot.key=RGAPI-XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
riot.platform-routing=euw1
riot.regional-routing=europe

# discord settings
discord.token=aBcDeFgHiJkLmNoPqRsTuVwXyZ

# custom settings
app.host = discorki.nl
app.developer-discord-ids[0] = yourDiscordId
```

Calls to the Riot API go through [R4J](https://github.com/stelar7/R4J), which reads the rate limits of your key from the responses, so they don't need to be configured. Champion data is always taken from the latest Data Dragon version.

3. Run the application

```Bash
./mvnw spring-boot:run
```

## Install - Docker compose

You can also run this containerized with Docker Compose. [Caddy](https://caddyserver.com) serves the website and automatically requests and renews an HTTPS certificate for your domain. The bot and the database are not exposed outside of the compose network.

### Prerequisites

+ [Docker](https://docs.docker.com/get-docker/) with the Compose plugin
+ A domain pointing to your server, with ports 80 and 443 open

### Step by step

1. Clone the repo

```Bash
git clone https://github.com/freshcoders/discorki
cd discorki
```

2. Copy `.env.example` to `.env` and fill in your keys:

```Bash
HOST=discorki.nl
RIOT_API_KEY=RGAPI-XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
RIOT_PLATFORM_ROUTING=euw1
RIOT_REGIONAL_ROUTING=europe
DISCORD_BOT_TOKEN=
```

3. Run the application:

```Bash
docker compose up -d --build
```

## Upgrading to Riot IDs

Riot retired summoner names, so accounts are now linked by Riot ID (`name#tag`) and stored by PUUID in new tables. Existing links can be carried over: start the new version once so the tables are created, then run [`docs/migrations/2026-riot-id.sql`](docs/migrations/2026-riot-id.sql) against the database and use `/leaderboard` with `force-update` to fetch current ranks. With Docker Compose:

```Bash
docker compose exec -T db sh -c 'mariadb -udiscorki -p"$MARIADB_PASSWORD" discorki' < docs/migrations/2026-riot-id.sql
```

If you used the old Traefik setup, its `letsencrypt` folder is no longer needed. Caddy stores its certificates in a Docker volume.

## Disclaimer

*Discorki isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc.*

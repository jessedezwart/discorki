# 🚀 Discorki

## Summary

Discorki is a Discord bot that tracks your games and sends out achievements for you and your friends. Did someone just promote? Did someone just hard int? Discorki will let you know.

In addition, the bot offers features such as generating teams and champions with the ARAM command, the ability to subscribe to a friend's game to know when they're available, leaderboards and more!

The bot is currently in closed beta. Open beta is expected to start early april!

As soon as the bot is live, you can add it via [discorki.nl](https://discorki.nl).

## Install - Windows/Linux/Mac OS

### Prerequisites

+ [Java JDK 17](https://adoptium.net/temurin/releases/?version=17)
+ A [MariaDB](https://mariadb.org/download) database (or configure something else in the `application.properties`

### Step by Step

1. Clone the repo

```Bash
git clone https://github.com/freshcoders/discorki
```

2. Set up the application settings at `src\main\resources\application.properties`

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
riot.platformRouting=euw1
riot.regionalRouting=europe
riot.rate-limit-per-two-minutes=100

# discord settings
discord.token=aBcDeFgHiJkLmNoPqRsTuVwXyZ

# custom settings
app.host = discorki.nl
app.developer-discord-ids[0] = yourDiscordId
```

3. Run the application

```Bash
./mvnw spring-boot:run
```

## Install - Docker Compose

Runs the app with Caddy (automatic HTTPS via Let's Encrypt) and MariaDB. The `docker-compose.yml` and `Caddyfile` are included in the repo.

### Prerequisites

+ [Docker](https://docs.docker.com/get-docker/)
+ [Docker Compose](https://docs.docker.com/compose/install/)

### Step by step

1. Clone the repo and copy the example env file:

```bash
git clone https://github.com/freshcoders/discorki
cd discorki
cp .env.example .env
```

2. Fill in `.env`.
3. Run:

```bash
docker compose up -d
```

## Disclaimer

*Discorki isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc.*

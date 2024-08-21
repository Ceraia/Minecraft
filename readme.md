# Double, a utility and pvp plugin built for the Ceraia discord server.
![GitHub branch check runs](https://img.shields.io/github/check-runs/Axodouble/Double/master?style=for-the-badge)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues-pr/Axodouble/Double?style=for-the-badge)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/Axodouble/Double?style=for-the-badge)

## Features
The plugin has a few simple features that are added to make the server more fun and engaging.
- **PvP**: A simple PvP system that allows players to fight each other, without worrying about losses, and with a ELO rating system.
- **Races**: A simple race system that allows you to play as a fantasy character, like elves, dwarves, halflings and anything else.
- **Seating**: A simple seating system that allows you to sit on stairs, slabs and anywhere else with `/sit`.

## PvP
The PvP system allows users to create player-built arenas, and share them with others.
The system is based on ELO, and the ELO is calculated based on the ELO of the players, and the outcome of the fight.
Here are the commands relating to the PvP system:
- `/pvp`: The main command for the PvP system.
- `/gvg`: The main command for the Group vs Group system.
- `/arena`: The main command for the Arena system, also creation and deletion of arenas.

## Races
The race can be selected with the following command:
- `/race become <race>`: This command allows you to become a race, and get the perks of that race.
- `/race reload`: This command reloads the races stored in the `races` folder.
- `/race default`: Restores **all** default races, will override existing changes.

## Seating
The seating system allows you to sit on stairs, slabs, and any other block that is not a full block.
You can sit on stairs and slabs by right clicking on them, and you can sit on any other block by using the `/sit` command.

## Versioning Schema
Versioning is done in the following format: `major.minor.patch`, following the [Semantic Versioning 2.0.0](https://semver.org/) as base.
However, the patch is the Year, Month, and Day of the release, in the format `YYMDD`.

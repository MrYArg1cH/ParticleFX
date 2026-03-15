# ParticleFX

ParticleFX is a Paper 1.21 plugin that adds customizable particle effects for many player actions.

Players can create their own presets, edit animation parameters, add or remove particle layers, preview effects, and bind presets to supported actions without editing the server config manually.

## Features

- Multiple built-in animation types, including `RING_WAVE`, `DOUBLE_HELIX`, `PHOENIX_BURST`, `TORNADO`, `CELESTIAL_SPHERE`, `ORBITAL`, `STARBURST`, and `PILLAR_PULSE`
- Many supported triggers: jump, death, respawn, join, quit, move, sprint start/stop, sneak start/stop, attack, hit, consume, teleport, block break, block place, bow shot, and level up
- Global presets and default bindings through `config.yml`
- Per-player presets and per-player bindings saved automatically in `plugins/ParticleFX/playerdata/`
- In-game preset editing with commands
- In-game preview system for testing custom effects

## Commands

| Command | Description |
| --- | --- |
| `/particlefx help` | Show command help |
| `/particlefx reload` | Reload the plugin configuration |
| `/particlefx actions` | List supported action triggers |
| `/particlefx animations` | List available animation types |
| `/particlefx presets` | Show personal and global presets |
| `/particlefx preview <preset>` | Preview a preset at your location |
| `/particlefx preset create <name>` | Create a personal preset |
| `/particlefx preset clone <source> <new-name>` | Clone a preset into your personal profile |
| `/particlefx preset rename <old> <new>` | Rename a personal preset |
| `/particlefx preset delete <name>` | Delete a personal preset |
| `/particlefx preset info <name>` | Show preset details |
| `/particlefx preset set <name> <property> <value>` | Edit preset settings |
| `/particlefx preset particle add <preset> <particle>` | Add a particle layer |
| `/particlefx preset particle remove <preset> <index>` | Remove a particle layer |
| `/particlefx preset particle set <preset> <index> <property> <value>` | Edit a particle layer |
| `/particlefx preset particle list <preset>` | List particle layers in a preset |
| `/particlefx preset particle clear <preset>` | Remove all particle layers |
| `/particlefx bind list` | Show current effective bindings |
| `/particlefx bind add <action> <preset>` | Bind a preset to an action |
| `/particlefx bind remove <action> <preset>` | Remove a preset from an action |
| `/particlefx bind clear <action>` | Disable an action in your personal profile |
| `/particlefx bind reset <action>` | Reset an action back to global defaults |

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `particlefx.reload` | Reload the plugin config | `op` |
| `particlefx.customize` | Manage personal presets and bindings | `true` |
| `particlefx.effect.jump` | Use the default jump preset | `true` |
| `particlefx.effect.death` | Use the default death preset | `true` |
| `particlefx.effect.respawn` | Use the default respawn preset | `true` |

## Configuration

The default configuration file is located at:

`src/main/resources/config.yml`

It contains:

- global presets
- default trigger bindings
- default permissions
- message settings
- disabled worlds list

Example workflow for a player:

```text
/particlefx preset create myflare
/particlefx preset set myflare animation TORNADO
/particlefx preset particle add myflare DUST
/particlefx preset particle set myflare 1 color 255,80,180
/particlefx bind add sprint-start myflare
/particlefx preview myflare
```

## Installation

1. Build the plugin or download the compiled jar.
2. Put the jar into the server `plugins/` folder.
3. Start the server.
4. Edit `plugins/ParticleFX/config.yml` if you want to change global presets or default bindings.
5. Use `/particlefx reload` after config changes.

## Build

This project uses Gradle.

```powershell
.\gradlew.bat build
```

The compiled jar will be generated in:

`build/libs/ParticleFX-1.0-SNAPSHOT.jar`

## Notes

- The plugin targets Paper `1.21`
- Player-created data is stored automatically per UUID
- Some `Sound` lookups use Paper's deprecated compatibility API, but the plugin builds and works correctly on the current target version

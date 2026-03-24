# Lomka

<div align="center">
    <img src="https://img.shields.io/badge/Minecraft-1.21.11-blue?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft Version">
    <img src="https://img.shields.io/badge/Fabric-Loader-orange?style=for-the-badge&logo=fabric&logoColor=white" alt="Fabric">
    <br>
    <a href="https://modrinth.com/mod/lomka">
        <img src="https://img.shields.io/modrinth/dt/lomka?style=for-the-badge&logo=modrinth&logoColor=white&label=Modrinth&color=00AF5C" alt="Modrinth Downloads">
    </a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/lomka">
        <img src="https://img.shields.io/curseforge/dt/1462263?style=for-the-badge&logo=curseforge&logoColor=white&label=CurseForge&color=F16436" alt="CurseForge Downloads">
    </a>
</div>

<br>

Lomka is a client-side optimization mod for Minecraft. Unlike many other optimization mods that focus solely on rendering or game logic, Lomka improves the game experience by optimizing various subsystems including texture caching, frustum culling, network protocols, NBT processing and shaders rendering-all without compromising visual quality.

## Benchmarks

*Test Hardware: Intel CC150, 16GB DDR4, GTX 1050 TI*
*Config:* `config/lomka.json`
*Note: All mod options enabled during testing.*

### Singleplayer World (1000 entities)

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 53 FPS |
| **With Lomka** | **78 FPS** |

### Iris Shaders (ComplementaryReimagined_r5.7.1, High)

| Setup | Max FPS |
| :--- | :--- |
| **Without Lomka** | 70 FPS |
| **With Lomka** | **80 FPS** |

### Multiplayer Server w/ lots of entities

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 197 FPS |
| **With mod** | **254 FPS** |

### Multiplayer Server w/ lots of *CEMT (tested on CounterMine 2)

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 219 FPS |
| **With mod** | **422 FPS** |

### Multiplayer Server, if too close to server

| Setup | Average ping |
| :--- | :--- |
| **Vanilla** | 40 ms |
| **With mod** | **39 ms** |

### Multiplayer Server, if too far away from server

| Setup | Average ping |
| :--- | :--- |
| **Vanilla** | 215 ms |
| **With mod** | **207 ms** |

CEMT - Custom Entity Model Textures

## Options
Lomka uses JSON config only: `config/lomka.json` (created on first launch).

- `java22Optimizations` : Enables experimental optimizations for Java 22+. (default: false)
- `entities.cullInvisible` : Enables entity culling of all invisible entities. (default: false)


### ✅ Compatibility

Lomka is designed to be highly compatible with the Fabric ecosystem. It works alongside most other mods and optimization tools (Sodium, Lithium, etc.).

**Lomka works better with quick pack and Helium**

### ⚠️ May be useless/with conflicts
- C2ME
- Entity Culling (uses friendly culling, can be unused)
### ❌ Known incompatibilities
- VulkanMod (for now not considered for future compatibility)

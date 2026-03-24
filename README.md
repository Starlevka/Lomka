# Lomka

<div align="center">
    <img src="https://img.shields.io/badge/Fabric-Loader-orange?style=for-the-badge&logo=fabric&logoColor=white" alt="Fabric">
    <img src="https://img.shields.io/badge/Quilt-Loader-blue?style=for-the-badge&logo=quilt&logoColor=white" alt="Quilt">
    <img src="https://img.shields.io/badge/NeoForge-Loader-purple?style=for-the-badge&logo=neoforge&logoColor=white" alt="NeoForge">
    <br>
    <a href="https://modrinth.com/mod/lomka">
        <img src="https://img.shields.io/modrinth/dt/lomka?style=for-the-badge&logo=modrinth&logoColor=white&label=Modrinth&color=00AF5C" alt="Modrinth Downloads">
    </a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/lomka">
        <img src="https://img.shields.io/curseforge/dt/1462263?style=for-the-badge&logo=curseforge&logoColor=white&label=CurseForge&color=F16436" alt="CurseForge Downloads">
    </a>
</div>

<br>

Lomka is a client-side optimization mod for Minecraft. Unlike many other optimization mods that focus solely on rendering or game logic, Lomka improves the game experience by optimizing various subsystems including opengl improvements, frustum culling, network protocols, NBT processing, rendering-all without compromising visual quality.

## Benchmarks

*Test Hardware: Intel CC150, 16GB DDR4, GTX 1050 TI*
*Note: All mod options enabled during testing including Java 25 optimizations*

### Singleplayer World (1000 mobs)

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 53 FPS |
| **With Lomka** | **76 FPS** |

### Iris Shaders (ComplementaryReimagined_r5.7.1, High)

| Setup | Max FPS |
| :--- | :--- |
| **Without Lomka** | 32 FPS |
| **With Lomka** | **39 FPS** |

### Multiplayer Server w/ lots of entities

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 197 FPS |
| **With mod** | **254 FPS** |

### Multiplayer Server w/ lots of *CEMT (tested on CounterMine 2)

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 220 FPS |
| **With mod** | **418 FPS** |

### Multiplayer Server, if too close to server

| Setup | Average ping |
| :--- | :--- |
| **Vanilla** | 40 ms |
| **With mod** | **39 ms** |

### Multiplayer Server, if too far away from server

| Setup | Average ping |
| :--- | :--- |
| **Vanilla** | 197 ms |
| **With mod** | **192 ms** |

CEMT - Custom Entity Model Textures

## Options
Lomka uses properties config only: `config/lomka.properties` (created on first launch).

- `java25Optimizations` : Enables experimental optimizations for Java 25. (default: false)


### ✅ Compatibility

Lomka is designed to be highly compatible with the Fabric ecosystem. It works alongside most other mods and optimization tools (Sodium, Lithium, etc.).

### ❌ Known incompatibilities
- VulkanMod

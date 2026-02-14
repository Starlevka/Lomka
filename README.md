# Lomka

<div align="center">
    <img src="https://img.shields.io/badge/Minecraft-1.21.11-blue?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft Version">
    <img src="https://img.shields.io/badge/Fabric-Loader-orange?style=for-the-badge&logo=fabric&logoColor=white" alt="Fabric">
    <br>
    <a href="https://modrinth.com/mod/lomka">
        <img src="https://img.shields.io/modrinth/dt/lomka?style=for-the-badge&logo=modrinth&logoColor=white&label=Modrinth&color=00AF5C" alt="Modrinth Downloads">
    </a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/lomka">
        <img src="https://img.shields.io/curseforge/dt/lomka?style=for-the-badge&logo=curseforge&logoColor=white&label=CurseForge&color=F16436" alt="CurseForge Downloads">
    </a>
</div>

<br>

Lomka is a client-side optimization mod for Minecraft. Unlike many other optimization mods that focus solely on rendering or game logic, Lomka improves the game experience by optimizing various subsystems including texture caching, frustum culling, network protocols, NBT processing, chunks optimizations and shader rendering-all without compromising visual quality.

## Benchmarks

*Test Hardware: Intel CC150, 16GB DDR4, GTX 1050 TI*

### Game Starting

| Setup | Load Time |
| :--- | :--- |
| **Vanilla** | 15.5s | - |
| **With mod** | **12.9s** |

### World Loading

| Setup | Load Time |
| :--- | :--- |
| **Vanilla** | 10.5s | - |
| **With mod** | **9.3s** |

### Singleplayer World

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 178 FPS |
| **With Lomka** | **193 FPS** |

### Multiplayer Server w/o textures

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 302 FPS |
| **With mod** | **326 FPS** |

### Multiplayer Server w/ textures (tested on CubeCraft's hub)

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 166 FPS |
| **With mod** | **205 FPS** |

### Multiplayer Server w/ lots of *CET (tested on CounterMine 2)

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 204 FPS |
| **With mod** | **322 FPS** |

CET - Custom Entity Textures

## Options
- `-Dlomka.java22_25.optimizations` : Enables experimental optimizations specifically for Java versions 22 to 25. (default: false)
- `-Dlomka.compat.sodium.allowChunkMixins` : Allows chunk mixins even with Sodium/C2ME loaded. (default: false)
- `-Dlomka.textures.maxDimension : Limits the maximum width/height of textures. (default: 0)
- `-Dlomka.textures.decode.offThread` : Moves texture decoding to background threads to prevent main thread stutters. (default: true)
- `-Dlomka.textures.decode.parallelism` : Configures the number of concurrent threads used for texture decoding. (default: 2)
- `-Dlomka.textures.decode.executor` : Sets the thread pool type ( virtual , common , fixed , or platform ) for decoding. (default: virtual)
- `-Dlomka.entities.cullInvisible` : Enables entity culling of all invisible entities. (default: false)
- `-Dlomka.resources.reload.debounceMs` : Resourse reloading after pause (default: 200, disable: 0)


### ✅ Compatibility

Lomka is designed to be highly compatible with the Fabric ecosystem. It works alongside most other mods and optimization tools (Sodium, Lithium, etc.).

### ⚠️ May be useless/with conflicts
- C2ME
- Entity Culling (uses friendly culling, can be unused)
### ❌ Known incompatibilities
- VulkanMod (for now not considered for future compatibility)
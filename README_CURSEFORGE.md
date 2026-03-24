<h1>Lomka</h1>

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

<p>Lomka is a client-side optimization mod for Minecraft. Unlike many other optimization mods that focus solely on rendering or game logic, Lomka improves the game experience by optimizing various subsystems including texture caching, frustum culling, network protocols, NBT processing, chunks optimizations and shader rendering-all without compromising visual quality.</p>

<h2>Benchmarks</h2>

<p><i>Test Hardware: Intel CC150, 16GB DDR4, GTX 1050 TI</i></p>
<p><i>Config: </i><code>config/lomka.json</code></p>
<p><i>Note: All mod options enabled during testing.</i></p>


<h3>Singleplayer World (1000 mobs)</h3>

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 53 FPS |
| **With Lomka** | **78 FPS** |

<h3>Iris Shaders (ComplementaryReimagined_r5.7.1, High)</h3>

| Setup | Max FPS |
| :--- | :--- |
| **Without Lomka** | 70 FPS |
| **With Lomka** | **80 FPS** |

<h3>Multiplayer Server w/ lots of entities</h3>

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 197 FPS |
| **With mod** | **254 FPS** |

<h3>Multiplayer Server w/ lots of *CEMT (tested on CounterMine 2)</h3>

| Setup | Average FPS |
| :--- | :--- |
| **Vanilla** | 219 FPS |
| **With mod** | **422 FPS** |

<h3>Multiplayer Server, if too close to server</h3>

| Setup | Average ping |
| :--- | :--- |
| **Vanilla** | 40 ms |
| **With mod** | **39 ms** |

<h3>Multiplayer Server, if too far away from server</h3>

| Setup | Average ping |
| :--- | :--- |
| **Vanilla** | 215 ms |
| **With mod** | **207 ms** |

<p><i>*CEMT - Custom Entity Model Textures</i></p>

<h2>Options</h2>
<ul>
    <li>Lomka uses JSON config only: <code>config/lomka.json</code> (created on first launch).</li>
    <li><code>java22Optimizations</code> : Enables experimental optimizations for Java 22+. (default: false)</li>
    <li><code>entities.cullInvisible</code> : Enables entity culling of all invisible entities. (default: false)</li>
</ul>

<h2>Compatibility</h2>

<p>Lomka is designed to be highly compatible with the Fabric ecosystem. It works alongside most other mods and optimization tools (Sodium, Lithium, etc.).</p>

**Lomka works better with quick pack and Helium**

<h3>⚠️ May be useless/with conflicts</h3>
<ul>
    <li>C2ME</li>
    <li>Entity Culling (uses friendly culling, can be unused)</li>
</ul>

<h3>❌ Known incompatibilities</h3>
<ul>
    <li>VulkanMod (for now not considered for future compatibility)</li>
</ul>

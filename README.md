![Lomka icon](https://cdn.modrinth.com/data/bd9cFfiC/827fe2486df2288c1fb2bc50ab513eb474a821fa_96.webp)
## Summary

- Reduces micro-stutters
- Slightly improves FPS and decreases texture loading (feels diffrent on some systems)
- Could decrease RAM usage sometimes

## What actually does the mod do?

- Speeds up some calculations (colors engine, maths, texture loading)
- Fixes code's bugs
- Prevents unnecessary allocations (sounds, calcs and rendering)
- Improves hashCode() game's caches 
- Slightly improves threading system
- Caching JOML calls (experimental)

## FAQ

**Q: I'm having performance issues, and removing Lomka fixes them!**

A: Make sure you are using the latest version. Lomka targets hot-paths across multiple Minecraft versions — if you encounter a compatibility issue, please report it. **At the moment the most perfomance friendly mod versions are 0.2.0 and 0.3.0.**

**Q: Can I run Lomka on the server-side?**

A: Yes. 

**Q: Forge support?**

A: No. Just no, please... (Only 1.20.1, maybe)

**Q: Any backports? (<1.21 or <1.20.1)**

A: No. The mod's current functionality largely follows Minecraft version 1.21.11 and its structure.

**Q: Does Lomka require Fabric API or any other dependencies?**

A: No. Lomka depends only on Fabric Loader or NeoForge Loader, without any dependencies.

**Q: Mobile devices support?**

A: Is it actually starts with Lomka? Cool. That's all.

**Q: Can I use Lomka in a modpack?**

A: Yes. Lomka is absolutely free and open-source. :sunglasses:

**Q: Does Lomka have a config file? (Lomka v0.3.0>=)**

A: Soon as possible. Againly the maintained version is 1.21.11. Maybe with time will be added but currently mixins can be disabled by editing *lomka.mixins.json* inside the mod's archive.

## Bug Reporting

When reporting a bug that only appears with Lomka installed:
- Report it on the project's issue tracker (not to other mod authors — Lomka's mixins are surgical and unlikely to cause crashes in unrelated systems).
- Try to reproduce the issue without mod Lomka first.

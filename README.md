DOWNLOAD ALL BUILDS FROM THE ACTIONS TAB!!! If you have issues with it, you can also download from the releases page, just I don't update it as often because the releases tab is only for tested builds that I know work perfectly. The main branch will be updated to match the rgds-speed-optimizations branch every once in a while once the new features are tested and confirmed to work.

Feel free to contribute (:

Azahar MMJ is a lightweight fork of Azahar Plus with performance tweaks for Android-based dual screen devices such as the Anbernic RGDS, which is the device it is tailored for. For most dual screen devices, this emulator is not needed as normal emulators should perform fine. Less powerful ones, like the RGDS, cannot run normal emulators. This emulator is tailored for those devices that need the extra performance to run 3DS games. It uses various hacks from Citra MMJ (as implied by the name) and some of my own, and merges them into one big set of performance tweaks. In other words, we prioritize speed over accuracy.

![App Logo](https://github.com/shunoni/AzahaRGDS/blob/AzaharPlus/logo.jpg?raw=true)

Why did I do this? No idea.

Here are the stats on my Anbernic RGDS (Base model, Android, Stock OS) :

| Game tested  | Game speed | Frameskip mode | New 3DS Mode | Additional notes |
| ------------- | ------------- | ------------- | ------------- | ------------- |
| Tomodachi life  | 100% locked  | Mode 1  | No | None |
| Streetpass Mii Plaza  | 100% average  | Mode 1  | No | Updating does not work, play coins unobtainable. Not a bug |
| 3DS HOME menu | Almost 100%, working on it | Mode 1 | No | Small stutters when first booted, fine after that |
| Mario Kart 7 | 95-100% average | Mode 1 | No | This is one of the main games I wanted to play, so I'm still optimizing it to hit 100% locked. |
| Minicraft 3D (Game of Tobi) | 100% locked on average settings | Mode 1 | No | This is an unofficial port made for the original 3DS. |
| Minecraft (Official port) | 60-70% average, unlikely to ever reach 100% | Mode 2 | Yes | I seriously doubt we'll ever reach full speed on this one. |
| Geometry Dash 3DS (AleFunky) | 100% locked | Mode 0/None | No | *Clutterfunk intensifies* |
| Mario 3D Land | 75-85% average | Mode 1 | No | None |
| Mii Maker | 100% locked | Mode 1 | No | None |

And here's the performance side by side, also on my RGDS, with all ~~11~~ 10 special ~~herbs and spices~~ hacks and optimizations enabled (holy KFC reference) :

| Game | Speed (Upstream, minimum settings) | Speed (Azahar MMJ) |
| ------------- | ------------- | ------------- |
| Mario Kart 7 | 65-80% | 95-100% average |
| 3DS Home | 70-80% average | 100% locked |
| Tomodachi Life | 80-100% | 100% locked |
| Mii Maker | 90-100% | 100% locked |
| Mario 3D Land | 60-71% (no six seven 4 u) | 75-85% average |

Not all games in the tested games were compared against upstream. Don't take this table for granted, results may vary on different hardware. Some devices will be unable to run this.

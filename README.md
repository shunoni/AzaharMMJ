DOWNLOAD ALL BUILDS FROM THE ACTIONS TAB!!! If you have issues with it, you can also download from the releases page, just I don't update it as often because the releases tab is only for tested builds that I know work perfectly. The main branch will be updated to match the rgds-speed-optimizations branch every once in a while once the new features are tested and confirmed to work.

Feel free to contribute, much appreciated (:

Azahar MMJ is a lightweight fork of Azahar Plus with performance tweaks and QOL features for Android-based dual screen devices such as the Anbernic RGDS, which is the device it is tailored for. For most dual screen devices, this emulator is not needed as normal emulators should perform fine. Less powerful ones, like the RGDS, cannot run normal emulators. This emulator is tailored for those devices that need the extra performance to run 3DS games. It uses various hacks from Citra MMJ (as implied by the name) and some of my own, and merges them into one big set of performance tweaks. In other words, we prioritize speed over accuracy.

![App Logo](https://github.com/shunoni/AzahaRGDS/blob/AzaharPlus/logo.jpg?raw=true)

Why did I do this? No idea.

Here are the stats on my Anbernic RGDS (Base model, Android, Stock OS) :

| Game tested  | Game speed | Frameskip mode | New 3DS Mode | Realtime GPU | Additional notes |
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
| Tomodachi life  | 100% locked  | Mode 1  | No | Optional | None |
| Streetpass Mii Plaza  | 100% average  | Mode 1  | No | Optional | Updating does not work, play coins unobtainable. Not a bug. I'm planning to add a way to obtain play coins without a pedometer, so stay tuned. |
| 3DS HOME menu | Almost 100%, working on it | Mode 1 | No | Optional | Small stutters when first booted, fine after that |
| Mario Kart 7 | 95-100% average | Mode 1 | No | Recommended | This is one of the main games I wanted to play, so I'm still optimizing it to hit 100% locked. Still right now it's totally playable. |
| Minicraft 3D (Game of Tobi) | 100% locked on average settings | Mode 1 | No | Optional | This is an unofficial port made for the original 3DS. Still cool tho. |
| Minecraft (Official port) | 75-90% average, unlikely to ever reach 100% without real-time GPU and even then it runs awfully in terms of fps | Mode 2 | Yes | Needed | I seriously doubt we'll ever reach full speed on this one, but with the new realtime GPU optimizations it might just happen. |
| Geometry Dash 3DS (AleFunky) | 100% locked | Mode 0/None | No | Optional | *Clutterfunk intensifies* |
| Mario 3D Land | 90-100% average | Mode 1 | No | Needed | For some reason in UI it's slower than in-game. In-game with realtime GPU I had nearly 100% speed locked with some small dips but in UI it was like 90% speed, not sure what the cause is but I'm looking into it |
| Mii Maker | 100% locked | Mode 1 | No | Optional | None |

And here's the performance side by side, also on my RGDS, with all ~~11~~ 10 special ~~herbs and spices~~ hacks and optimizations enabled (holy KFC reference) :

| Game | Speed (Upstream, minimum settings) | Speed (Azahar MMJ + Realtime GPU) |
| ------------- | ------------- | ------------- |
| Mario Kart 7 | 65-80% | 95-100% average |
| 3DS Home | 70-80% average | 100% locked |
| Tomodachi Life | 80-100% | 100% locked |
| Mii Maker | 90-100% | 100% locked |
| Mario 3D Land | 60-71% (no six seven 4 u) | 90-100% average |

Not all games in the tested games were compared against upstream. Don't take this table for granted, results may vary on different hardware. Some devices will be unable to run this.

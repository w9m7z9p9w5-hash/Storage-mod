# Infinite Storage - client mod (Fabric, Minecraft 26.3)

A client-only mod. It does nothing on its own: it is the screen for the **istore data pack**.
Right-click a **Lodestone** to open it. Sneak + right-click still places blocks against it, and a compass
still binds to it. To use another block, change `STORAGE_BLOCK` in `IStoreClient.java`.

* Items are listed sorted by count (click the comparator to change sort).
* Click an item = take a stack, right-click = half a stack, shift-click = take 1.
* Hopper button = deposit your main inventory (not hotbar). Enchanted, named and damaged items are skipped.

It talks to the data pack by sending the pack's functions silently, and hides the replies from chat.
Needs operator permission on the world/Realm, because functions are operator commands.

## Building

Push this folder to a GitHub repository and open the Actions tab: the jar is built for you
(artifact "istore-client-jar"). Or locally with JDK 25 and Gradle 9.3+: `gradle build`, jar in `build/libs/`.
Put the jar (not the -sources one) in your `mods` folder with Fabric API 0.161.0+26.3.

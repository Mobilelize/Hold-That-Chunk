# Hold That Chunk V2
A clean, from-scratch re-implementation of the original idea behind [Hold That Chunk](https://modrinth.com/mod/hold-that-chunk) updated for **Minecraft 1.21+** with some extra configuration options.

Instead of dropping terrain the instant the server says “unload,” this mod keeps already-rendered chunks around and lets go of them later under your control: **chunks are held and only removed later based on configurable rules**. Great for keeping what you’ve already seen on screen—like your **base** on low render-distance servers.

> Credit & inspiration: the original **Hold That Chunk** (created to help ice boat racing).  
> For **1.18–1.20.6**, use the original: https://modrinth.com/mod/hold-that-chunk

---

## ✨ Features

- **Enable Hold That Chunk** — master toggle.  
  *Note: changes take effect when switching worlds or reconnecting.*

- **Ignore Server’s Render Distance**  
  - **True:** your render distance can exceed the server’s render distance (vanilla fog may sit far out).  
  - **False:** your maximum render distance is capped to the server’s render distance.  
  *Note: changes take effect when switching worlds or reconnecting.*
  

- **Respect Server’s Render Distance** — always honor server unloads immediately (no holding).

- **Link to Render Distance** — uses your render distance as the hold limit.

- **Hold Distance (2–256)** — set a custom limit if you don’t want to link to render distance.

- **Server Render Distance readout** — see the server-reported distance in the config screen.

---

## 🧠 How it works

- When an **unload** packet arrives for a chunk you already rendered, the client **holds** it instead of removing it instantly.  
- Every tick the mod decides whether each held chunk **should be removed** (based on your settings).  
- You can **ignore** the server’s distance (keep more of what you’ve seen) or **cap** yourself to it so **fog** matches.

> This does **not** force the server to send new chunks or raise its view distance. It only changes how long the **client** keeps chunks you’ve already rendered.

---

## 🔎 Notes & limitations

- Client-side only; does **not** increase server view distance or generate new data.  
- Holding more chunks can use additional memory—tune **Hold Distance** to your preference.  
- Options that change the client/server distance relationship (ignore/cap) typically apply **after a world switch or reconnect** because the server distance is negotiated on join.

---

## 🙏 Credits & links

- Idea & original project: **Hold That Chunk** → https://modrinth.com/mod/hold-that-chunk  
- This mod is a **ground-up re-implementation** for 1.21+ with extra configuration.

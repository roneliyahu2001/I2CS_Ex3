# Pac-Man Project – Intelligent Agent & Server Logic Implementation

## Overview
This project presents a comprehensive implementation of both the **Game Server Logic** and an **Autonomous AI Agent** for the Pac-Man environment.

This solution was built from scratch to replace the standard game engine while maintaining full interface compatibility.

**Course:** Introduction to Computer Science
**Institution:** Ariel University, School of Computer Science
**Year:** 2026

The project focuses on two main components:
1.  **`MyPacmanGame`:** A robust server-side logic handling game rules, collision detection, and intelligent ghost behavior.
2.  **`Ex3Algo`:** A sophisticated client-side algorithm capable of navigating complex mazes, avoiding dynamic threats, and maximizing score stability.

---

## Part 1 – The AI Agent (`Ex3Algo`)

### Goal
The objective was to create a deterministic, stable, and survival-oriented agent that solves the maze while managing risk against multiple chasing ghosts.

### Key Strategies & Algorithms

* **Hybrid BFS Navigation:**
    The movement engine uses a dual-layer Breadth-First Search (BFS):
    1.  **Safe Mode:** Calculates paths treating ghosts and their immediate surroundings as "Walls."
    2.  **Fallback Mode:** If the agent is surrounded by fear (no safe path exists), it switches to a standard BFS to ensure movement and avoid getting stuck against walls.

* **Panic Mode (Survival Instinct):**
    The agent constantly monitors a **danger radius (15 units)**. If a ghost breaches this perimeter, the algorithm overrides the food-hunting logic and triggers a `runAway()` routine. This routine calculates the vector that maximizes the distance from the nearest threat.

* **Anti-Jitter Stabilization:**
    A common issue in real-time pathfinding is "direction flipping" (rapidly switching between `UP` and `DOWN`).
    * **Solution:** The algorithm implements a stabilization buffer. It prioritizes maintaining the current direction over making immediate 180-degree turns unless a physical obstacle forces it. This results in smooth, human-like movement without "dancing" in place.

* **Coordinate Synchronization:**
    Fixed a critical disparity between continuous coordinates (`double`) and discrete grid logic (`int`). The algorithm uses precise integer casting to align perfectly with the server's collision detection system, preventing the "Wall Stuck" bug.

---

## Part 2 – Server Logic (`MyPacmanGame`)

This component implements the `PacmanGame` interface, serving as the authoritative logic for the game state.

### Core Mechanics
* **Game Loop & Timing:** Manages the tick-rate difference between Pac-Man and Ghosts (Ghosts move every 4th tick), balancing the difficulty to allow for fair gameplay.
* **Collision Detection:** Precise hitbox calculation for Pac-Man vs. Ghosts and Pac-Man vs. Collectibles (Dots/Power Pellets).
* **Y-Axis Logic:** Handles the screen coordinate system where `Y=0` is at the top, ensuring `UP` decreases the Y value.

### Advanced Ghost AI
Unlike standard random ghosts, this server implements **Smart Chasing Logic**:
* **BFS Chasing:** Ghosts calculate the shortest path to Pac-Man using BFS.
* **Ghost House Logic:** Implemented a specific constraint to force ghosts *out* of the central box and prevent them from re-entering. This was achieved by treating the house coordinates as "One-Way Walls" in the ghost's pathfinding algorithm.

---

## Technical Challenges & Solutions

| Challenge | Solution |
| :--- | :--- |
| **The "Stuck in Wall" Bug** | The agent previously rounded coordinates incorrectly. Fixed by aligning the parsing logic (`(int) cast`) strictly with the server's grid system. |
| **Oscillation ("Dancing")** | Pac-Man would freeze, vibrating between two tiles. Solved by the **Anti-Jitter** logic that penalizes reversing direction without cause. |
| **Y-Axis Orientation** | The game engine uses screen coordinates (Y grows downwards). The algorithm's direction logic was inverted to match this specific coordinate system. |
| **Ghost House Traps** | Ghosts would sometimes get stuck inside the spawn box. Fixed by adding specific "Exit Force" logic in the server. |

---

## Summary

✔ **Robust AI:** Survives high-speed ghosts using a large panic radius.
✔ **Smooth Movement:** Eliminates jitter and wall-clipping issues.
✔ **Smart Server:** Ghosts that chase intelligently but respect "House Rules."
✔ **Clean Architecture:** Strict separation between the map parsing, game logic, and algorithmic decision-making.

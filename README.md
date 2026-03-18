# FoodTok Recipe Discovery App 🍳
*50.001 1D Information Systems Design Project*

## Project Overview
Food Tok is an infinite scrolling app for cooks to share and discover various recipes from different parts of the world for everyone to make
## Architecture & Package Structure
To maintain Separation of Concerns, please place all new files in their designated packages:
* `models/` - Plain Java data entities (`User`, `Recipe`, `Ingredient`).
* `services/` - Stateless business logic and algorithms (`RecommendationService`, `InteractionManager`).
* `ui/` - Android Activities and Fragments (`MainActivity`).
* `adapters/` - RecyclerView adapters (`FeedAdapter`).

## Git Workflow
* **DO NOT commit directly to the `main` branch.**
* Create a new branch for every feature you work on.
* Pull from `main` frequently to avoid merge conflicts.
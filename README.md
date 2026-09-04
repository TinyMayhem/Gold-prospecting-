# Gold Prospecting Solver Overlay

Android companion prototype for the Last War: Survival Gold Prospecting mini-game.

## Features

- Floating **SOLVE** bubble over the game.
- Android screen capture, with user permission.
- Reads the 8 x 8 board and three current pieces.
- Searches legal placements and piece orders.
- Draws numbered placements over the game.
- Tracks expected gem totals between batches.
- Does not tap, drag, or control Last War.

## Cloud APK build

This version includes `.github/workflows/build-apk.yml`.

After the project is pushed to GitHub, open the repository's **Actions** tab and run **Build Gold Prospecting APK**. The workflow builds a debug APK and uploads an artifact named `GoldProspectingSolver-APK` containing `GoldProspectingSolver.apk`.

For phone-only instructions, read `PHONE_ONLY_GITHUB_BUILD.md`.

## First run

1. Open the app.
2. Allow floating overlay permission.
3. Reset gem totals for a new round.
4. Start the SOLVE bubble and approve screen capture.
5. Open Last War and enter Gold Prospecting.
6. Wait until all three pieces are visible.
7. Tap SOLVE.
8. Follow placements 1, 2, and 3.

Long-press the bubble to reopen the app.

## Prototype status

Automatic recognition is calibrated to the portrait screenshots supplied during development. Verify the detected placements on a non-critical attempt before relying on it for a scored run.

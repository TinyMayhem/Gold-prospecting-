# Build the APK from an Android phone with GitHub

This folder is already configured to build `GoldProspectingSolver.apk` in GitHub Actions.

## What you need

- A free GitHub account.
- Chrome or Samsung Internet on your phone.
- The extracted `GoldProspectingOverlay_GitHub` project folder.

## Easiest phone-only method

GitHub's normal mobile upload page is awkward with folders, so the cleanest phone-only route is a GitHub Codespace.

1. Go to GitHub and create a new **private** repository. Name it `gold-prospecting-solver`.
2. Open the new repository in your browser.
3. Choose **Code** > **Codespaces** > **Create codespace on main**.
4. When the Codespace opens, upload the contents of this project folder into the Codespace file explorer. If you uploaded the ZIP instead, open the terminal and run:

   ```bash
   unzip GoldProspectingOverlay_GitHub.zip
   cp -a GoldProspectingOverlay_GitHub/. .
   rm -rf GoldProspectingOverlay_GitHub GoldProspectingOverlay_GitHub.zip
   ```

5. In the Codespace terminal run:

   ```bash
   git add .
   git commit -m "Add Gold Prospecting solver"
   git push
   ```

6. Go back to the repository page and tap **Actions**.
7. Open **Build Gold Prospecting APK**.
8. The build normally starts automatically after the push. If not, tap **Run workflow**.
9. When the build is green, open it and scroll to **Artifacts**.
10. Download **GoldProspectingSolver-APK**. GitHub downloads it as a ZIP.
11. Extract that ZIP on your Samsung. Inside is `GoldProspectingSolver.apk`.
12. Tap the APK to install it. Android may ask you to allow installation from your browser or Files app.

## After installation

1. Open **Gold Prospecting Solver**.
2. Allow **Display over other apps**.
3. Set gem totals to 0 / 0 / 0 for a new Gold Prospecting round.
4. Tap **Start SOLVE bubble** and approve screen capture.
5. Open Last War.
6. When the three pieces are visible, tap the floating **SOLVE** bubble.
7. Follow the numbered placements.

## Important testing note

This is still a prototype. The solver was calibrated using the supplied 709 x 1536 screenshots. Test it on a non-critical attempt first and make sure the highlighted pieces match the visible shapes before placing them.

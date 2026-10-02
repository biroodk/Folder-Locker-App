# Folder Locker — Android APK

This project keeps the supplied Folder Locker HTML design and adds a native Android vault layer.

## Main changes
- Home page category cards now open dedicated Images, Videos, Music and Documents pages.
- Each category page accepts only its related file types.
- Imported files are copied into the app's private internal vault storage on Android.
- Files remain available after closing and reopening the APK.
- Category pages provide open, rename and delete menus at the top-right of every file card.
- Styled in-app alerts/toasts replace the important browser alerts used by the supplied pages.
- The original colors, animations, wording and page design are preserved as the base UI.
- GitHub Actions builds a debug APK automatically.

## Build
Push this entire folder to a new GitHub repository. The workflow runs on every push to `main` and can also be started manually from Actions.

The APK artifact is named `Folder-Locker-debug`.

# Storage: bundled reference vs. uninstall-surviving user data

## Categories

| Data | Mechanism | Location |
|---|---|---|
| Flashcards + scheduling | JSONL, atomic write | user-selected SAF folder `user/cards/cards.jsonl` |
| Card + history images (owned copies) | files | SAF folder `user/cards/media/`, `scan-history/images/` |
| Confirmed scan history (capped 100) | JSONL | SAF folder `scan-history/history.jsonl` |
| Preferences (translation mode, active model per role) | SharedPreferences | app-private (re-created with defaults) |
| MOE dictionary (reference) | read-only SQLite copy | `assets/dictionary/` bundled in APK |
| MOE stroke order (reference) | SHA-verified JSON | `assets/stroke_order/` bundled in APK |
| Downloaded model weights | app-private, SHA-verified | `noBackupFilesDir/models/<role>/<id>/` |
| Temp camera/crop photos | cache (OS may wipe) | `cacheDir/photos/` |

## Why a user-selected folder

No app-private directory survives Android uninstall: `filesDir`,
`noBackupFilesDir`, and `cacheDir` are all deleted. To keep user data across
`uninstall -> reinstall`, irreplaceable user data lives in a **user-selected
external folder** via the Storage Access Framework (SAF), for example
`Documents/ChineseLearning/`.

- Reference data is reinstallable: reinstalling the APK restores it. It is
  never backed up in full.
- User data is irreplaceable: it stays on disk after uninstall because the
  folder is outside app-private storage.
- On reinstall, the app offers `Find existing data folder`: the user picks
  the same folder again, the app verifies `metadata/root.json` and resumes.
  Persisted URI permissions are re-granted at that moment (uninstall revokes
  grants, but files remain).
- `Delete all data` is explicit, confirmed twice, and wipes the contents of
  the selected folder plus local preferences. It is the only destructive path.

## Layout under every data root

```
reference/
user/cards/cards.jsonl
user/cards/media/
scan-history/history.jsonl
scan-history/images/
media/
import/
export/
metadata/root.json
```

Implementation: `storage/AppDataRoot.kt` (tree URI ownership, access probe,
shared backend instance) and `storage/UserDataRepository.kt` (atomic
`cards.jsonl` + orphan media GC). With no folder selected, the app falls back
to an internal directory so screens keep working.

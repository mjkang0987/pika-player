---
title: Pika Player Privacy Policy
---

# Privacy Policy

**Last updated: August 25, 2026**

Pika Player ("the app") does not collect any personal information.
This document states that fact and explains what the app does on your device.

---

## 1. Information We Collect

**None.**

The app has no server. There are no accounts, no sign-in, and no registration.
No information is transmitted to the developer or to any third party.

The app does not:

- collect, store, or transmit personal information
- display advertising or use the advertising ID (AAID)
- use analytics tools (Google Analytics, Firebase, etc.)
- send usage data, playback history, or crash reports anywhere

---

## 2. What Is Stored on Your Device

The following is stored **only on your device** and never leaves it.

| Stored | Purpose |
|---|---|
| Playback position | Resume where you left off |
| Settings (speed, subtitles, theme, …) | Keep your choices between launches |
| Playlists | Lists you created and the addresses of the videos in them |
| Private folder list | Folders to hide from the library |
| PIN | Unlocking private folders and child lock |

**Your PIN is not stored as the digits you typed.** Only an irreversible
derivation (PBKDF2-HMAC-SHA256, 120,000 iterations) and a random salt are stored.
The stored value does not reveal the PIN, and the developer cannot recover it.

**Uninstalling the app removes all of the above.**

---

## 3. Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_VIDEO` (Android 13+)<br>`READ_EXTERNAL_STORAGE` (Android 12 and below) | To read and display the list of videos stored on your device |

Information read through this permission is used **only to render the list on
screen**. It is not stored or transmitted.

You can use the app without granting this permission. In that case the app reads
only the folder you explicitly choose (Storage Access Framework).

The app does not request the `INTERNET` permission. **It is therefore technically
incapable of sending anything over a network.**

---

## 4. Deleting Files

Deleting a video from the list **removes it from your device.** This cannot be
undone. On Android 11 and above the system asks for confirmation, and the file is
deleted only if you approve.

The app never deletes or moves files without your action.

---

## 5. Note on Private Folders

The private folder feature **hides folders from this app's library**. It does not
encrypt files. Videos in hidden folders remain visible to other apps and to a
connected computer.

---

## 6. Children's Privacy

The app collects no information of any kind, including from children.

---

## 7. Third Parties

There is nothing to share. The app bundles no third-party SDKs.

---

## 8. Changes to This Policy

If new features change how information is handled, this document will be updated
and the date above revised. Significant changes will also be noted in the app's
release notes.

---

## 9. Contact

**snowdrop1202@gmail.com**

---

*[한국어](privacy.md)*

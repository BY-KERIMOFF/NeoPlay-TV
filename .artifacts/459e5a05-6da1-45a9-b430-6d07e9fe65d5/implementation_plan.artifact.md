# Implementation Plan - Admin Scrolling Announcements

The goal is to implement a remote-controlled scrolling text announcement at the top of the player. This will allow the admin to push messages to all users via the `update.json` file.

## Proposed Changes

### [utils]

#### [MODIFY] [DataManager.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/utils/DataManager.java)
- Add a static field `adminAnnouncement` with a getter and setter.

#### [MODIFY] [UpdateManager.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/utils/UpdateManager.java)
- Parse the `"announcement"` field from the server's JSON response and store it in `DataManager`.

### [app]

#### [MODIFY] [activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/layout/activity_player.xml)
- Add a marquee `TextView` at the very top of the layout.
- Use a semi-transparent dark background for better legibility.

#### [MODIFY] [PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/PlayerActivity.java)
- Check `DataManager.getAdminAnnouncement()` during `onCreate`.
- If not empty, show the `tvAnnouncement` and enable the marquee effect.

## Verification Plan

### Manual Verification
- Update the server JSON with an announcement string.
- Open the player and confirm the text scrolls continuously at the top.
- Confirm it works for both M3U and Xtream modes.

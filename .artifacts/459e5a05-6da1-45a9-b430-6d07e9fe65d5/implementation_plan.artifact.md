# Implementation Plan - Netflix-style VOD Grid

The goal is to provide a premium look for the Movies and Series sections by switching from a standard list to a 5-column poster grid (Netflix-style) when a VOD category is selected.

## Proposed Changes

### [app]

#### [NEW] [item_vod.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/layout/item_vod.xml)
- Vertical poster layout with a 2:3 aspect ratio.
- Rounded corners and a glass-morphism selected state.

#### [MODIFY] [adapters/ChannelAdapter.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/adapters/ChannelAdapter.java)
- Support multiple view types (`VIEW_TYPE_LIST` and `VIEW_TYPE_GRID`).
- Implement layout switching logic.

#### [MODIFY] [LiveTvActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/LiveTvActivity.java)
- Implement `setVodMode(boolean)` to toggle between Grid and List layouts.
- Automatically hide the mini-player panel in VOD mode to maximize screen real estate for movie posters.
- Update weights of the UI panels dynamically.

## Verification Plan

### Manual Verification
- Open Live TV: Verify it's still a list with a mini-player on the right.
- Open Movies: Verify it switches to a full-screen 5-column grid with posters.
- Confirm focus navigation (Up/Down/Left/Right) works correctly in the grid.

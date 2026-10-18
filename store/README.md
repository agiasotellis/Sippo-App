# Play Store assets

## Upload

| Play Console field | File |
|---|---|
| Phone screenshots (up to 8) | `play-store/phone/01_friends.png` … `08_themes.png` |
| Feature graphic (1024×500) | `play-store/feature_graphic.png` |
| Promo video (YouTube URL) | upload `video/sippo_promo_landscape.mp4` to YouTube, paste the link |

The phone screenshots are 1080×1920 (9:16), which meets Google's size rules. They are also eligible for the "promoted on Play" placements. There are 11 frames; Play shows up to 8. `09_goal`, `10_reminders` and `11_plus` are spares if you'd rather swap one in.

The Play Store only accepts a promo video as a YouTube link, so upload the landscape (16:9) version to YouTube; it can be unlisted. `sippo_promo_portrait.mp4` is the same cut at 9:16 for Shorts, Reels or TikTok. The music is generated in code, so there's no licence to worry about.

## Regenerating

1. Run the **Store assets** workflow from the Actions tab and choose what to capture. It runs `StoreCapture` on an emulator with demo data and pushes the raw screenshots and recording to the `store-captures` branch.
2. Compose:

   ```
   git fetch origin store-captures && git worktree add /tmp/captures origin/store-captures
   cd store/tools && npm i playwright
   node render.mjs /tmp/captures/screenshots ../play-store/phone        # frames + feature_graphic.png (move it up one level)
   node cards.mjs video.json /tmp/cards /path/to/outro.png
   python3 make_video.py /tmp/captures/video /tmp/cards ../video         # needs ffmpeg + numpy
   ```

Captions and colours live in `tools/frames.json` (screenshots) and `tools/video.json` (video segments, synced to the tour's timeline marks).

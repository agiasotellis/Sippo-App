"""Assembles the promo video from the emulator tour recording, caption cards and generated music.

usage: python3 make_video.py <captures/video dir> <cards dir> <out dir>
Writes sippo_promo_landscape.mp4 (1920×1080) and sippo_promo_portrait.mp4 (1080×1920).
"""
import json
import os
import subprocess
import sys
import wave

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
FPS = 30
XFADE = 0.35
INTRO, OUTRO = 2.6, 4.2


def run(cmd):
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def timeline(video_dir):
    marks = {}
    with open(os.path.join(video_dir, "timeline.tsv")) as f:
        for line in f:
            t, label = line.rstrip("\n").split("\t")[:2]
            if t != "start":  # first line is "start <t0>"
                marks[label] = int(t)
    return marks


def music(path, seconds, sr=44100):
    """Soft, bright pluck arpeggios over a warm pad: C – G – Am – F at 100 bpm."""
    beat = 60 / 100
    n = int(seconds * sr)
    out = np.zeros(n)
    t_note = np.arange(int(sr * beat * 2)) / sr
    chords = [[60, 64, 67, 72], [55, 59, 62, 67], [57, 60, 64, 69], [53, 57, 60, 65]]
    hz = lambda m: 440 * 2 ** ((m - 69) / 12)

    def pluck(f, amp):
        env = np.exp(-t_note * 5.5) * np.minimum(1, t_note / 0.004)
        return amp * env * (np.sin(2 * np.pi * f * t_note) + 0.35 * np.sin(4 * np.pi * f * t_note) + 0.12 * np.sin(6 * np.pi * f * t_note))

    bar = beat * 4
    pattern = [0, 2, 1, 3, 2, 1, 3, 2]  # eighth-note arpeggio
    i = 0
    while i * bar < seconds:
        chord = chords[i % 4]
        start = int(i * bar * sr)
        # pad
        pad_len = int(bar * sr)
        tt = np.arange(pad_len) / sr
        pad_env = np.minimum(1, tt / 0.6) * np.minimum(1, (bar - tt) / 0.6)
        pad = sum(np.sin(2 * np.pi * hz(m - 12) * tt) for m in chord[:3]) * 0.045 * pad_env
        end = min(n, start + pad_len)
        out[start:end] += pad[: end - start]
        for k, idx in enumerate(pattern):
            s = start + int(k * beat / 2 * sr)
            note = pluck(hz(chord[idx] + 12), 0.11 if k % 2 == 0 else 0.08)
            e = min(n, s + len(note))
            if s < n:
                out[s:e] += note[: e - s]
        # soft bass on beat 1 and 3
        for b in (0, 2):
            s = start + int(b * beat * sr)
            f = hz(chord[0] - 24)
            bass = 0.16 * np.exp(-t_note * 3) * np.minimum(1, t_note / 0.01) * np.sin(2 * np.pi * f * t_note)
            e = min(n, s + len(bass))
            if s < n:
                out[s:e] += bass[: e - s]
        i += 1
    fade = int(2.5 * sr)
    out[-fade:] *= np.linspace(1, 0, fade)
    out[: int(0.4 * sr)] *= np.linspace(0, 1, int(0.4 * sr))
    out = out / max(1e-6, np.abs(out).max()) * 0.6
    stereo = np.repeat((out * 32767).astype(np.int16)[:, None], 2, axis=1)
    with wave.open(path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(stereo.tobytes())


def main(video_dir, cards, out_dir):
    spec = json.load(open(os.path.join(HERE, "video.json")))
    layouts = json.load(open(os.path.join(cards, "layouts.json")))
    marks = timeline(video_dir)
    src = os.path.join(video_dir, spec["source"])
    seg0 = spec["segStart"]
    tmp = os.path.join(out_dir, "parts")
    os.makedirs(tmp, exist_ok=True)

    for key, L in layouts.items():
        hole = L["hole"]
        parts = []

        def still(png, dur, name):
            p = os.path.join(tmp, f"{key}_{name}.mp4")
            run(["ffmpeg", "-y", "-loop", "1", "-t", str(dur), "-i", png, "-vf", f"fps={FPS},format=yuv420p",
                 "-c:v", "libx264", "-crf", "16", "-preset", "medium", p])
            parts.append((p, dur))

        still(os.path.join(cards, f"{key}_intro.png"), INTRO, "intro")
        for i, s in enumerate(spec["segments"]):
            a = (marks[s["from"]] - seg0) / 1000 + s.get("skipStart", 0.3)
            b = (marks[s["to"]] - seg0) / 1000
            pieces = [(a, b, s["speed"])]
            if s.get("tailNormal"):
                pieces = [(a, b - s["tailNormal"], s["speed"]), (b - s["tailNormal"], b, 1.0)]
            for j, (pa, pb, sp) in enumerate(pieces):
                dur = (pb - pa) / sp
                p = os.path.join(tmp, f"{key}_{i:02d}_{j}.mp4")
                card = os.path.join(cards, f"{key}_{i:02d}.png")
                fc = (
                    f"[0:v]setpts=(PTS-STARTPTS)/{sp},fps={FPS},scale={hole['w']}:{hole['h']}:flags=lanczos[v];"
                    f"color=c=white:s={L['w']}x{L['h']}:r={FPS}:d={dur:.3f}[bg];"
                    f"[bg][v]overlay={hole['x']}:{hole['y']}:shortest=1[m];"
                    f"[m][1:v]overlay=0:0:shortest=1,format=yuv420p[o]"
                )
                run(["ffmpeg", "-y", "-ss", f"{pa:.3f}", "-to", f"{pb:.3f}", "-i", src, "-loop", "1", "-i", card,
                     "-filter_complex", fc, "-map", "[o]", "-t", f"{dur:.3f}", "-c:v", "libx264", "-crf", "16",
                     "-preset", "medium", "-r", str(FPS), p])
                parts.append((p, dur))
        still(os.path.join(cards, f"{key}_outro.png"), OUTRO, "outro")

        # Crossfade everything together.
        inputs, fc, last, offset = [], [], "0:v", 0.0
        for k, (p, d) in enumerate(parts):
            inputs += ["-i", p]
        for k in range(1, len(parts)):
            offset += parts[k - 1][1] - XFADE
            # Pieces of the same segment cut straight through; new segments crossfade.
            trans = "fade"
            label = f"x{k}"
            fc.append(f"[{last}][{k}:v]xfade=transition={trans}:duration={XFADE}:offset={offset:.3f}[{label}]")
            last = label
        total = sum(d for _, d in parts) - XFADE * (len(parts) - 1)
        wav = os.path.join(tmp, f"music_{key}.wav")
        music(wav, total)
        out = os.path.join(out_dir, f"sippo_promo_{'landscape' if key == 'land' else 'portrait'}.mp4")
        run(["ffmpeg", "-y", *inputs, "-i", wav, "-filter_complex", ";".join(fc), "-map", f"[{last}]",
             "-map", f"{len(parts)}:a", "-c:v", "libx264", "-crf", "18", "-preset", "slow", "-pix_fmt", "yuv420p",
             "-c:a", "aac", "-b:a", "160k", "-shortest", "-movflags", "+faststart", out])
        print(f"wrote {out} ({total:.1f}s)")


if __name__ == "__main__":
    main(*sys.argv[1:4])

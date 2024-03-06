# mari

>osu! utility lib in kotlin to manipulate replays and judgement data in a safe and performant way.

This module allows [nise.moe](https://nise.moe) to juggle a ton of replays and data around.

# Usage

### Compress / decompress judgement data

The `Judgement` data class ought to represent the way a player has played a specific beatmap. It contains the hit error, distance, etc for each hit object. The structure is based off the Circleguard `Investigations.judgements` return type.

You can use `CompressJudgements.compress` and `CompressJudgements.decompress` to losslessly store and retrieve judgement data. According to my estimates, the compressed data is about 33% the size of the original data.

### Decode a replay

>This method is fundamentally written with the assumption that it'll be used on user-provided replays. It is thus designed to be safe and to not crash on invalid replays.

`OsuReplay` allows you to safely decode an `.osr` file. Once you've parsed the file, you can instantiate a new class:

```kotlin
val replay = OsuReplay(replayFile.bytes)
```

If everything goes well, you can then start reading the replay data.

```kotlin
println(replay.playerName) // mrekk
```

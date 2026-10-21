# Void Slime

<img src="art/slime-icon-512.png" width="160" align="right" alt="Void Slime emblem">

A 3D slime sandbox for Android (13+), from the VOID family: squish it, make it, sell it. Kotlin +
Jetpack Compose with a hand-written OpenGL ES 2 renderer and soft-body physics, no third-party libraries,
no ads, no accounts. It goes online only to check for its own updates.

## Play
The slime is a real soft body (2,562 points on an icosphere held together by springs, a shape memory
and a volume it tries to keep), so it dents, bulges, jiggles and slowly flows back.
- **Poke**: press in and it dents under your finger, with a squelch and a little buzz.
- **Stretch**: grab and pull; pull too far and it *snaps*.
- **Squish**: press flat with your palm and it bulges out the sides.
- **Roll**: a rolling pin flattens it into a sheet.
- **Bubbles**: blow bubbles out of it and pop them.

Each base feels different: Glossy, Clear and Butter to start, then Cloud, Jelly, Icee (it crunches),
Chrome, Holo and Glow (turn the lights off) from the supply shop.

## The lab
Five steps: pick a **base**, up to three of 24 **colours** (they swirl rather than blend), **mix-ins**
(foam beads, sprinkles, stars, hearts, pearls, fishbowl beads, sugar crystals, fruit slices,
snowflakes, confetti) and **glitter**, then pour in the activator and **stir** it until the runny
puddle comes together into slime. Name it and it goes into a jar on your shelf.

## And more
- **Shelf**: every slime you've made, in a jar that shows its colours and mix-ins; sort by newest,
  favourites or A–Z, play with any of them again.
- **Slime shop**: customers ask for things ("a glossy slime with stars!"); make it or bring one from the
  shelf and earn coins (up to three stars for getting it right). Spend them on supplies and new dishes
  (marble, bubblegum, mint, wood, neon).
- **Bubble rush**: pop as many bubbles as you can in 45 seconds; golden ones are worth 5.
- **Stickers**: 23 to collect for poking, stretching, making and selling, each with a coin reward.
- **A daily gift**: coins, a mystery jar and sometimes a new supply.
- Levels as you play, which bring harder (better paid) orders.

## Feel
Every sound (squelches, pops, crunches, the ambient pads) is synthesized in code, and every touch has
matching haptics; both can be turned down or off in Settings. The seven VOID themes are there too, and
the app icon can follow the theme.

## Updates
Settings › Updates shows the version and checks this repository's GitHub releases (and does so by itself twice a
day, with a notification when there's something new). The download is checked against the published SHA-256
before Android's installer takes it. Void Slime is also in Void Store; both carry the same signed APK, so
either one can update the other's install.

## Build
```
./build.sh testDebugUnitTest assembleRelease   # → VoidSlime.apk
```
Needs `../void-ui` next to it. `./release.sh "What changed"` bumps the version and publishes to Void Store and
GitHub releases. `art/gen_icon.py` regenerates the launcher icons for every theme.

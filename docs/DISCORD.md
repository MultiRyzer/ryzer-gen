# Discord setup

A setup sheet for the Ryzer Gen Discord, and the posts to paste into it. Work top to bottom; it takes about 20 to 30 minutes. Built on current Discord features (Community mode, Onboarding, the Server Guide, forum channels, AutoMod), so no reaction-role bot is needed.

The idea: start small (about 11 channels), since a server full of empty channels feels dead. Add more only when people ask for them.

## 1. Server settings

1. **Server Settings > Enable Community.** Discord asks for a rules channel and an updates channel; pick `#rules` and `#mod-log` (made in step 3; you can come back). This unlocks announcement channels, the welcome screen, Onboarding, the Server Guide and Insights.
2. **Server Settings > Safety Setup:**
   - Verification level: **High** (email verified, and a member for 10 minutes before they can post). "Highest" (phone) stops more spam but puts off younger players; move to it only if raids start.
   - Explicit media filter: scan media from **all members**.
   - Raid protection and activity alerts: **on**, alerts to `#mod-log`.
   - DM spam filter: **on**.
3. **Server Settings > Safety Setup > AutoMod:**
   - Block mention spam: **on**, 5 mentions, and time the member out.
   - Block suspected spam content: **on**.
   - Commonly flagged words: **on** (profanity optional; slurs and sexual content yes).
   - Custom keywords: add `free nitro`, `nitro gift`, `steam gift`, `discord.gift`, `@everyone http` (the usual scam lines). Action: block the message and alert `#mod-log`.
4. **Invite:** Invite People > Edit invite link > Expire after **Never**, max uses **No limit**. Put this link everywhere (step 8).

## 2. Roles

Top to bottom in the role list (higher roles override lower ones):

| Role | Colour | Permissions | Who |
|---|---|---|---|
| **Ryzer** | the mod's cyan (`#35C8F5`) | Administrator | you |
| **Moderator** | orange (`#FF8A1E`) | Timeout, Kick, Manage Messages, Manage Threads, View Audit Log, Mention @everyone off. No Administrator, no Manage Roles | trusted regulars, later |
| **Contributor** | green (`#44D65E`) | none extra | people who help the mod: bug hunters, tool makers (u/MushroomMan234 is the first), translators |
| **Pack Maker** | none | none extra | self-assigned in Onboarding |
| **Player** | none | none extra | self-assigned in Onboarding |
| **Updates** | none | none extra | self-assigned: pinged for releases |
| **Dev Log** | none | none extra | self-assigned: pinged for new WIP posts |

Display Contributor and Moderator separately in the member list ("Display role members separately"). Ping roles (Updates, Dev Log) must be mentionable by you only: leave "Allow anyone to @mention this role" **off**.

## 3. Channels

| Category | Channel | Type | Who can post | Topic line |
|---|---|---|---|---|
| **INFO** | `#rules` | Text | you only | The rules. Short, read them. |
| | `#announcements` | Announcement | you only | Releases and news. Follow it to get posts in your own server. |
| | `#roadmap` | Text | you only | What's done, what's next. |
| **COMMUNITY** | `#general` | Text | everyone | Talk Ryzer Gen, reactors and modded Minecraft. |
| | `#showcase` | Forum (gallery view) | everyone | Show your stations, reactors and bases. |
| | `#core-designs` | Forum | everyone | Share and rate fission core layouts. |
| **SUPPORT** | `#help` | Forum | everyone | Questions and problems. One post per question. |
| | `#suggestions` | Forum | everyone | Ideas for the mod. One idea per post. |
| **DEVELOPMENT** | `#dev-log` | Text | you only; everyone can reply in threads | Work in progress, straight from the workshop. |
| **STAFF** (private) | `#mod-log` | Text | Moderator and up | AutoMod and raid alerts. |
| | `#staff` | Text | Moderator and up | Mod discussion. |

Settings per channel:
- **Read-only channels** (`#rules`, `#announcements`, `#roadmap`, `#dev-log`): deny @everyone *Send Messages* and *Add Reactions* stays allowed. For `#dev-log`, also allow *Send Messages in Threads* and *Create Public Threads*, so people can reply to a WIP post in its own thread.
- **`#showcase` (Forum):** Media channels need boosts, so a Forum with the default layout set to **Gallery view** stands in: each post's first image is its thumbnail, and each build gets its own thread for comments. Tags `Microreactor`, `Fission Station`, `Fuel Cycle`, `Dyson Swarm`, `Base`. Require a tag. Post guidelines: "Show off your reactors, stations and bases. Put your best screenshot first: it becomes the thumbnail. Tag it, and say what we're looking at."
- **`#core-designs` (Forum):** tags `Uranium`, `MOX`, `Overdrive`, `Max Power`, `Economy`, `Target Rods`. Require a tag. Default layout: gallery.
- **`#help` (Forum):** tags `Question`, `Bug?`, `Compatibility`, `Solved`. Require a tag. Default sort: latest activity. Auto-hide after 1 week.
- **`#suggestions` (Forum):** tags `Idea`, `Planned`, `Not Planned`, `Done`. Members pick `Idea`; only you move posts to the others (set the other three to "Only moderators can apply").
- **Slow mode:** `#general` 5 s once it gets busy; not needed at first.

## 4. Onboarding

**Server Settings > Onboarding.**

- **Default channels:** `#rules`, `#announcements`, `#roadmap`, `#general`, `#showcase`, `#core-designs`, `#help`.
- **Question 1 (required, one answer):** *What brings you here?*
  - I play with Ryzer Gen: gives **Player**
  - I make modpacks: gives **Pack Maker**, and shows `#suggestions`
  - Just following the development: gives **Dev Log**
- **Question 2 (optional, several answers):** *Want to be pinged?*
  - New releases: gives **Updates**
  - New work-in-progress posts: gives **Dev Log**
- **Server Guide:** welcome message (post 2 below), and to-dos:
  1. Read the rules: `#rules`
  2. Say hi: `#general`
  3. See what's coming: `#roadmap`
  4. Show off a build: `#showcase`
- **Resource pages** in the Server Guide: "Getting started" (post 7) and "Links" (Modrinth, CurseForge, GitHub, the calculator).

## 5. Release posts from GitHub (optional, no bot)

1. In `#announcements`: Edit Channel > Integrations > Webhooks > New Webhook. Name it "Ryzer Gen Releases", give it the mod's icon, copy the URL.
2. On GitHub: MultiRyzer/ryzer-gen > Settings > Webhooks > Add webhook.
   - Payload URL: the Discord URL with `/github` on the end.
   - Content type: `application/json`.
   - Events: "Let me select individual events" > **Releases** only.
3. Each GitHub release then posts to `#announcements`. Ping @Updates yourself with a short handwritten post; the webhook can't ping.

## 6. Bots

None needed to start: Onboarding does roles, AutoMod does moderation, the webhook does releases. If the server grows past a few hundred members, add one moderation and logging bot (Carl-bot or Dyno) for message edit and delete logs in `#mod-log`.

## 7. Later

- **Server Tag:** a 4-letter badge members show next to their name (for example `RGEN` with the reactor icon). Needs 3 server boosts. Worth it once the community is there.
- **Moderators:** pick one or two regulars who help in `#help` without being asked.
- **Voice channel:** only when people ask for one.

## 8. Put the invite everywhere

The permanent invite: **https://discord.gg/cmUZcRtqfg** (already in the README, `docs/MOD-PAGE.md` and the in-game mod description).

- Modrinth and CurseForge: the Discord link field in the project settings.
- The Reddit post: edit it, and the pinned comment.
- The GitHub README and `docs/MOD-PAGE.md`.
- The mod's metadata (`neoforge.mods.toml`), so it shows in the in-game mod list.

---

# Posts to paste

## 1. `#rules`

> **Welcome to the Ryzer Gen Discord!**
>
> 1. **Be decent.** No harassment, hate or personal attacks. Disagree with ideas, not people.
> 2. **Keep it family friendly.** No NSFW, gore or shock content.
> 3. **No spam or self-promotion** outside `#showcase`. Pack makers: sharing your pack that includes Ryzer Gen is welcome in `#showcase`.
> 4. **Help goes in `#help`,** one post per problem, with your versions and a log. Confirmed bugs go to [GitHub issues](https://github.com/MultiRyzer/ryzer-gen/issues) so nothing gets lost.
> 5. **No piracy, cheats or leaked content.**
> 6. **Don't ping the developer or mods for help.** Someone will get to your post.
> 7. **English in the main channels,** so everyone can follow and moderate.
>
> Moderators may remove anything that breaks the spirit of these rules. Discord's [Terms](https://discord.com/terms) and [Community Guidelines](https://discord.com/guidelines) apply too.

## 2. Welcome message (Server Guide)

> Welcome to **Ryzer Gen**, power progression grounded in real physics: a portable microreactor, a real fuel cycle, a fission station you design channel by channel, and one day, a Dyson swarm round the sun.
>
> Grab your roles in Channels & Roles, show off your builds in #showcase, and if you're stuck, #help has you covered.

## 3. `#announcements`: the first post

> **Welcome, everyone!** 👋
>
> The Reddit post blew up, so here's a home for it. Thank you all for the support, the ideas and the brutal honesty.
>
> **Ryzer Gen 0.1.1-alpha is out** on [GitHub](https://github.com/MultiRyzer/ryzer-gen/releases/tag/v0.1.1-alpha) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ryzer-gen) (Modrinth is still in review):
> - The reactors talk: an emergency announcer calls out SCRAMs, coolant loss and meltdowns
> - The Flow Scanner shows what flows into every pipe and cable nearby
> - The fission station got a whole new look, with its core glowing through the glass
> - A first look at the Dyson swarm, with a creative-only controller
>
> **Coming in 0.1.2:** Hold Shift for details on every item, a tidier creative tab, and two fission station fixes. Coolant is now properly local, and a SCRAM trips the turbine, so overloading a core is no longer free. Both were found by u/MushroomMan234, whose [Fission Station Calculator](https://moddecoded.com/tools/ryzer-gen/fission-station-calculator/) simulates the station tick by tick. Go try it and see how close to 100% you can get.
>
> Want release pings? Pick the **Updates** role in Channels & Roles.

## 4. `#roadmap`

> **Where Ryzer Gen is going**
>
> ✅ **Tier 1: the microreactor.** Portable reactor, battery rack, pipes, steam
> ✅ **Tier 2: the fuel cycle.** Crack cores, reprocess, make MOX
> ✅ **Tier 3: the fission station.** Design the core channel by channel
> 🔧 **Bridge to fusion.** Tritium from lithium target rods, heavy water, palladium from waste, superconducting magnets, tungsten
> 📋 **Tier 4: breeder reactor.** Sodium-cooled, burns what the station leaves behind
> 📋 **Tier 5: fusion.** A tokamak built and fuelled by fission
> 📋 **Tier 6: the Dyson swarm.** Launch sails round a walkable sun, beam the power home
>
> Also planned: **steady state**, a bonus for reactors that run without downtime, rewarding good automation (up to 1.5x for the microreactor, 2x for fission, more for later tiers).
>
> The full roadmap lives on [GitHub](https://github.com/MultiRyzer/ryzer-gen/blob/main/docs/ROADMAP.md).

## 5. `#help`: forum guidelines (Edit Channel > Post Guidelines)

> **One post per problem.** Include:
> - Ryzer Gen version (e.g. 0.1.1-alpha) and NeoForge version
> - Your modpack, if any (ATM10, etc.)
> - What you expected, and what happened instead
> - For crashes: your log via [mclo.gs](https://mclo.gs), not a screenshot
>
> Tag it, and mark it **Solved** when it is. Confirmed bugs go to [GitHub issues](https://github.com/MultiRyzer/ryzer-gen/issues).

## 6. `#core-designs`: forum guidelines

> Share a fission core layout: a screenshot of the control screen, or a link from the [Fission Station Calculator](https://moddecoded.com/tools/ryzer-gen/fission-station-calculator/) (made by u/MushroomMan234).
>
> Say what it's for (max power, economy, MOX, breeding tritium) and its power and rating. The planner rates against the best layout the mod's own search found, and 100% isn't the ceiling.

## 7. Server Guide resource page: "Getting started"

> **New to Ryzer Gen?**
> 1. Find **uranium** and **lead** (they generate like other ores; the creative tab lists everything).
> 2. Build an **Alloy Smelter** for steel. It burns furnace fuel, no power needed.
> 3. Build a **microreactor**: a Reactor Heart, 2 Reactor Machine Units and a Coolant Jacket. Hold a part to see where the others go.
> 4. Feed it water from an **Intake Pump** and it makes more power (and steam).
> 5. When its core runs out, the **fuel cycle** begins: crack, reprocess, fabricate.
>
> Hold **Shift** over any Ryzer Gen item for details.

## 8. `#suggestions`: forum guidelines

> One idea per post, and search first in case it's already there. Say what you'd like and why it would make the mod better. I tag posts **Planned**, **Not Planned** or **Done** as I go through them. The mod aims to be somewhat accurate, so ideas grounded in real physics and engineering have the best odds.

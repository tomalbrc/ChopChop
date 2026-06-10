# ChopChop

ChopChop is a serverside Fabric mod that adds a Tree Feller enchantment available from the enchantment table or villager trades.

When you mine a log with an axe enchanted with Tree Feller, the entire tree is destroyed in one go. All logs, leaves, vines, bee nests, cocoa beans, and other attached blocks break together and drop their usual items. 

## What trees work

The mod supports five distinct tree types out of the box. Each type has its own detection logic and configurable settings.

- Generic trees: oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry. The mod scans for connected logs, then finds all leaves within a configurable radius around those logs.
- Vertical plants: cactus and bamboo. Doesn't have much effect when the animation option is disabled.
- Chorus plants: the entire chorus tree structure including chorus flowers.
- Red mushrooms: the stem and the red mushroom cap.
- Brown mushrooms: the stem and the brown mushroom cap.

Custom tree types can be added in the mod's config.

## Default behavior without configuration

Tree Feller works on all five tree types above. 
When you mine a log with the enchanted axe:

- The mod scans for connected logs up to a maximum of 256 logs.
- For generic trees, leaves within 7 blocks of any log are destroyed. Player placed leaves are ignored.
- The tree either falls with an animation that lasts 60 ticks (3 seconds) by default or breaks block-by-block similar to Hytale.
- Tool durability is reduced by the number of blocks mined.

To break a single log, sneak while mining.

## Configuration

The mod generates `config/chopchop.json`. 

Options:

- `animated` (default false): When true, the tree falls as a single animated entity. When false, blocks break one by one sequentially.
- `animation_duration` (default 60): How many ticks the falling animation lasts. Only used when `animated` is true.
- `speed_multiplication` (default 2.0): Mining speed multiplier applied per additional log.
- `max_speed_multiplication` (default 512.0): Upper limit for the speed multiplier.
- `configs`: A list of per tree type settings. You can disable a tree type, change the required tool, or adjust the block filters.

Each entry in `configs` corresponds to a tree type (`generic`, `vertical`, `chorus`, `red_mushroom`, `brown_mushroom`). 
Available fields per type:

- `enabled` (default true): Whether this tree type can be felled.
- `requireTool` (default true for most, false for chorus): If true, the mod checks the `allowedToolFilter` before felling.
- `allowedToolFilter` (default requires the Tree Feller enchantment): An item predicate that determines which tools work. You can change this to require a specific material or any axe without the enchantment.
- `logFilter` (default depends on type): A block predicate that defines what counts as a log or stem.
- `leavesFilter` (optional): A block predicate for leaves or mushroom caps. If absent, leaves are not destroyed.
- `algorithm` (for generic type only): Contains `maxLeavesRadius` (default 7), `maxLogAmount` (default 256), and `shouldIgnorePersistentLeaves` (default true).


## Dependencies

Fabric API and Polymer are required on the server.

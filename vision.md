# InnerNet & HumanityTree – Vision & Technical Blueprint

## Purpose

InnerNet turns the internet from a consumption-driven distraction machine
into a **self-realization engine**.

HumanityTree provides the **decentralized index** for discovering talent,
missions, skills and collaborators — **without servers, without surveillance,
without addiction-loops**.

The world doesn’t need more “social media”.
It needs a **meaningful way for humans to evolve together**.

All *structured data examples* in this document use **SML**, not JSON.
SML is the human-readable core language of the system and SHOULD be used
everywhere instead of JSON whenever possible.

---

## High-Level Architecture

- **InnerNet (client-side app)**
  - Kotlin + Compose Web (SPA)
  - UI & content described in **SML**
  - State stored locally (browser or native) in **SML format**
  - Optional export of a **public SML profile** to IPFS

- **HumanityTree (decentralized index)**
  - Global, append-only, content-addressed **Index Tree** on IPFS
  - Structure stored as SML documents
  - Root referenced via **IPNS**
  - Users pin:
    - Genesis node
    - Their own Leaf node(s)
    - Their own public profile SML

No central server is REQUIRED for the core logic.

---

## PHASE 1 – HumanityTree Core

> **First implementation task for Codex:**  
> Implement HumanityTree core (data structures + write/update logic) using SML on IPFS.

### 1.1 Goals

- Represent a **global index of tags / #indices** as a **tree of SML documents**.
- Support efficient lookup of:
  - `#tags` → list of profile CIDs
- Allow **insertion** of new tag/profile references using a **copy-on-write tree**:
  - All nodes and leaves are immutable SML files on IPFS.
  - Updates create new versions (new CIDs).
  - A single IPNS name always points to the **current root**.

### 1.2 Basic Concepts

- **Leaf**: SML document containing up to `maxEntries` tag→profiles mappings.
- **InnerNode**: SML document routing ranges of tags to children (binary or B-Tree-like).
- **Root**: top node of the tree (may be InnerNode or Leaf for small trees).
- **GenesisRecord**: SML document describing HumanityTree, schemas, and root pointer(s).
- **IPNS Root Name**: a single IPNS name that always points to the current root CID.

### 1.3 SML Schemas (Conceptual)

#### 1.3.1 Leaf Node (SML)

A Leaf stores mappings from a tag to one or more profile CIDs.

```sml
Leaf {
    type: "Leaf"
    version: 1
    maxEntries: 128

    Entry {
        tag: "#GuitaristSuchtBand"
        Profile cid: "bafyProfileCid1"
        Profile cid: "bafyProfileCid2"
    }
    Entry {
        tag: "#SucheNachDingsbums"
        Profile cid: "bafyProfileCid7"
    }
    PrevCid: "bafyPrevLeafCid"    // # or "" if none
    NextCid: "bafyNextLeafCid"    // # or "" if none
}
```

Notes:

- `Entry` can appear multiple times per Leaf.
- Inside `Entry`, `Profile` can appear multiple times (one CID per profile).
- Leaves are **double-linked** via `Prev`/`Next` for range scans.

---

#### 1.3.2 Inner Node (SML, binary version)

A simple binary inner node that splits entries by a `splitKey` (lexicographic tag string).

```sml
InnerNode {
    type: "InnerNode"
    version: 1
    splitKey: "#M"

    LeftCid: "bafyLeftChildCid"
    RightCid: "bafyRightChildCid"
}
```

Semantics:

- All tags `< splitKey` are stored somewhere in the left subtree.
- All tags `>= splitKey` are stored somewhere in the right subtree.

Optional later: upgrade to a broader B-Tree-style node with multiple keys:

```sml
InnerNode {
    type: "InnerNode"
    version: 2

    KeyValue: "#D"
    KeyValue: "#M"
    KeyValue: "#T"

    ChildCid: "bafyChild0"
    ChildCid: "bafyChild1"
    ChildCid: "bafyChild2"
    ChildCid: "bafyChild3"
}
```

---

#### 1.3.3 HumanityTree Genesis Record (SML)

The GenesisRecord defines the tree, schemas and basic rules.

```sml
GenesisRecord {
    type: "HumanityTreeGenesis"
    version: 1
    createdAt: "2025-11-23T00:00:00Z"

    Description: "Genesis node of HumanityTree – a decentralized index of human potential, profiles and projects."

    Project {
        id: "innerNet"
        description: "Self-realization & co-creation network."
        schemaVersion: 1
        leafSchemaDescriptionCid: "cid-of-leaf-schema-sml"
        publicProfileSchemaDescriptionCid: "cid-of-public-profile-schema-sml"
    }
    Rules {
        PinningRequired: "genesisNode"
        PinningRequired: "ownLeaf"
        PinningRequired: "ownPublicProfile"

        IdentityRecommendedType: "publicKey"
        IdentityNote: "Nicknames are for display only. Identity is anchored in keys or DIDs."
    }
    Stats {
        estimatedUsers: 1
        updatedAt: "2025-11-23T00:00:00Z"
        note: "Stats are approximate and may be updated by publishing a new GenesisRecord CID."
    }
}
```

GenesisRecord itself is immutable. New versions are created over time and can be pointed to by a higher-level descriptor or separate IPNS name if needed.

---

### 1.4 Root and IPNS

Two options, Codex can pick the simpler one for MVP:

#### Option A – IPNS points directly to Root Node

- IPNS name: `humanitytree-root` (or similar).
- Value: CID of the current Root node (InnerNode or Leaf).

On each update:

1. New Leaf(s) / InnerNode(s) created.
2. New Root is constructed (copy-on-write up the path).
3. IPNS name is updated to the new Root CID.

#### Option B – IPNS points to RootDescriptor (optional later)

A small SML wrapper that contains the current root CID and meta info:

```sml
RootDescriptor {
    type: "HumanityTreeRootDescriptor"
    version: 1

    CurrentRoot cid: "bafyRootCidV3"
    CreatedAt: "2025-11-23T01:23:45Z"
    Note: "Can reference GenesisRecord and schemas."
}
```

---

### 1.5 Insert Algorithm (Conceptual)

High-level description for Codex:

Input: `tag` (string like "#GuitaristSuchtBand"), `profileCid` (string)

1. Resolve IPNS to current Root CID.
2. Load Root node (InnerNode or Leaf).
3. Traverse down:
   - If InnerNode:
     - Compare `tag` with `splitKey` (or keys array).
     - Follow appropriate child (Left/Right or Child index).
   - If Leaf:
     - Stop traversal.

4. On Leaf:
   - If Leaf has capacity (`Entry count < maxEntries`):
     - Clone Leaf into a new Leaf SML structure.
     - Add or update `Entry` for the `tag`:
       - If tag exists, append new `Profile cid`.
       - If not, create new `Entry` with this tag and profile.
     - Write new Leaf SML to IPFS → get `leafNewCid`.
   - Else (Leaf full, needs split):
     - Combine existing entries + new entry.
     - Sort by `tag`.
     - Split entries into two halves: `entriesLeft`, `entriesRight`.
     - Create `LeafLeft` and `LeafRight` SML docs.
     - Set `Prev`/`Next` pointers:
       - `LeafLeft.Next = LeafRight`
       - `LeafRight.Prev = LeafLeft`
       - If original Leaf had neighbors, update them accordingly (new versions) to maintain double-linked chain.
     - Write both Leaves → `leafLeftCid`, `leafRightCid`.

5. Propagate changes up the tree (copy-on-write):
   - For each parent node on the path:
     - Clone parent as new SML node.
     - Replace old child CID with new one (or two, if split occurred).
     - If parent overflows (too many children/keys), split parent similarly.
   - Continue until a new Root is formed.

6. Write new Root node → `rootNewCid`.

7. Update IPNS:
   - Publish IPNS update: `humanitytree-root` → `rootNewCid`.

Important: old CIDs remain valid. The tree is **persistent** and fully versioned.

---

### 1.6 Query / Lookup

To find all profiles for a tag `#GuitaristSuchtBand`:

1. Resolve IPNS → `rootCid`.
2. Traverse down via InnerNodes (binary or B-Tree-like).
3. Find the Leaf that contains the tag range.
4. Scan Leaf’s `Entry` elements.
5. Return `Profile cid` entries for the given tag.

Range queries (e.g. all tags between `#A` and `#K`) can use the double-linked Leaves via `Prev`/`Next`.

---

## PHASE 2 – InnerNet MVP (Offline Daily Avatar Coach)

> **Second implementation task for Codex:**  
> Implement InnerNet MVP (Daily Avatar Coach) as a single-page Compose Web app using SML for UI and state description.

### 2.1 Core UX

Daily loop:

1. **Question of the day** (Home Screen):
   - “Which question will guide you today?”
   - Buttons:
     - “Who do you want to be today?”
     - “What do you truly want?”
     - “What do you want to learn today?”
     - “Surprise me”

2. **Avatar selection** (Who do you want to be today?):
   - User chooses one of several Avatars:
     - Visionary – *Build the future others can’t yet see.*
     - Creator – *Turn ideas into reality.*
     - Musician – *Express the soul through sound.*
     - Rebel – *Break the old system.*
     - Explorer – *Experience life beyond comfort.*

   - Avatar is **not** just a label; user can also **visually assemble** an avatar:
     - gender expression (male/female/neutral/other)
     - hair color, hair length
     - eye color
     - skin tone
     - basic outfit / style
   - These visual attributes are also stored in SML as part of the avatar definition (see below).

3. **Daily Quests Screen**:
   - Show 3–5 suggested mini-quests.
   - User selects or confirms 3 for today.
   - Each completed quest adds XP to current Avatar.

4. **Reflection Screen (optional)**:
   - Short question: “What did you learn today?”
   - Free text, +1 XP bonus.

Everything is stored in local SML (localStorage or file).

---

### 2.2 InnerNet State Model (SML-Based)

Example of how the state for one user could be stored in SML:

```sml
InnerNetState {
    Version: 1

    Profile {
        Nickname: "VisionSeeker"
    }
    Avatar {
        id: "visionary"
        level: 2
        xp: 22

        Visual {
            gender: "neutral"
            hairColor: "brown"
            hairLength: "long"
            eyeColor: "green"
            skinTone: "light"
            style: "casual"
        }
    }
    Avatar {
        id: "creator"
        level: 1
        xp: 0

        Visual {
            gender: "male"
            hairColor: "black"
            hairLength: "short"
            eyeColor: "blue"
            skinTone: "medium"
            style: "hoodie"
        }
    }
    Today {
        date: "2025-11-23"
        selectedAvatarId: "visionary"

        Quest {
            id: "q1"
            title: "Write 1 sentence for your legacy."
            xp: 3
            done: true
        }
        Quest {
            id: "q2"
            title: "Work 10 minutes on InnerNet."
            xp: 5
            done: false
        }
    }
}
```

Codex should:
- implement Kotlin data classes that can be mapped to/from this SML structure
- create a simple SML parser/serializer (or use an existing one if available)
- read and write this SML to localStorage (browser) or a file (desktop later).

---

### 2.3 Home Screen UI (SML Description)

```sml
Page {
    id: "home" 
    title: "InnerNet"
    
    Column {
        alignment: "center"
        spacing: 24
        padding: 32

        Text { 
            id: "appTitle"
            text: "InnerNet"
            style: "title"
        }
        Text {
            id: "tagline"
            text: "Free your mind. Connect with yourself."
            style: "subtitle"
        }
        Spacer { amount: 16 }

        Text { 
            id: "questionIntro"
            text: "Which question will guide you today?"
            style: "question"
        }
        Column {
            spacing: 12
            
            Button {
                id: "q_who"
                text: "Who do you want to be today?"
                action: "goto_choose_avatar"
            }
            Button {
                id: "q_what"
                text: "What do you truly want?"
                action: "goto_focus_goals"
            }
            Button {
                id: "q_learn"
                text: "What do you want to learn today?"
                action: "goto_learning"
            }
            Button {
                id: "q_random"
                text: "Surprise me"
                action: "goto_random_question"
            }
        }
    } 
}
```

Codex should:

- Define a simple SML → Compose renderer:
  - `Page`, `Column`, `Text`, `Button`, `Spacer`, etc.
- Interpret `action` strings via a simple navigation/state machine:
  - `goto_choose_avatar` → switch currentScreen to ChooseAvatar
  - etc.

---

### 2.4 Avatar Selection Screen (SML)

```sml
Page {
    id: "choose_avatar" 
    title: "Choose Your Avatar"
    
    Column {
        alignment: "center"
        spacing: 24
        padding: 32

        Text {
            text: "Who do you want to be today?"
            style: "title"
        }
        Grid {
            columns: 2 spacing: 16
            
            AvatarCard {
                id: "visionary"
                title: "Visionary"
                subtitle: "Build the future others can't yet see."
                action: "select_avatar:visionary"
            }
            AvatarCard {
                id: "creator"
                title: "Creator"
                subtitle: "Turn ideas into reality."
                action: "select_avatar:creator"
            }
            AvatarCard {
                id: "musician"
                title: "Musician"
                subtitle: "Express the soul through sound."
                action: "select_avatar:musician"
            }
            AvatarCard {
                id: "rebel"
                title: "Rebel"
                subtitle: "Break the old system."
                action: "select_avatar:rebel"
            }
            AvatarCard {
                id: "explorer"
                title: "Explorer"
                subtitle: "Experience life beyond comfort."
                action: "select_avatar:explorer"
            }
        }
    }
}
```

Additional SML for visual customization (Option Button opens a sub-screen):

```sml
Button {
    id: "customize_avatar"
    text: "Customize appearance"
    action: "goto_customize_avatar"
}
```

---

### 2.5 Visual Avatar Customization (SML Sketch)

```sml
Page {
    id: "customize_avatar" 
    title: "Customize Avatar"
    
    Column {
        alignment: "center"
        spacing: 16
        padding: 24

        Text {
            text: "Customize your avatar"
            style: "title"
        }
        Dropdown {
            id: "gender"
            label: "Gender expression"
            option: "male"
            option: "female"
            option: "neutral"
            option: "other"
        }
        Dropdown {
            id: "hairColor"
            label: "Hair color"
            option: "black"
            option: "brown"
            option: "blonde"
            option: "red"
            option: "gray"
            option: "other"
        }
        Dropdown {
            id: "hairLength"
            label: "Hair length"
            option: "short"
            option: "medium"
            option: "long"
            option: "shaved"
        }
        Dropdown {
            id: "eyeColor"
            label: "Eye color"
            option: "brown"
            option: "blue"
            option: "green"
            option: "hazel"
            option: "gray"
        }
        Dropdown {
            id: "skinTone"
            label: "Skin tone"
            option: "light"
            option: "medium"
            option: "dark"
        }
        Dropdown {
            id: "style"
            label: "Style"
            option: "casual"
            option: "elegant"
            option: "sporty"
            option: "alternative"
        }
        Button {
            id: "save_avatar"
            text: "Save avatar"
            action: "save_avatar_visual"
        }
    }
}
```

The exact rendering (visual avatar figure) can be done later.
For now, Codex only needs to store the selected attributes in the Avatar’s `Visual` SML section.

---

### 2.6 XP & Level System (Simple MVP Rules)

- Each Avatar starts at **level 1**, xp = 0.
- Level 1–5: 20 XP per level.
- Level 6–10: 50 XP per level.  
  (Future extension – for MVP we can stay in 1–5 range.)

XP events:

- Completing a daily quest: +XP as defined in quest (`xp` field).
- Optional reflection: +1 XP.
- Later: streak bonus, co-quests, etc.

Codex should implement:

- A function to **add XP** to current avatar.
- A function to **check for level up**:
  - If xp ≥ required, increase level, keep overflow XP, trigger simple level-up feedback.

---

## Implementation Tasks for Codex

### Task 1 – HumanityTree Core (priority)

1. Implement SML data structures for:
   - Leaf
   - InnerNode
   - GenesisRecord
2. Implement:
   - insert(tag, profileCid)
   - lookup(tag) → list of profileCids
3. Use copy-on-write logic to:
   - create new Leaves/InnerNodes
   - recompute Root
4. Provide abstraction for:
   - `loadNode(cid: String): Node`
   - `saveNode(node: Node): cid`
   - `resolveRootFromIpns(name: String): cid`
   - `updateIpns(name: String, newCid: String)`
5. Keep everything decoupled so it can run:
   - locally with a fake IPFS layer (for tests)
   - later with real IPFS/IPNS bindings.

### Task 2 – InnerNet MVP (after Task 1 scaffolding)

1. Implement SML → Compose renderer for basic widgets:
   - Page, Column, Grid, Text, Button, Spacer, Dropdown, AvatarCard.
2. Implement state model matching the SML examples.
3. Persist state in browser via localStorage as SML string.
4. Implement:
   - Home screen
   - Avatar selection screen
   - Simple daily quests screen with XP logic.
5. (Optional for now) Implement avatar visual customization screen
   and store the selected attributes in Avatar.Visual.

---

## Philosophy Summary

- Users own their state (SML).
- HumanityTree is a **shared index**, carried by its participants.
- InnerNet helps people **live and create**, not scroll and watch.
- AI can later assist with quest generation, reflections, translations – but the core works without it.

**Start simple. Keep everything human-readable in SML.  
Evolve tree structures and UI incrementally.**

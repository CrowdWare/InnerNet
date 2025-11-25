# InnerNet -- Circle of Trust (CoT) Specification

Version: 0.1 (Draft) Status: Work in Progress Author: Crowdware / Art
License: CC0 (Public Domain)

------------------------------------------------------------------------

## 1. Overview

The *Circle of Trust* (CoT) is the cryptographic and social foundation
of the InnerNet. Its purpose is to establish a decentralized,
censorship-resistant, privacy-preserving network of verifiable human
connections.

The system distinguishes between:

-   **ONLINE contacts**\
    Contacts known through the internet; the public identity key is
    known but there is no real-world verification.

-   **PERSONAL contacts**\
    Contacts verified through an in-person key exchange. This
    establishes a higher level of trust and unlocks additional
    communication features.

------------------------------------------------------------------------

## 2. Identity Model

Each user owns a cryptographic identity represented by:

-   `id_pub` --- Public identity key\
-   `id_priv` --- Private identity key (stored locally, never shared)

Optional device-level keypairs may be used but are not required for the
base protocol.

------------------------------------------------------------------------

## 3. Contact Types

Each contact is stored as:

    {
      "id_pub": "<public-key>",
      "trustLevel": "ONLINE | PERSONAL",
      "relation": "FRIEND | FOAF | OTHER"
    }

Meaning:

-   **FRIEND** --- Directly added contact\
-   **FOAF** --- Friend of a Friend (derived automatically)\
-   **OTHER** --- All other nodes in the network

`trustLevel` determines cryptographic permissions.

------------------------------------------------------------------------

## 4. Key Sharing (Personal Trust Upgrade)

When two users meet in person, they may upgrade their relationship from
ONLINE to PERSONAL through a **key-sharing handshake**:

-   Scan QR code\
-   Exchange short code\
-   Exchange NFC payload\
-   Exchange shared secret token

After completion:

-   Both sides mark each other `trustLevel = PERSONAL`
-   Additional encryption paths are unlocked

This allows the InnerNet to distinguish true personal connections.

------------------------------------------------------------------------

## 5. Visibility Rules

Users can mark content with:

-   `PRIVATE` --- visible only to explicit recipients
-   `CIRCLE` --- visible to Circle of Trust (FRIEND + FOAF)
-   `PUBLIC` --- visible to the world

Users can additionally attach hashtags such as:

-   `#VerkaufMotorrad`
-   `#TantraDating`
-   `#Nachbarschaftshilfe`
-   `#Ubuntu`

The client displays only content matching:

-   visibility filters
-   Circle of Trust relations
-   user-defined interest filters

------------------------------------------------------------------------

## 6. Local Storage Rules

The InnerNet client only persists data that is socially relevant:

### 6.1 Always stored

-   Own identity
-   Own index entry
-   Own content (leafs)
-   HumanityTree Genesis record

### 6.2 Cached & pinned automatically

-   FRIENDS' index entries
-   FRIENDS' public leafs\
-   FOAF index entries (optionally rate-limited)
-   FOAF selected leafs (configurable)

### 6.3 Temporary (not pinned)

-   Discovered public posts via hashtags

This keeps disk usage minimal while maintaining a living local view of
the network.

------------------------------------------------------------------------

## 7. Public Data: HumanityTree + IPFS

Public data is stored using:

-   **IPFS** --- immutable and distributed objects\
-   **HumanityTree** --- global Merkle-DAG linking all public content

Each user has:

### UserIndex (public)

Contains: - Public profile\
- Public keys\
- Root CIDs for posts / offers / profile leafs

### Leaf Objects

Each piece of content is: - Stored on IPFS - Signed with `id_priv` -
Linked in HumanityTree

------------------------------------------------------------------------

## 8. Private Messaging (IPFS + IPNS)

Private communication uses encrypted IPFS leafs:

1.  Sender generates symmetric key `k_msg`
2.  Message object is created
3.  Message is encrypted with `k_msg`
4.  Encrypted message is uploaded to IPFS → `cid_msg`
5.  `k_msg` is encrypted using `receiver.id_pub`
6.  A shared conversation leaf is updated via IPNS

Only the receiver can decrypt `k_msg`, therefore:

**Only I can read messages from private contacts.**

------------------------------------------------------------------------

## 9. Circle of Trust + XP System Integration

XP rewards real contributions and combats free riders.

XP influences: - Marketplace priority\
- Trustworthiness in Dating\
- Ability to create Hubs\
- Visibility within the Circle of Trust

XP is gained through visible social contributions and confirmed
interactions; it is not awarded for private messages to avoid spam
incentives.

------------------------------------------------------------------------

## 10. Summary

Circle of Trust binds together:

-   Cryptographic identity\
-   Real-world trust\
-   Decentralized data storage\
-   Encrypted private messaging\
-   Social graph filtering\
-   Reputation (XP) dynamics

Together, these components form the foundation of the decentralized,
unlöschbar, anonymous, human-centered InnerNet.

------------------------------------------------------------------------

#!/usr/bin/env python3
"""One-shot generator for the Lycanites structure-territory integration.

Run from the repo root:
    python3 tools/lycanites_integration.py

Outputs:
  - Rewrites src/main/resources/defaults/runicstructures-structures.json
    (appends tier-scaled Lycanites IDs to each structure's mobPool by archetype)
  - Writes  src/main/resources/defaults/runicstructures-lycanites-elements.json
    (entityId -> {element, type}) from the Lycanites source clone.
"""
import json
import os
import sys
import glob

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STRUCT_JSON = os.path.join(REPO, "src/main/resources/defaults/runicstructures-structures.json")
ELEM_JSON = os.path.join(REPO, "src/main/resources/defaults/runicstructures-lycanites-elements.json")
LYC_CREATURES = "/home/joshua/Documents/GitHub/LycanitesMobs/src/main/resources/common/lycanitesmobs/creatures"

# Archetype detection: (archetype, signature mob). First match wins; ORDER MATTERS:
#  - ancient_city and cave both contain creature_compendium:moleman (ancient first).
#  - graveyards/crypts contain BOTH iceandfire:dread_ghoul AND minecraft:wither_skeleton,
#    so every crypt signature must precede the wither_skeleton nether signature.
SIGNATURES = [
    # crypt / undead (incl. Ice and Fire dread + Iron's catacombs variants)
    ("crypt", "cataclysm:draugr"),
    ("crypt", "iceandfire:dread_ghoul"),
    ("crypt", "irons_spellbooks:catacombs_zombie"),
    # nether (blaze-bearing fortresses first; camps without blaze handled lower)
    ("nether", "minecraft:blaze"),
    ("ocean", "cataclysm:deepling"),
    ("end", "cataclysm:endermaptera"),
    ("ancient_city", "minecraft:phantom"),
    # mage / arcane (Ars wilden dens, Iron's mage towers, arcane shrines)
    ("mage", "ars_nouveau:wilden_guardian"),
    ("mage", "irons_spellbooks:archevoker"),
    # nether camps (piglin / wither-skeleton, no blaze) — AFTER all crypt signatures
    ("nether", "minecraft:piglin_brute"),
    ("nether", "minecraft:wither_skeleton"),
    ("nether", "cataclysm:ignited_berserker"),
    ("illager", "irons_spellbooks:magehunter_vindicator"),
    ("cave", "creature_compendium:moleman"),
    ("early", "variantsandventures:gelid"),
]

TIER_ORDER = ["early", "mid", "late", "veteran"]
DEFAULT_TIER = {
    "nether": "mid", "ocean": "mid", "cave": "mid", "crypt": "late",
    "ancient_city": "late", "end": "late", "early": "early", "illager": "early",
    "mage": "mid",
}

# Lycanites additions per archetype. Tiered archetypes accumulate cumulatively by
# tier index; "_all" archetypes apply their single set regardless of tier.
ADDITIONS = {
    "nether": {
        "mid": ["afrit", "cinder", "belphegor", "salamander", "trite"],
        "late": ["cherufe", "behemophet", "wraith", "malwrath", "volcan", "khalk"],
    },
    "ocean": {
        "early": ["skylus", "herma", "abtu"],
        "mid": ["jengu", "ioray", "lacedon", "roa", "thresher"],
        "late": ["abaia"],
    },
    "cave": {
        "early": ["darkling", "gnekk", "gorgomite", "eyewig"],
        "mid": ["grue", "troll", "maug", "geonach", "shade"],
        "late": ["wraamon", "tremor"],
    },
    "crypt": {"_all": ["ghoul", "geist", "cryptkeeper", "necrovore", "banshee", "reaper", "shade", "grue"]},
    "ancient_city": {"_all": ["grue", "shade", "fear", "spectre", "banshee", "epion", "darkling"]},
    "end": {"_all": ["argus", "astaroth", "grell", "spectre", "trite", "naxiris"]},
    "early": {"_all": ["conba", "lycosa", "ghoul"]},
    # arcane/eldritch guardians for mage towers, libraries, and arcane shrines
    "mage": {"_all": ["argus", "naxiris", "grell", "sutiramu"]},
    "illager": {},
}

BOSSES = {"rahovart", "asmodeus", "amalgalich"}


def peaceful_creatures():
    """Names of creatures flagged peaceful in the Lycanites source — these are
    CREATURE-category (passive) and must never be used as hostile guardians."""
    out = set()
    for path in glob.glob(os.path.join(LYC_CREATURES, "*.json")):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        if d.get("peaceful"):
            out.add(os.path.basename(path)[:-5])
    return out


def classify(pool):
    s = set(pool)
    for arch, sig in SIGNATURES:
        if sig in s:
            return arch
    return None


def additions_for(archetype, tier):
    spec = ADDITIONS.get(archetype, {})
    if "_all" in spec:
        return list(spec["_all"])
    if tier not in TIER_ORDER:
        tier = DEFAULT_TIER[archetype]
    idx = TIER_ORDER.index(tier)
    result = []
    for t in TIER_ORDER:  # deterministic early->late ordering
        if t in spec and TIER_ORDER.index(t) <= idx:
            result += spec[t]
    return result


def regen_structures():
    peaceful = peaceful_creatures()
    for arch, spec in ADDITIONS.items():
        for tier_mobs in spec.values():
            for name in tier_mobs:
                assert name not in peaceful, f"{name} ({arch}) is peaceful; not a valid hostile guardian"
                assert name not in BOSSES, f"{name} ({arch}) is a boss; never add to a pool"

    with open(STRUCT_JSON, encoding="utf-8") as f:
        root = json.load(f)
    structures = root["structures"] if "structures" in root else root

    matched = {}
    unmatched = []
    for key, entry in structures.items():
        if key.startswith("_") or not isinstance(entry, dict):
            continue
        pool = entry.get("mobPool")
        if not pool:
            continue
        # Idempotent: drop any previously-added Lycanites entries before re-adding,
        # so the generator produces identical output whether run on the pristine
        # file or on its own output (it only ever adds lycanitesmobs: IDs).
        pool[:] = [m for m in pool if not m.startswith("lycanitesmobs:")]
        arch = classify(pool)
        if arch is None:
            unmatched.append(key)
            continue
        matched[arch] = matched.get(arch, 0) + 1
        tier = entry.get("progressionTier") or DEFAULT_TIER[arch]
        existing = set(pool)
        for name in additions_for(arch, tier):
            assert name not in BOSSES, f"boss {name} must never be added to a pool"
            rid = "lycanitesmobs:" + name
            if rid not in existing:
                pool.append(rid)
                existing.add(rid)

    with open(STRUCT_JSON, "w", encoding="utf-8") as f:
        json.dump(root, f, separators=(",", ":"), ensure_ascii=False)

    # Self-check: output re-parses, and no boss leaked into any pool.
    with open(STRUCT_JSON, encoding="utf-8") as f:
        check = json.load(f)
    cs = check["structures"] if "structures" in check else check
    for key, entry in cs.items():
        if key.startswith("_") or not isinstance(entry, dict):
            continue
        for m in entry.get("mobPool", []):
            assert not any(m == "lycanitesmobs:" + b for b in BOSSES), f"{key} contains boss {m}"

    print(f"structures: matched {matched}, unmatched ({len(unmatched)}): {unmatched}")


def regen_elements():
    out = {}
    for path in sorted(glob.glob(os.path.join(LYC_CREATURES, "*.json"))):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        name = os.path.basename(path)[:-5]
        rec = {}
        elems = d.get("elements") or []
        if elems:
            rec["element"] = elems[0]
        ctype = d.get("creatureType")
        if ctype:
            rec["type"] = ctype
        out["lycanitesmobs:" + name] = rec
    with open(ELEM_JSON, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=1, ensure_ascii=False, sort_keys=True)
    print(f"elements: wrote {len(out)} creatures")


if __name__ == "__main__":
    if not os.path.isdir(LYC_CREATURES):
        sys.exit(f"Lycanites source not found at {LYC_CREATURES}")
    regen_elements()
    regen_structures()
    print("done")

package de.habits.core

/** Body shape of the avatar; the only thing the user can choose. */
enum class BodyShape(val label: String) { NEUTRAL("Neutral"), FEMININE("Weiblich"), MASCULINE("Männlich") }

/** The three big chapters of the avatar's story; each starts at [fromLevel]. */
enum class AvatarPhase(val fromLevel: Int) {
    HUMAN(1),
    MOSQUITO(15),
    GOOSE(30);

    fun title(shape: BodyShape) = when (this) {
        HUMAN -> "Mensch"
        MOSQUITO -> when (shape) {
            BodyShape.FEMININE -> "Mückenprinzessin"
            BodyShape.MASCULINE -> "Mückenprinz"
            BodyShape.NEUTRAL -> "Mückenhoheit"
        }
        GOOSE -> "Wrestling-Gans"
    }
}

fun avatarPhase(level: Int): AvatarPhase = AvatarPhase.entries.last { it.fromLevel <= level }

/**
 * Parts of the avatar in the order they appear, one per level, so every level-up changes something
 * visible. Later parts may replace earlier ones (the cape turns into wings, the gown into a singlet).
 */
enum class AvatarPart(val unlockLevel: Int, val label: String) {
    // Human (1–14)
    STICK(1, "Strichmännchen"),
    ARMS(2, "Arme"),
    FACE(3, "Gesicht"),
    HEAD(4, "Kopf"),
    BODY(5, "Körper"),
    HAIR(6, "Haare"),
    TROUSERS(7, "Hose bzw. Rock"),
    SHIRT(8, "Shirt"),
    SHOES(9, "Schuhe"),
    BELT(10, "Gürtel"),
    SCARF(11, "Halstuch"),
    BAG(12, "Umhängetasche"),
    CAPE(13, "Umhang"),
    WAND(14, "Zauberstab"),

    // Ascension: mosquito royalty (15–29)
    AURA(15, "Aufstieg – Aura"),
    ANTENNAE(16, "Fühler"),
    COMPOUND_EYES(17, "Facettenaugen"),
    WINGS(18, "Der Umhang wird zu Flügeln"),
    PALE_SKIN(19, "Schimmernde Haut"),
    TIARA(20, "Diadem"),
    PROBOSCIS(21, "Stechrüssel"),
    BIG_WINGS(22, "Große Flügel"),
    GOWN(23, "Ballkleid"),
    STRIPES(24, "Mückenstreifen"),
    EXTRA_ARMS(25, "Zweites Armpaar"),
    SCEPTER(26, "Zepter"),
    HOVER(27, "Schweben"),
    CROWN(28, "Krone"),
    SWARM(29, "Mückenschwarm"),

    // Wrestling goose (30–45): the mosquito traits go one by one, the goose takes over
    BEAK(30, "Schnabel"),
    GOOSE_EYES(31, "Kampfblick"),
    FEATHERS(32, "Federkleid"),
    GOOSE_WINGS(33, "Gänseflügel statt Mückenflügel"),
    LONG_NECK(34, "Langer Hals"),
    GOOSE_FEET(35, "Watschelfüße"),
    SINGLET(36, "Ringeranzug"),
    MASK(37, "Wrestling-Maske"),
    WRISTBANDS(38, "Schweißbänder"),
    MUSCLES(39, "Muskeln"),
    MICROPHONE(40, "Mikrofon"),
    TITLE_BELT(41, "Meistergürtel"),
    SUNGLASSES(42, "Sonnenbrille"),
    SPOTLIGHT(43, "Scheinwerfer"),
    RING(44, "Wrestling-Ring"),
    PYRO(45, "Pyrotechnik"),
}

/** Level of the last fixed part; from there on every level adds one sparkle around the figure. */
val lastPartLevel = AvatarPart.entries.maxOf { it.unlockLevel }

fun avatarParts(level: Int): Set<AvatarPart> = AvatarPart.entries.filter { it.unlockLevel <= level }.toSet()

/** The part that appears exactly at [level], if any. */
fun avatarPartAt(level: Int): AvatarPart? = AvatarPart.entries.firstOrNull { it.unlockLevel == level }

/** The next part still to come after [level]. */
fun nextAvatarPart(level: Int): AvatarPart? = AvatarPart.entries.firstOrNull { it.unlockLevel > level }

/** Open-ended: the figure is never "finished", every level beyond the last part adds a sparkle. */
fun avatarSparkles(level: Int): Int = (level - lastPartLevel).coerceAtLeast(0)

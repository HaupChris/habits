package de.habits.core

import de.habits.core.AvatarPart.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

/*
 * Pixel-art generator for the avatar (chunky, outlined, shaded – in the spirit of Ball x Pit).
 * Parts are painted back to front as shapes; every shape gets a dark outline ring, a shaded
 * bottom/right edge and a highlighted top/left edge automatically, so the look stays consistent
 * and the parts only describe geometry. Which parts exist is decided by avatarParts(level).
 * Pure JVM (no Android) so it can be unit-tested; the app only blits the pixels.
 *
 * Geometry is described in figure units on a 32×42 grid (centre at x = 16, feet at y ≈ 39), with
 * room around it for wings, aura, ring and floating things (x −8..40, y −8..42). Every unit is
 * SPRITE_SCALE pixels, so curves are smooth and details like eye glints can be half a unit.
 */
const val SPRITE_SCALE = 2
const val SPRITE_W = 48 * SPRITE_SCALE
const val SPRITE_H = 50 * SPRITE_SCALE
private const val S = SPRITE_SCALE
private const val OX = 8
private const val OY = 8

/** ARGB pixels, row by row; 0 = transparent. */
class Sprite(val width: Int, val height: Int, val argb: IntArray)

/** Spots (in sprite pixels) where the sparkles beyond the last fixed part appear, in this order. */
val sparkleSpots: List<Pair<Int, Int>> =
    listOf(4 to 4, 44 to 4, 2 to 15, 45 to 13, 3 to 26, 44 to 25, 11 to 2, 37 to 1, 1 to 8, 46 to 19).map { (x, y) -> x * S to y * S }

private typealias Mask = (Double, Double) -> Boolean

private class Tone(val base: Int, val shade: Int, val light: Int = 0)

private fun c(argb: Long) = argb.toInt()

private val INK = c(0xFF2A1E38)
private val WHITE = c(0xFFFFFFFF)
private val BLUSH = c(0xFFF59A7A)
private val GROUND = c(0x30000000)
private val TEAL = c(0xFF7FE3E0)
private val VEIN = c(0x602A1E38)
private val STRIPE = c(0xFF5E2470)
private val MAT = c(0xFFC3CEDF)
private val SPOT = c(0x40FFF2B0)
private val FLAME_IN = c(0xFFFFE066)
private val GLINT = c(0xB0FFFFFF)

private val SKIN_HUMAN = Tone(c(0xFFFFD23F), c(0xFFF0A81E), c(0xFFFFE98A))
private val SKIN_PALE = Tone(c(0xFFE4C8F0), c(0xFFC29BD8), c(0xFFF6E9FB))
private val FEATHER = Tone(c(0xFFF7F5EE), c(0xFFC9CCD6), WHITE)
private val HAIR_TONE = Tone(c(0xFF7B4A2A), c(0xFF55301C), c(0xFFA56A3E))
private val SHIRT_TONE = Tone(c(0xFF3E7BD9), c(0xFF2C58A8), c(0xFF6FA3F0))
private val PANTS_TONE = Tone(c(0xFF8C6A4A), c(0xFF6B4E36), c(0xFFAA8660))
private val SHOE_TONE = Tone(c(0xFFF4EDE0), c(0xFFCFC4B0))
private val BELT_TONE = Tone(c(0xFF5A3A22), c(0xFF3E2716))
private val ORANGE = Tone(c(0xFFF08A24), c(0xFFC2661A), c(0xFFFFB466))
private val BAG_TONE = Tone(c(0xFFA0703F), c(0xFF7A5230), c(0xFFC08F5A))
private val CAPE_TONE = Tone(c(0xFF5B3F8C), c(0xFF3F2A66), c(0xFF7B5FB0))
private val WAND_TONE = Tone(c(0xFF9A6A3A), c(0xFF6B4524))
private val MAGIC = Tone(c(0xFFB454F0), c(0xFF8A2FC4), c(0xFFE1A8FF))
private val GOLD = Tone(c(0xFFF2C94C), c(0xFFC9962A), c(0xFFFFF0A8))
private val WING_TONE = Tone(c(0xB0DDEFFF), c(0xB0A9CBE6), c(0xC0FFFFFF))
private val ANTENNA = Tone(c(0xFF5E2470), c(0xFF3E1450))
private val EYE_TONE = Tone(c(0xFF3A1E5C), c(0xFF26123F), c(0xFF5A3A8C))
private val GOWN_TONE = Tone(c(0xFFB05BC8), c(0xFF8A3FA0), c(0xFFD58BE6))
private val SINGLET_TONE = Tone(c(0xFF2F5BD3), c(0xFF1F3F9A), c(0xFF5C86EE))
private val MASK_TONE = Tone(c(0xFF7A3FC4), c(0xFF5A2A96), c(0xFFA66FE6))
private val SHADES = Tone(c(0xFF1E2433), c(0xFF10141E), c(0xFF3A4560))
private val MIC_HANDLE = Tone(c(0xFF4A4F5C), c(0xFF30343E), c(0xFF6E7484))
private val MIC_HEAD = Tone(c(0xFF8A90A0), c(0xFF5E6474), c(0xFFC4C9D4))
private val POST = Tone(c(0xFF9AA3B5), c(0xFF6E778A), c(0xFFC9D0DD))
private val ROPE = Tone(c(0xFFF4F4F4), c(0xFFC9CCD6), WHITE)
private val FLAME = Tone(c(0xFFF08A24), c(0xFFC2661A), FLAME_IN)

private fun Number.d() = toDouble()

private fun rect(x0: Number, y0: Number, x1: Number, y1: Number): Mask {
    val a = x0.d(); val b = y0.d(); val e = x1.d(); val f = y1.d()
    return { x, y -> x >= a && x < e && y >= b && y < f }
}
private fun ellipse(cx: Number, cy: Number, rx: Number, ry: Number): Mask {
    val a = cx.d(); val b = cy.d(); val e = rx.d(); val f = ry.d()
    return { x, y -> val dx = (x - a) / e; val dy = (y - b) / f; dx * dx + dy * dy <= 1.0 }
}
/** Rectangle with rounded corners of radius [r] units. */
private fun rrect(x0: Number, y0: Number, x1: Number, y1: Number, r: Double = 1.0): Mask {
    val a = x0.d(); val b = y0.d(); val e = x1.d(); val f = y1.d()
    return { x, y ->
        x >= a && x < e && y >= b && y < f && run {
            val dx = x - x.coerceIn(a + r, e - r)
            val dy = y - y.coerceIn(b + r, f - r)
            dx * dx + dy * dy <= r * r
        }
    }
}
/** Diamond around the unit cell (cx, cy). */
private fun diamond(cx: Number, cy: Number, r: Number): Mask {
    val a = cx.d() + 0.5; val b = cy.d() + 0.5; val e = r.d() + 0.5
    return { x, y -> abs(x - a) + abs(y - b) <= e }
}
/** Rows [y0, y1) whose half width grows from [half] by [grow] per unit – skirts, gowns, light cones. */
private fun flare(y0: Number, y1: Number, half: Number, grow: Double): Mask {
    val a = y0.d(); val b = y1.d(); val h = half.d()
    return { x, y -> y >= a && y < b && abs(x - 16) <= h + (y - a) * grow }
}
/** Stroke of width [w] units along the segment (x0, y0)–(x1, y1). */
private fun seg(x0: Number, y0: Number, x1: Number, y1: Number, w: Number): Mask {
    val ax = x0.d(); val ay = y0.d(); val bx = x1.d(); val by = y1.d(); val r = w.d() / 2
    val lx = bx - ax; val ly = by - ay; val len2 = (lx * lx + ly * ly).coerceAtLeast(1e-9)
    return { x, y ->
        val t = (((x - ax) * lx + (y - ay) * ly) / len2).coerceIn(0.0, 1.0)
        val dx = x - (ax + t * lx); val dy = y - (ay + t * ly)
        sqrt(dx * dx + dy * dy) <= r
    }
}
/** A [thick]-unit stroke between unit cells, like a pixel line of that thickness. */
private fun line(x0: Int, y0: Int, x1: Int, y1: Int, thick: Int): Mask {
    val o = thick / 2.0
    return seg(x0 + o, y0 + o, x1 + o, y1 + o, thick + 0.2)
}
private infix fun Mask.and(o: Mask): Mask = { x, y -> this(x, y) && o(x, y) }
private infix fun Mask.or(o: Mask): Mask = { x, y -> this(x, y) || o(x, y) }
private operator fun Mask.not(): Mask = { x, y -> !this(x, y) }

/** Source-over blend, so translucent wings and auras sit on top of what is already there. */
private fun over(src: Int, dst: Int): Int {
    val sa = src ushr 24
    if (sa == 255 || dst == 0) return src
    if (sa == 0) return dst
    val da = (dst ushr 24) * (255 - sa) / 255
    val a = sa + da
    fun ch(s: Int) = (((src shr s) and 255) * sa + ((dst shr s) and 255) * da) / a
    return (a shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

private class Pixels {
    val px = IntArray(SPRITE_W * SPRITE_H)
    /** Where figure y = 0 lies, in units; shrinks while hovering or for the long neck. */
    var oy = OY
    private val inside = BooleanArray((SPRITE_W + 4) * (SPRITE_H + 4))

    private fun blend(ax: Int, ay: Int, color: Int) {
        if (ax in 0 until SPRITE_W && ay in 0 until SPRITE_H) px[ay * SPRITE_W + ax] = over(color, px[ay * SPRITE_W + ax])
    }
    private fun ux(ax: Int) = (ax + 0.5) / S - OX
    private fun uy(ay: Int) = (ay + 0.5) / S - oy

    fun fill(mask: Mask, color: Int) {
        for (ay in 0 until SPRITE_H) for (ax in 0 until SPRITE_W) if (mask(ux(ax), uy(ay))) blend(ax, ay, color)
    }

    /** One unit cell (S×S pixels). */
    fun set(x: Int, y: Int, color: Int) {
        for (dy in 0 until S) for (dx in 0 until S) blend((x + OX) * S + dx, (y + oy) * S + dy, color)
    }

    /** A single pixel at figure position (x, y). */
    fun dot(x: Double, y: Double, color: Int) = blend(floor((x + OX) * S).toInt(), floor((y + oy) * S).toInt(), color)

    /** Paints [mask] with an outline ring, a shaded bottom/right band of [depth] pixels and a lit top/left edge. */
    fun shape(mask: Mask, tone: Tone, depth: Int = 2) {
        val w = SPRITE_W + 4
        for (ay in -2 until SPRITE_H + 2) for (ax in -2 until SPRITE_W + 2) inside[(ay + 2) * w + ax + 2] = mask(ux(ax), uy(ay))
        fun at(ax: Int, ay: Int) = inside[(ay + 2) * w + ax + 2]
        for (ay in -1..SPRITE_H) for (ax in -1..SPRITE_W) {
            if (!at(ax, ay) && (at(ax - 1, ay) || at(ax + 1, ay) || at(ax, ay - 1) || at(ax, ay + 1))) blend(ax, ay, INK)
        }
        for (ay in 0 until SPRITE_H) for (ax in 0 until SPRITE_W) {
            if (!at(ax, ay)) continue
            blend(ax, ay, when {
                (1..depth).any { !at(ax + it, ay) || !at(ax, ay + it) } -> tone.shade
                tone.light != 0 && (!at(ax - 1, ay) || !at(ax, ay - 1)) -> tone.light
                else -> tone.base
            })
        }
    }
}

fun avatarSprite(level: Int, shape: BodyShape): Sprite {
    val p = Pixels()
    p.figure(avatarParts(level), shape)
    return Sprite(SPRITE_W, SPRITE_H, p.px)
}

private fun Pixels.figure(parts: Set<AvatarPart>, bodyShape: BodyShape) {
    fun has(part: AvatarPart) = part in parts
    val female = bodyShape == BodyShape.FEMININE
    // Half torso width: broader shoulders for the masculine shape, broader still with muscles.
    val half = (if (bodyShape == BodyShape.MASCULINE) 5 else 4) + (if (has(MUSCLES)) 1 else 0)
    val tl = 16 - half
    val tr = 16 + half
    val feathered = has(FEATHERS)
    val skin = when {
        feathered -> FEATHER
        has(PALE_SKIN) -> SKIN_PALE
        else -> SKIN_HUMAN
    }
    // Later parts replace earlier ones: the everyday clothes go with the gown, the gown with the singlet, …
    val everyday = !has(GOWN)
    val gown = has(GOWN) && !has(SINGLET)
    val gooseWings = has(GOOSE_WINGS)
    val hover = has(HOVER) && !has(GOOSE_FEET)
    val armW = when {
        !gooseWings -> 3
        has(MUSCLES) -> 5
        else -> 4
    }

    // Background: ring, light, ground shadow; the figure lifts off when hovering.
    if (has(RING)) ring()
    if (has(SPOTLIGHT)) {
        fill({ x, y -> y >= -8 && y < 40 && abs(x - 16) <= 3 + (y + 8) * 0.3 }, SPOT)
        fill(ellipse(16, 39.5, 13, 2.8), SPOT)
    } else if (has(AURA)) {
        val glow = MAGIC.base and 0xFFFFFF
        fill(ellipse(16, 18, 18, 23), glow or 0x22000000)
        fill(ellipse(16, 18, 13, 18), glow or 0x22000000)
    }
    fill(ellipse(16, 39.5, if (hover) 6 else 9, if (hover) 1.5 else 2), GROUND)
    if (hover) oy -= 3

    if (has(WINGS) && !gooseWings) insectWings(big = has(BIG_WINGS))
    else if (has(CAPE) && !has(WINGS)) shape(flare(18, 38, half + 1, 0.3), CAPE_TONE)
    if (has(HAIR) && female && !feathered) shape(rrect(7, 8, 25, 27, 2.0), HAIR_TONE) // long hair, behind the body

    if (!has(BODY)) {
        stick(has(ARMS))
    } else {
        when {
            has(GOOSE_FEET) -> for (cx in listOf(13.5, 18.5)) {
                shape(rect(cx - 1, 30, cx + 1, 37.5), ORANGE, 1)
                shape({ x, y -> y >= 36.5 && y < 39.8 && abs(x - cx) <= (y - 36.0) * 1.2 }, ORANGE, 1)
                for (dx in listOf(-1.6, 1.6)) fill(seg(cx, 37.2, cx + dx, 39.5, 0.5), ORANGE.shade)
            }
            hover -> for (l in listOf(line(13, 32, 11, 39, 1), line(14, 32, 14, 40, 1), line(18, 32, 20, 39, 1), line(17, 32, 17, 40, 1))) fill(l, ANTENNA.base)
            else -> {
                val legs = if (has(TROUSERS) && !female) PANTS_TONE else skin
                shape(rect(12, 30, 15, 37), legs)
                shape(rect(17, 30, 20, 37), legs)
                if (has(SHOES)) {
                    shape(rrect(11, 36, 15, 40), SHOE_TONE)
                    shape(rrect(17, 36, 21, 40), SHOE_TONE)
                }
            }
        }
        shape(rrect(tl, 19, tr, 31, 1.5), if (has(SHIRT) && everyday) SHIRT_TONE else skin)
        if (has(SHIRT) && everyday) fill(seg(14.5, 19.5, 16, 21.5, 0.6) or seg(17.5, 19.5, 16, 21.5, 0.6), SHIRT_TONE.shade) // collar
        if (has(TROUSERS) && everyday) {
            if (female) shape(rect(4, 27, 28, 34) and flare(27, 34, 4, 0.55), PANTS_TONE)
            else shape(rect(tl, 27, tr, 31), PANTS_TONE)
        }
        if (has(BELT) && everyday) {
            fill(rect(tl, 27, tr, 29), BELT_TONE.base)
            fill(rect(15, 27, 17, 29), GOLD.base)
        }
        if (gown) {
            val dress = rrect(tl, 19, tr, 28) or flare(27, 37, half, 0.6)
            shape(dress, GOWN_TONE)
            if (has(STRIPES)) fill(dress and { _, y -> y >= 29 && (y - 29) % 3 < 1 }, STRIPE)
        }
        if (has(SINGLET)) {
            val suit = rrect(tl, 23, tr, 32) or rect(tl + 1, 19, tl + 3, 24) or rect(tr - 3, 19, tr - 1, 24) or rect(12, 30, 15, 34) or rect(17, 30, 20, 34)
            shape(suit, SINGLET_TONE)
            fill(suit and { x, y -> y >= 24 && (x < tl + 1 || x >= tr - 1) }, ORANGE.base) // side stripes
        }
        if (has(EXTRA_ARMS) && !gooseWings) { // thin insect arms, in front of the gown
            shape(seg(tl + 0.5, 24.5, tl - 5, 31, 1.2), skin, 1)
            shape(seg(tr - 0.5, 24.5, tr + 5, 31, 1.2), skin, 1)
        }
        if (has(MUSCLES)) fill(seg(tl + 1.5, 21.2, 15.6, 22.4, 0.6) or seg(tr - 1.5, 21.2, 16.4, 22.4, 0.6), skin.shade) // pecs
        if (has(TITLE_BELT)) {
            shape(rect(tl, 27.5, tr, 30), BELT_TONE, 1)
            shape(ellipse(16, 28.75, 3.8, 2.7), GOLD)
            shape(ellipse(16, 28.75, 1.3, 1.1), SHIRT_TONE, 1)
        }

        for (x in listOf(tl - armW, tr)) {
            if (gooseWings) {
                // Feathered wing-arm with three feather tips at the end.
                val wing = rrect(x, 20, x + armW, 29, 1.5) or ellipse(x + 0.9, 29.4, 0.9, 1.6) or
                    ellipse(x + armW / 2.0, 30, 0.9, 1.8) or ellipse(x + armW - 0.9, 29.4, 0.9, 1.6)
                shape(wing, skin)
                fill(seg(x + 1.3, 22.5, x + 1.3, 28, 0.5) or seg(x + armW - 1.3, 22.5, x + armW - 1.3, 28, 0.5), skin.shade)
                if (has(WRISTBANDS)) shape(rect(x - 1, 25, x + armW + 1, 27.5) and rrect(x - 0.3, 20, x + armW + 0.3, 30), ORANGE, 1)
            } else {
                shape(rrect(x, 21, x + 3, 30), skin)
                if (gown) shape(ellipse(x + 1.5, 22.5, 2.5, 2.5), GOWN_TONE) // puffed sleeve
                else if (has(SHIRT)) shape(rect(x, 21, x + 3, 26), SHIRT_TONE)
            }
        }
        if (has(BAG) && everyday) {
            fill(line(tl, 19, tr - 2, 27, 2), BELT_TONE.base)
            shape(rrect(tr - 2, 27, tr + 3, 32), BAG_TONE)
        }
    }

    // Head (raised on a long neck in the goose phase).
    val neck = if (has(LONG_NECK)) 3 else 0
    if (neck > 0) shape(rrect(13.5, 14, 18.5, 21, 1.5), skin)
    oy -= neck
    if (has(HEAD)) {
        if (!feathered) {
            shape(rrect(6, 11, 9, 15), skin)
            shape(rrect(23, 11, 26, 15), skin)
        }
        shape(ellipse(16, 12.5, 8, 7.5), skin)
    } else {
        fill(ellipse(16, 12.5, 8, 7.5) and !ellipse(16, 12.5, 6, 5.5), INK)
    }
    if (has(MASK)) {
        shape(ellipse(16, 12.5, 8, 7.5) and { _, y -> y < 16.2 }, MASK_TONE)
        for (cx in listOf(13.0, 19.0)) shape(ellipse(cx, 14.4, 2.4, 1.9), GOLD, 1)
        fill(rect(15.25, 5.3, 16.75, 11), GOLD.base)
    }
    if (has(FACE)) face(parts, skin, female)
    if (has(SCARF) && everyday) {
        shape(rrect(tl + 1, 19, tr - 1, 22), ORANGE)
        shape(rect(tr - 4, 21, tr - 2, 26), ORANGE)
    }
    if (has(HAIR)) {
        if (feathered) {
            val tuft = when (bodyShape) {
                BodyShape.NEUTRAL -> ellipse(14, 3.8, 1.1, 2.4) or ellipse(16, 2.8, 1.2, 2.9) or ellipse(18, 3.8, 1.1, 2.4)
                BodyShape.FEMININE -> ellipse(14.5, 3.8, 1.1, 2.4) or ellipse(16.3, 3.0, 1.2, 2.7) or seg(18, 4.5, 22.5, 0.5, 1.6)
                BodyShape.MASCULINE -> ellipse(14, 3, 1.1, 3) or ellipse(16, 1.8, 1.3, 3.8) or ellipse(18, 3, 1.1, 3) // mohawk
            }
            shape(tuft, FEATHER, 1)
        } else {
            shape((ellipse(16, 12, 8.9, 8.3) and { _, y -> y < 12 }) or rect(9, 12, 11, 14) or rect(21, 12, 23, 14), HAIR_TONE)
            fill(seg(13, 5.5, 12, 10.5, 0.5) or seg(18, 5, 19, 10, 0.5), HAIR_TONE.shade) // strands
            when (bodyShape) {
                BodyShape.NEUTRAL -> shape(ellipse(16.5, 3.5, 1.6, 1.8), HAIR_TONE)
                BodyShape.MASCULINE -> shape(ellipse(12.5, 4, 1.6, 1.3) or ellipse(19.5, 4, 1.6, 1.3), HAIR_TONE)
                BodyShape.FEMININE -> {}
            }
        }
    }
    if (has(ANTENNAE) && !feathered) {
        shape(line(13, 6, 10, -1, 1), ANTENNA, 1)
        shape(line(18, 6, 21, -1, 1), ANTENNA, 1)
        shape(diamond(10, -2, 1), ANTENNA, 1)
        shape(diamond(21, -2, 1), ANTENNA, 1)
    }
    if (!has(MASK)) when {
        has(CROWN) -> {
            shape(rect(10, 2, 22, 7) and { x, y -> y >= 4 || x < 12 || (x >= 15 && x < 17) || x >= 20 }, GOLD)
            set(15, 5, MAGIC.base); set(16, 5, MAGIC.base); set(11, 5, TEAL); set(20, 5, TEAL)
        }
        has(TIARA) -> {
            shape(rect(11, 5, 21, 7) or rect(15, 3, 17, 5), GOLD)
            set(15, 5, MAGIC.base); set(16, 5, MAGIC.base)
        }
    }
    oy += neck

    if (has(BODY)) {
        // The thing in the right hand evolves: wand → sceptre → microphone.
        val x = tr + armW - 1
        when {
            has(MICROPHONE) -> {
                val hx = tr + armW / 2.0
                shape(seg(hx, 30.5, hx + 2.5, 25.5, 1.1), MIC_HANDLE, 1)
                shape(ellipse(hx + 3, 24.2, 1.9, 1.9), MIC_HEAD)
                fill(seg(hx + 1.6, 24.2, hx + 4.4, 24.2, 0.3) or seg(hx + 3, 22.8, hx + 3, 25.6, 0.3), MIC_HEAD.shade)
            }
            has(SCEPTER) -> {
                shape(line(x, 31, x + 4, 13, 1), GOLD, 1)
                shape(diamond(x + 4, 11, 2), MAGIC)
            }
            has(WAND) -> {
                shape(line(x, 28, x + 5, 20, 1), WAND_TONE, 1)
                shape(diamond(x + 7, 18, 2), MAGIC)
            }
        }
    }

    if (has(SWARM) && !has(RING)) {
        for ((x, y) in listOf(-6 to 12, 37 to 16, -4 to 32, 37 to 34, -7 to 22, 34 to 25)) {
            set(x, y, INK); set(x + 1, y, INK)
            set(x, y - 1, WING_TONE.light); set(x + 1, y - 1, WING_TONE.light)
        }
    }
    if (has(PYRO)) {
        for (cx in listOf(-4.0, 36.0)) {
            shape(ellipse(cx, 37, 2.8, 3.2) or { x, y -> y >= 28 && y < 37 && abs(x - cx) <= (y - 28) * 0.32 }, FLAME)
            fill(ellipse(cx, 37.5, 1.3, 1.8), FLAME_IN)
            for ((dx, dy) in listOf(-2 to 24, 1 to 22, -1 to 19, 2 to 26)) set(cx.toInt() + dx, dy, GOLD.base)
        }
    }
}

/** Translucent insect wings behind the body: first one pair, then upper and lower wings. */
private fun Pixels.insectWings(big: Boolean) {
    for (side in listOf(-1, 1)) {
        fun cx(dx: Double) = 16.0 + side * dx
        val m = if (big) ellipse(cx(13.5), 14, 9.5, 8) or ellipse(cx(11.0), 27, 7, 6) else ellipse(cx(10.0), 18, 6, 9)
        shape(m, WING_TONE)
        val root = 16 + side * 6.5
        fill(seg(root, 19.5, 16 + side * (if (big) 20.5 else 13.5), if (big) 9.5 else 12.5, 0.5), VEIN)
        if (big) fill(seg(root, 22.5, 16 + side * 15.5, 30.5, 0.5), VEIN)
    }
}

/** Wrestling ring behind the figure: mat, corner posts, three ropes. */
private fun Pixels.ring() {
    fill(rect(-8, 35.5, 40, 42), MAT)
    fill(rect(-8, 35.5, 40, 36), INK)
    for (x in listOf(-7, 37)) shape(rrect(x, 8, x + 2, 37, 0.5), POST)
    for ((i, y) in listOf(16, 22, 28).withIndex()) shape(rect(-5, y, 37, y + 1), if (i == 1) SINGLET_TONE else ROPE, 1)
}

/** Levels 1–4: a line drawing in ink – lines for body, arms, legs. */
private fun Pixels.stick(arms: Boolean) {
    fill(rect(15, 20, 17, 31), INK)
    fill(line(14, 30, 11, 38, 2), INK)
    fill(line(16, 30, 19, 38, 2), INK)
    if (arms) {
        fill(line(14, 23, 8, 28, 2), INK)
        fill(line(16, 23, 22, 28, 2), INK)
    }
}

private fun Pixels.face(parts: Set<AvatarPart>, skin: Tone, female: Boolean) {
    fun has(part: AvatarPart) = part in parts
    val filled = has(HEAD)
    val dy = if (filled) 0.0 else -1.0 // the hollow stick head is smaller inside
    when {
        has(GOOSE_EYES) -> {
            for (cx in listOf(13.0, 19.0)) {
                shape(ellipse(cx, 14.6, 1.9, 1.6), Tone(WHITE, FEATHER.shade), 1)
                fill(ellipse(cx + (if (cx < 16) 0.4 else -0.4), 14.9, 0.9, 1.0), INK) // pupils look inward
                dot(cx - 0.2, 14.3, WHITE)
            }
            fill(seg(10.6, 12.2, 14.8, 13.4, 1.0) or seg(21.4, 12.2, 17.2, 13.4, 1.0), INK) // angry brows
            if (female) fill(seg(10.9, 14.0, 10.0, 13.2, 0.5) or seg(21.1, 14.0, 22.0, 13.2, 0.5), INK) // lashes
        }
        has(COMPOUND_EYES) -> {
            for (cx in listOf(13.0, 20.0)) {
                shape(ellipse(cx, 15, 2.6, 2.6), EYE_TONE)
                set(cx.toInt() - 2, 13, TEAL); set(cx.toInt(), 15, TEAL)
            }
        }
        else -> for (cx in listOf(13.0, 19.0)) {
            fill(ellipse(cx, 14.5 + dy, 1.0, 1.5), INK)
            if (filled) { dot(cx - 0.5, 13.5, WHITE); dot(cx, 13.5, WHITE) }
        }
    }
    if (filled && skin === SKIN_HUMAN) fill(ellipse(10.8, 16.6, 1.3, 0.6) or ellipse(21.2, 16.6, 1.3, 0.6), BLUSH)
    when {
        has(BEAK) -> {
            shape(ellipse(16, 19.1, 2.7, 1.2), ORANGE)
            shape(ellipse(16, 17.5, 3.4, 1.8), ORANGE)
            dot(15.0, 16.8, ORANGE.shade); dot(17.0, 16.8, ORANGE.shade)
        }
        has(PROBOSCIS) -> {
            fill(rect(15, 17, 17, 20), ANTENNA.base)
            fill(seg(16, 20, 15.6, 24, 0.7), ANTENNA.base)
        }
        else -> fill(seg(14.3, 17.4 + dy, 15.4, 18.4 + dy, 0.9) or seg(15.4, 18.4 + dy, 16.6, 18.4 + dy, 0.9) or seg(16.6, 18.4 + dy, 17.7, 17.4 + dy, 0.9), INK)
    }
    if (has(SUNGLASSES)) {
        shape(rrect(10.3, 12.6, 15.4, 16.2) or rrect(16.6, 12.6, 21.7, 16.2) or rect(15, 13.4, 17, 14.4), SHADES, 1)
        fill(seg(11.4, 13.5, 13.2, 13.5, 0.5) or seg(17.7, 13.5, 19.5, 13.5, 0.5), GLINT)
    }
}

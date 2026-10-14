package de.habits.app.ui

import java.time.LocalDate

/** A short "be proud of yourself" message, shown once everything due today is done. */
data class Encouragement(val text: String, val author: String? = null, val emoji: String)

private val messages = listOf(
    "Alles erledigt. Genau so sieht Dranbleiben aus.",
    "Heute hast du Wort gehalten – dir selbst gegenüber.",
    "Haken dran. Du darfst richtig stolz auf dich sein.",
    "Kleine Schritte, große Wirkung. Heute war so ein Tag.",
    "Du hast heute für dein zukünftiges Ich vorgesorgt.",
    "Erledigt ist erledigt. Lehn dich zurück.",
    "Disziplin heute, Freiheit morgen. Gut gemacht.",
    "Wieder ein Tag, an dem du gezeigt hast, was in dir steckt.",
    "Das war kein Zufall – das warst du.",
    "Du wirst gerade jemand, auf den du dich verlassen kannst: du selbst.",
    "Alles geschafft. Gönn dir einen Moment Ruhe.",
    "Gewohnheiten entstehen an Tagen wie heute.",
    "Heute zählte. Und du hast geliefert.",
    "Stark – nicht weil es leicht war, sondern weil du es gemacht hast.",
    "Jeder Haken ist eine Stimme für die Person, die du sein willst.",
    "Nichts aufgeschoben. Das verdient Respekt.",
    "Feierabend für deine Habits. Heute gibt es nichts mehr zu tun.",
    "Tag für Tag wird aus Mühe Selbstverständlichkeit.",
    "Du bist deinem Ziel heute ein echtes Stück näher gekommen.",
    "Fühlt sich gut an, oder? Halt das Gefühl kurz fest.",
    "Dranbleiben ist leise – aber es wirkt laut.",
    "Der innere Schweinehund hatte heute keine Chance.",
    "Fertig. Und zwar richtig.",
    "Dein zukünftiges Ich sagt danke.",
    "Nicht perfekt sein, sondern dranbleiben – heute warst du beides.",
    "Mission erfüllt. Jetzt darfst du den Tag genießen.",
    "Heute hast du bewiesen, dass du es kannst. Morgen weißt du es schon.",
    "Kein großes Drama, einfach gemacht. So geht das.",
    "Du hast heute in dich investiert. Die Zinsen kommen.",
    "Ein ruhiger Kopf, ein voller Haken-Zettel. Schön.",
)

private val quotes = listOf(
    "Wir sind, was wir wiederholt tun. Vortrefflichkeit ist daher keine Handlung, sondern eine Gewohnheit." to "Will Durant (über Aristoteles)",
    "Auch eine Reise von tausend Meilen beginnt mit dem ersten Schritt." to "Laozi",
    "Es ist nicht wenig Zeit, die wir haben, sondern viel Zeit, die wir nicht nutzen." to "Seneca",
    "Es ist nicht genug zu wissen, man muss auch anwenden; es ist nicht genug zu wollen, man muss auch tun." to "Johann Wolfgang von Goethe",
    "Steter Tropfen höhlt den Stein." to "Ovid",
    "Jede Gewohnheit und Fähigkeit wird durch die entsprechenden Handlungen gestärkt: das Gehen durch Gehen, das Laufen durch Laufen." to "Epiktet",
    "Eine Schwalbe macht noch keinen Frühling, auch nicht ein einziger Tag." to "Aristoteles",
    "Erfolg ist die Summe kleiner Anstrengungen, die Tag für Tag wiederholt werden." to "Robert Collier",
    "Motivation bringt dich in Gang. Gewohnheit hält dich in Bewegung." to "Jim Ryun",
    "Es erscheint immer unmöglich, bis es getan ist." to "Nelson Mandela",
)

private val emojis = listOf("🌿", "✨", "🌅", "🌙", "🍃", "🌻", "🏔️", "☕")

val encouragements: List<Encouragement> =
    messages.mapIndexed { i, m -> Encouragement(m, emoji = emojis[i % emojis.size]) } +
        quotes.mapIndexed { i, (q, a) -> Encouragement(q, a, emojis[(i + 3) % emojis.size]) }

/** Stable for a day (no flicker on recomposition); [offset] steps through the list, e.g. on tap. */
fun encouragementFor(date: LocalDate, offset: Int = 0): Encouragement =
    encouragements[Math.floorMod(date.toEpochDay() * 13 + offset, encouragements.size.toLong()).toInt()]

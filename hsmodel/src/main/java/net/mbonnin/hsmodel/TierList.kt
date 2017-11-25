package net.mbonnin.hsmodel

class TierScore(val Hero: String?, val Score: Float)
// null Hero is the neutral hero
class TierCard(val CardId: String, val Hero: String?, val Scores: List<TierScore>)
class TierCards(val Cards: List<TierCard>)

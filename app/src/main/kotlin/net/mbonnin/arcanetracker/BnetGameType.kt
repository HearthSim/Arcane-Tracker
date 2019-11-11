package net.mbonnin.arcanetracker

import net.hearthsim.hslog.parser.power.FormatType
import net.hearthsim.hslog.parser.power.GameType

enum class BnetGameType(val intValue: Int) {
    BGT_UNKNOWN(0),
    BGT_FRIENDS(1),
    BGT_RANKED_STANDARD(2),
    BGT_ARENA(3),
    BGT_VS_AI(4),
    BGT_TUTORIAL(5),
    BGT_ASYNC(6),
    UNUSED7(7),
    UNUSED8(8),
    BGT_CASUAL_STANDARD_NEWBIE(9),
    BGT_CASUAL_STANDARD_NORMAL(10),
    BGT_TEST1(11),
    BGT_TEST2(12),
    BGT_TEST3(13),
    UNUSED14(14),
    UNUSED15(15),
    BGT_TAVERNBRAWL_PVP(16),
    BGT_TAVERNBRAWL_1P_VERSUS_AI(17),
    BGT_TAVERNBRAWL_2P_COOP(18),
    UNUSED19(19),
    UNUSED20(20),
    UNUSED21(21),
    UNUSED22(22),
    UNUSED23(23),
    UNUSED24(24),
    UNUSED25(25),
    UNUSED26(26),
    UNUSED27(27),
    UNUSED28(28),
    UNUSED29(29),
    BGT_RANKED_WILD(30),
    BGT_CASUAL_WILD(31),
    UNUSED32(32),
    UNUSED33(33),
    BGT_FSG_BRAWL_VS_FRIEND(40),
    BGT_FSG_BRAWL_PVP(41),
    BGT_FSG_BRAWL_1P_VERSUS_AI(42),
    BGT_FSG_BRAWL_2P_COOP(43),
    BGT_RANKED_STANDARD_NEW_PLAYER(45),
    BGT_BATTLEGROUNDS(50),

}

fun fromGameAndFormat(gameType: GameType, format: FormatType): BnetGameType {
    return when(gameType) {
        GameType.GT_ARENA -> BnetGameType.BGT_ARENA
        GameType.GT_TAVERNBRAWL-> BnetGameType.BGT_TAVERNBRAWL_1P_VERSUS_AI
        GameType.GT_VS_AI -> BnetGameType.BGT_VS_AI
        GameType.GT_CASUAL -> {
            when(format) {
                FormatType.FT_STANDARD -> BnetGameType.BGT_CASUAL_STANDARD_NORMAL
                FormatType.FT_WILD -> BnetGameType.BGT_CASUAL_WILD
                else -> BnetGameType.BGT_UNKNOWN
            }
        }
        GameType.GT_RANKED -> {
            when(format) {
                FormatType.FT_STANDARD -> BnetGameType.BGT_RANKED_STANDARD
                FormatType.FT_WILD -> BnetGameType.BGT_RANKED_WILD
                else -> BnetGameType.BGT_UNKNOWN
            }
        }
        GameType.GT_BATTLEGROUNDS -> BnetGameType.BGT_BATTLEGROUNDS
        else -> BnetGameType.BGT_UNKNOWN
    }
}
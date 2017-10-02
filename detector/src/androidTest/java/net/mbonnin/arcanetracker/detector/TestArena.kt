package net.mbonnin.arcanetracker.detector

import android.support.test.InstrumentationRegistry
import android.util.Log
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.cardid.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.FileInputStream

class TestArena {
    val expected = ArrayList<String>()
    val detected = ArrayList<String>()
    val file = ArrayList<String>()
    val position = ArrayList<Int>()

    var method: ((ByteBufferImage) -> Array<String>)? = { byteBufferImage -> detector.detectArenaPhash(byteBufferImage)}

    companion object {
        lateinit var detector: Detector

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            CardJson.init("enUS")
            detector = Detector(InstrumentationRegistry.getTargetContext(), false)
        }
    }


    @Test
    fun testPhash() {
        doTest("/sdcard/tests/arena_choices/0.png", COLDLIGHT_SEER, TOLVIR_STONESHAPER, DEVILSAUR_EGG)
    }

    @Test
    fun doDetect() {
        doTest("/sdcard/tests/arena_choices/0.png", COLDLIGHT_SEER, TOLVIR_STONESHAPER, DEVILSAUR_EGG)
        doTest("/sdcard/tests/arena_choices/1.png", DEADSCALE_KNIGHT, RECKLESS_ROCKETEER, MAGMA_RAGER)
        doTest("/sdcard/tests/arena_choices/2.png", HOLY_SMITE, BIGTIME_RACKETEER, SQUIRMING_TENTACLE)
        doTest("/sdcard/tests/arena_choices/3.png", THE_CURATOR, NZOTH_THE_CORRUPTOR, FINJA_THE_FLYING_STAR)
        doTest("/sdcard/tests/arena_choices/4.png", NETHERSPITE_HISTORIAN, PRIMALFIN_LOOKOUT, MARK_OF_YSHAARJ)
        doTest("/sdcard/tests/arena_choices/5.png", STORMWIND_CHAMPION, DEADSCALE_KNIGHT, GURUBASHI_BERSERKER)
        doTest("/sdcard/tests/arena_choices/6.png", EARTHEN_SCALES, STRONGSHELL_SCAVENGER, MIND_CONTROL_TECH)
        doTest("/sdcard/tests/arena_choices/7.png", EVOLVED_KOBOLD, KOOKY_CHEMIST, GRIM_NECROMANCER)
        doTest("/sdcard/tests/arena_choices/8.png", PRINCE_VALANAR, GRUUL, PRINCE_MALCHEZAAR)
        doTest("/sdcard/tests/arena_choices/9.png", BOMB_SQUAD, ETERNAL_SERVITUDE, PINTSIZE_POTION)
        doTest("/sdcard/tests/arena_choices/10.png", DEVILSAUR_EGG, GADGETZAN_AUCTIONEER, BOOK_WYRM)
        doTest("/sdcard/tests/arena_choices/11.png", MENAGERIE_MAGICIAN, PRIMALFIN_LOOKOUT, ROCKPOOL_HUNTER)
        doTest("/sdcard/tests/arena_choices/12.png", MOAT_LURKER, SARONITE_CHAIN_GANG, SPIKED_HOGRIDER)
        doTest("/sdcard/tests/arena_choices/13.png", RALLYING_BLADE, TOLVIR_STONESHAPER, HOWLING_COMMANDER)
        doTest("/sdcard/tests/arena_choices/14.png", PRIMALFIN_CHAMPION, LIGHTS_SORROW, FIGHT_PROMOTER)
        doTest("/sdcard/tests/arena_choices/15.png", HAPPY_GHOUL, MINDBREAKER, MIDNIGHT_DRAKE)
        doTest("/sdcard/tests/arena_choices/16.png", DIVINE_STRENGTH, RED_MANA_WYRM, SMUGGLERS_RUN)
        doTest("/sdcard/tests/arena_choices/17.png", PRINCE_VALANAR, SUNKEEPER_TARIM, HOGGER_DOOM_OF_ELWYNN)
        doTest("/sdcard/tests/arena_choices/18.png", FACELESS_MANIPULATOR, ARCANE_GIANT, MEAT_WAGON)
        doTest("/sdcard/tests/arena_choices/19.png", ELVEN_ARCHER, REDEMPTION, HOZEN_HEALER)
        doTest("/sdcard/tests/arena_choices/20.png", SMUGGLERS_RUN, DUSKBOAR, SILVERMOON_PORTAL)
        doTest("/sdcard/tests/arena_choices/21.png", CORE_HOUND, FALLEN_SUN_CLERIC, CONSECRATION)
        doTest("/sdcard/tests/arena_choices/22.png", MALFURION_STORMRAGE, UTHER_LIGHTBRINGER, THRALL)
        doTest("/sdcard/tests/arena_choices/23.png", DEVILSAUR_EGG, COLDLIGHT_SEER, GADGETZAN_AUCTIONEER)
        doTest("/sdcard/tests/arena_choices/24.png", PRIMALFIN_LOOKOUT, NIGHTBANE_TEMPLAR, ROCKPOOL_HUNTER)
        doTest("/sdcard/tests/arena_choices/25.png", STEWARD_OF_DARKSHIRE, EMPEROR_COBRA, CORPSE_RAISER)
        doTest("/sdcard/tests/arena_choices/26.png", HUMILITY, GADGETZAN_SOCIALITE, SKELEMANCER)
        doTest("/sdcard/tests/arena_choices/27.png", HYDROLOGIST, ACOLYTE_OF_PAIN, GROOK_FU_MASTER)
        doTest("/sdcard/tests/arena_choices/28.png", WINDFURY_HARPY, MISTRESS_OF_MIXTURES, DUSKBOAR)
        doTest("/sdcard/tests/arena_choices/29.png", HOLY_LIGHT, WICKED_SKELETON, VOODOO_DOCTOR)
        doTest("/sdcard/tests/arena_choices/30.png", SOUTHSEA_DECKHAND, BOG_CREEPER, GROOK_FU_MASTER)
        doTest("/sdcard/tests/arena_choices/31.png", KEENING_BANSHEE, YOUNG_PRIESTESS, LIGHTFUSED_STEGODON)
        doTest("/sdcard/tests/arena_choices/32.png", ELVEN_ARCHER, EARTHEN_RING_FARSEER, DARK_CONVICTION)
        doTest("/sdcard/tests/arena_choices/33.png", ARCHMAGE, HAMMER_OF_WRATH, REDEMPTION)
        doTest("/sdcard/tests/arena_choices/34.png", FURNACEFIRE_COLOSSUS, SCALED_NIGHTMARE, LIGHTS_SORROW)
        doTest("/sdcard/tests/arena_choices/35.png", ARCANOSMITH, HOLY_LIGHT, SPITEFUL_SMITH)
        doTest("/sdcard/tests/arena_choices/36.png", MENAGERIE_MAGICIAN, YOUNG_DRAGONHAWK, SQUIRMING_TENTACLE)
        doTest("/sdcard/tests/arena_choices/37.png", LIGHTWARDEN, CRAZED_ALCHEMIST, HOWLING_COMMANDER)
        doTest("/sdcard/tests/arena_choices/38.png", BLOWGILL_SNIPER, AMGAM_RAGER, STEGODON)
        doTest("/sdcard/tests/arena_choices/39.png", ARROGANT_CRUSADER, TWILIGHT_DRAKE, ALARMOBOT)
        doTest("/sdcard/tests/arena_choices/40.png", RAVENHOLDT_ASSASSIN, TICKING_ABOMINATION, COLDLIGHT_ORACLE)
        doTest("/sdcard/tests/arena_choices/41.png", SPIKERIDGED_STEED, LIGHTFUSED_STEGODON, ARGENT_COMMANDER)
        doTest("/sdcard/tests/arena_choices/42.png", HIRED_GUN, COBALT_SCALEBANE, STAND_AGAINST_DARKNESS)
        doTest("/sdcard/tests/arena_choices/43.png", SILVERMOON_PORTAL, PRIESTESS_OF_ELUNE, SATED_THRESHADON)
        doTest("/sdcard/tests/arena_choices/44.png", HUMILITY, ABERRANT_BERSERKER, DARK_CONVICTION)
        doTest("/sdcard/tests/arena_choices/45.png", SPIKERIDGED_STEED, BONE_DRAKE, GETAWAY_KODO)
        doTest("/sdcard/tests/arena_choices/46.png", NIGHT_HOWLER, BILEFIN_TIDEHUNTER, GRAVE_SHAMBLER)
        doTest("/sdcard/tests/arena_choices/47.png", SARONITE_CHAIN_GANG, GRIMESTREET_ENFORCER, ARROGANT_CRUSADER)
        doTest("/sdcard/tests/arena_choices/48.png", REXXAR, MALFURION_STORMRAGE, TYRANDE_WHISPERWIND)

        var failed = 0
        for (i in 0 until expected.size) {
            if (expected[i] != detected[i]) {
                val expectedName = CardJson.getCard(expected[i])
                val detectedName = CardJson.getCard(detected[i])
                Log.d("TestArena", String.format("%10s[%d]: %.20s detected instead of %20s", file[i], position[i], detectedName, expectedName))
                failed++
            }
        }
        Log.d("TestArena", String.format("%d/%d (%f%%)", failed, expected.size, failed.toDouble() / expected.size))
    }

    fun doTest(imagePath: String, vararg id: String) {
        val byteBufferImage = inputStreamToByteBufferImage(FileInputStream(imagePath))
        val result = method!!.invoke(byteBufferImage)

        for (i in 0 until 3) {
            expected.add(id[i])
            detected.add(result[i])
            file.add(imagePath.substringAfterLast("/"))
            position.add(i)
        }
    }
}
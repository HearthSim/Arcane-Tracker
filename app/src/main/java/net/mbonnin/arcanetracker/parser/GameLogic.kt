package net.mbonnin.arcanetracker.parser

/*
 * Created by martin on 11/11/16.
 */

import com.annimon.stream.function.Predicate
import net.mbonnin.arcanetracker.SecretLogic
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.parser.power.*
import net.mbonnin.arcanetracker.parser.power.BlockTag.TYPE_TRIGGER
import net.mbonnin.hsmodel.enum.CardId
import net.mbonnin.hsmodel.enum.Rarity
import net.mbonnin.hsmodel.enum.Type
import timber.log.Timber
import java.util.*

class GameLogic private constructor() {

    private val mListenerList = ArrayList<Listener>()
    private var mGame: Game? = null
    private var mCurrentTurn: Int = 0
    private var mLastTag: Boolean = false
    private var spectator = false

    private fun secretEntityList(): EntityList {
        return mGame!!
                .getEntityList { e -> Entity.ZONE_SECRET == e.tags[Entity.KEY_ZONE] }
                .filter (Predicate{ e -> Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY] }) // LEGENDARY secrets are actually quests
    }

    fun handleRootTag(tag: Tag) {
        //Timber.d("handle tag: " + tag);
        when (tag) {
            is CreateGameTag -> handleCreateGameTag(tag)
            is SpectatorTag -> spectator = tag.spectator
        }

        if (mGame != null) {
            handleTagRecursive(tag)
            if (mGame!!.isStarted) {
                handleTagRecursive2(tag)

                guessIds(tag)

                notifyListeners()
            }

            if (mLastTag) {
                if (mGame!!.isStarted) {
                    mGame!!.victory = Entity.PLAYSTATE_WON == mGame!!.player!!.entity!!.tags[Entity.KEY_PLAYSTATE]
                    for (listener in mListenerList) {
                        listener.gameOver()
                    }
                }

                mGame = null
                mLastTag = false
            }
        }
    }

    private fun guessIds(tag: Tag) {
        val stack = ArrayList<BlockTag>()

        guessIdsRecursive(stack, tag)
    }

    private fun guessIdsRecursive(stack: ArrayList<BlockTag>, tag: Tag) {
        if (tag is FullEntityTag) {
            tryToGuessCardIdFromBlock(stack, tag)
        } else if (tag is BlockTag) {
            stack.add(tag)
            for (child in tag.children) {
                guessIdsRecursive(stack, child)
            }
        }
    }

    private fun handleBlockTag(tag: BlockTag) {}

    private fun handleBlockTag2(tag: BlockTag) {
        val game = mGame!!

        if (BlockTag.TYPE_PLAY == tag.BlockType) {
            val playedEntity = mGame!!.findEntitySafe(tag.Entity)
            if (playedEntity!!.CardID == null) {
                Timber.e("no CardID for play")
                return
            }

            val play = Play()
            play.turn = mCurrentTurn
            play.cardId = playedEntity.CardID
            play.isOpponent = game.findController(playedEntity).isOpponent

            mGame!!.lastPlayedCardId = play.cardId
            Timber.i("%s played %s", if (play.isOpponent) "opponent" else "I", play.cardId)

            SecretLogic.blockPlayed(game, tag.Target, playedEntity)

            game.plays.add(play)
        } else if (BlockTag.TYPE_ATTACK == tag.BlockType) {

            SecretLogic.blockAttack(game, tag)
        }
    }


    private fun handleTagRecursive(tag: Tag) {
        when (tag) {
            is TagChangeTag -> handleTagChange(tag)
            is FullEntityTag -> handleFullEntityTag(tag)
            is BlockTag -> {
                handleBlockTag(tag)
                for (child in tag.children) {
                    handleTagRecursive(child)
                }
            }
            is ShowEntityTag -> handleShowEntityTag(tag)
            is BuildNumberTag -> mGame!!.buildNumber = tag.buildNumber
            is GameTypeTag -> mGame!!.gameType = tag.gameType
            is FormatTypeTag -> mGame!!.formatType = tag.formatType
            is ScenarioIdTag -> mGame!!.scenarioId = tag.scenarioId
            is PlayerMappingTag -> handlePlayerMapping(tag)
        }
    }

    private fun handlePlayerMapping(tag: PlayerMappingTag) {
        val player = mGame!!.playerMap[tag.playerId]
        if (player == null) {
            Timber.e("Cannot find player Id '%s'", tag.playerId)
            return
        }

        val battleTagEntity = mGame!!.entityMap[tag.playerName]
        if (battleTagEntity != null) {
            /**
             * merge all tags
             */
            player.entity!!.tags.putAll(battleTagEntity.tags)
        }

        Timber.w(tag.playerName + " now points to entity " + player.entity!!.EntityID)

        player.battleTag = tag.playerName

        /*
         * make the battleTag point to the same entity..
         */
        mGame!!.entityMap.put(tag.playerName, player!!.entity!!)
    }

    private fun handleTagRecursive2(tag: Tag) {
        when (tag) {
            is TagChangeTag -> handleTagChange2(tag)
            is FullEntityTag -> handleFullEntityTag2(tag)
            is BlockTag -> {
                handleBlockTag2(tag)
                for (child in tag.children) {
                    handleTagRecursive2(child)
                }
            }
            is ShowEntityTag -> handleShowEntityTag2(tag)
            is MetaDataTag -> handleMetaDataTag2(tag)
        }
    }

    private fun handleMetaDataTag2(tag: MetaDataTag) {
        if (MetaDataTag.META_DAMAGE == tag.Meta) {
            SecretLogic.damage(mGame!!, tag)
        }
    }

    private fun handleShowEntityTag(tag: ShowEntityTag) {
        val entity = mGame!!.findEntitySafe(tag.Entity)

        if (!Utils.isEmpty(entity!!.CardID) && entity.CardID != tag.CardID) {
            Timber.e("[Inconsistent] entity " + entity + " changed cardId " + entity.CardID + " -> " + tag.CardID)
        }
        entity.setCardId(tag.CardID)

        for (key in tag.tags.keys) {
            tagChanged(entity, key, tag.tags[key])
        }
    }


    private fun handleShowEntityTag2(tag: ShowEntityTag) {
        val entity = mGame!!.findEntitySafe(tag.Entity)

        for (key in tag.tags.keys) {
            tagChanged2(entity!!, key, tag.tags[key])
        }
    }

    private fun tagChanged2(entity: Entity, key: String, newValue: String?) {}

    private fun tagChanged(entity: Entity, key: String, newValue: String?) {
        val oldValue = entity.tags[key]

        entity.tags.put(key, newValue!!)

        if (Entity.ENTITY_ID_GAME == entity.EntityID) {
            if (Entity.KEY_TURN == key) {
                try {
                    mCurrentTurn = Integer.parseInt(newValue)
                    Timber.d("turn: " + mCurrentTurn)
                } catch (e: Exception) {
                    Timber.e(e)
                }

            } else if (Entity.KEY_STEP == key) {
                if (Entity.STEP_BEGIN_MULLIGAN == newValue) {
                    gameStepBeginMulligan()
                    if (mGame!!.isStarted) {
                        for (listener in mListenerList) {
                            listener.gameStarted(mGame!!)
                        }
                    }
                } else if (Entity.STEP_FINAL_GAMEOVER == newValue) {
                    // do not set mGame = null here, we might be part of a block where other tag handlers
                    // require access to mGame
                    mLastTag = true
                }
            }
        }

        if (Entity.KEY_ZONE == key) {
            if (Entity.ZONE_HAND != oldValue && Entity.ZONE_HAND == newValue) {
                val step = mGame!!.gameEntity!!.tags[Entity.KEY_STEP]
                if (step == null) {
                    // this is the original mulligan
                    entity.extra.drawTurn = 0
                } else if (Entity.STEP_BEGIN_MULLIGAN == step) {
                    entity.extra.drawTurn = 0
                    entity.extra.mulliganed = true
                } else {
                    entity.extra.drawTurn = mCurrentTurn
                }

                if (Entity.ZONE_DECK == oldValue) {
                    // we should not give too much information about what cards the opponent has
                    entity.extra.hide = true
                }
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_PLAY == newValue) {
                entity.extra.playTurn = mCurrentTurn
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_SECRET == newValue) {
                entity.extra.playTurn = mCurrentTurn
            } else if (Entity.ZONE_PLAY == oldValue && Entity.ZONE_GRAVEYARD == newValue) {
                entity.extra.diedTurn = mCurrentTurn
                SecretLogic.minionDied(mGame!!, entity)
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_HAND != newValue) {
                /*
                 * card was put back in the deck (most likely from mulligan)
                 */
                entity.extra.drawTurn = -1
                /*
                 * no reason to hide it anymore
                 */
                entity.extra.hide = false
            }
        }

        if (Entity.KEY_TURN == key) {
            SecretLogic.newTurn(mGame!!)
        }
    }

    private fun handleCreateGameTag(tag: CreateGameTag) {
        mLastTag = false

        if (mGame != null && tag.gameEntity.tags[Entity.KEY_TURN] != null) {
            Timber.w("CREATE_GAME during an existing one, resuming")
        } else {
            mGame = Game()

            var player: Player
            var entity: Entity

            entity = Entity()
            entity.EntityID = tag.gameEntity.EntityID
            entity.tags.putAll(tag.gameEntity.tags)
            mGame!!.addEntity(entity)
            mGame!!.gameEntity = entity
            mGame!!.spectator = spectator

            for (playerTag in tag.playerList) {
                entity = Entity()
                entity.EntityID = playerTag.EntityID
                entity.PlayerID = playerTag.PlayerID
                entity.tags.putAll(playerTag.tags)
                mGame!!.addEntity(entity)
                player = Player()
                player.entity = entity
                mGame!!.playerMap[entity.PlayerID!!] = player
            }
        }
    }

    fun removeListener(listener: Listener) {
        mListenerList.remove(listener)
    }

    interface Listener {
        /**
         * when gameStarted is called, game.player and game.opponent are set
         * the initial mulligan cards are known too. It's ok to store 'game' as there can be only one at a time
         */
        fun gameStarted(game: Game)

        fun gameOver()

        /**
         * this is called whenever something changes :)
         */
        fun somethingChanged()
    }

    fun addListener(listener: Listener) {
        mListenerList.add(listener)
    }

    private fun gameStepBeginMulligan() {

        var knownCardsInHand = 0
        var totalCardsInHand = 0

        val player1 = mGame!!.playerMap["1"]
        val player2 = mGame!!.playerMap["2"]

        if (player1 == null || player2 == null) {
            Timber.e("cannot find players")
            return
        }

        val entities = mGame!!.getEntityList { entity -> "1" == entity.tags[Entity.KEY_CONTROLLER] && Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE] }

        for (entity in entities) {
            if (!Utils.isEmpty(entity.CardID)) {
                knownCardsInHand++
            }
            totalCardsInHand++
        }

        player1.isOpponent = knownCardsInHand < 3
        player1.hasCoin = totalCardsInHand > 3

        player2.isOpponent = !player1.isOpponent
        player2.hasCoin = !player1.hasCoin

        mGame!!.player = if (player1.isOpponent) player2 else player1
        mGame!!.opponent = if (player1.isOpponent) player1 else player2
    }


    private fun notifyListeners() {
        if (mGame != null && mGame!!.isStarted) {
            for (listener in mListenerList) {
                listener.somethingChanged()
            }
        }
    }

    private fun handleTagChange(tag: TagChangeTag) {
        tagChanged(mGame!!.findEntitySafe(tag.ID)!!, tag.tag, tag.value)
    }

    private fun handleTagChange2(tag: TagChangeTag) {
        tagChanged2(mGame!!.findEntitySafe(tag.ID)!!, tag.tag, tag.value)
    }

    private fun tryToGuessCardIdFromBlock(stack: ArrayList<BlockTag>, fullEntityTag: FullEntityTag) {
        if (stack.isEmpty()) {
            return
        }

        val blockTag = stack[stack.size - 1]

        val blockEntity = mGame!!.findEntitySafe(blockTag.Entity)
        val entity = mGame!!.findEntitySafe(fullEntityTag.ID)

        if (Utils.isEmpty(blockEntity!!.CardID)) {
            return
        }

        var guessedId: String? = null

        if (BlockTag.TYPE_POWER == blockTag.BlockType) {

            // battlecry or active effect
            guessedId = when (blockEntity.CardID) {
                CardId.GANG_UP,
                CardId.RECYCLE,
                CardId.SHADOWCASTER,
                CardId.MANIC_SOULCASTER,
                CardId.DIRE_FRENZY,
                CardId.BALEFUL_BANKER,
                CardId.HOLY_WATER,
                CardId.SPLINTERGRAFT -> mGame!!.findEntitySafe(blockTag.Target)!!.CardID
            //CardId.DOLLMASTER_DORIAN
                CardId.WANTED -> CardId.THE_COIN
                CardId.BENEATH_THE_GROUNDS -> CardId.NERUBIAN_AMBUSH
                CardId.IRON_JUGGERNAUT -> CardId.BURROWING_MINE
                CardId.FORGOTTEN_TORCH -> CardId.ROARING_TORCH
                CardId.CURSE_OF_RAFAAM -> CardId.CURSED
                CardId.ANCIENT_SHADE -> CardId.ANCIENT_CURSE
                CardId.EXCAVATED_EVIL -> CardId.EXCAVATED_EVIL
                CardId.ELISE_STARSEEKER -> CardId.MAP_TO_THE_GOLDEN_MONKEY
                CardId.MAP_TO_THE_GOLDEN_MONKEY -> CardId.GOLDEN_MONKEY
                CardId.DOOMCALLER -> CardId.CTHUN
                CardId.JADE_IDOL -> CardId.JADE_IDOL
                CardId.FLAME_GEYSER, CardId.FIRE_FLY -> CardId.FLAME_ELEMENTAL
                CardId.STEAM_SURGER -> CardId.FLAME_GEYSER
                CardId.RAZORPETAL_VOLLEY, CardId.RAZORPETAL_LASHER -> CardId.RAZORPETAL
                CardId.MUKLA_TYRANT_OF_THE_VALE, CardId.KING_MUKLA -> CardId.BANANAS
                CardId.JUNGLE_GIANTS -> CardId.BARNABUS_THE_STOMPER
                CardId.THE_MARSH_QUEEN -> CardId.QUEEN_CARNASSA
                CardId.OPEN_THE_WAYGATE -> CardId.TIME_WARP
                CardId.THE_LAST_KALEIDOSAUR -> CardId.GALVADON
                CardId.AWAKEN_THE_MAKERS -> CardId.AMARA_WARDEN_OF_HOPE
                CardId.THE_CAVERNS_BELOW -> CardId.CRYSTAL_CORE
                CardId.UNITE_THE_MURLOCS -> CardId.MEGAFIN
                CardId.LAKKARI_SACRIFICE -> CardId.NETHER_PORTAL
                CardId.FIRE_PLUMES_HEART -> CardId.SULFURAS
                CardId.GHASTLY_CONJURER -> CardId.MIRROR_IMAGE
                CardId.EXPLORE_UNGORO -> CardId.CHOOSE_YOUR_PATH
                CardId.ELISE_THE_TRAILBLAZER -> CardId.UNGORO_PACK
                CardId.FALDOREI_STRIDER -> CardId.SPIDER_AMBUSH
                CardId.DECK_OF_WONDERS -> CardId.SCROLL_OF_WONDER
                CardId.FERAL_GIBBERER -> CardId.FERAL_GIBBERER
                else -> null
            }
        } else if (TYPE_TRIGGER == blockTag.BlockType) {

            // deathrattle or passive effect
            guessedId = when (blockEntity!!.CardID) {
                CardId.PYROS -> CardId.PYROS1
                CardId.PYROS1 -> CardId.PYROS2
                CardId.WHITE_EYES -> CardId.THE_STORM_GUARDIAN
                CardId.DEADLY_FORK -> CardId.SHARP_FORK
                CardId.BURGLY_BULLY -> CardId.THE_COIN
                CardId.IGNEOUS_ELEMENTAL -> CardId.FLAME_ELEMENTAL
                CardId.RHONIN -> CardId.ARCANE_MISSILES
                CardId.FROZEN_CLONE -> stack.firstOrNull { BlockTag.TYPE_PLAY == it.BlockType }?.let { mGame!!.findEntitySafe(it.Entity)!!.CardID }
                CardId.BONE_BARON -> CardId.SKELETON
                CardId.WEASEL_TUNNELER -> CardId.WEASEL_TUNNELER
                CardId.RAPTOR_HATCHLING -> CardId.RAPTOR_PATRIARCH
                CardId.DIREHORN_HATCHLING -> CardId.DIREHORN_MATRIARCH
                CardId.MANA_BIND -> stack.firstOrNull { BlockTag.TYPE_PLAY == it.BlockType }?.let { mGame!!.findEntitySafe(it.Entity)!!.CardID }
                CardId.ARCHMAGE_ANTONIDAS -> CardId.FIREBALL
                CardId.HOARDING_DRAGON -> CardId.THE_COIN
                CardId.ASTRAL_TIGER -> CardId.ASTRAL_TIGER
                CardId.DRYGULCH_JAILOR -> CardId.SILVER_HAND_RECRUIT
                CardId.GILDED_GARGOYLE -> CardId.THE_COIN
                CardId.RIN_THE_FIRST_DISCIPLE -> CardId.THE_FIRST_SEAL
                CardId.THE_FIRST_SEAL -> CardId.THE_SECOND_SEAL
                CardId.THE_THIRD_SEAL -> CardId.THE_FOURTH_SEAL
                CardId.THE_FOURTH_SEAL -> CardId.THE_FINAL_SEAL
                CardId.THE_FINAL_SEAL -> CardId.AZARI_THE_DEVOURER
                CardId.MALORNE -> CardId.MALORNE
                else -> null
            }
        }

        if (!Utils.isEmpty(guessedId)) {
            entity!!.setCardId(guessedId!!)
        }

        // even if we don't know the guessedId, record that this was createdBy this entity
        entity!!.extra.createdBy = blockEntity!!.CardID
    }

    private fun handleFullEntityTag2(tag: FullEntityTag) {

    }

    private fun handleFullEntityTag(tag: FullEntityTag) {
        var entity: Entity? = mGame!!.entityMap[tag.ID]

        if (entity == null) {
            entity = Entity()
            mGame!!.entityMap.put(tag.ID, entity)
        }
        entity.EntityID = tag.ID
        if (!Utils.isEmpty(tag.CardID)) {
            entity.setCardId(tag.CardID)
        }
        entity.tags.putAll(tag.tags)

        if (Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE]) {
            entity.extra.drawTurn = mCurrentTurn
        }

        val playerId = entity.tags[Entity.KEY_CONTROLLER]
        val cardType = entity.tags[Entity.KEY_CARDTYPE]
        val player = mGame!!.findController(entity)

        Timber.i("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags[Entity.KEY_ZONE])

        if (Type.HERO == cardType) {
            player.hero = entity
        } else if (Type.HERO_POWER == cardType) {
            player.heroPower = entity
        } else {
            if (mGame!!.gameEntity!!.tags[Entity.KEY_STEP] == null) {
                if (Entity.ZONE_DECK == entity.tags[Entity.KEY_ZONE]) {
                    entity.extra.originalController = entity.tags[Entity.KEY_CONTROLLER]
                } else if (Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE]) {
                    // this must be the coin
                    entity.setCardId(CardId.THE_COIN)
                    entity.extra.drawTurn = 0
                }
            }
        }
    }

    companion object {
        private var sGameLogic: GameLogic? = null

        fun get(): GameLogic {
            if (sGameLogic == null) {
                sGameLogic = GameLogic()
            }
            return sGameLogic!!
        }

        fun gameTurnToHumanTurn(turn: Int): Int {
            return (turn + 1) / 2
        }
    }
}

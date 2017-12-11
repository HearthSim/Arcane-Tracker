package net.mbonnin.arcanetracker.parser

/*
 * Created by martin on 11/11/16.
 */

import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.parser.power.*
import net.mbonnin.arcanetracker.parser.power.BlockTag.TYPE_TRIGGER
import net.mbonnin.hsmodel.CardId
import net.mbonnin.hsmodel.Rarity
import net.mbonnin.hsmodel.Type
import timber.log.Timber
import java.util.*

class GameLogic private constructor() {

    private val mListenerList = ArrayList<Listener>()
    private var mGame: Game? = null
    private var mCurrentTurn: Int = 0
    private var mLastTag: Boolean = false
    private var spectator = false

    private/*
         * don't factor in the epic secret which are all quests for now
         */ val secretEntityList: EntityList
        get() = mGame!!.getEntityList { e -> Entity.ZONE_SECRET == e.tags[Entity.KEY_ZONE] }
                .filter { e -> Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY] }

    fun handleRootTag(tag: Tag) {
        //Timber.d("handle tag: " + tag);
        when(tag) {
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
                    mGame!!.victory = Entity.PLAYSTATE_WON == mGame!!.player.entity.tags[Entity.KEY_PLAYSTATE]
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
        val game = mGame

        if (BlockTag.TYPE_PLAY == tag.BlockType) {
            val playedEntity = mGame!!.findEntitySafe(tag.Entity)
            if (playedEntity.CardID == null) {
                Timber.e("no CardID for play")
                return
            }

            val play = Play()
            play.turn = mCurrentTurn
            play.cardId = playedEntity.CardID
            play.isOpponent = game!!.findController(playedEntity).isOpponent

            mGame!!.lastPlayedCardId = play.cardId
            Timber.i("%s played %s", if (play.isOpponent) "opponent" else "I", play.cardId)

            /*
             * secret detector
             */
            val secretEntityList = secretEntityList
            for (secretEntity in secretEntityList) {
                if (!Utils.equalsNullSafe(secretEntity.tags[Entity.KEY_CONTROLLER], playedEntity.tags[Entity.KEY_CONTROLLER]) && !Utils.isEmpty(playedEntity.CardID)) {
                    /*
                     * it can happen that we don't know the id of the played entity, for an example if the player has a secret and its opponent plays one
                     * it should be ok to ignore those since these are opponent plays
                     */
                    if (Type.MINION == playedEntity.tags[Entity.KEY_CARDTYPE]) {
                        secretEntity.extra.otherPlayerPlayedMinion = true
                        if (getMinionsOnBoardForController(playedEntity.tags[Entity.KEY_CONTROLLER]?:"").size >= 3) {
                            secretEntity.extra.otherPlayerPlayedMinionWithThreeOnBoardAlready = true
                        }
                    } else if (Type.SPELL == playedEntity.tags[Entity.KEY_CARDTYPE]) {
                        secretEntity.extra.otherPlayerCastSpell = true
                        val targetEntiy = mGame!!.findEntityUnsafe(tag.Target)
                        if (targetEntiy != null && Type.MINION == targetEntiy.tags[Entity.KEY_CARDTYPE]) {
                            secretEntity.extra.selfMinionTargetedBySpell = true
                        }
                    } else if (Type.HERO_POWER == playedEntity.tags[Entity.KEY_CARDTYPE]) {
                        secretEntity.extra.otherPlayerHeroPowered = true
                    }
                }
            }

            game.plays.add(play)
        } else if (BlockTag.TYPE_ATTACK == tag.BlockType) {
            /*
             * secret detector
             */
            val targetEntity = mGame!!.findEntitySafe(tag.Target)

            val secretEntityList = secretEntityList
            for (secretEntity in secretEntityList) {
                if (Utils.equalsNullSafe(secretEntity.tags[Entity.KEY_CONTROLLER], targetEntity.tags[Entity.KEY_CONTROLLER])) {
                    if (Type.MINION == targetEntity.tags[Entity.KEY_CARDTYPE]) {
                        secretEntity.extra.selfMinionWasAttacked = true
                    } else if (Type.HERO == targetEntity.tags[Entity.KEY_CARDTYPE]) {
                        secretEntity.extra.selfHeroAttacked = true
                        val attackerEntity = mGame!!.findEntitySafe(tag.Entity)
                        if (Type.MINION == attackerEntity.tags[Entity.KEY_CARDTYPE]) {
                            secretEntity.extra.selfHeroAttackedByMinion = true
                        }
                    }
                }
            }

        }
    }


    private fun handleTagRecursive(tag: Tag) {
        if (tag is TagChangeTag) {
            handleTagChange(tag)
        } else if (tag is FullEntityTag) {
            handleFullEntityTag(tag)
        } else if (tag is BlockTag) {
            handleBlockTag(tag)
            for (child in tag.children) {
                handleTagRecursive(child)
            }
        } else if (tag is ShowEntityTag) {
            handleShowEntityTag(tag)
        }
    }

    private fun handleTagRecursive2(tag: Tag) {
        if (tag is TagChangeTag) {
            handleTagChange2(tag)
        } else if (tag is FullEntityTag) {
            handleFullEntityTag2(tag)
        } else if (tag is BlockTag) {
            handleBlockTag2(tag)
            for (child in tag.children) {
                handleTagRecursive2(child)
            }
        } else if (tag is ShowEntityTag) {
            handleShowEntityTag2(tag)
        } else if (tag is MetaDataTag) {
            handleMetaDataTag2(tag)
        }
    }

    private fun handleMetaDataTag2(tag: MetaDataTag) {
        if (MetaDataTag.META_DAMAGE == tag.Meta) {
            /*
             * secret detector
             */
            try {
                val damage = Integer.parseInt(tag.Data)
                if (damage > 0) {
                    for (id in tag.Info) {
                        val damagedEntity = mGame!!.findEntitySafe(id)
                        val secretEntityList = secretEntityList
                        for (e2 in secretEntityList) {
                            if (Utils.equalsNullSafe(e2.tags[Entity.KEY_CONTROLLER], damagedEntity.tags[Entity.KEY_CONTROLLER])) {
                                if (Type.HERO == damagedEntity.tags[Entity.KEY_CARDTYPE]) {
                                    e2.extra.selfHeroDamaged = true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

        }
    }

    private fun handleShowEntityTag(tag: ShowEntityTag) {
        val entity = mGame!!.findEntitySafe(tag.Entity)

        if (!Utils.isEmpty(entity.CardID) && entity.CardID != tag.CardID) {
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
            tagChanged2(entity, key, tag.tags[key])
        }
    }

    private fun tagChanged2(entity: Entity, key: String, newValue: String?) {}

    private fun tagChanged(entity: Entity, key: String, newValue: String?) {
        val oldValue = entity.tags[key]

        entity.tags.put(key, newValue)

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
                val step = mGame!!.gameEntity.tags[Entity.KEY_STEP]
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
                /*
                 * secret detector
                 */
                val secretEntityList = mGame!!.getEntityList { e -> Entity.ZONE_SECRET == e.tags[Entity.KEY_ZONE] }
                for (secretEntity in secretEntityList) {
                    if (Utils.equalsNullSafe(secretEntity.tags[Entity.KEY_CONTROLLER], entity.tags[Entity.KEY_CONTROLLER]) && Type.MINION == entity.tags[Entity.KEY_CARDTYPE]) {
                        val controllerEntity = mGame!!.findControllerEntity(entity)
                        if (controllerEntity != null && "0" == controllerEntity.tags[Entity.KEY_CURRENT_PLAYER]) {
                            secretEntity.extra.selfPlayerMinionDied = true
                        }
                    }
                }
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
            /*
             * secret detector
             */
            val secretEntityList = secretEntityList
            var currentPlayer: Entity? = null
            for (player in mGame!!.playerMap.values) {
                if ("1" == player.entity.tags[Entity.KEY_CURRENT_PLAYER]) {
                    currentPlayer = player.entity
                    Timber.d("Current player: " + currentPlayer!!.PlayerID + "(" + player.battleTag + ")")
                    break
                }
            }
            for (secretEntity in secretEntityList) {
                if (currentPlayer != null && Utils.equalsNullSafe(secretEntity.tags[Entity.KEY_CONTROLLER], currentPlayer.PlayerID)) {
                    val list = getMinionsOnBoardForController(secretEntity.tags[Entity.KEY_CONTROLLER])
                    if (!list.isEmpty()) {
                        Timber.d("Competitive condition")
                        secretEntity.extra.competitiveSpiritTriggerConditionHappened = true
                    }
                }
            }
        }
    }

    private fun getMinionsOnBoardForController(playerId: String?): EntityList {
        return mGame!!.getEntityList { e ->
            if (Entity.ZONE_PLAY != e.tags[Entity.KEY_ZONE]) {
                false
            } else if (Type.MINION != e.tags[Entity.KEY_CARDTYPE]) {
                false
            }
            Utils.equalsNullSafe(playerId, e.tags[Entity.KEY_CONTROLLER])
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
                mGame!!.playerMap.put(entity.PlayerID, player)
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

        /*
         * now try to match a battle tag with a player
         */
        for (battleTag in mGame!!.battleTags) {
            val battleTagEntity = mGame!!.entityMap[battleTag]
            val playsFirst = battleTagEntity!!.tags[Entity.KEY_FIRST_PLAYER]
            val player: Player

            if ("1" == playsFirst) {
                player = if (player1.hasCoin) player2 else player1
            } else {
                player = if (player1.hasCoin) player1 else player2
            }

            player.entity.tags.putAll(battleTagEntity.tags)
            player.battleTag = battleTag

            /*
             * make the battleTag point to the same entity..
             */
            Timber.w(battleTag + " now points to entity " + player.entity.EntityID)
            mGame!!.entityMap.put(battleTag, player.entity)
        }

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
        tagChanged(mGame!!.findEntitySafe(tag.ID), tag.tag, tag.value)
    }

    private fun handleTagChange2(tag: TagChangeTag) {
        tagChanged2(mGame!!.findEntitySafe(tag.ID), tag.tag, tag.value)
    }

    private fun tryToGuessCardIdFromBlock(stack: ArrayList<BlockTag>, fullEntityTag: FullEntityTag) {
        if (stack.isEmpty()) {
            return
        }

        val blockTag = stack[stack.size - 1]

        val blockEntity = mGame!!.findEntitySafe(blockTag.Entity)
        val entity = mGame!!.findEntitySafe(fullEntityTag.ID)

        if (Utils.isEmpty(blockEntity.CardID)) {
            return
        }

        var guessedId: String? = null

        if (BlockTag.TYPE_POWER == blockTag.BlockType) {

            when (blockEntity.CardID) {
                CardId.GANG_UP, CardId.RECYCLE, CardId.SHADOWCASTER, CardId.MANIC_SOULCASTER -> guessedId = mGame!!.findEntitySafe(blockTag.Target).CardID
                CardId.BENEATH_THE_GROUNDS -> guessedId = CardId.NERUBIAN_AMBUSH
                CardId.IRON_JUGGERNAUT -> guessedId = CardId.BURROWING_MINE
                CardId.FORGOTTEN_TORCH -> guessedId = CardId.ROARING_TORCH
                CardId.CURSE_OF_RAFAAM -> guessedId = CardId.CURSED
                CardId.ANCIENT_SHADE -> guessedId = CardId.ANCIENT_CURSE
                CardId.EXCAVATED_EVIL -> guessedId = CardId.EXCAVATED_EVIL
                CardId.ELISE_STARSEEKER -> guessedId = CardId.MAP_TO_THE_GOLDEN_MONKEY
                CardId.MAP_TO_THE_GOLDEN_MONKEY -> guessedId = CardId.GOLDEN_MONKEY
                CardId.DOOMCALLER -> guessedId = CardId.CTHUN
                CardId.JADE_IDOL -> guessedId = CardId.JADE_IDOL
                CardId.FLAME_GEYSER, CardId.FIRE_FLY -> guessedId = CardId.FLAME_ELEMENTAL
                CardId.STEAM_SURGER -> guessedId = CardId.FLAME_GEYSER
                CardId.RAZORPETAL_VOLLEY, CardId.RAZORPETAL_LASHER -> guessedId = CardId.RAZORPETAL
                CardId.MUKLA_TYRANT_OF_THE_VALE, CardId.KING_MUKLA -> guessedId = CardId.BANANAS
                CardId.JUNGLE_GIANTS -> guessedId = CardId.BARNABUS_THE_STOMPER
                CardId.THE_MARSH_QUEEN -> guessedId = CardId.QUEEN_CARNASSA
                CardId.OPEN_THE_WAYGATE -> guessedId = CardId.TIME_WARP
                CardId.THE_LAST_KALEIDOSAUR -> guessedId = CardId.GALVADON
                CardId.AWAKEN_THE_MAKERS -> guessedId = CardId.AMARA_WARDEN_OF_HOPE
                CardId.THE_CAVERNS_BELOW -> guessedId = CardId.CRYSTAL_CORE
                CardId.UNITE_THE_MURLOCS -> guessedId = CardId.MEGAFIN
                CardId.LAKKARI_SACRIFICE -> guessedId = CardId.NETHER_PORTAL
                CardId.FIRE_PLUMES_HEART -> guessedId = CardId.SULFURAS
                CardId.GHASTLY_CONJURER -> guessedId = CardId.MIRROR_IMAGE
                CardId.EXPLORE_UNGORO -> guessedId = CardId.CHOOSE_YOUR_PATH
                CardId.ELISE_THE_TRAILBLAZER -> guessedId = CardId.UNGORO_PACK
            }
        } else if (TYPE_TRIGGER == blockTag.BlockType) {
            when (blockEntity.CardID) {
                CardId.PYROS -> guessedId = CardId.PYROS1
                CardId.PYROS1 -> guessedId = CardId.PYROS2
                CardId.WHITE_EYES -> guessedId = CardId.THE_STORM_GUARDIAN
                CardId.DEADLY_FORK -> guessedId = CardId.SHARP_FORK
                CardId.BURGLY_BULLY -> guessedId = CardId.THE_COIN
                CardId.IGNEOUS_ELEMENTAL -> guessedId = CardId.FLAME_ELEMENTAL
                CardId.RHONIN -> guessedId = CardId.ARCANE_MISSILES
                CardId.FROZEN_CLONE -> for (parent in stack) {
                    if (BlockTag.TYPE_PLAY == parent.BlockType) {
                        guessedId = mGame!!.findEntitySafe(parent.Entity).CardID
                        break
                    }
                }
                CardId.BONE_BARON -> guessedId = CardId.SKELETON
                CardId.WEASEL_TUNNELER -> guessedId = CardId.WEASEL_TUNNELER
                CardId.RAPTOR_HATCHLING -> guessedId = CardId.RAPTOR_PATRIARCH
                CardId.DIREHORN_HATCHLING -> guessedId = CardId.DIREHORN_MATRIARCH
                CardId.MANA_BIND -> for (parent in stack) {
                    if (BlockTag.TYPE_PLAY == parent.BlockType) {
                        guessedId = mGame!!.findEntitySafe(parent.Entity).CardID
                        break
                    }
                }
                CardId.ARCHMAGE_ANTONIDAS -> guessedId = CardId.FIREBALL
            }
        }
        if (!Utils.isEmpty(guessedId)) {
            entity.setCardId(guessedId)
        }

        // even if we don't know the guessedId, record that this was createdBy this entity
        entity.extra.createdBy = blockEntity.CardID
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
            if (mGame!!.gameEntity.tags[Entity.KEY_STEP] == null) {
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

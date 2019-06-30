package net.hearthsim.hslog.parser.power

/*
 * Created by martin on 11/11/16.
 */

import net.hearthsim.console.Console
import net.hearthsim.hslog.parser.power.BlockTag.Companion.TYPE_TRIGGER
import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId
import net.hearthsim.hsmodel.enum.Type


class GameLogic(private val console: Console, private val cardJson: CardJson) {

    private val gameStartListenerList = mutableListOf<(Game) -> Unit>()
    private val gameEndListenerList = mutableListOf<(Game) -> Unit>()
    private val somethingChangedListenerList = mutableListOf<(Game) -> Unit>()
    private var mGame: Game? = null
    private var mCurrentTurn: Int = 0
    private var mLastTag: Boolean = false
    private var spectator = false
    private val secretLogic = SecretLogic(console)

    /**
     * This is the exposed game. As long as one game has started, this will always be non-null
     */
    var currentOrFinishedGame: Game? = null

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
                    for (listener in gameEndListenerList) {
                        listener(mGame!!)
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

    @Suppress("UNUSED_PARAMETER")
    private fun handleBlockTag(tag: BlockTag) {
    }

    private fun handleBlockTag2(tag: BlockTag) {
        val game = mGame!!

        if (BlockTag.TYPE_PLAY == tag.BlockType) {
            val playedEntity = mGame!!.findEntitySafe(tag.Entity!!)
            if (playedEntity!!.CardID == null) {
                console.error("no CardID for play")
                return
            }

            val isOpponent = game.findController(playedEntity).isOpponent

            mGame!!.lastPlayedCardId = playedEntity.CardID
            console.debug("${if (isOpponent) "opponent" else "I"} played ${playedEntity.CardID}")

            secretLogic.blockPlayed(game, tag.Target, playedEntity)

        } else if (BlockTag.TYPE_ATTACK == tag.BlockType) {

            secretLogic.blockAttack(game, tag)
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
            console.error("Cannot find player Id '${tag.playerId}'")
            return
        }

        val battleTagEntity = mGame!!.entityMap[tag.playerName]
        if (battleTagEntity != null) {
            /**
             * merge all tags
             */
            player.entity!!.tags.putAll(battleTagEntity.tags)
        }

        console.debug("${tag.playerName} now points to entity ${player.entity!!.EntityID}")

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
            secretLogic.damage(mGame!!, tag)
        }
    }

    private fun handleShowEntityTag(tag: ShowEntityTag) {
        val entity = mGame!!.findEntitySafe(tag.Entity!!)

        if (!entity!!.CardID.isNullOrBlank() && entity.CardID != tag.CardID) {
            console.error("[Inconsistent] entity $entity changed cardId ${entity.CardID}  -> ${tag.CardID}")
        }
        entity.setCardId(tag.CardID!!, cardJson.getCard(tag.CardID!!))

        for (key in tag.tags.keys) {
            tagChanged(entity, key, tag.tags[key])
        }
    }


    private fun handleShowEntityTag2(tag: ShowEntityTag) {
        val entity = mGame!!.findEntitySafe(tag.Entity!!)

        for (key in tag.tags.keys) {
            tagChanged2(entity!!, key, tag.tags[key])
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun tagChanged2(entity: Entity, key: String, newValue: String?) {
    }

    private fun tagChanged(entity: Entity, key: String, newValue: String?) {
        val oldValue = entity.tags[key]

        entity.tags.put(key, newValue!!)

        if (Entity.ENTITY_ID_GAME == entity.EntityID) {
            if (Entity.KEY_TURN == key) {
                try {
                    mCurrentTurn = newValue.toInt()
                    console.debug("turn: $mCurrentTurn")
                } catch (e: Exception) {
                    console.error(e)
                }

            } else if (Entity.KEY_STEP == key) {
                if (Entity.STEP_BEGIN_MULLIGAN == newValue) {
                    gameStepBeginMulligan()
                    if (mGame!!.isStarted) {
                        for (listener in gameStartListenerList) {
                            listener.invoke(mGame!!)
                        }
                        currentOrFinishedGame = mGame!!
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
                secretLogic.minionDied(mGame!!, entity)
            } else if (Entity.ZONE_HAND == oldValue && Entity.ZONE_DECK == newValue) {
                /*
                 * card was put back in the deck (most likely from mulligan)
                 */
                entity.extra.drawTurn = -1
            }

            if (Entity.ZONE_HAND == oldValue && Entity.ZONE_HAND != newValue) {
                /*
                 * no reason to hide it anymore. Hopefully when the card leaves the hand, it is
                 * revealed
                 */
                entity.extra.hide = false
            }
        }

        if (Entity.KEY_TURN == key) {
            secretLogic.newTurn(mGame!!)
        }
    }

    private fun handleCreateGameTag(tag: CreateGameTag) {
        mLastTag = false

        if (mGame != null && tag.gameEntity.tags[Entity.KEY_TURN] != null) {
            console.debug("CREATE_GAME during an existing one, resuming")
        } else {
            mGame = Game(console)

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

    interface Listener {
        /**
         * when gameStarted is called, game.player and game.opponent are set
         * the initial mulligan cards are known too. It's ok to store 'game' as there can be only one at a time
         */
        fun gameStarted(game: Game)

        /**
         * this is called whenever something changes :)
         */
        fun somethingChanged()

        fun gameOver()
    }


    private fun gameStepBeginMulligan() {

        var knownCardsInHand = 0
        var totalCardsInHand = 0

        val player1 = mGame!!.playerMap["1"]
        val player2 = mGame!!.playerMap["2"]

        if (player1 == null || player2 == null) {
            console.error("cannot find players")
            return
        }

        val entities = mGame!!.getEntityList { entity -> "1" == entity.tags[Entity.KEY_CONTROLLER] && Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE] }

        for (entity in entities) {
            if (!entity.CardID.isNullOrBlank()) {
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
            for (listener in somethingChangedListenerList) {
                listener(mGame!!)
            }
        }
    }

    private fun handleTagChange(tag: TagChangeTag) {
        tagChanged(mGame!!.findEntitySafe(tag.ID!!)!!, tag.tag!!, tag.value)
    }

    private fun handleTagChange2(tag: TagChangeTag) {
        tagChanged2(mGame!!.findEntitySafe(tag.ID!!)!!, tag.tag!!, tag.value)
    }

    private fun tryToGuessCardIdFromBlock(stack: ArrayList<BlockTag>, fullEntityTag: FullEntityTag) {
        if (stack.isEmpty()) {
            return
        }

        val blockTag = stack[stack.size - 1]

        val blockEntity = mGame!!.findEntitySafe(blockTag.Entity!!)
        val entity = mGame!!.findEntitySafe(fullEntityTag.ID!!)

        if (blockEntity!!.CardID.isNullOrBlank()) {
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
                CardId.LAB_RECRUITER,
                CardId.SEANCE,
                CardId.SPLINTERGRAFT -> mGame!!.findEntitySafe(blockTag.Target!!)!!.CardID
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
                CardId.SPARK_ENGINE -> CardId.SPARK
                CardId.EXTRA_ARMS -> CardId.MORE_ARMS
                CardId.SEAFORIUM_BOMBER -> CardId.BOMB
                CardId.SPRINGPAW -> CardId.LYNX
                CardId.HALAZZI_THE_LYNX -> CardId.LYNX

                else -> null
            }
        } else if (TYPE_TRIGGER == blockTag.BlockType) {

            // deathrattle or passive effect
            guessedId = when (blockEntity.CardID) {
                CardId.PYROS -> CardId.PYROS1
                CardId.PYROS1 -> CardId.PYROS2
                CardId.WHITE_EYES -> CardId.THE_STORM_GUARDIAN
                CardId.DEADLY_FORK -> CardId.SHARP_FORK
                CardId.BURGLY_BULLY -> CardId.THE_COIN
                CardId.IGNEOUS_ELEMENTAL -> CardId.FLAME_ELEMENTAL
                CardId.RHONIN -> CardId.ARCANE_MISSILES
                CardId.FROZEN_CLONE -> stack.firstOrNull { BlockTag.TYPE_PLAY == it.BlockType }?.let { mGame!!.findEntitySafe(it.Entity!!)!!.CardID }
                CardId.BONE_BARON -> CardId.SKELETON
                CardId.WEASEL_TUNNELER -> CardId.WEASEL_TUNNELER
                CardId.RAPTOR_HATCHLING -> CardId.RAPTOR_PATRIARCH
                CardId.DIREHORN_HATCHLING -> CardId.DIREHORN_MATRIARCH
                CardId.MANA_BIND -> stack.firstOrNull { BlockTag.TYPE_PLAY == it.BlockType }?.let { mGame!!.findEntitySafe(it.Entity!!)!!.CardID }
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
                CardId.SPARK_DRILL -> CardId.SPARK
                CardId.HIGH_PRIESTESS_JEKLIK -> CardId.HIGH_PRIESTESS_JEKLIK
                CardId.HAKKAR_THE_SOULFLAYER -> CardId.CORRUPTED_BLOOD
                else -> null
            }
        }

        if (!guessedId.isNullOrBlank()) {
            entity!!.setCardId(guessedId!!, cardJson.getCard(guessedId!!))
        }

        // even if we don't know the guessedId, record that this was createdBy this entity
        entity!!.extra.createdBy = blockEntity.CardID
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleFullEntityTag2(tag: FullEntityTag) {

    }

    private fun handleFullEntityTag(tag: FullEntityTag) {
        var entity: Entity? = mGame!!.entityMap[tag.ID]

        if (entity == null) {
            entity = Entity()
            mGame!!.entityMap.put(tag.ID!!, entity)
        }
        entity.EntityID = tag.ID
        if (!tag.CardID.isNullOrEmpty()) {
            entity.setCardId(tag.CardID!!, cardJson.getCard(tag.CardID!!))
        }
        entity.tags.putAll(tag.tags)

        if (Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE]) {
            entity.extra.drawTurn = mCurrentTurn
        }

        val playerId = entity.tags[Entity.KEY_CONTROLLER]
        val cardType = entity.tags[Entity.KEY_CARDTYPE]
        val player = mGame!!.findController(entity)

        console.debug("entity created ${entity.EntityID} controller=${playerId} zone=${entity.tags[Entity.KEY_ZONE]} ")

        if (Type.HERO == cardType) {
            player.hero = entity

            val card = cardJson.getCard(entity.CardID!!)
            player.classIndex = getClassIndex(card.playerClass)
            player.playerClass = card.playerClass
        } else if (Type.HERO_POWER == cardType) {
            player.heroPower = entity
        } else {
            if (mGame!!.gameEntity!!.tags[Entity.KEY_STEP] == null) {
                if (Entity.ZONE_DECK == entity.tags[Entity.KEY_ZONE]) {
                    entity.extra.originalController = entity.tags[Entity.KEY_CONTROLLER]
                } else if (Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE]) {
                    // this must be the coin
                    entity.setCardId(CardId.THE_COIN, cardJson.getCard(CardId.THE_COIN))
                    entity.extra.drawTurn = 0
                }
            }
        }
    }

    fun onGameStart(block: (Game) -> Unit) {
        gameStartListenerList.add(block)
    }

    fun whenSomethingChanges(block: (Game) -> Unit) {
        somethingChangedListenerList.add(block)
    }

    fun onGameEnd(block: (Game) -> Unit) {
        gameEndListenerList.add(block)
    }

    companion object {
        fun isPlayerWhizbang(game: Game): Boolean {
            return !game.player!!.entity!!.tags["WHIZBANG_DECK_ID"].isNullOrBlank()
        }

        fun isPlayerZayle(game: Game): Boolean {
            return game.getEntityList {
                it.CardID == CardId.ZAYLE_SHADOW_CLOAK
                        // We only set originalController for entities that start in a player's deck
                        // && it.extra.originalController == game.player!!.entity!!.PlayerID
                        && it.tags.get(Entity.KEY_ZONE) == Entity.ZONE_SETASIDE
            }.isNotEmpty()
        }

        fun gameTurnToHumanTurn(turn: Int): Int {
            return (turn + 1) / 2
        }
    }
}

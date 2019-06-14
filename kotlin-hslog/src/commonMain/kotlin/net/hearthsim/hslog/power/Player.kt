package net.hearthsim.hslog.power

class Player {
    var entity: Entity? = null
    var battleTag: String? = null
    var isOpponent: Boolean = false
    var hasCoin: Boolean = false

    var hero: Entity? = null
    var heroPower: Entity? = null
    var classIndex: Int ?= null
    var playerClass: String ?= null
}

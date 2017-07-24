#!/usr/bin/env python3
import requests
import os
import sys
import json

try:
    file = open("cards.json", "r")
    cardList = json.load(file)
except Exception as ex:
    print(ex)
    file = open("cards.json", "w")
    r = requests.get("https://api.hearthstonejson.com/v1/latest/enUS/cards.json", verify=False)
    cardList = r.json()
    json.dump(cardList, file)


def getVal(key, card):
    if (key in card):
        return card[key]
    else:
        return ""

for card in cardList:
    id = ""
    if (getVal("set", card) == "UNGORO"
        and not getVal("type", card) == "ENCHANTMENT"
        and not getVal("type", card) == "HERO_POWER"):
            sys.stdout.write(card["id"] + ",")

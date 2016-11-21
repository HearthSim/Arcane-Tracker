#!/usr/bin/env python
import requests
import os

r = requests.get("https://api.hearthstonejson.com/v1/15300/enUS/cards.json", verify=False)

cardList = r.json()


png_path = "./png/"
webp_path = "./webp/"

try:
    os.makedirs(png_path)
    os.makedirs(webp_path)
except Exception:
    pass

for card in cardList:
    if ("id" in card):
        imageUrl = "http://vps208291.ovh.net/cards/enus/" + card["id"] + ".png"
        print(imageUrl)

        r = requests.get(imageUrl)
        with open(png_path + card["id"] + ".png", 'wb') as f:
            for chunk in r.iter_content():
                f.write(chunk)

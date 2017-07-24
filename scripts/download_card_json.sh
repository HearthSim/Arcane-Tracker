#!/bin/bash
for i in enUS frFR ptBR ruRU koKR zhCN zhTW esES
do
    wget "https://api.hearthstonejson.com/v1/latest/$i/cards.json" -O "cards_$i".json
done

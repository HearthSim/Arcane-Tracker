var loadJS = function(url, implementationCode, location){
    //url is URL of external file, implementationCode is the code
    //to be called from the file, location is the location to
    //insert the <script> element

    var scriptTag = document.createElement('script');
    scriptTag.src = url;

    scriptTag.onload = implementationCode;
    scriptTag.onreadystatechange = implementationCode;

    location.appendChild(scriptTag);
};

function downloadLang(lang, zip, cb) {
    folder = zip.folder(lang);

    console.log("Downloading " + lang);

    $.getJSON("http://hearthstonelabs.com/v1/cardDB/" + lang + "/?others=2", function(result) {
        var mycards = result
        var i = 0;

        function doCard() {
            var card = mycards[i]
            if (typeof card.gameId != 'undefined') {
                console.log(lang + " " + card.gameId +": " + i + "/" + mycards.length)
                cardImage=sunwell.createCard(card, 764, null)

                function retry() {
                    if (cardImage.target.src.length < 22) {
                        setTimeout(retry, 20);
                    } else {
                        //console.log("data = " + cardImage.target.src.substring(0, 25));
                        folder.file(card.gameId + ".png",cardImage.target.src.substring(22),{base64: true})

                        i++
                        if (i == mycards.length - 1) {
                            cb()
                        } else {
                            doCard()
                        }
                    }
                }
                retry();
            }
        }

        doCard();
    })
}

loadJS("https://cdnjs.cloudflare.com/ajax/libs/jszip/3.1.3/jszip.min.js", function() {
    console.log("jszip loaded")
    var zip = new JSZip();

    allLang = ["enUS"];//, "enUS", "koKR", "ruRU", "ptBR"]

    j = 0;
    function doLang() {
        downloadLang(allLang[j], zip, function() {
            if (j == allLang.length - 1) {
                console.log("now zip");
                zip.generateAsync({type:"blob"})
                    .then(function(content) {
                        // see FileSaver.js
                        saveAs(content, "cards.zip");
                    });
            } else {
                j++;
                doLang()
            }
        })
    };

    doLang()
}, document.body)
loadJS("https://cdnjs.cloudflare.com/ajax/libs/FileSaver.js/1.3.3/FileSaver.js", function() {console.log("tata")}, document.body)



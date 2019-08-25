const Pusher = require('pusher');
const settings = require("./settings");
const axios = require("axios");
const nlp = require("compromise");
const cal = require("../cal");
const prisma = require("./prisma").prisma;

const firstPersonPronouns = new Set(["I", "we", "my", "mine", "myself", "our", "ours", "ourselves"]);
const secondPersonPronouns = {
    "you": "my",
    "your": "my",
    "yours": "mine",
    "yourself": "myself",
}

const channels_client = new Pusher({
    appId: '',
    key: '',
    secret: '',
    cluster: 'us2',
    encrypted: true
});  

module.exports = async function (context, req) {
    console.log(req);
    if ((!req.query.id && !req.query.discordID) || !req.query.text || !req.query.from) {
        context.res = {
            status: 400,
            body: {"error": "Please include a user id, text to analyze, who the message is from and who the message is to."}
        }
        return;
    }
    if (req.query.discordID) {
        if (await prisma.$exists.user({discord: req.query.discordID})) {
            req.query.id = (await prisma.user({discord: req.query.discordID})).id;
            console.log("Found ID for Discord", req.query.id);
        } else {
            context.res = {
                status: 400,
                body: {"error": "user does not exist in database"}
            }
            return; 
        }
    }
    // normalize sentence
    let sentence = nlp(req.query.text).normalize();
    let finalSentence = [];
    // map every pronoun to the person for better recognition
    for (let word of sentence.out("text").split(" ")) {
        if (firstPersonPronouns.has(word)) {
            finalSentence.push(req.query.from);
        } else if (secondPersonPronouns[word]) {
            finalSentence.push(secondPersonPronouns[word]);
        } else {
            finalSentence.push(word);
        }
    }
    finalSentence = finalSentence.join(" ")
    console.log("Post processed sentence:", finalSentence);
    const res = await axios.get(`https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/${settings.app}?subscription-key=${settings.token}&q=${finalSentence}`);
    console.log(JSON.stringify(res.data, null, 2))
    
    let date;
    // get date/time
    for (let entity of res.data.entities) {
        if (!entity.type.startsWith("builtin.datetimeV2")) continue;
        if (entity.type.indexOf("range") !== -1) {
            let res = entity.resolution.values[0];
            if (res.start) date = Date.parse(res.start);
            else if (res.end) date = Date.parse(res.end);
        } else {
            date = Date.parse(entity.resolution.values[0].value);
        }
    }
    
    if (!date) {
        context.res = {
            body: {
                result: false,
                query: finalSentence
            }
        }
        return;
    }

    date += 1000 * 60 * 60 * 4; // 4 hours

    let keyphrases = [];
    for (let entity of res.data.entities) {
        if (entity.type !== "builtin.keyPhrase" && entity.type !== "ToDo.TaskContent") {
            continue;
        }
        if (entity.type === "ToDo.TaskContent") {
            keyphrases = [entity.entity];
            break;
        }
        keyphrases.push(entity.entity);
    }

    keyphrases = keyphrases.map((phrase) => {
        return phrase[0].toUpperCase() + phrase.substr(1);
    })

    let automated = false, chosenPhrase = null;
    
    for (let keyphrase of keyphrases) {
        if (keyphrase.split` `.length > 1) {
            automated = true;
            chosenPhrase = keyphrase;
        }
    }

    const user = await prisma.user({id: req.query.id});

    if (automated) {
        channels_client.trigger(req.query.id, 'addCalendar', {
            "message": {
                chosenPhrase,
                date,
                data: await cal.add(chosenPhrase, date, user.refreshToken),
                original: req.query.text
            }
        });
    } else {
        channels_client.trigger(req.query.id, 'promptUser', {
            "message": {
                keyphrases,
                date,
                query: req.query.text,
                original: req.query.text
            }
        });
    }

    context.res = {
        body: {
            result: true,
            data: {
                keyphrases: keyphrases,
                date: date,
                query: finalSentence
            }
        }
    }
    return;
};
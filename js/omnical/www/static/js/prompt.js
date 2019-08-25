function time(date) {
    return `
    <label class="btn btn-outline-info">
        <input type="radio" class="dateSelect" data-value="${date}" autocomplete="off">
        ${moment(date).calendar()}
    </label>`
}

function event(phrase) {
    return `
    <label class="btn btn-outline-info">
        <input type="checkbox" class="phraseSelect" data-value="${phrase}" autocomplete="off"> ${phrase}
    </label>`
}

function run() {
    const data = JSON.parse(window.localStorage.getItem("eventDetails"));
    $("#message").text(window.localStorage.getItem("original"));
    let phrases = [];
    if (data.data.message.chosenPhrase) {
        phrases = [data.data.message.chosenPhrase];
    } else {
        phrases = data.data.message.keyphrases
    }
    let dates = [data.data.message.date];
    
    for (let phrase of phrases) {
        $("#buttons").append(event(phrase));
    }
    
    for (let date of dates) {
        $("#times").append(time(date))
    }

    let chosenTime;
    $(document).on("change", ".dateSelect", async (e) => {
        let target = e.currentTarget;
        if (!target.checked) return;
        chosenTime = parseInt($(target).attr("data-value"), 10);
    })

    $(document).on("change", ".phraseSelect", async (e) => {
        let text = $(e.currentTarget).attr("data-value");
        $("#finalPhrase").val($("#finalPhrase").val() + " " + text);
    })

    $("#submit").on("click", async () => {
        let title = $("#finalPhrase").val();
        let date = chosenTime;
        try {
            await axios(`https://omnical-ai.azurewebsites.net/api/remove?code=dCB9FpHWVhqwFYaxpJc5xq2cuQoBU1t9O30JS2SrPZL5iN/yAYw6QQ==&id=${window.token}&eventId=${data.data.message.data.data.id}`);
        } catch(e) {};
        await axios(`https://omnical-ai.azurewebsites.net/api/add?code=dCB9FpHWVhqwFYaxpJc5xq2cuQoBU1t9O30JS2SrPZL5iN/yAYw6QQ==&id=${window.token}&title=${title}&date=${date}&to=`);
        alert("Updated!");
        window.location.href = "index.html";
    })
}

document.addEventListener("deviceready", run, false);
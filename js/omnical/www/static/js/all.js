window.emaildomain = "@sandbox03c1b87ec2af40528fa5f72316b163de.mailgun.org";
document.addEventListener("deviceready", onDeviceReady, false);

function getLocation(href) {
    var match = href.match(/^(https?\:)\/\/(([^:\/?#]*)(?:\:([0-9]+))?)([\/]{0,1}[^?#]*)(\?[^#]*|)(#.*|)$/);
    return match && {
        href: href,
        protocol: match[1],
        host: match[2],
        hostname: match[3],
        port: match[4],
        pathname: match[5],
        search: match[6],
        hash: match[7]
    }
}

function checkNotifications() {
    cordova.plugins.notification.local.requestPermission(granted => {
        if (!granted) {
            navigator.app.exitApp();
        }
        cordova.plugins.backgroundMode.enable();
        subscribeToNotifications();
    });
}

function subscribeToNotifications() {
    const socket = new Pusher("67772fab0c422aced02f", {
        cluster: "us2",
    });
    const key = socket.subscribe(window.token);
    key.bind("addCalendar", (data) => {
        window.localStorage.setItem("original", data.message.original);
        cordova.plugins.notification.local.schedule({
            title: "Event added to your calendar",
            text: `${data.message.chosenPhrase} (${moment(data.message.date).calendar()})`,
            data,
            actions: [
                {
                    id: "remove",
                    title: "Remove",
                },
                {
                    id: "change",
                    title: "Change",
                    launch: true
                }
            ]
        });
    })
    key.bind("promptUser", (data) => {
        window.localStorage.setItem("original", data.message.original);
        cordova.plugins.notification.local.schedule({
            title: "Please confirm these events details",
            data,
            actions: [
                {
                    id: "change",
                    title: "Change",
                    launch: true
                }
            ]
        });
    })
    cordova.plugins.notification.local.on("remove", async (notification) => {
        await axios(`https://omnical-ai.azurewebsites.net/api/remove?code=dCB9FpHWVhqwFYaxpJc5xq2cuQoBU1t9O30JS2SrPZL5iN/yAYw6QQ==&id=${window.token}&eventId=${notification.data.message.data.data.id}`);
    });
    cordova.plugins.notification.local.on("change", async(notification) => {
        window.localStorage.setItem("eventDetails", JSON.stringify(notification));
        window.location.href = "userPrompt.html"
    })
}

function onDeviceReady() {
    checkNotifications();
    if (window.localStorage.getItem("token")) {
        window.token = window.localStorage.getItem("token");
        return;
    }
    let child = cordova.InAppBrowser.open('https://omnical-auth.azurewebsites.net', '_blank', 'toolbar=no');
    child.addEventListener("loadstop", function(event) {
        if (getLocation(event.url).pathname !== "/oauth") return;
        child.executeScript({code: "window.token"}, function(values) {
            if (!values[0]) {
                alert("Failed to authenticate!")
                navigator.app.exitApp();
                return;
            }
            window.token = values[0];
            window.localStorage.setItem("token", values[0]);
            child.close();
        })
    })
}


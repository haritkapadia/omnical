const google = require("googleapis").google;
const client = require("./client.json").web;
const moment = require("moment");

function getCal(refreshToken) {
    const oauth2Client = new google.auth.OAuth2(
        client.client_id,
        client.client_secret,
        "https://omnical-auth.azurewebsites.net/oauth"
    );
    oauth2Client.setCredentials({
        refresh_token: refreshToken,
    });
    return google.calendar({version: 'v3', auth: oauth2Client});
}

module.exports = {
    add: async (title, time, refreshToken) => {
        return await getCal(refreshToken).events.insert({
            calendarId: "primary",
            resource: {
                summary: title,
                start: {
                    dateTime: moment(time).format("YYYY-MM-DDTHH:mm:ssZ"),
                    timeZone: "America/New_York"
                },
                end: {
                    dateTime: moment(time).format("YYYY-MM-DDTHH:mm:ssZ"),
                    timeZone: "America/New_York"
                }
            }
        });
    },
    remove: async (eventId, refreshToken) => {
        return await getCal(refreshToken).events.delete({
            calendarId: "primary",
            eventId: eventId
        });
    },
}
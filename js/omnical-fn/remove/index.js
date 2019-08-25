const prisma = require("../analyze/prisma").prisma;
const cal = require("../cal");

module.exports = async function (context, req) {
    if (!req.query.id || !req.query.eventId) {
        context.res = {
            status: 400,
            body: "Please include the user id and the event id."
        };
        return;
    }
    const user = await prisma.user({id: req.query.id});
    await cal.remove(req.query.eventId, user.refreshToken);
    context.res = {
        status: 200,
        body: "success"
    }
};
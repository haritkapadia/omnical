const prisma = require("../analyze/prisma").prisma;
const cal = require("../cal");

module.exports = async function (context, req) {
    if (!req.query.id || !req.query.title || !req.query.date) {
        context.res = {
            status: 400,
            body: "Please include an id, title and date."
        };
        return;
    }
    const user = await prisma.user({id: req.query.id});
    await cal.add(req.query.title, req.query.time, user.refreshToken);
    context.res = {
        status: 200,
        body: "success"
    }
};
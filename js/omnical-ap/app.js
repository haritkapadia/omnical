const app = require("fastify")();
const google = require("googleapis").google;
const client = require("./client").web;
const prisma = require('./prisma').prisma;

const scopes = [
	"https://www.googleapis.com/auth/calendar",
	"https://www.googleapis.com/auth/userinfo.email",
	"https://www.googleapis.com/auth/contacts.readonly"
];

function setToken(token) {
	return `<script>window.token = "${token}"</script>`
}

app.get("/", async (req, res) => {
	res.redirect(new google.auth.OAuth2(
		client.client_id,
		client.client_secret,
		"https://omnical-auth.azurewebsites.net/oauth"
	).generateAuthUrl({
  		access_type: "offline",
  		scope: scopes
	}))
})

app.get("/oauth", async (req, res) => {
	if (!req.query.code) {
		return res.send({error: "No code specified"});
	}
	try {
		const oauth2client = await new google.auth.OAuth2(
			client.client_id,
			client.client_secret,
			"https://omnical-auth.azurewebsites.net/oauth"
		);
		const tokens = (await oauth2client.getToken(req.query.code)).tokens;
		oauth2client.setCredentials(tokens);
		const oauth2 = google.oauth2({
			auth: oauth2client,
			version: "v2"
		});
		const email = (await oauth2.userinfo.v2.me.get()).data.email;
		if (await prisma.$exists.user({email: email})) {
			return res.type('text/html').send(setToken((await prisma.user({email: email})).id));
		}
		let user = await prisma.createUser({
			accessToken: tokens.access_token,
			refreshToken: tokens.refresh_token,
			email: email
		})
		res.type('text/html').send(setToken(user.id));
	} catch(e) {
		res.send({error: "Invalid code"});
		console.log(e);
	}
})

app.listen(process.env.PORT || 10001, "0.0.0.0", (err) => {
	if (err) throw err;
});


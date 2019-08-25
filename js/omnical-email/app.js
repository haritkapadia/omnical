const app = require("express")();
const multer  = require("multer");
const google = require("googleapis").google;
const bodyParser = require('body-parser');
const axios = require("axios");
const nlp = require("compromise");
const client = require("./client.json").web;
const prisma = require("./prisma").prisma;

app.use(bodyParser.urlencoded({extended: true}));

const upload = multer();

function flatten(array) {
    var flattend = [];
    (function flat(array) {
		array.forEach(function(el) {
			if (Array.isArray(el)) flat(el);
			else flattend.push(el);
		});
	})(array);
    return flattend;
}

async function getContacts(refreshToken) {
    let contacts = [];
    const oauth2Client = new google.auth.OAuth2(
        client.client_id,
        client.client_secret,
        "https://omnical-auth.azurewebsites.net/oauth"
    );
    oauth2Client.setCredentials({
        refresh_token: refreshToken,
    });
    let gcontacts = google.people({
        version: "v1",
        auth: oauth2Client
    });
    let emails = (await gcontacts.people.connections.list({
        resourceName: "people/me",
        personFields: "emailAddresses",
        pageSize: 2000
    })).data.connections.map(c => c.emailAddresses)
                        .filter(x => x)
                        .map(x => x.map(y => y.value));
    return flatten(emails);
}
app.post("/webhook", upload.any(), async function (req, res) {
    const recipient = req.body.recipient;
	const id = recipient.substr(0, recipient.indexOf("@"));
	console.log(id);
    if (!(await prisma.$exists.user({id: id}))) {
        return res.send({success: false});
    }
    const user = await prisma.user({id: id});
    //let users = new Set(await getContacts(user.refreshToken));
	const sender = req.body.sender;
	//if (!users.has(sender)) return res.send({success: false});
    const from = req.body.from.replace(/<.*>/g, "").trim();
    const body = req.body["body-plain"].replace(/\r/g, "").split("\n");
    // remove any replies
    let filtered = body.filter(line => !line.startsWith(">") && line.length !== 0);
    // removes the extra "ON XX/XX/XXXX XX:XX XX, XXXX wrote...."
    if (body.length !== filtered.length) {
        filtered.pop();
	}
	filtered = filtered.join('\n');
	nlp(filtered).sentences().forEach(async (s) => {
		let res = await axios.get(`https://omnical-ai.azurewebsites.net/api/analyze?code=dCB9FpHWVhqwFYaxpJc5xq2cuQoBU1t9O30JS2SrPZL5iN/yAYw6QQ==&id=${id}&text=${s.out()}&from=${from}`);
		console.log(res.data);
	})
    res.send({status: "success"});
})

app.listen(process.env.PORT || 10001, () => console.log("Listening!"));
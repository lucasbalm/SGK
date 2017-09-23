var fs = require('fs');
var mqtt = require('mqtt');
var cloudinary = require('cloudinary');
var client = mqtt.connect('mqtt://localhost:1883');
var Kairos = require('kairos-api');
var kairosClient = new Kairos('516d4a8b', '0933f0e94aca51e12a7419458247be49');
var httpRequest = require('request');
var access = require('../server/access.json');
const telegramBot = require('node-telegram-bot-api');
const im = require('imagemagick');
const gm = require('gm').subClass({
	imageMagick: true
});
const telegramToken = `402730709:AAHHpm5YRBw1VzFxuu9ULK1cPYPnDAEZUQM`

const use = require('node-telegram-bot-api-middleware').use;
const simpleauth = require('node-telegram-bot-api-middleware-simpleauth').createMiddleware();
const response = use(simpleauth);

const becomeAdminCode = 'a2888';


// Set up the sender with you API key 
var url;
const bot = new telegramBot(telegramToken, {
	polling: true
})

cloudinary.config({
	cloud_name: 'makssie',
	api_key: '337679993513188',
	api_secret: 'ItvczwBrlix4mM1c6jGcDMeWTQM'
});

client.on('connect', function () {
	console.log('Conectado ao MQTT Broker em http://localhost:1883');
	client.subscribe('camera');
	client.subscribe('picture');
});

// Registered users only will be able to get through this middleware
const onlyAuth = response.use(function* () {
	if (!this.simpleauth.isUserAuthenticated()) {
		yield bot.sendMessage(this.chatId, 'Você não possui permissão para usar este comando');
		this.stop();
	}
});

// Using this for messages that are only for admin
const onlyAdmin = onlyAuth.use(function* () {
	if (!this.simpleauth.isCurrentUserAdmin()) {
		yield bot.sendMessage(this.chatId, 'You are not authorised to do this');
	}
});

//Comando para liberar o Acesso pelo telegram digitando "/open"/
bot.onText(/\/open/, onlyAuth((msg) => {
	bot.sendMessage(msg.chat.id, "Liberando Acesso")
		.then(() => {
			client.publish('Result', 'Access Granted');
			bot.sendMessage(msg.chat.id, "Acesso liberado");
		})
		.catch(() => {
			console.log("Erro")
		});
}));

let isOnRegisterMode = false;

bot.onText(/\/register/, onlyAuth((msg) => {
	const userName = msg.chat.username;
	const chatId = msg.chat.id;
	const json = {
		"username": `${userName}`,
		"chatId": `${chatId}`
	}
	const stringJson = JSON.stringify(json);
	fs.writeFileSync("access.json", stringJson);

	if (isOnRegisterMode === false) {
		bot.sendMessage(msg.chat.id, "Poste uma foto sua")
		isOnRegisterMode = true;
		bot.on('message', function (msg) {
			//Verifica se uma imagem foi enviada enviada
			if (msg.photo != undefined) {
				var objArray = msg.photo;
				var result = objArray.map((item) => {
					const size = Object.keys(msg.photo).length
					return msg.photo[size - 1];
				});
				//Pega o Id do File para que possa salvar no local
				const fileId = result[result.length - 1].file_id;
				bot.getFile(fileId).then((data) => {
					const filePath = data.file_path;
					const urlImage = `https://api.telegram.org/file/bot${telegramToken}/${filePath}`;
					httpRequest(urlImage, {
						encoding: 'binary'
					}, function (error, response, body) {
						fs.writeFileSync('downloaded.jpg', body, 'binary', function (err) {});
						gm('downloaded.jpg')
							.resize(800, 800, '!')
							.write('resized.jpg', function (err) {
								if (!err)
									console.log('done');
								cloudinaryUpload(userName);
							});
					});
				});
				isOnRegisterMode = false;
				bot.off('message');
			} else if (msg.text === '/cancel') {
				bot.sendMessage(msg.chat.id, "Registro Cancelado");
				isOnRegisterMode = false;
				bot.off('message');
			} else {
				bot.sendMessage(msg.chat.id, "Por favor envie a foto ou digite /cancel para cancelar");
			}
		})
	}
}));

bot.onText(/\/makeadmin (.*)/, response(function* (msg, matches) {
	if (!this.simpleauth.isUserAuthenticated()) {
		// Registering admin
		if (becomeAdminCode === matches[1]) {
			const code = yield this.simpleauth.generateAuthCode();

			yield this.simpleauth.registerCurrentTelegramUserWithCodeAsync(code);

			yield this.simpleauth.makeCurrentUserAdminAsync();
		} else {
			bot.sendMessage(this.chatId, 'Invalid code for becoming an admin');

			return;
		}
	} else {
		if (this.simpleauth.isCurrentUserAdmin()) {
			bot.sendMessage(this.chatId, 'You are already admin');

			return;
		}

		yield this.simpleauth.makeCurrentUserAdminAsync();
	}

	bot.sendMessage(this.chatId, 'You are now admin');
}));

bot.onText(/\/users/, onlyAuth((msg) => {
	let chatId = msg.chat.id
	kairosClient.galleryView({
		gallery_name: 'smartdoor'
	}).then((result) => {
		const users = result.body.subject_ids;
		bot.sendMessage(chatId, "Você possui os seguinte(s) usuário(s)");		
		if (users) {
			users.forEach((value) => {
				bot.sendMessage(chatId, value);
			});
		} else {
			bot.sendMessage(chatId, "Não possui nenhum usuário cadastrado");
		}
	}).catch(() => {
		console.log("Erro")
	});
}));

bot.onText(/\/delete (.*)/, onlyAuth((msg, matches) => {
	let chatId = msg.chat.id;
	const params = {
		gallery_name : 'smartdoor',
		subject_id : matches[1]
	}
	kairosClient.galleryRemoveSubject(params).then((result) => {
		if(result.body.Errors){
			bot.sendMessage(chatId, "Usuário não encontrado");
			bot.off('message');			
		}else{
			bot.sendMessage(chatId, `Usuário ${params.subject_id} removido`);
			bot.off('message');
		}
	})
}));

bot.onText(/\/takepicture/, onlyAuth((msg) => {
	client.publish('Result', 'Picture');	
	client.on('message', function (topic, message) {
		console.log("Topic", topic)
		if (topic == "picture") {
			var base64data = new Buffer(message).toString('base64');
			base64_decode(base64data, 'realtime.jpg', 'takepicture');
		}
		bot.sendPhoto(msg.chat.id, 'realtime.jpg');
		bot.off('message');			
	});
}))

function cloudinaryUpload(username) {
	cloudinary.uploader.upload("downloaded.jpg", function (result) {
		console.log(result);
		url = result.url;
		kairos_recogNewUser(url, username, function () {
			console.log("success"); //Ou response.send
		});
	});
}

function kairos_recogNewUser(url, userName, callback) {

	var params = {
		image: url,
		subject_id: userName,
		gallery_name: 'smartdoor',
		selector: 'SETPOSE'
	};

	kairosClient.enroll(params)
		.then(function (result) {
			console.log("Inside then");
			console.log(result.status);
			console.log('------------------------------------');
			console.log(result);
			console.log('------------------------------------');
			callback();
		})
		// err -> array: jsonschema validate errors 
		//        or throw Error 
		.catch(function (err) {
			console.log("Inside err");
			console.log(err);
		});

}

client.on('message', function (topic, message) {
	console.log("Topic", topic)
	if (topic == "camera") {
		var base64data = new Buffer(message).toString('base64');
		base64_decode(base64data, 'match.jpg', 'recognize');
	}

});

// function to create file from base64 encoded string and upload to cloudinary
function base64_decode(base64str, file, command) {
	// create buffer object from base64 encoded string, it is important to tell the constructor that the string is base64 encoded
	var bitmap = new Buffer(base64str, 'base64');
	// write buffer to file
	fs.writeFileSync(file, bitmap);
	console.log('******** File created from base64 encoded string ********');
if(command == "recognize"){
	// upload image to cloudinary
	cloudinary.uploader.upload("match.jpg", function (result) {
		console.log(result.url);
		url = result.url;
		kairos_recog(url);
	});
}
}

function kairos_recog(url) {

	var params = {
		image: url,
		gallery_name: 'smartdoor'
	};

	kairosClient.recognize(params) // return Promise 
		//  result: { 
		//    status: <http status code>, 
		//    body: <data> 
		//  } 
		.then(function (result) {
			console.log("Inside then");
			console.log("reconhecendo");
			console.log(result.body);
			// check if status is success
			if (result.body['images'][0]['transaction']['status'] == "success") {

				// check the confidence
				var resultado = result.body['images'][0]['transaction']['confidence'];
				console.log(result.body['images'][0]['transaction']['confidence']);
				if (resultado > 0.60) {
					client.publish('Result', 'Access Granted');
				} else {
					client.publish('Result', 'Sorry, Try Again');
					bot.sendMessage(access.chatId, "Quer abrir a porta para essa pessoa? Digite /open para abrir, e /cancel para não")
					bot.sendPhoto(access.chatId, result.body.uploaded_image_url);
				}
			}

			// If status failure, send a push notification (with the image)
			else {
				console.log("Inside failure, sending telegram message");
				client.publish('Result', 'Wait, calling owner..');
				bot.sendMessage(access.chatId, "Quer abrir a porta para essa pessoa? Digite /open para abrir, e /cancel para não")
				bot.sendPhoto(access.chatId, result.body.uploaded_image_url);
			}

		})
		.catch(function (err) {
			console.log("Inside err");
			console.log(err);
			client.publish('Result', 'Take pic again');
		});

}

console.log("server.js started on localhost")
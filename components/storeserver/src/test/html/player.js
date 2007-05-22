function noteAuthentication(res) {
	note("Authenticated");
}

function noteDetatched(res) {
	note("Detatched");
}

function note(msg) {
	document.getElementById("note").innerHTML = msg;
}

function msg(s) {
	document.getElementById("msg").innerHTML = s;
}

function startSendStatus() {
		setInterval("sendStatus()",1000);	
}

function onLoad() {
	reAuthenticate();
}

function recMsg(res) {
	msg(res);
}

function doBack() {
	sendMsg("Back","recMsg");
}

function doStop() {
	sendMsg("Stop","recMsg");
}

function doPlay() {
	sendMsg("Play","recMsg");
}

function doPlayURL(url) {
	sendMsg("PlayURL?url=" + escape(url),"recMsg");
}

function doNext() {
	sendMsg("Next","recMsg");
}
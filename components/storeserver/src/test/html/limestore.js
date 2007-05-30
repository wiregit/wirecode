/**
 * The main file containing functionality for the lime store
 *
 * @author jpalm
 */
 
// ------------------------------------------------------------------
//
// The client must provide:
//
//  - noteAuthentication(String)
//  - noteDetatched(String)
//
// ------------------------------------------------------------------
 
var publicKey;
var privateKey;
var mainDiv;
var LOCAL_PORT = '8080';
var REMOTE_PORT = '8090';

function getPublicKey() {
	return publicKey;
}

function getPrivateKey() {
	return privateKey;
}
	
function setPublicKey(key) {
	publicKey = key;
}
	
function setPrivateKey(key) {
	privateKey = key;
}

function sendMsg(cmd,callback) {
	go("Msg?command=" + cmd + "&private=" + getPrivateKey(),callback,'8080');
}


// ------------------------------------------------------------------
// Convenience methods	
// ------------------------------------------------------------------
	
function doAuthentication() {
	go('Authenticate?private=' + getPrivateKey(),'noteAuthentication',LOCAL_PORT);
}

function doStartCom() {
	go('StartCom','setPublicKey',LOCAL_PORT);
}
	
function doGiveKey() {
	go('GiveKey?public=' + getPublicKey(),'setPrivateKey',REMOTE_PORT);
}
	
function doDetatch() {
	go('Detatch','noteDetatched',LOCAL_PORT);
}
	
function reAuthenticate() {
	N = 200;
	n = 0;
	setTimeout('doDetatch()'				,	N*(n++));
	setTimeout('doStartCom()'				,	N*(n++));
	setTimeout('doGiveKey()'				,	N*(n++));
	setTimeout('doAuthentication()'	,	N*(n++));
}
	
function go(cmd,callback,port) {
	
	if (!mainDiv) {
		mainDiv = document.getElementById("main");
	}
	
	// Make the call to the local server
	var script = document.createElement('script');
	var url = 'http://localhost:' + port + '/' + cmd;
	url += url.match(/\?/) ? '&' : '?';
	url += 'callback=' + callback;
	url += '&_f=' + Math.random();
	script.setAttribute('src', url);
	script.setAttribute('type', 'text/javascript');
  mainDiv.appendChild(script);		
}

// ------------------------------------------------------------------
// Private
// ------------------------------------------------------------------

function _nullFunction(res) {}
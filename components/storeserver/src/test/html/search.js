function noteAuthentication(res) {
	setInterval("checkMsgs()",1000);	
	setTimeout("startSendStatus()",500);
}

function startSendStatus() {
		setInterval("sendStatus()",1000);	
}

function onLoad() {
	reAuthenticate();
}

function checkMsgs() {
	sendMsg("GetMsg","recMsg");
}

function sendStatus() {
	var cartCount = parseInt(document.getElementById("cartCount").innerHTML);
	var cartPrice = parseFloat(document.getElementById("cartPrice").innerHTML);
	sendMsg("SetStatus:count=" + cartCount + "&price=" + cartPrice,"recMsg");
}

function recMsg(res) {
	if (res == 'Clear') {
		clearShoppingCart();
	} else if (res == 'Add') {
		addToCart(10.00);
	}
}

function addToCart(price) {
	var cartCount = parseInt(document.getElementById("cartCount").innerHTML);
	var cartPrice = parseFloat(document.getElementById("cartPrice").innerHTML);
	var newCartCount = cartCount + 1;
	var newCartPrice = cartPrice + price;
	document.getElementById("cartCount").innerHTML = newCartCount;
	document.getElementById("cartPrice").innerHTML = newCartPrice;
}

function clearShoppingCart() {
	document.getElementById("cartCount").innerHTML = "0";
	document.getElementById("cartPrice").innerHTML = "0.00";
}
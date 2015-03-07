var OOCSI = (function() {

	var wsUri = "ws://localhost:9000/";
	var username;
	var handlers = {};
	var websocket;
	var connected = false;

	function init() {
		websocket = new WebSocket(wsUri);
		websocket.onopen = function(evt) {
			onOpen(evt)
		};
		websocket.onclose = function(evt) {
			onClose(evt)
		};
		websocket.onmessage = function(evt) {
			onMessage(evt)
		};
		websocket.onerror = function(evt) {
			onError(evt)
		};
	}

	function onOpen(evt) {
		if(websocket.readyState == WebSocket.OPEN) {
			submit(username);
			connected = true;
		}	 
		log("CONNECTED");
	}

	function onClose(evt) {
		log("DISCONNECTED");
	}

	function onMessage(evt) {
		try {
			var e = JSON.parse(evt.data);
			if(handlers[e.recipient] !== undefined) {
				handlers[e.recipient](e);
			} else {
				log('no handler for event: ' + evt.data);
			}
		} catch(e) {
			log('parse exception for event: ' + evt.data);
		}
		log('RESPONSE: ' + evt.data);
	}

	function onError(evt) {
		log('ERROR: ' + evt.data);
	}

	function internalClose() {
		websocket && websocket.close();
	}

	function submit(message) {
		if(websocket && websocket.send(message)) {
			log("SENT: " + message);	
		}
	}

	function log(message) {
		console.log(message);
	}

	function internalSend(client, data) {
		connected && submit('sendjson ' + client + ' '+ JSON.stringify(data));
	} 

	function internalSubscribe(channel, fn) {
		if(connected) {
			submit('subscribe ' + channel);
			handlers[channel] = fn;
		} 
	} 

	function internalUnsubscribe(channel) {
		if(connected) {
			submit('unsubscribe ' + channel);
			handlers[channel] = function() {};
		}
	} 

	return {
		connect: function(server, clientName, fn) {
			wsUri = server;
			username = clientName;
			handlers[clientName] = fn;
			init()
		},
		send: function(recipient, data) {
			internalSend(recipient, data);
		},
		subscribe: function(channel, fn) {
			internalSubscribe(channel, fn);
		},
		unsubscribe: function(channel) {
			internalUnsubscribe(channel);
		},
		close: function() {
			internalClose();
		},
		handlers: function() {
			return handlers;
		}
	};

})();

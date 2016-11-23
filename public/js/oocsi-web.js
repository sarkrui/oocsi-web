var OOCSI = (function() {

    var wsUri = "ws://localhost:9000/";
    var username;
    var handlers = {};
    var websocket;
    var connected = false;
    var logger = internalLog;

    function init() {
        logger("CONNECTING to " + wsUri);
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
        if (websocket.readyState == WebSocket.OPEN) {
            submit(username);
            connected = true;
        }
        logger("CONNECTED");
    }

    function onClose(evt) {
        logger("DISCONNECTED");
    }

    function onMessage(evt) {
        websocket.send(".");

        // ignore pings
        if (evt.data == "ping") {
            return;
        }

        // parse message
        try {
            var e = JSON.parse(evt.data);
            if (handlers[e.recipient] !== undefined) {
                handlers[e.recipient](e);
            } else {
                logger('no handler for event: ' + evt.data);
            }
        } catch (e) {
            logger('parse exception for event: ' + evt.data);
        }
        logger('RESPONSE: ' + evt.data);
    }

    function onError(evt) {
        logger('ERROR: ' + evt.data);
    }

    function waitForSocket(fn) {
        if (!websocket || websocket.readyState == WebSocket.CONNECTING) {
            setTimeout(function() {
                waitForSocket(fn)
            }, 200);
        } else {
            fn();
        }
    }

    function internalClose() {
        websocket && websocket.close();
    }

    function submit(message) {
        if (websocket) {
            try {
                websocket.send(message)
                logger("SENT: " + message);
            } catch (e) {
                console.log(e.message, e.name);
            }
        }
    }

    function internalLog(message) {
        // do nothing by default
    }

    function internalSend(client, data) {
        connected && submit('sendjson ' + client + ' ' + JSON.stringify((typeof(data) == 'string' ? {
            message: data
        } : data)));
    }

    function internalSubscribe(channel, fn) {
        if (connected) {
            submit('subscribe ' + channel);
            handlers[channel] = fn;
        }
    }

    function internalUnsubscribe(channel) {
        if (connected) {
            submit('unsubscribe ' + channel);
            handlers[channel] = function() {};
        }
    }

    return {
        connect: function(server, clientName, fn) {
            wsUri = server;
            username = clientName && clientName.length > 0 ? clientName : "webclient_" + +(new Date());
            handlers[clientName] = fn;
            init();
        },
        send: function(recipient, data) {
            internalSend(recipient, data);
        },
        subscribe: function(channel, fn) {
            waitForSocket(function() {
                internalSubscribe(channel, fn);
            });
        },
        unsubscribe: function(channel) {
            waitForSocket(function() {
                internalUnsubscribe(channel);
            });
        },
        close: function() {
            waitForSocket(function() {
                internalClose();
            });
        },
        handlers: function() {
            return handlers;
        },
        logger: function(fn) {
            logger = fn;
        }
    };

})();
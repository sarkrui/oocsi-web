
// connect to the OOCSI server
OOCSI.connect("ws://" + window.location.host + "/ws");

// subscribe to a channel and add data to HTML
OOCSI.subscribe("testchannel", function(e) {
	$("#color")
		.text("color: " + e.data.color)
		.css({height: e.data.color});
	$("#position")
		.text("position: " + e.data.position)
		.css({height: e.data.position});
});

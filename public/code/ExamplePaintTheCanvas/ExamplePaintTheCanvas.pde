import nl.tue.id.oocsi.*;

// ******************************************************
// This example requires a running OOCSI server!
//
// How to do that? Check: Examples > Tools > LocalServer
//
// More information how to run an OOCSI server
// can be found here: https://iddi.github.io/oocsi/)
// ******************************************************

OOCSI oocsi;

void setup() {
  size(600, 600);
  background(10);
  frameRate(10);

  // connect ot OOCSI server running on the same machine (localhost)
  // with "senderName" to be my channel others can send data to
  // (for more information how to run an OOCSI server refer to: https://iddi.github.io/oocsi/)
  oocsi = new OOCSI(this, "senderName", "localhost");
}

void draw() {
  filter(BLUR, 1);
  ellipse(mouseX, mouseY, 5, 5);
  
  oocsi
    .channel("OOCSI_Tutorials_Paint_the_Canvas")
    .data("x", mouseX)
    .data("y", mouseY)
    .data("colorR", 50)
    .data("colorG", 100)
    .data("colorB", 200)
    .send();
}

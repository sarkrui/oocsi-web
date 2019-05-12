
package model.codegen;

public class Interactable {

	public String type;
	public String name;
	public String par;
	public int min;
	public int max;
	public String def;

	public boolean isButton() {
		return type.equals("button");
	}

	public boolean isSlider() {
		return type.equals("slider");
	}

	public String getDefault() {
		return def;
	}

	public String getVarName() {
		return par.replaceAll("\\s", "");
	}
}

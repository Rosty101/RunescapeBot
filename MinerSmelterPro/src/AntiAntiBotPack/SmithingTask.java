package AntiAntiBotPack;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.script.AbstractScript;

public class SmithingTask  extends AbstractScript {
	@Override
	public int onLoop() {
		return Calculations.random(500, 600);
	}
}

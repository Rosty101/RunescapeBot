
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;

public class TreeCutterTask extends AbstractScript {


	@Override
	public void onStart() { //0th state

	}


	@Override
	public int onLoop()
	{
		//RUN EVERY SECOND-ISH
		return Calculations.random(200, 800);
	}
}
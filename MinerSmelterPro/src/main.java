import java.awt.Point;
import java.util.List;

import javax.script.ScriptEngineManager;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.walking.path.AbstractPath;
import org.dreambot.api.methods.walking.path.impl.LocalPath;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.loader.LocalLoader;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.map.TileMap;

import AntiAntiBotPack.AntiAntiBot;
import AntiAntiBotPack.MiningTask;

import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManager;

@ScriptManifest(author = "Satan", name = "Miner/Smelter out of Varrock", version = 1.0, description = "Mines and smelts for you around Verrock Area", category = Category.MINING)
public class main extends AbstractScript {
	// booleans to execute operations only ones per state enter or as needed

	AntiAntiBot antiBotKiller = new AntiAntiBot();// initializes Anti-Anti-Bot and gives it a local variable name
    MiningTask mainTask = new MiningTask();
	public void onStart() {
		antiBotKiller.onStart();
		mainTask.onStart();// sets up Anti-Anti-Bot with its onStart() function
	}

	public void onExit() {

	}

	@Override
	public int onLoop() {
		antiBotKiller.onLoop();
		if(!antiBotKiller.isExecuting) {
			mainTask.onLoop();
		}
		return Calculations.random(500, 600);// waits 500 to 600 milliseconds to prevent Anti-bot detection
	}

}
package AntiAntiBotPack;

import java.util.ArrayList;
import java.util.List;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;

public class TreeCutterTask extends AbstractScript {
	// Control booleans
	private boolean isTreeFound = false;
	private boolean isTimeToCut = false;
	private boolean isTimeToMakeArrows = false;
	private boolean isTimeToMakeFire = false;
	private GameObject treeToCutDown;
	private GameObject lastCutDownTree;

	private Area pickedArea;
	private List<GameObject> suroundingGameObjects;
	Area[] workAreas = { new Area(3271, 3463, 3284, 3439), new Area(3265, 3484, 3279, 3470),
			new Area(3274, 3495, 3254, 3513), new Area(3124, 3438, 3142, 3421), new Area(3159, 3416, 3172, 3402),
			new Area(3157, 3388, 3175, 3370), new Area(3285, 3408, 3275, 3425), new Area(3310, 3460, 3297, 3483),
			new Area(3284, 3345, 3272, 3335), new Area(3302, 3334, 3273, 3355), new Area(3273, 3359, 3258, 3374) };

	Tile destinationTile;

	public Timer taskLengthTimer;

	private ArrayList<String> toolNames = new ArrayList<String>();
	// variables to watch inventory
	private int logsInInventoryBeforeTriger;
	private int arrowsToMake;
	private int firesToStart;
	private int startedFires = 0;
	ToolNameAdjuster toolNameAdjuster = new ToolNameAdjuster();

	private enum State {
		FULLINVENTORY, GOINGTOAREA, GETTREE, CUTTREE, FLETCHING, STARTINGFIRE, WAIT,
	};

	private State getState() {
		if (getInventory().isFull()) {
			return State.FULLINVENTORY;
		}
		if (!pickedArea.contains(getLocalPlayer())) {
			return State.GOINGTOAREA;
		}
		if (!isTreeFound) {
			return State.GETTREE;
		}
		if (isTimeToCut) {
			return State.CUTTREE;
		}
		if (isTimeToMakeArrows) {
			return State.FLETCHING;
		}
		if (isTimeToMakeFire) {
			return State.STARTINGFIRE;
		}
		return State.WAIT;
	}

	@Override
	public void onStart() { // 0th state
		log("TreeCutterTask: Setting Up Task");
		pickNewArea();
		inventoryLimitSetUp();
		taskLengthTimer = new Timer(Calculations.random(1200000, 2700000));
		toolNames.add(toolNameAdjuster.Start("axe"));
		toolNames.add("Knife");
		toolNames.add("Tinderbox");
	}

	@Override
	public int onLoop() {
		switch (getState()) {
		case FULLINVENTORY:
			log("TreeCutterTask: Clearing logs from Inventory");
			if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15 || getSkills().getRealLevel(Skill.FIREMAKING) < 15) {
				getInventory().dropAll("Logs");
				while (getInventory().contains("Logs")) {
					sleep(Calculations.random(200, 800));
				}
				inventoryLimitSetUp();
				pickNewArea();
				break;
			} else {
				getInventory().dropAll("Oak logs");
				while (getInventory().contains("Oak logs")) {
					sleep(Calculations.random(200, 800));
				}
				inventoryLimitSetUp();
				pickNewArea();
				break;
			}
		case GOINGTOAREA:
			log("TreeCutterTask: Going to tree cutting area");
			if (getWalking().shouldWalk(7)) {
				getWalking().walk(destinationTile);
			}
			break;
		case GETTREE:
			log("TreeCutterTask: finding tree to cut...");
			if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15 || getSkills().getRealLevel(Skill.FIREMAKING) < 15) {
				treeToCutDown = getGameObjects().closest("Tree");
				if (pickedArea.contains(treeToCutDown)) {
					if (treeToCutDown != null) {
						isTreeFound = true;
						actionSelector(cutOrOther());
					} else {
						pickNewArea();
					}
				} else {
					pickNewArea();
				}
			} else {
				treeToCutDown = getGameObjects().closest("Oak");
				if (pickedArea.contains(treeToCutDown)) {
					if (treeToCutDown != null) {
						isTreeFound = true;
						actionSelector(cutOrOther());
					} else {
						pickNewArea();
					}
				} else {
					pickNewArea();
				}
			}
			break;
		case CUTTREE:
			log("TreeCutterTask: Cutiting Down Tree");
			if (getEquipment().slotContains(EquipmentSlot.WEAPON.getSlot(), toolNames.get(0))) {
				log("TreeCutterTask: Tree Object " + treeToCutDown.getName());
				treeToCutDown.interact("Chop down");
				int logsCount = getInventory().count("Logs");
				Timer checkTimer = new Timer(15000);
				if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15
						|| getSkills().getRealLevel(Skill.FIREMAKING) < 15) {
					while (getInventory().count("Logs") <= logsCount && !checkTimer.finished()) {
						sleep(Calculations.random(200, 800));
					}
				} else {
					while (getInventory().count("Oak logs") <= logsCount && !checkTimer.finished()) {
						sleep(Calculations.random(200, 800));
					}
				}

				isTreeFound = false;
				isTimeToCut = false;
			}
			break;
		case FLETCHING:
			break;
		case STARTINGFIRE:
			int checkInt = 0;
			if (getTabs().isOpen(Tab.INVENTORY)) {
				if (startedFires <= firesToStart) {
					Tile fireTile = pickedArea.getRandomTile();
					Area fireMakingArea = fireStartingArea(fireTile);
					while (!fireMakingArea.contains(getLocalPlayer())) {
						if (getWalking().shouldWalk(1)) {
							log("TreeCutterTask: Walking to fire starting area");
							getWalking().walk(fireTile);
							checkInt++;
							if (checkInt > 5) {
								break;
							}
						}
					}
					if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15
							|| getSkills().getRealLevel(Skill.FIREMAKING) < 15) {
						getInventory().get(toolNames.get(2)).useOn("Logs");
					} else {
						getInventory().get(toolNames.get(2)).useOn("Oak logs");
					}
					boolean isFireStarted = false;
					checkInt = 0;
					while (!isFireStarted) {
						log("TreeCutterTask: Looking for fire around me");
						List<GameObject> objectsAround = getGameObjects().all();
						for (int i = 0; i < objectsAround.size(); i++) {
							GameObject fire = objectsAround.get(i);
							if (fire.getName().contains("Fire")) {
								fireMakingArea = Area.generateArea(2, getLocalPlayer().getTile());
								if (fireMakingArea.contains(fire)) {
									log("TreeCutterTask: Fire started");
									isFireStarted = true;
								}
							}
						}
					}
					startedFires++;
				} else {
					if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15
							|| getSkills().getRealLevel(Skill.FIREMAKING) < 15) {
						if (getInventory().contains("Logs")) {
							log("TreeCutterTask: Droping logs and reseting for new area");
							getInventory().dropAll("Logs");
							while (getInventory().contains("Logs")) {
								sleep(Calculations.random(200, 800));
							}
							inventoryLimitSetUp();
							pickNewArea();
						}
					} else {
						if (getInventory().contains("Oak logs")) {
							if(getInventory().contains("Logs")) {
								getInventory().dropAll("Logs");
								while (getInventory().contains("Logs")) {
									sleep(Calculations.random(200, 800));
								}
							}
							log("TreeCutterTask: Droping logs and reseting for new area");
							getInventory().dropAll("Oak logs");
							while (getInventory().contains("Logs")) {
								sleep(Calculations.random(200, 800));
							}
							inventoryLimitSetUp();
							pickNewArea();
						}
					}
				}
			} else {
				getTabs().open(Tab.INVENTORY);
			}
			break;
		case WAIT:
			log("TreeCutterTask: Error in tree cutter task entered WAIT state");
			break;
		}

		// RUN EVERY SECOND-ISH
		return Calculations.random(200, 800);
	}

	private void actionSelector(int selectedAction) {
		switch (selectedAction) {
		case 0:
			isTimeToCut = true;
			isTimeToMakeArrows = false;
			isTimeToMakeFire = false;
			break;
		case 1:
			isTimeToCut = false;
			isTimeToMakeArrows = true;
			isTimeToMakeFire = false;
			break;
		case 2:
			isTimeToCut = false;
			isTimeToMakeArrows = false;
			isTimeToMakeFire = true;
			break;
		default:
			break;
		}
	}

	private Area fireStartingArea(Tile centerTile) {
		int min_x = centerTile.getX() + 1;
		int min_y = centerTile.getY() - 1;
		int max_x = centerTile.getX() - 1;
		int max_y = centerTile.getY() + 1;
		Area returnArea = new Area(min_x, min_y, max_x, max_y);
		return returnArea;
	}

	private int cutOrOther() {
		int returnAction = 0;
		if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15 || getSkills().getRealLevel(Skill.FIREMAKING) < 15) {
			if (getInventory().count("Logs") > logsInInventoryBeforeTriger) {
				returnAction = Calculations.random(0, 2);
				if (!getInventory().contains(toolNames.get(1)) && returnAction == 1) {
					returnAction = 2;
				}
			}
		} else {
			if (getInventory().count("Oak logs") > logsInInventoryBeforeTriger) {
				returnAction = Calculations.random(0, 2);
				if (!getInventory().contains(toolNames.get(1)) && returnAction == 1) {
					returnAction = 2;
				}
			}
		}
		return returnAction;
	}

	private void pickNewArea() {
		pickedArea = workAreas[Calculations.random(0, workAreas.length - 1)];
		destinationTile = pickedArea.getRandomTile();
		isTreeFound = false;
		isTimeToCut = false;
		isTimeToMakeArrows = false;
		isTimeToMakeFire = false;
	}

	private void inventoryLimitSetUp() {
		logsInInventoryBeforeTriger = Calculations.random(10, 23);
		firesToStart = Calculations.random(1, logsInInventoryBeforeTriger);
		startedFires = 0;
	}

}
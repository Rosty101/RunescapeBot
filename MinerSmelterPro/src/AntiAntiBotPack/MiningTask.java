package AntiAntiBotPack;

import java.util.List;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

public class MiningTask extends AbstractScript {

	boolean depositing = false;
	boolean lowLVOreSwitcher = false;
	Area miningZone = new Area(3281, 3360, 3290, 3371);// original minig Area new Area(3277, 3372, 3291, 3356);
	Area bankingZone = new Area(3257, 3424, 3250, 3420); //original Area new Area(3244, 3429, 3263, 3412);
	int inventoryCap;
	Timer depositResetTimer;

	private enum State {
		GETTOOL, MINING, TRAVELTOMINE, TRAVELTOBANK, BANKING, CLOSEBANK, ANTIBOTSTOP, WAIT
	}

	// Sets State of State Machine Based on Conditions
	private State getState() {
		// if the bot is in the mining zone and bots inventory isn't filled up to
		// inventory cap than mine

		if (!getEquipment().contains("Bronze pickaxe")) {
			return State.GETTOOL;
		}
		if (miningZone.contains(getLocalPlayer()) && getInventory().count("Copper ore") < inventoryCap) {
			return State.MINING;
		}
		// if the bot is not in the mining zone and its inventory is empty and bank
		// screen isn't open than go to the mining zone
		if (!miningZone.contains(getLocalPlayer()) && !getBank().isOpen()
				&& getInventory().count("Copper ore") < inventoryCap) {
			return State.TRAVELTOMINE;
		}
		// if the bot's inventory is filled to up inventory cap and bot is not in the
		// banking zone go to the banking zone
		if (getInventory().count("Copper ore") >= inventoryCap && !bankingZone.contains(getLocalPlayer())) {
			return State.TRAVELTOBANK;
		}
		// if the bot is int he banking zone and bot's inventory isnt empty go to bank
		// and empty inventory
		if (bankingZone.contains(getLocalPlayer()) && !getInventory().isEmpty()) {
			return State.BANKING;
		}
		// if the banking menu is open and bot's inventory is empty close bot inventory
		if (getBank().isOpen() && getInventory().isEmpty()) {
			return State.CLOSEBANK;
		}
		// should never trigger if triggers error is State Machines logic check State
		// conditions
		return State.WAIT;
	}

	@Override
	public void onStart() { // 0th state
		setInventoryCount();
	}

	@Override
	public int onLoop() {
		switch (getState()) {
		case GETTOOL:
			log("MiningTask: Grabing Tools");
			ToolGraber toolGraber = new ToolGraber();
			toolGraber.toolsToGrab.add("Bronze pickaxe");
			while (!toolGraber.isDone) {
				toolGraber.onLoop();
				sleep(Calculations.random(200, 500));
			}
			break;
		case MINING:
			log("MiningTask: Mining");
			List<GameObject> everything = getGameObjects().all();// gets list of all GameObject with in the mining area
			// goes through all object in everything list
			for (int i = 0; i < everything.size() - 1; i++) {
				GameObject fromList = everything.get(i);// gets object from list to be checked
				// makes sure that object form the list was grabbed
				if (fromList != null) {
					// checks if object name is rock
					if (fromList.getName().contains("Rock")) {
						// checks if rock has a getModelColors array
						if (fromList.getModelColors() != null) {
							if (fromList.hasAction("Mine")) {
								if (getSkills().getRealLevel(Skill.MINING) < 15
										|| getSkills().getRealLevel(Skill.SMITHING) < 15) {
									if (!lowLVOreSwitcher) {
										if (fromList.getModelColors()[0] == 53) {
											if (miningZone.contains(fromList)) {
												fromList.interact("Mine");
												int count = getInventory().count("Tin Ore");
												Timer timer = new Timer(20000);
												boolean activationMethod = false;
												while (getInventory().count("Tin Ore") <= count) {
													log("MiningTask: mining wait loop");
													sleep(Calculations.random(500, 600));
													if (timer.finished()) {
														if (!activationMethod) {
															log("MiningTask: Miner fromList.interact didn't work trying getMouse().click(Point)");
															timer = new Timer(20000);
															getMouse().click(fromList.getClickablePoint());
															activationMethod = true;
														} else {
															log("MiningTask: Miner Second fail safe timer finished");
															Area rockArea = fromList.getSurroundingArea(3);
															log("MiningTask: Fall back travel area - "
																	+ rockArea.getBoundingBox().getMaxX() + " "
																	+ rockArea.getBoundingBox().getMaxY() + " "
																	+ rockArea.getBoundingBox().getMinX() + " "
																	+ rockArea.getBoundingBox().getMinY());
															getWalking().walk(rockArea.getRandomTile());
															while (!getWalking().shouldWalk(2)) {
																sleep(Calculations.random(100, 250));
															}
															break;
														}
													}
												}

												lowLVOreSwitcher = true;
												break;
											}
										}
									} else {
										if (miningZone.contains(fromList)) {
											if (fromList.getModelColors()[0] == 4645) {
												fromList.interact("Mine");
												int count = getInventory().count("Copper Ore");
												Timer timer = new Timer(20000);
												boolean activationMethod = false;
												while (getInventory().count("Copper Ore") <= count) {
													log("MiningTask: mining wait loop");
													sleep(Calculations.random(500, 600));
													if (timer.finished()) {
														if (!activationMethod) {
															log("MiningTask: Miner fromList.interact didn't work trying getMouse().click(Point)");
															timer = new Timer(20000);
															getMouse().click(fromList.getClickablePoint());
															activationMethod = true;
														} else {
															log("MiningTask: Miner Second fail safe timer finished");
															Area rockArea = fromList.getSurroundingArea(3);
															log("MiningTask: Fall back travel area - "
																	+ rockArea.getBoundingBox().getMaxX() + " "
																	+ rockArea.getBoundingBox().getMaxY() + " "
																	+ rockArea.getBoundingBox().getMinX() + " "
																	+ rockArea.getBoundingBox().getMinY());
															getWalking().walk(rockArea.getRandomTile());
															while (!getWalking().shouldWalk(2)) {
																sleep(Calculations.random(100, 250));
															}
															break;
														}
													}
												}
												lowLVOreSwitcher = false;
												break;
											}
										}
									}
								} else {
									// Handle geting iron ore
								}
							}
						}
					}
				}
			}
			// Resents mining part of the bot after it done mining an ore
			break;
		case TRAVELTOMINE:
			log("MiningTask: Traveling to mine");
			// prevents unneeded bot inputs when its in the
			// zone
			if (!miningZone.contains(getLocalPlayer())) {
				// check distance before current destination
				if (getWalking().shouldWalk(7)) {
					getWalking().walk(miningZone.getRandomTile());// sets the next destination flag
				}

			}
			break;
		case TRAVELTOBANK:
			log("MiningTask: Traveling to bank");
			// prevent unneeded bot inputs when its in the zone
			if (!bankingZone.contains(getLocalPlayer())) {
				// check distance before current destination
				if (getWalking().shouldWalk(7)) {
					getWalking().walk(bankingZone.getRandomTile());// sets the next destination flag
				}
			}
			break;
		case BANKING:
			log("MiningTask: Depositing in bank");

			NPC banker = getNpcs().closest("Banker");// gets the closet banker
			// check if getting the closets banker was successful
			if (banker != null) {
				/*
				 * This code was used to debug possible actions on the banker to determine how
				 * to begin interaction String[] bankerActions = banker.getActions(); for (int i
				 * = 0; i < bankerActions.length; i++) { log("Banker Action: " +
				 * bankerActions[i]); }
				 */
				banker.interact("Bank");// opens banking screen
				sleep(Calculations.random(4000, 6000));// sleeps between 4 to 6 seconds to prevent bot spaming
				// check is depositing action has been initiated already to prevent bot spaming
				// and check if banking menu is open to prevent unneeded bot actions
				if (!depositing && getBank().isOpen()) {
					depositing = true;// sets depositing boolean to prevent bot tacking the same action more than ones
					getBank().depositAllItems();// starts deposit action
					depositResetTimer = new Timer(7500);// starts a timer for 7.5 seconds to reset deposit action if
														// need
				}
			} else {
				log("MiningTask: Banker NPC is null");
			}
			// makes sure depositResetTimer isn't null to prevent errors... Timer set to 7.5
			// seconds
			if (depositResetTimer != null) {
				// check if depositResetTimer is done
				if (depositResetTimer.finished()) {
					depositing = false;// resets deposit action
				}
			}
			break;
		case CLOSEBANK:
			log("MiningTask: Closing Banking Menu");
			getBank().close();// closes banking page
			setInventoryCount();
			break;
		case WAIT:
			// should never trigger if it dose than there is an error with in StateMachine
			// logic check State conditions
			log("MiningTask: Waiting");
			break;

		}
		return Calculations.random(500, 600);// waits 500 to 600 milliseconds to prevent Anti-bot detection
	}

	public void setInventoryCount() {
		if (getSkills().getRealLevel(Skill.WOODCUTTING) < 15 || getSkills().getRealLevel(Skill.SMITHING) < 15) {
			inventoryCap = Calculations.random(1, 13);
		} else {
			// Set higher inventory count when mining iron
		}
	}
}

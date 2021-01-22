import java.util.List;

import javax.script.ScriptEngineManager;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.path.AbstractPath;
import org.dreambot.api.methods.walking.path.impl.LocalPath;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.loader.LocalLoader;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.map.TileMap;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManager;

@ScriptManifest(author = "Satan", name = "Miner/Smelter out of Varrock", version = 1.0, description = "Mines and smelts for you around Verrock Area", category = Category.MINING)
public class main extends AbstractScript {
	// booleans to execute operations only ones per state enter or as needed
	boolean mining = false;
	boolean calculatedTravelTile = false;
	boolean depositing = false;
	boolean forceWalkToBank = false;
	// Area object defining different coordinates bot will have to travel to for
	// tasks
	Area miningZone = new Area(3281, 3372, 3290, 3360);
	Area bankingZone = new Area(3244, 3429, 3263, 3412);
	// Tiles are used by navigation algorithm to determine navigation to need areas
	// name on the tile in the final destination
	Tile bankEnteranceTile = new Tile(3253, 3426);
	Tile mineEnteranceTile = new Tile(3287, 3367);
	int inventoryCap = 20;// max allowed item count in inventory before trip to the bank
	int inventoryTracker = 0;// used to reset mining boolean
	int miningResetCounter = 0;
	LocalPath path;// Path tracking variable
	List<GameObject> everything;// List of all object in the mining area
	GameObject oreToMine; // GameObject of ore to be mine
	GameObject lastMinedOre;// Used to store last mined ore GameObject to avoid mining the same object over
							// and over
	Timer mineingResetTimer;// Resets mining boolean if bot gets stuck on mining task for longer that 20
							// seconds
	Timer depositResetTimer;// Resets depositing boolean if bot gets stuck depositing for longer than 7.5
							// seconds
	
	AntiAntiBot antiBotKiller = new AntiAntiBot();//initializes Anti-Anti-Bot and gives it a local variable name
	
	public void onStart() {
		antiBotKiller.onStart();// sets up Anti-Anti-Bot with its onStart() function
	}

	/*
	 * State machine states: 
	 * MINING - Takes care or mining task ones bot is at
	 * mining location 
	 * TRAVELTOMINE - takes care of bot's path finding to the mine
	 * TRAVELTOBANK - takes care of bot's path finding to the bank bot to the bank and opens banking menu and takes care of depositing all the items 
	 * CLOSEBANK - closes banking page to counter anti-bot 
	 * WAIT - should nevert rigger if it dose than there is an error with in StateMachine logic check State conditions
	 * ANTIANTIBOTSTOP - used to pause main task execution for deturrent task
	 */
	private enum State {
		MINING, TRAVELTOMINE, TRAVELTOBANK, BANKING, CLOSEBANK, ANTIBOTSTOP, WAIT
	}

	// Sets State of State Machine Based on Conditions
	private State getState() {
		// if the bot is in the mining zone and bots inventory isn't filled up to
		// inventory cap than mine

		if(antiBotKiller.isExecuting)
		{
			return State.ANTIBOTSTOP;
		}
		if(forceWalkToBank)
		{
			return State.TRAVELTOBANK;
		}
		if (miningZone.contains(getLocalPlayer()) && getInventory().count("Copper ore") < inventoryCap) {
			return State.MINING;
		}
		// if the bot is not in the mining zone and its inventory is empty and bank
		// screen isn't open than go to the mining zone
		if (getInventory().isEmpty() && !miningZone.contains(getLocalPlayer()) && !getBank().isOpen()) {
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

	public void onExit() {

	}

	@Override
	public int onLoop() {
		antiBotKiller.onLoop();//calls Anti-Anti-Bot's onLoop()
		switch (getState()) {
		case MINING:
			// Debugs Mining boolean state amount of coper in inventory will be removed in
			// final version
			//log("Mining " + mining + " Inventory count of copper: " + getInventory().count("Copper ore"));
			// Debugs Mining Boolean reset timer will be removed in final version
			if (mineingResetTimer != null) {
				//log(" Time remaining before force reset: " + mineingResetTimer.remaining());
				//log(" Mining reset attempts: " + miningResetCounter);
			}
			everything = getGameObjects().all();// gets list of all GameObject with in the mining area
			// check mining boolean to make sure bot isn't already mining
			if (!mining) {
				// goes through all object in everything list
				for (int i = 0; i < everything.size() - 1; i++) {
					GameObject fromList = everything.get(i);// gets object from list to be checked
					// makes sure that object form the list was grabbed
					if (fromList != null) {
						// checks if object name is rock
						if (fromList.getName().contains("Rock")) {
							// checks if rock has a getModelColors array
							if (fromList.getModelColors() != null) {
								// check rock's color to determine material
								if (fromList.getModelColors()[0] == 4645) {
									// check if lastMinedOre exist before trying to avoid going to the same rock to
									// mine
									if (lastMinedOre != null) {
										// makes sure that picked rock isn't the same as last time
										if (fromList.getTile() != lastMinedOre.getTile()) {
											oreToMine = fromList;
											mine();
										}
										// Handles lastMinedOre being null
									} else {
										oreToMine = fromList;
										mine();
									}
								}
							}
						}
					}
				}
			}
			// Resents mining part of the bot after it done mining an ore
			else {
				// check if bot has more copper ore in inventory before last time recorded
				if (getInventory().count("Copper ore") > inventoryTracker) {
					mining = false;// resets mining boolean to get the next rock to mine
					lastMinedOre = oreToMine;// sets last mined GameObject
					oreToMine = null;// sets oreToMine to null in to not get stuck mining a rock that dosen't exist
					sleep(Calculations.random(1000, 2000));// sleeps 1 to 2 seconds to prevent Anti-Bot detection
					miningResetCounter = 0;//counter restarted after action was done so no need to increment 
				}
			}
			// Hard resets mining if time to mine a rock runs out... Timer set to 20 seconds
			if (mineingResetTimer.finished()) {
				mining = false;// resets mining boolean to get the next rock to mine
				lastMinedOre = oreToMine;// sets last mined GameObject
				oreToMine = null;// sets oreToMine to null in to not get stuck mining a rock that dosen't exist
				miningResetCounter++;// Mining hard reset increase counter
			}
			if(miningResetCounter > 1)
			{
				forceWalkToBank = true;
			}
			break;
		case TRAVELTOMINE:
			log("Traveling to mine");
			// prevents unneeded bot inputs when its in the
			// zone
			if (getInventory().isEmpty() && !miningZone.contains(getLocalPlayer())) {
				// check distance before current destination
				if (getWalking().shouldWalk(7)) {
					getWalking().walk(miningZone.getRandomTile());// sets the next destination flag
				}

			}
			break;
		case TRAVELTOBANK:
			// prevent unneeded bot inputs when its in the zone
			if (!bankingZone.contains(getLocalPlayer())) {
				// check distance before current destination
				if (getWalking().shouldWalk(7)) {
					getWalking().walk(bankingZone.getRandomTile());// sets the next destination flag
				}
				log("Traveling to bank");
			}
			else{
				forceWalkToBank = false;
				miningResetCounter = 0;//counter restarted after action was done so no need to increment 
			}
			break;
		case BANKING:
			log("Depositing in bank");
			
			NPC banker = getNpcs().closest("Banker");// gets the closet banker
			// check if getting the closets banker was successful
			if (banker != null) {
				/* This code was used to debug possible actions on the banker to determine how to begin interaction
				 * String[] bankerActions = banker.getActions(); for (int i = 0; i <
				 * bankerActions.length; i++) { log("Banker Action: " + bankerActions[i]); }
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
				log("Banker NPC is null");
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
				getBank().close();// closes banking page
			break;
		case ANTIBOTSTOP:
			sleep(Calculations.random(1000, 3000));
			break;
		case WAIT:
			// should never trigger if it dose than there is an error with in StateMachine
			// logic check State conditions
			log("Waiting");
			break;

		}
		return Calculations.random(500, 600);// waits 500 to 600 milliseconds to prevent Anti-bot detection
	}
	
	// Funtion to take care of mining operation
	private void mine() {
		// check is oreToMine is not null to prevent errors and check if bot is mining
		// to prevent unneeded actions
		if (oreToMine != null && !mining) {
			mining = true;// sets mining boolean to true
			inventoryTracker = getInventory().count("Copper ore");// sets current copper ore amount in the inventory
																	// before mining
			oreToMine.interact("Mine");// starts to mine the rock
			mineingResetTimer = new Timer(20000);// starts a timer for 20 seconds to reset mining action if need
		}
	}
	

}
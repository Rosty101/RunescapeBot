package AntiAntiBotPack;
import java.util.ArrayList;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.script.Category;

public class ToolGraber extends AbstractScript {

	public boolean isDone = false;
	public ArrayList<String> toolsToGrab = new ArrayList<String>();
	private Area bankingZone = new Area(3250, 3426, 3257, 3430);
	private boolean isFirstCheckDone = false;
	private boolean isDepositing = false;
	Timer depositResetTimer;
	Timer checkTimer;
	public void onStart() {

	}

	public void onExit() {

	}

	public enum State {
		FIRSTCHECK, TRAVELTOBANK, GOTOTELLER, GRABTOOLS, HAVETOOLS, WAIT
	};

	private State getState() {
		if (!isFirstCheckDone && !isDone) {
			return State.FIRSTCHECK;
		}
		if (getBank().isOpen() && !isDone) {
			return State.GRABTOOLS;
		}
		if (!bankingZone.contains(getLocalPlayer()) && isFirstCheckDone && !isDone) {
			return State.TRAVELTOBANK;
		}
		if (bankingZone.contains(getLocalPlayer()) && !getBank().isOpen() && !isDone) {
			return State.GOTOTELLER;
		}
		return State.WAIT;
	}

	public int onLoop() {
		switch (getState()) {
		case FIRSTCHECK:
			for (int i = 0; i < toolsToGrab.size(); i++) {
				if (getInventory().count(toolsToGrab.get(i)) == 0) {
					isFirstCheckDone = true;
					break;
				} else if (i == 0) {
					isDone = true;
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
			break;
		case GOTOTELLER:
			log("ToolGraber unequiting tool and open bank menue");
			NPC banker = getNpcs().closest("Banker");// gets the closet banker
			// check if getting the closets banker was successful
			if (banker != null) {
				if (getEquipment().isSlotFull(EquipmentSlot.WEAPON.getSlot())) {
					getEquipment().open();
					while (!getEquipment().open()) {
						sleep(Calculations.random(400, 700));
					}
					getEquipment().unequip(EquipmentSlot.WEAPON);
					while (getEquipment().isSlotFull(EquipmentSlot.WEAPON.getSlot())) {
						sleep(Calculations.random(300, 700));
					}
				} else {
					banker.interact("Bank");// opens banking screen
					checkTimer = new Timer(10000);
					while (!getBank().isOpen()) {// sleeps between 4 to 6 seconds to prevent bot spaming
						sleep(Calculations.random(200, 400));
						if(checkTimer.finished())
						{
							break;
						}
					}
				}
				// check is depositing action has been initiated already to prevent bot spaming
				// and check if banking menu is open to prevent unneeded bot actions

				depositResetTimer = new Timer(7500);// starts a timer for 7.5 seconds to reset deposit action if // need
			} else {
				log("Banker NPC is null");
			}
			// makes sure depositResetTimer isn't null to prevent errors... Timer set to 7.5
			// seconds
			break;
		case GRABTOOLS:
			log("Depositing inventory and grabing tools");
			if (getBank().isOpen()) {
				if (!getInventory().isEmpty()) {
					getBank().depositAllItems();
					while (!getInventory().isEmpty()) {
						sleep(Calculations.random(200, 800));
					}
				}
				for (int i = 0; i < toolsToGrab.size(); i++) {
					if (getBank().contains(toolsToGrab.get(i))) {
						getBank().withdraw(toolsToGrab.get(i));
						while (!getInventory().contains(toolsToGrab.get(i))) {
							sleep(Calculations.random(1000, 2000));
						}
						log("GotTools");
						if (i == toolsToGrab.size() - 1) {
							getEquipment().equip(EquipmentSlot.WEAPON, toolsToGrab.get(0));
							
							getBank().close();
							while(getBank().isOpen())
							{
								sleep(Calculations.random(300, 650));
							}
							isDone = true;
						}
					} else {
						if (i == toolsToGrab.size() - 1) {
							getEquipment().equip(EquipmentSlot.WEAPON, toolsToGrab.get(0));
							getBank().close();
							while(getBank().isOpen())
							{
								sleep(Calculations.random(300, 650));
							}
							isDone = true;
						}
						// handle making tool
					}
				}
			}
			break;
		case WAIT:
			log("toolGraber Wait sleep");
			sleep(Calculations.random(350, 775));
			break;

		}
		return Calculations.random(500, 600);
	}
}

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.impl.TaskScript;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.script.Category;

public class AntiAntiBot extends AbstractScript {

	// State machine control Booleans
	private boolean isWaitingToGuess = true;
	private boolean isGuessing = false;
	public boolean isExecuting = false;

	// booleans to controll execution
	private boolean isCheckingTool = false;
	private boolean isRuningAction = false;
	private boolean isSetToolGraber = false;
	// Array of areas for different actions

	private ArrayList<Integer> trigerNumbers = new ArrayList<Integer>();// List of numbers on which deterrent executes

	private Timer mainTimer;// Main timer for Anti-Anti-Bot
	private Timer testTimer;
	private int minAmountOfTimeBeforeTriger = 0;// minimum amount of time before deterrent can trigger
	private int randomSelectedAction = 0;
	ArrayList<ObjectiveSettings> settingsList = new ArrayList<ObjectiveSettings>(); // list of different setting to
																					// execute deterrent properly
	ToolGraber toolGraber;

	// Executes one time;
	public void onStart() {
		populateSettnigsList();// Populates list of setting for different actions
		log("Don't fear Anti-Anti-Bot is here!!!");
		mainTimer = new Timer(6000000);// Sets initial timer the whole deterrent is based on 100 minutes of delay;
		testTimer = new Timer(60000);
		minAmountOfTimeBeforeTriger = Calculations.random(1200000, 3600000);
		log("Deterent Timer Loop Started");
		sleep(Calculations.random(1000, 3000));// short sleep to avoid extreamly small triggerNumbers list size... NOTE:
												// consider removing this line it mite not be needed
	}

	/*
	 * State machine states: WAITINGFORCOUNTER - a delay with in Anti-Anti-Bot
	 * script GUESSINGCOUNTER - check probality curve to adjust trigerNumbers list
	 * based on time elapsed and guesses a random number with in the range to start
	 * execution; COUNTEREXECUTE - runs out a set on instructions to act out the
	 * deturrent
	 */
	private enum State {
		WAITINGFORCOUNTER, COUNTEREXECUTE, GUESSINGCOUNTER, WAIT
	};

	private State getState() {
		if (testTimer != null) {
			if (testTimer.finished()) {
				return State.COUNTEREXECUTE;
			}
		}
		if (isWaitingToGuess) // case waiting for time to check if apply counter measure triggers
		{
			return State.WAITINGFORCOUNTER;
		}
		if (isGuessing) {// case roll dice for a chance of counter measure to trigger and what counter

			return State.GUESSINGCOUNTER;// case act out counter measure
		}
		if (isExecuting) {
			return State.COUNTEREXECUTE;
		}
		return State.WAIT;
	}

	public void onExit() {

	}

	public int onLoop() {
		switch (getState()) {
		case WAITINGFORCOUNTER:
			// log("Anti-Anti-Bot sleep");
			sleep(Calculations.random(100, 200));// random sleep for this script
			isGuessing = true;// boolean to move on to GUESSINGCOUNTER state
			isWaitingToGuess = false;// boolean to exit this state
			break;
		case GUESSINGCOUNTER:
			// log("Anti-Anti-Bot guess");
			adjustTrigerListPopulation(1, calculateDeterentStartChance((int) mainTimer.elapsed()));
			// gets needed population size for trigerNumbers from calculateDeterentStartChance and adjusts trigerNumbers list according
			int trigerNumber = Calculations.random(0, 1175);// rolls a random number to start deturrent execution
			// check if trigerNumber number is in trigerNumbers list and if is move on to
			// COUNTEREXECUTE state
			if (trigerNumbers.contains(trigerNumber) && minAmountOfTimeBeforeTriger > mainTimer.elapsed()) {
				isExecuting = true;// boolean to move on to COUNTEREXECUTE state
				log("Starting Deterent Action");
				isGuessing = false;// boolean to exit this state
			} else {
				isWaitingToGuess = true;// boolean to move on to WAITINGFORCOUNTER state
				isGuessing = false;// boolean to exit this state
			}
			break;
		case COUNTEREXECUTE:
			if (testTimer != null) {
				if (testTimer.finished() && !isSetToolGraber) {
					isWaitingToGuess = false;
					isGuessing = false;
					isExecuting = true;
					testTimer = null;
				}
			}
			if (!isSetToolGraber) {
				log("Making toolGraber for the task !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				toolGraber = new ToolGraber();
				ObjectiveSettings curentObjective = settingsList.get(randomSelectedAction);
				for (int i = 0; i < curentObjective.toolNeeded.size(); i++) {
					toolGraber.toolsToGrab.add(curentObjective.toolNeeded.get(i));
				}
				isSetToolGraber = true;
			}
			if (toolGraber != null) {
				if (!toolGraber.isDone) {
					log("Grabing tools");
					toolGraber.onLoop();
				}
				if (toolGraber.isDone) {
					isRuningAction = true;
				}
			}
			if (isRuningAction) {
				log("Runing Selected Action");
				
				isRuningAction = false;
			}
			if (toolGraber != null) {
				if (toolGraber.isDone && !isRuningAction) {
					log("Anti-Anti-Bot reseting");
					toolGraber = null;
					adjustTrigerListPopulation(2, 0);
					mainTimer = new Timer(6000000);
					isSetToolGraber = false;
					isWaitingToGuess = true;// boolean to move on to WAITINGFORCOUNTER state
					isExecuting = false;// boolean to exit this state
				}
			}
			break;
		case WAIT:
			// should never trigger if it dose than there is an error with in StateMachine
			// logic check State conditions
			break;
		}

		return Calculations.random(1000, 3500);
	}

	/*
	 * Function to work on trigerNumbers list: case 1: adjust trigerNumbers list
	 * base on desiredPopulation input case 2: wipes trigerNumbers list clean
	 * default: should never trigger error
	 */
	private void adjustTrigerListPopulation(int action, int desiredPopulation) {
		switch (action) {
		case 1:
			boolean populate = true;// boolean to keep the while loop active when filling list to desired amount
			int i = 0;// Integer to keep track of how many number have been added to trigerNumbers
						// list
			// check if desiredPopulation is 0 to not execute code thats not needed
			if (desiredPopulation != 0) {
				// start of while loop
				while (populate) {
					/*
					 * log("Inside while loop for trigerList population adjustment... i = " + i + "
					 * Desired Population: " + desiredPopulation + " Boolean Populate: " +
					 * populate);
					 */
					// checks trigerNumbers list population size to handle empty list exception
					if (!trigerNumbers.isEmpty()) {
						// checks if population size changed and if not turns on and breaks while loop
						if (desiredPopulation == trigerNumbers.size()) {
							populate = false; //
							break;
						}
						int extraNeeded = desiredPopulation - trigerNumbers.size();// calculates amount on numbers that
																					// need to be added
						// checks if amount added isn't more that needed
						if (i <= extraNeeded) {
							int newTrigerNumber = Calculations.random(0, 1175);// rolls a random number to add to the
																				// list
							// check if number is not on the list and if it's not than add it to the list
							// and increase added amount variable
							if (!trigerNumbers.contains(newTrigerNumber)) {

								trigerNumbers.add(newTrigerNumber);
								i++;
							}
						}
						// check if script finished adding number and if it is break out of while loop
						if (i == extraNeeded) {
							populate = false;
							break;
						}
						// hadles trigerNumbers
					} else {
						// checks if amount added isn't more that needed
						if (i <= desiredPopulation) {
							int newTrigerNumber = Calculations.random(0, 1175);// rolls a random number to add to the
																				// list
							// check if number is not on the list and if it's not than add it to the list
							// and increase added amount variable
							if (!trigerNumbers.contains(newTrigerNumber)) {
								trigerNumbers.add(newTrigerNumber);
								i++;
							}
						}
						// check if script finished adding number and if it is break out of while loop
						if (i == desiredPopulation) {
							populate = false;
							break;
						}
					}

				}
			}
			break;
		// This case will take care of reseting the list ones the deterrent action has
		// executed
		case 2:
			trigerNumbers.clear();
			break;
		default:
			// will never trigger
			break;
		}
	}

	// Uses formula F(x)=.09x^2 + 2.5x to figure out how many random numbers to put
	// in trigger list... x being time passed between deterrent actions
	// HINT: THE FORMULA IS RIGHT UP THERE
	private int calculateDeterentStartChance(int timePassed) {
		int returnVar = 0;
		float milliSecondsToSeconds = timePassed / 1000;
		float secondsToMinutes = milliSecondsToSeconds / 60;
		float timePassedSqr = secondsToMinutes * secondsToMinutes;
		float upper = timePassedSqr * (float) .09;
		float lower = secondsToMinutes * (float) 2.5;
		float answer = upper + lower;
		returnVar = Math.round(answer);
		/*
		 * log("Math curve return val: " + returnVar + " Upper: " + upper + "Lower: " +
		 * lower + " SecondsToMinutes: " + secondsToMinutes + " timePassed: " +
		 * timePassed);
		 */
		return returnVar;
	}

	// function that will create setting for different deterrents and add them to
	// the settingsList
	private void populateSettnigsList() {
		// Creating and Adding Setting for Wood Cuter task
		ObjectiveSettings woodCutterTask = new ObjectiveSettings();
		woodCutterTask.taskName = "WoodCutting";
		woodCutterTask.toolNeeded.add("Bronze axe");
		settingsList.add(woodCutterTask);

	}
}

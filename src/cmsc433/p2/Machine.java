package cmsc433.p2;

/**
 * A Machine is used to make a particular Food. Each Machine makes just one kind
 * of Food. Each machine has a capacity: it can make that many food items in
 * parallel; if the machine is asked to produce a food item beyond its capacity,
 * the requester blocks. Each food item takes at least item.cookTimeS seconds to
 * produce.
 */

public class Machine {
	// Types of machines used in Ratsie's. Recall that enum types are
	// effectively "static" and "final", so each instance of Machine
	// will use the same MachineType.

	public static enum MachineType {
		fountain, fryer, grillPress, oven
	};

	public static int getCookingTime(MachineType foodType) {
		if (foodType == MachineType.fountain) {
			return 15;
		} else if (foodType == MachineType.fryer) {
			return 350;
		} else if (foodType == MachineType.grillPress) {
			return 200;
		} else if (foodType == MachineType.oven) {
			return 600;
		} else {
			throw new UnsupportedOperationException("Unknown food type.");
		}
	}

	// Converts Machine instances into strings based on MachineType.

	public String toString() {
		switch (machineType) {
		case fountain:
			return "Fountain";
		case fryer:
			return "Fryer";
		case grillPress:
			return "Grill Presss";
		case oven:
			return "Oven";
		default:
			return "INVALID MACHINE";
		}
	}

	public final MachineType machineType;
	public final Food machineFoodType;
	public final Integer capacity;
	public Integer numCooking;

	// YOUR CODE GOES HERE...

	/**
	 * The constructor takes at least the type of the machine, the Food item it
	 * makes, and its capacity. You may extend it with other arguments, if you
	 * wish. Notice that the constructor currently does nothing with the
	 * capacity; you must add code to make use of this field (and do whatever
	 * initialization etc. you need).
	 */
	public Machine(MachineType machineType, Food food, int capacityIn) {
		this.machineType = machineType;
		this.machineFoodType = food;
		this.capacity = capacityIn;
		this.numCooking = 0;

		// At startup:
		Simulation.logEvent(SimulationEvent.machineStarting(this, food, capacity));

		// YOUR CODE GOES HERE...
	}

	/**
	 * This method is called by a Cook in order to make the Machine's food item.
	 * You can extend this method however you like, e.g., you can have it take
	 * extra parameters or return something other than Object. It should block
	 * if the machine is currently at full capacity. If not, the method should
	 * return, so the Cook making the call can proceed. You will need to
	 * implement some means to notify the calling Cook when the food item is
	 * finished.
	 */
	public Thread makeFood(Food food) {
		Thread thread = new Thread(new CookAnItem(this, food));
		thread.start();
		return thread;
	}

	private boolean isAvailable() {
		synchronized (this) {
			return numCooking < capacity;
		}
	}

	public void shutDown() {
		// When shut down, at the end of the simulation:
		Simulation.logEvent(SimulationEvent.machineEnding(this));
	}

	private class CookAnItem implements Runnable {
		Machine machine;
		Food food;

		public CookAnItem(Machine machine, Food food) {
			this.machine = machine;
			this.food = food;
		}

		public void run() {
			synchronized (machine) {
				while (!isAvailable()) {
					try {
						machine.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// When beginning to make a food item:
				Simulation.logEvent(SimulationEvent.machineCookingFood(machine, food));
				numCooking++;
			}
			try {
				Thread.sleep(getCookingTime(machineType));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// TODO: Else
			// TODO: Notify the calling cook when finished
			synchronized (machine) {
				// When done making a food item:
				Simulation.logEvent(SimulationEvent.machineDoneFood(machine, machineFoodType));
				numCooking--;
				machine.notifyAll();
			}
		}
	}
}
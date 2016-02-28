package cmsc433.p2;


/**
 * Cooks are simulation actors that have at least one field, a name.
 * When running, a cook attempts to retrieve outstanding orders placed
 * by Eaters and process them.
 */
public class Cook implements Runnable {
	private final String name;

	/**
	 * You can feel free to modify this constructor.  It must
	 * take at least the name, but may take other parameters
	 * if you would find adding them useful. 
	 *
	 * @param: the name of the cook
	 */
	public Cook(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	/**
	 * This method executes as follows.  The cook tries to retrieve
	 * orders placed by Customers.  For each order, a List<Food>, the
	 * cook submits each Food item in the List to an appropriate
	 * Machine, by calling makeFood().  Once all machines have
	 * produced the desired Food, the order is complete, and the Customer
	 * is notified.  The cook can then go to process the next order.
	 * If during its execution the cook is interrupted (i.e., some
	 * other thread calls the interrupt() method on it, which could
	 * raise InterruptedException if the cook is blocking), then it
	 * terminates.
	 */
	public void run() {

		Simulation.logEvent(SimulationEvent.cookStarting(this));
		try {
			while(true) {
				
				
				// TODO: Try to retrieve orders placed by Customers
				
				
				
				
				// TODO: For each List<Food> order
					// TODO: Upon starting an order:
					Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, order, orderNumber));
				
					// TODO: Submit each Food item to an appropriate Machine. makeFood()
					
					// TODO: For each Food food:
						// TODO: Upon submitted request to food machine:
						Simulation.logEvent(SimulationEvent.cookStartedFood(this, food, orderNumber));
						
						// TODO: Upon receiving a completed food item:
						Simulation.logEvent(SimulationEvent.cookFinishedFood(this, food, orderNumber));
					
					// TODO: Upon completing an order:
					Simulation.logEvent(SimulationEvent.cookCompletedOrder(this, orderNumber));
						
					// TODO: Once all machines have produced the desired food,  
					// notify the Customer since the order is complete.
		
					
					
				
				// Just before terminating:
				Simulation.logEvent(SimulationEvent.cookEnding(this));
				
			}
		} catch(InterruptedException e) {
			// If the cook is interrupted, terminate since all customers are done. 
			Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
	}
}
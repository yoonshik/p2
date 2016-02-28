package cmsc433.p2;

import java.sql.Time;
import java.util.List;

/**
 * Cooks are simulation actors that have at least one field, a name.
 * When running, a cook attempts to retrieve outstanding orders placed
 * by Eaters and process them.
 */
public class Cook implements Runnable {
	private static final int WING = 0;
	private static final int PIZZA = 1;
	private static final int SUB = 2;
	private static final int SODA = 3;
	private static final int FOOD_TYPE_COUNT = 4;
	
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
				
				List<Food> order;
				
				int orderNumber;
				// TODO: Try to retrieve orders placed by Customers
				synchronized(Simulation.ordersNew) {
					if (Simulation.ordersNew.isEmpty()) {
						Thread.sleep(50);
						continue;
					}
					order = Simulation.ordersNew.remove();
				}
				synchronized(Simulation.ordersInProgress) {
					Simulation.ordersInProgress.add(order);
				}
				
				synchronized(Simulation.orderNumbers) {
					orderNumber = Simulation.orderNumbers.get(order);
				}
				
				// TODO: Upon starting an order:
				Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, order, orderNumber));				
				
				int[] foodCount = new int[FOOD_TYPE_COUNT];
				for (Food food : order) {
					if (food == FoodType.wings) {
						++foodCount[WING];
					} else if (food == FoodType.pizza) {
						++foodCount[PIZZA];
					} else if (food == FoodType.sub) {
						++foodCount[SUB];
					} else if (food == FoodType.soda) {
						++foodCount[SODA];
					} else {
						throw new UnsupportedOperationException("Unknown food type: " + food.name);
					}
				}
				
				// TODO: For each List<Food> order
				for (int i = 0; i < FOOD_TYPE_COUNT; i++) {
					
					Food food;
					switch (i) {
					case WING: food = FoodType.wings;
					case PIZZA: food = FoodType.pizza;
					case SUB: food = FoodType.sub;
					case SODA: food = FoodType.soda;
					break;
					default: throw new UnsupportedOperationException("Unknown food count: " + i);
					}
					
					// Find appropriate machine
					Machine machine = Simulation.machines.get(food);
					
					// Submit each food item
					synchronized(machine) {
						machine.makeFood(food, foodCount[i]);
					}
					
					// Upon submitted request to food machine:
					Simulation.logEvent(SimulationEvent.cookStartedFood(this, food, orderNumber));

					// Upon receiving a completed food item:
					Simulation.logEvent(SimulationEvent.cookFinishedFood(this, food, orderNumber));
					
					// Upon completing an order:
					Simulation.logEvent(SimulationEvent.cookCompletedOrder(this, orderNumber));
					
					// TODO: Once all machines have produced the desired food,  
					// notify the Customer since the order is complete.
					
				}
					
		
					
					
				
				// Just before terminating:
				Simulation.logEvent(SimulationEvent.cookEnding(this));
				
			}
		} catch(InterruptedException e) {
			// If the cook is interrupted, terminate since all customers are done. 
			Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
	}
}
package cmsc433.p2;

import java.util.HashMap;
import java.util.LinkedList;
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

	private HashMap<Food, Machine> machines;

	private HashMap<Integer, HashMap<Food, LinkedList<Thread>>> makeFoodThreads;

	/**
	 * You can feel free to modify this constructor.  It must
	 * take at least the name, but may take other parameters
	 * if you would find adding them useful. 
	 *
	 * @param: the name of the cook
	 */
	public Cook(String name, HashMap<Food, Machine> machines) {
		this.name = name;
		this.machines = machines;
		
		this.makeFoodThreads = new HashMap<Integer, HashMap<Food, LinkedList<Thread>>>();
	}
	
	/**
	 * @return Cook's name.
	 */
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
			while (true) {
				List<Food> order = null;
				Integer orderNumber;
				
				synchronized(Ratsies.singleton.ordersNew) {
					while(!Ratsies.singleton.newOrderAvailable()) {
						Ratsies.singleton.ordersNew.wait();
					}
					// Try to retrieve orders placed by Customers
					orderNumber = Ratsies.singleton.getNextOrder();
				}
				
				order = Ratsies.singleton.getOrder(orderNumber);

				processOrder(order, orderNumber);
			}
		} catch (InterruptedException e) {
			// Just before terminating:
			Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
	}

	/**
	 * Process a List<Food> order. Spawn a new thread for each food item using makeFood(), and join at the end.
	 * @param order
	 * @param orderNumber
	 * @throws InterruptedException
	 */
	private void processOrder(List<Food> order, Integer orderNumber) throws InterruptedException{
		
		synchronized(Ratsies.singleton.getOrderLock(orderNumber)) {
			// Upon starting an order:
			Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, order, orderNumber));
			
			makeFoodThreads.put(orderNumber, new HashMap<Food, LinkedList<Thread>>());

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
			
			for (int i = 0; i < FOOD_TYPE_COUNT; i++) {
				Food food = getFood(i);
				
				makeFoodThreads.get(orderNumber).put(food, new LinkedList<Thread>());
				
				for (int j = 0; j < foodCount[i]; j++) {

					// Find appropriate machine
					Machine machine = machines.get(food);
					
					// Upon submitted request to food machine:
					Simulation.logEvent(SimulationEvent.cookStartedFood(this, food, orderNumber));
					
					Ratsies.singleton.cookStartedFood(this, orderNumber);
					
					Thread thread = machine.makeFood(food);
					makeFoodThreads.get(orderNumber).get(food).add(thread);
				}
			}
			
			
			//Join all makeFoodThreads.
			for (Food food : makeFoodThreads.get(orderNumber).keySet()) {
				for (Thread thread : makeFoodThreads.get(orderNumber).get(food)) {
					thread.join();
					Simulation.logEvent(SimulationEvent.cookFinishedFood(this, food, orderNumber));
				}
			}
			
			// Upon completing an order:
			Simulation.logEvent(SimulationEvent.cookCompletedOrder(this, orderNumber));
			
			Ratsies.singleton.cookCompletedOrder(this, orderNumber);
		}
	}

	/**
	 * 
	 * @param foodNumber
	 * @return Food type associated with the food number.
	 */
	private Food getFood(int foodNumber) {
		switch (foodNumber) {
		case WING: return FoodType.wings;
		case PIZZA: return FoodType.pizza;
		case SUB: return FoodType.sub;
		case SODA: return FoodType.soda;
		default: throw new UnsupportedOperationException("Unknown food count: " + foodNumber);
		}
	}

	/**
	 * 
	 * @return Cook's name.
	 */
	public String getName() {
		return name;
	}
}
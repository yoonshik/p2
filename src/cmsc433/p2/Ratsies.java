package cmsc433.p2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Ratsies {
	public static Ratsies singleton;

	private int numCustomers;
	private int numCooks;
	private int numTables;
	private int machineCapacity;
	private boolean randomOrders;
	private int numFinished;

	public List<Customer> tables;

	private HashMap<Food, Machine> machines;

	public HashMap<Integer, List<Food>> ordersByOrderNumber;
	public HashMap<Integer, Object> orderLocks;

	public LinkedHashSet<Integer> ordersNew;
	public LinkedHashSet<Integer> ordersInProgress;
	public LinkedHashSet<Integer> ordersFinished;

	private Thread[] cookThreads;
	private Thread[] customerThreads;

	private Object numFinishedLock;

	/**
	 * 
	 * @param numCustomers
	 * @param numCooks
	 * @param numTables
	 * @param machineCapacity
	 * @param randomOrders
	 */
	public Ratsies(int numCustomers, int numCooks,
			int numTables, 
			int machineCapacity,
			boolean randomOrders) {
		if (singleton==null) {
			singleton = this;
		} else {
			throw new UnsupportedOperationException();
		}

		this.numCustomers = numCustomers;
		this.numCooks = numCooks;
		this.numTables = numTables;
		this.machineCapacity = machineCapacity;
		this.randomOrders = randomOrders;
		this.numFinished = 0;
		this.numFinishedLock = new Object();

		// Set things up you might need
		tables = new ArrayList<Customer>(numTables);
		machines = new HashMap<Food, Machine>(this.machineCapacity);

		ordersByOrderNumber = new HashMap<Integer, List<Food>>();
		orderLocks = new HashMap<Integer, Object>();

		ordersNew = new LinkedHashSet<Integer>();
		ordersInProgress = new LinkedHashSet<Integer>();
		ordersFinished = new LinkedHashSet<Integer>();

		machines.put(FoodType.wings, new Machine(Machine.MachineType.fryer, FoodType.wings, machineCapacity));
		machines.put(FoodType.pizza, new Machine(Machine.MachineType.oven, FoodType.pizza, machineCapacity));
		machines.put(FoodType.sub, new Machine(Machine.MachineType.grillPress, FoodType.sub, machineCapacity));
		machines.put(FoodType.soda, new Machine(Machine.MachineType.fountain, FoodType.soda, machineCapacity));
	}

	/**
	 * Run a Ratsies simulation.
	 * 
	 */
	public boolean runSimulation() {
		// Let cooks in
		cookThreads = new Thread[numCooks];
		for (int i = 0; i < cookThreads.length; i++) {
			cookThreads[i] = new Thread(new Cook("Cook "+i, machines));
		}

		// Build the customers.
		customerThreads = new Thread[numCustomers];
		LinkedList<Food> order;
		if (!randomOrders) {
			order = new LinkedList<Food>();
			order.add(FoodType.wings);
			order.add(FoodType.pizza);
			order.add(FoodType.sub);
			order.add(FoodType.soda);
			for(int i = 0; i < customerThreads.length; i++) {
				customerThreads[i] = new Thread(
						new Customer("Customer " + (i+1), order)
						);
			}
		}
		else {
			for(int i = 0; i < customerThreads.length; i++) {
				Random rnd = new Random();
				int wingsCount = rnd.nextInt(4);
				int pizzaCount = rnd.nextInt(4);
				int subCount = rnd.nextInt(4);
				int sodaCount = rnd.nextInt(4);
				order = new LinkedList<Food>();
				for (int b = 0; b < wingsCount; b++) {
					order.add(FoodType.wings);
				}
				for (int f = 0; f < pizzaCount; f++) {
					order.add(FoodType.pizza);
				}
				for (int f = 0; f < subCount; f++) {
					order.add(FoodType.sub);
				}
				for (int c = 0; c < sodaCount; c++) {
					order.add(FoodType.soda);
				}
				customerThreads[i] = new Thread(
						new Customer("Customer " + (i+1), order)
						);
			}
		}

		// Start cook threads.
		for (int i = 0; i < cookThreads.length; i++) {
			cookThreads[i].start();
		}


		// Now "let the customers know the shop is open" by
		//    starting them running in their own thread.
		for(int i = 0; i < customerThreads.length; i++) {
			customerThreads[i].start();
			//NOTE: Starting the customer does NOT mean they get to go
			//      right into the shop.  There has to be a table for
			//      them.  The Customer class' run method has many jobs
			//      to do - one of these is waiting for an available
			//      table...
		}



		try {
			// Wait for customers to finish
			for (int i = 0; i < customerThreads.length; i++) {
				customerThreads[i].join();
			}

			while (!allOrdersFinished()) {
				synchronized(numFinishedLock){
					try {
						numFinishedLock.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}


			// Then send cooks home by interrupting the theads.
			for(int i = 0; i < cookThreads.length; i++)
				cookThreads[i].interrupt();
			
			// Wait for the cooks to finish.
			for(int i = 0; i < cookThreads.length; i++)
				cookThreads[i].join();

		}
		catch(InterruptedException e) {
			System.out.println("Simulation thread interrupted.");
		}
		
		// Shut down machines
		Simulation.logEvent(SimulationEvent.machineEnding(machines.remove(FoodType.wings)));
		Simulation.logEvent(SimulationEvent.machineEnding(machines.remove(FoodType.pizza)));
		Simulation.logEvent(SimulationEvent.machineEnding(machines.remove(FoodType.sub)));
		Simulation.logEvent(SimulationEvent.machineEnding(machines.remove(FoodType.soda)));	
		return true;
	}

	/**
	 * @param customer
	 * @return true if customer is successfully added; false otherwise.
	 * @throws 
	 */
	public boolean enterRatsies(Customer customer) {
		if (customer == null || tables == null) {
			return false;
		}
		synchronized(tables) {
			while (tables.size() >= numTables) {
				try {
					tables.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			tables.add(customer);
			return true;
		}
	}


	/**
	 * @param customer
	 * @return true if customer successfully leaves Ratsies, false otherwise. 
	 */
	public boolean leaveRatsies(Customer customer) {
		if (customer == null || tables == null || !tables.contains(customer)) {
			return false;
		}
		synchronized(tables) {
			tables.remove(customer);
			tables.notify();
			tables.notify();
		}
		return true;
	}


	/**
	 * @param customer
	 * @param order
	 * @param orderNumber
	 * @return Submits a new order from a customer. Return true if order is successfully submitted, false otherwise.
	 */
	public boolean submitOrder(Customer customer, List<Food> order, int orderNumber) {

		if (customer == null || order == null || ordersNew == null || ordersByOrderNumber == null) {	
			return false;
		}

		synchronized(ordersByOrderNumber) {
			ordersByOrderNumber.put(orderNumber, order);
		}

		synchronized(orderLocks) {
			orderLocks.put(orderNumber, new Object());
		}

		synchronized(ordersNew) {
			ordersNew.add(orderNumber);
			synchronized(this) {
				this.notify();
				this.notify();
			}
			ordersNew.notifyAll();
			return true;
		}
	}

	/**
	 * @param orderNumber
	 * @return the lock associated with the given orderNumber.
	 */
	public Object getOrderLock(int orderNumber) {
		synchronized(orderLocks) {
			return orderLocks.get(orderNumber);
		}
	}

	/**
	 * Add orderNumber to ordersInProgress.
	 * @param cook
	 * @param orderNumber
	 */
	public void cookStartedFood(Cook cook, Integer orderNumber) {
		synchronized(getOrderLock(orderNumber)) {
			synchronized(ordersInProgress) {
				ordersInProgress.add(orderNumber);
				getOrderLock(orderNumber).notify();
				getOrderLock(orderNumber).notify();
			}
		}
	}

	/**
	 * Move orderNumber from ordersInProgress to ordersFinished.
	 * @param cook
	 * @param orderNumber
	 * 
	 */
	public void cookCompletedOrder(Cook cook, int orderNumber) {
		synchronized(Ratsies.singleton.getOrderLock(orderNumber)) {
			synchronized(ordersInProgress) {
				ordersInProgress.remove((Object)orderNumber);

				synchronized(ordersFinished) {
					ordersFinished.add(orderNumber);
				}

				synchronized(numFinishedLock) {
					numFinished++;
				}			
			}
			// Once all machines have produced the desired food,
			// notify the Customer since the order is complete.	
			getOrderLock(orderNumber).notifyAll();
		}
	}

	/**
	 * 
	 * @return Return a new order that has yet not been started.
	 */
	public Integer getNextOrder() {
		int orderNumber = -1;
		synchronized(ordersNew) {
			while (ordersNew.isEmpty() && !allOrdersFinished()) {
				if (allOrdersFinished()) {
					return null;
				}
				try {
					ordersNew.wait();
				} catch (InterruptedException e) {

				}
				if (allOrdersFinished()) {
					return null;
				}
			}

			Iterator<Integer> it = ordersNew.iterator();

			while (it.hasNext()) {
				orderNumber = it.next();
				break;
			}

			ordersNew.remove(orderNumber);
			return orderNumber;
		}
	}

	/**
	 * Check if there are orders available.
	 * @return True is there are orders to work on; False otherwise.
	 */
	public boolean newOrderAvailable() {
		synchronized(ordersNew) {
			return ordersNew.size() > 0;
		}
	}

	/**
	 * Return the order associated with the orderNumber.
	 * @param orderNumber
	 * @return
	 */
	public List<Food> getOrder(int orderNumber) {
		synchronized(ordersByOrderNumber) {
			return ordersByOrderNumber.get(orderNumber);
		}
	}

	/**
	 * 
	 * @return True if all orders have been completed; False otherwise.
	 */
	public boolean allOrdersFinished() {
		synchronized(numFinishedLock) {
			return numFinished == numCustomers;
		}
	}

	/**
	 * 
	 * @param orderNumber
	 * @return False if the order has been finished; True otherwise.
	 */
	public boolean orderInProgress(int orderNumber) {
		synchronized(getOrderLock(orderNumber)) {
			synchronized(ordersFinished) {
				return !ordersFinished.contains(orderNumber);
			}
		}
	}
}

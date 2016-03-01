package cmsc433.p2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class Ratsies {
	public static Ratsies singleton;
	
	private int numCustomers;
	private int numCooks;
	private int numTables;
	private int machineCapacity;
	private boolean randomOrders;

	public List<Customer> tables;
	public HashMap<Food, Machine> machines;

	public HashMap<List<Food>, Integer> orderNumbers;

	public Queue<List<Food>> ordersNew;
	public HashSet<List<Food>> ordersInProgress;
	public HashSet<List<Food>> ordersFinished;

	private Thread[] cookThreads;
	private Thread[] customerThreads;

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


	}

	public void runSimulation() {
		// Set things up you might need
		tables = new ArrayList<Customer>(numTables);
		machines = new HashMap<Food, Machine>(machineCapacity);

		orderNumbers = new HashMap<List<Food>, Integer>();

		ordersNew = new PriorityQueue<List<Food>>();
		ordersInProgress = new HashSet<List<Food>>();
		ordersFinished = new HashSet<List<Food>>();

		// Start up machines
		machines.put(FoodType.wings, new Machine(Machine.MachineType.fryer, FoodType.wings, machineCapacity));
		machines.put(FoodType.pizza, new Machine(Machine.MachineType.oven, FoodType.pizza, machineCapacity));
		machines.put(FoodType.sub, new Machine(Machine.MachineType.grillPress, FoodType.sub, machineCapacity));
		machines.put(FoodType.soda, new Machine(Machine.MachineType.fountain, FoodType.soda, machineCapacity));

		// Let cooks in
		cookThreads = new Thread[numCooks];
		for (int i = 0; i < numCooks; i++) {
			cookThreads[i] = new Thread(new Cook("Cook" + i));
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
			//   -- you need to add some code here...






			// Then send cooks home...
			// The easiest way to do this might be the following, where
			// we interrupt their threads.  There are other approaches
			// though, so you can change this if you want to.
			for(int i = 0; i < cookThreads.length; i++)
				cookThreads[i].interrupt();
			for(int i = 0; i < cookThreads.length; i++)
				cookThreads[i].join();

		}
		catch(InterruptedException e) {
			System.out.println("Simulation thread interrupted.");
		}

		// Shut down machines
		machines.remove(FoodType.wings);
		machines.remove(FoodType.pizza);
		machines.remove(FoodType.sub);
		machines.remove(FoodType.soda);

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
		synchronized (tables) {
			while (tables.size() == numTables) {
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
	 * @param order
	 * @param orderNumber
	 * @return true if order is successfully submitted, false otherwise
	 */
	public boolean submitOrder(Customer customer, List<Food> order, int orderNumber) {
		if (customer == null || order == null || ordersNew == null || orderNumbers == null) {
			return false;
		}
		synchronized(orderNumbers) {
			orderNumbers.put(order, orderNumber);
		}
		synchronized(ordersNew) {
			ordersNew.add(order);
			ordersNew.notify();
		}

		return true;
	}

	/**
	 * @param customer
	 * @return true if customer successfully leaves Ratsies, false otherwise. 
	 */
	public boolean leaveRatsies(Customer customer) {
		if (customer == null || tables == null || !tables.contains(customer)) {
			return false;
		}
		synchronized (tables) {
			tables.remove(customer);
			tables.notify();
		}
		return true;
	}

	/**
	 * 
	 * @return next order in the queue.
	 */
	public List<Food> getNextOrder() {
		List<Food> order;
		synchronized(ordersNew) {
			while (ordersNew.isEmpty()) {
				try {
					ordersNew.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			order = ordersNew.remove();
			synchronized(ordersInProgress) {
				ordersInProgress.add(order);
			}
		}
		return order;
	}

	public int getOrderNumber(List<Food> order) {
		synchronized(orderNumbers) {
			return Ratsies.singleton.orderNumbers.get(order);
		}
	}


}

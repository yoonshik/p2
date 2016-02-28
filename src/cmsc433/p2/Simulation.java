package cmsc433.p2;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

/**
 * Simulation is the main class used to run the simulation.  You may
 * add any fields (static or instance) or any methods you wish.
 */
public class Simulation {
	// List to track simulation events during simulation
	
	private static final int NUM_MACHINES = 4;

	public static List<SimulationEvent> events;
	public static HashMap<String, Customer> customers;
	public static List<Cook> cooks;
	public static HashMap<Food, Machine> machines;
	
	public static HashMap<List<Food>, Integer> orderNumbers;
	
	public static Queue<List<Food>> ordersNew;
	public static Queue<List<Food>> ordersInProgress;
	public static Queue<List<Food>> ordersFinished;
	
	

	/**
	 * Used by other classes in the simulation to log events
	 * @param event
	 */
	public static void logEvent(SimulationEvent event) {
		events.add(event);
		System.out.println(event);
	}

	/**
	 * 	Function responsible for performing the simulation. Returns a List of 
	 *  SimulationEvent objects, constructed any way you see fit. This List will
	 *  be validated by a call to Validate.validateSimulation. This method is
	 *  called from Simulation.main(). We should be able to test your code by 
	 *  only calling runSimulation.
	 *  
	 *  Parameters:
	 *	@param numCustomers the number of customers wanting to enter Ratsie's
	 *	@param numCooks the number of cooks in the simulation
	 *	@param numTables the number of tables in Ratsie's (i.e. Ratsie's capacity)
	 *	@param machineCapacity the capacity of all machines in Ratsie's
	 *  @param randomOrders a flag say whether or not to give each customer a random order
	 *
	 */
	public static List<SimulationEvent> runSimulation(
			int numCustomers, int numCooks,
			int numTables, 
			int machineCapacity,
			boolean randomOrders
			) {

		//This method's signature MUST NOT CHANGE.  
		
		//We are providing this events list object for you.  
		//  It is the ONLY PLACE where a concurrent collection object is 
		//  allowed to be used.
		events = Collections.synchronizedList(new ArrayList<SimulationEvent>());




		// Start the simulation
		logEvent(SimulationEvent.startSimulation(numCustomers,
				numCooks,
				numTables,
				machineCapacity));



		// Set things up you might need
		customers = new HashMap<String, Customer>(numCustomers);
		cooks = new ArrayList<Cook>(numCooks);
		machines = new HashMap<Food, Machine>(machineCapacity);
		
		orderNumbers = new HashMap<List<Food>, Integer>();
		
		ordersNew = new PriorityQueue<List<Food>>();
		ordersInProgress = new PriorityQueue<List<Food>>();
		ordersFinished = new PriorityQueue<List<Food>>();
		

		// Start up machines
		machines.put(FoodType.wings, new Machine(Machine.MachineType.fryer, FoodType.wings, machineCapacity));
		machines.put(FoodType.pizza, new Machine(Machine.MachineType.oven, FoodType.pizza, machineCapacity));
		machines.put(FoodType.sub, new Machine(Machine.MachineType.grillPress, FoodType.sub, machineCapacity));
		machines.put(FoodType.soda, new Machine(Machine.MachineType.fountain, FoodType.soda, machineCapacity));

		// Let cooks in
		Thread[] cookThreads = new Thread[numCooks];
		for (int i = 0; i < numCooks; i++) {
			cookThreads[i] = new Thread(new Cook("Cook" + i));
		}


		// Build the customers.
		Thread[] customerThreads = new Thread[numCustomers];
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

		// Done with simulation		
		logEvent(SimulationEvent.endSimulation());

		return events;
	}

	/**
	 * Entry point for the simulation.
	 *
	 * @param args the command-line arguments for the simulation.  There
	 * should be exactly four arguments: the first is the number of customers,
	 * the second is the number of cooks, the third is the number of tables
	 * in Ratsie's, and the fourth is the number of items each machine
	 * can make at the same time.  
	 */
	public static void main(String args[]) throws InterruptedException {
		// Parameters to the simulation
		/*
		if (args.length != 4) {
			System.err.println("usage: java Simulation <#customers> <#cooks> <#tables> <capacity> <randomorders");
			System.exit(1);
		}
		int numCustomers = new Integer(args[0]).intValue();
		int numCooks = new Integer(args[1]).intValue();
		int numTables = new Integer(args[2]).intValue();
		int machineCapacity = new Integer(args[3]).intValue();
		boolean randomOrders = new Boolean(args[4]);
		 */
		int numCustomers = 10;
		int numCooks =1;
		int numTables = 5;
		int machineCapacity = 4;
		boolean randomOrders = false;


		// Run the simulation and then 
		//   feed the result into the method to validate simulation.
		System.out.println("Did it work? " + 
				Validate.validateSimulation(
						runSimulation(
								numCustomers, numCooks, 
								numTables, machineCapacity,
								randomOrders
								)
						)
				);
	}

}




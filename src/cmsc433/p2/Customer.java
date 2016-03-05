package cmsc433.p2;

import java.util.List;


/**
 * Customers are simulation actors that have two fields: a name, and a list
 * of Food items that constitute the Customer's order.  When running, an
 * customer attempts to enter the Ratsie's (only successful if the
 * Ratsie's has a free table), place its order, and then leave the
 * Ratsie's when the order is complete.
 */
public class Customer implements Runnable {
	private final String name;
	private final List<Food> order;
	private final Integer orderNumber;
	
	private static int runningCounter = 0;
	private static Object runningCounterLock = new Object();

	/**
	 * You can feel free modify this constructor.  It must take at
	 * least the name and order but may take other parameters if you
	 * would find adding them useful.
	 */
	public Customer(String name, List<Food> order) {
		this.name = name;
		this.order = order;
		synchronized(runningCounterLock) {
			this.orderNumber = ++runningCounter;
		}
	}

	/**
	 * @return Customer's name.
	 */
	public String toString() {
		return name;
	}

	/** 
	 * This method defines what an Customer does: The customer attempts to
	 * enter the Ratsie's (only successful when the Ratsie's has a
	 * free table), place its order, and then leave the Ratsie's
	 * when the order is complete.
	 */
	public void run() {
		
		// Before entering Ratsie's
		Simulation.logEvent(SimulationEvent.customerStarting(this));
		
		// If Ratsie's has a free table, enter Ratsie's
		Ratsies.singleton.enterRatsies(this);
		
		//After entering Ratsie’s:
		Simulation.logEvent(SimulationEvent.customerEnteredRatsies(this));
		
		// Immediately before placing order:
		Simulation.logEvent(SimulationEvent.customerPlacedOrder(this, order, orderNumber));
		
		// Place its order
		Ratsies.singleton.submitOrder(this, order, orderNumber);
		synchronized(Ratsies.singleton.getOrderLock(orderNumber)) {
			while (Ratsies.singleton.orderInProgress(orderNumber)){
				try {
					Ratsies.singleton.getOrderLock(orderNumber).wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		// After receiving order:
		Simulation.logEvent(SimulationEvent.customerReceivedOrder(this, order, orderNumber));
		
		// Just before leaving the Ratsie’s:
		Simulation.logEvent(SimulationEvent.customerLeavingRatsies(this));
		
		// When the order is complete, leave Ratsie's
		Ratsies.singleton.leaveRatsies(this);
	}

	/**
	 * 
	 * @return Customer's name.
	 */
	public String getName() {
		return name;
	}
}
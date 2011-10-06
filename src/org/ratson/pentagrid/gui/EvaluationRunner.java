package org.ratson.pentagrid.gui;

import java.util.List;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.TotalisticRule;

/**Thread, that runs in background and continuously evaluates the field, when requested*/
public class EvaluationRunner extends  Thread {

	private final Field cells;
	private int delayMs = 300;
	private int limitPopulation = 50000; //stop evaluation, when reached this limit
	volatile boolean stopRequested = false;
	private NotificationReceiver receiver=null;
	private TotalisticRule rule;
	
	public EvaluationRunner( Field cells, TotalisticRule rul, NotificationReceiver r ) {
		super("Evaluator");
		this.cells = cells;
		rule = rul;
		receiver = r;
	}
	
	@Override
	public void run() {
		int count = 0;
		while( ! stopRequested ){
			try{
				cells.evaluate(rule);
				receiver.notifyUpdate( cells );
				count += 1;
				Thread.sleep( delayMs );
			}catch(OutOfMemoryError err){
				System.out.println( "Memory exceeded on population " + cells.population() + ", evaluation stopped." );
				stopRequested = true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Evaluation stopped after "+count+" steps on population "+cells.population());
	}

	public void requestStop() {
		stopRequested = true;
	}

}

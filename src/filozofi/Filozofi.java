package filozofi;



import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import os.simulation.Application;
import os.simulation.Container;
import os.simulation.Item;
import os.simulation.Operation;
import os.simulation.Thread;

/*
 * Pet filozofa sede za okruglim stolom na kojem se nalazi cinija spageta.
 * Izmedju svaka dva filozofa se nalazi po jedna viljuska. U toku svog zivota
 * svaki filozof naizmenicno razmislja i jede. Filozof moze da jede samo ako
 * ima dve viljuske onu sa svoje leve strane i onu sa svoje desne strane. Svaku
 * viljusku u nekom trenutku moze da koristi samo jedan filozof. Kada filozof
 * zavrsi jedenje, vraca viljuske koje je koristio na sto i time one postaju
 * raspolozive za ostale filozofe.
 * 
 * Filozof moze da uzme svaku viljusku pored njega cim se oslobodi ali ne sme
 * da jede pre nego sto drzi obe viljuske. Pretpostaviti da je kolicina spageta
 * neogranicena i da se filozofi nikada nece zasititi.
 * 
 * Implementirati sinhronizaciju filozofa tako da niko od njih ne gladuje.
 */
public class Filozofi extends Application {
	protected enum Stanje {
		MISLI, GLADAN, JEDE;
	}
	
	protected class StoSem{
		protected Semaphore mutex;
		protected Semaphore[] sem;
		
		private  int brojFilozofa;
		private Stanje[] filozofi;
		public StoSem(int brojF) {
			this.brojFilozofa = brojF;
			this.mutex = new Semaphore(1);
			this.filozofi = new Stanje[brojF];
			this.sem = new Semaphore[brojF];
			for(int i = 0;i<brojF;i++) {
				this.filozofi[i] =Stanje.MISLI;
				this.sem[i] =new Semaphore(0);
			}
			
			
		}
	}
	
	protected class StoLock{
		protected Lock brava = new ReentrantLock();
		protected Condition imaViljusaka = brava.newCondition();
		
		private int brojFilozofa;
		private boolean[] viljuske;
		
		public StoLock(int broj ) {
			
			this.brojFilozofa = broj;
			this.viljuske = new boolean[broj];
			// TODO Auto-generated constructor stub
		}
		
		private int leva(int id) {
			return (id + 1) % brojFilozofa;
		}
		private int desna(int id) {
			return id;
		}
		public void uzmiViljuske(int id) throws InterruptedException {
			brava.lock();
			try {
				while(viljuske[leva(id)] || viljuske[desna(id)]) {
					imaViljusaka.await();
				}
				viljuske[leva(id)] = true;
				viljuske[id] = true;
			} finally {
				// TODO: handle finally clause
				brava.unlock();
			}
		}
		public void vratiViljuske(int id) {
			brava.lock();
			try {
				viljuske[leva(id)] = false;
				viljuske[desna(id)]=false;
				imaViljusaka.signalAll();
				
			} finally {
				// TODO: handle finally clause
				brava.unlock();
			}
		}
	}

	public StoLock sto = new StoLock(5);
	protected class Filozof extends Thread {

		protected final int id = getID();

		@Override
		protected void step() {
			misli();
			// Sinhronizacija
			try {
				sto.uzmiViljuske(id);
				try {
					jede();
				} finally {
					// TODO: handle finally clause
					sto.vratiViljuske(id);
				}
			} catch (Exception e) {
				// TODO: handle exception
				stopGracefully();
			}
			
			// Sinhronizacija
		}
	}

	// ------------------- //
	//    Sistemski deo    //
	// ------------------- //
	// Ne dirati kod ispod //
	// ------------------- //

	protected final String[] IMENA          = { "Аристотел", "Диоген", "Еуклид", "Питагора", "Талес" };
	protected final Container main          = circle("Филозофи").color(WARM_GRAY);
	protected final Operation filozof       = init().name(IMENA).color(ROSE);
	protected final Operation razmisljanje  = duration("3±2").color(AZURE).text("Мисли").colorAfter(ROSE).textAfter("Гладан");
	protected final Operation jedenje       = duration("3±1").color(CHARTREUSE).text("Једе").colorAfter(CHARTREUSE).textAfter("Јео");

	protected void misli() {
		razmisljanje.performUninterruptibly();
	}

	protected void jede() {
		jedenje.performUninterruptibly();
	}

	protected class Viljuske extends Item {

		private Filozof[] filozofi;
		private int max;

		public Viljuske(int max, Filozof... filozofi) {
			setName("Виљушке");
			this.max = max;
			this.filozofi = filozofi;
			for (Filozof filozof : filozofi) {
				filozof.addPropertyChangeListener(PROPERTY_TEXT, e -> azuriraj());
			}
			azuriraj();
		}

		private void azuriraj() {
			int broj = 0;
			for (Filozof filozof : filozofi) {
				if (jedenje.getTextBefore().equals(filozof.getText())) {
					broj++;
				}
			}
			int slobodno = max - broj;
			if (slobodno > 0) {
				setColor(GREEN);
			} else if (slobodno == 0) {
				setColor(YELLOW);
			} else {
				setColor(RED);
			}
			setText("Слободно: " + slobodno);
		}
	}

	@Override
	protected void initialize() {
		Filozof[] filozofi = new Filozof[IMENA.length];
		for (int i = 0; i < filozofi.length; i++) {
			filozofi[i] = new Filozof();
		}
		Viljuske[] viljuske = new Viljuske[IMENA.length];
		for (int i = 0; i < viljuske.length; i++) {
			viljuske[i] = new Viljuske(1, filozofi[i], filozofi[(i + 1) % filozofi.length]);
		}
		for (int i = 0; i < filozofi.length; i++) {
			filozofi[i].setContainer(main);
			viljuske[i].setContainer(main);
		}
		for (int i = 0; i < filozofi.length; i++) {
			filozofi[i].start();
		}
	}

	public static void main(String[] arguments) {
		launch("Филозофи");
	}
}
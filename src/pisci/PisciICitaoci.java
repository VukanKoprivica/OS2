package pisci;



import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import os.simulation.Application;
import os.simulation.AutoCreate;
import os.simulation.Container;
import os.simulation.Item;
import os.simulation.Operation;
import os.simulation.Thread;

/*
 * Data je zajednicka baza podataka. Vise procesa zeli da istovremeno pristupa
 * ovoj bazi kako bi citali ili upisivali podatke u nju. Kako bi korektno
 * realizovali ove istovremene pristupe bez rizika da dodje do problema,
 * procesi moraju da postuju sledeca pravila: istovremena citanja su dozvoljena
 * posto citaoci ne smetaju jedan drugom, istovremeno citanje i pisanje nije
 * dozvoljeno jer se moze desiti da citalac procita pogresne podatke (do pola
 * upisane), istovremena pisanja takodje nisu dozvoljena jer mogu prouzrokovati
 * ostecenje podataka.
 * 
 * Implementirati sinhronizaciju procesa pisaca i procesa citalaca tako da se
 * postuju opisana pravila.
 */
public class PisciICitaoci extends Application {
	
	protected class BazaSem{
		protected Semaphore mutex  = new Semaphore(1);
		protected Semaphore baza = new Semaphore(1);
		
		private int brCitalaca;
		
		public void zapocniPisanje() throws InterruptedException {
			baza.acquire();
		}
		public void zapocniCitanje() throws InterruptedException {
			mutex.acquire();
			try {
				brCitalaca++;
				if(brCitalaca == 1) {
					try {
						baza.acquire();
						
					} catch (Exception e) {
						// TODO: handle exception
						brCitalaca--;
						throw e;
					}
				}
				
			} finally {
				// TODO: handle finally clause
				mutex.release();
			}
		}
		public void zavrsiPisanje() {
			baza.release();
		}
		public void zavrsiCitanje() throws InterruptedException {
			mutex.acquire();
			try {
				brCitalaca--;
				if(brCitalaca==0) {
					try {
						baza.release();
					} catch (Exception e) {
						brCitalaca++;
						throw e;
						// TODO: handle exception
					}
				}
				
			} finally {
				// TODO: handle finally clause
				mutex.release();
			}
		}
		
		
	}
	
	
	protected class BazaLock{
		protected Lock brava = new ReentrantLock();
		protected Condition pisci = brava.newCondition();
		protected Condition citaoci = brava.newCondition();
		
		private int brPisaca;
		private int brCit;
		
		public void zapocniPisanje() throws InterruptedException {
			brava.lock();
			try {
				while(brPisaca + brCit > 0) {
					pisci.await();
				}
				brPisaca++;
				
			} finally {
				// TODO: handle finally clause
				brava.unlock();
			}
		}
		
		public void zapocniCitanje() throws InterruptedException {
			brava.lock();
			try {
				while(brPisaca >0) {
					citaoci.await();
				}
				brCit++;
			} finally {
				// TODO: handle finally clause
				brava.unlock();
			}
		}
		
		public void zavrsiPisanje() {
			brava.lock();
			try {
				brPisaca--;
				pisci.signal();
				citaoci.signalAll();
				
			} finally {
				// TODO: handle finally clause
				brava.unlock();
			}
		}
		public void zavrsiCitanje() {
			brava.lock();
			try {
				brCit--;
				if(brCit ==0) {
					pisci.signalAll();
				}
			} finally  {
				// TODO: handle exception
				brava.unlock();
			}
		}
	}
	
	protected BazaSem baza = new BazaSem();
	@AutoCreate(2)
	protected class Pisac extends Thread {

		@Override
		protected void step() {
			radiNestoDrugo();
			try {
				baza.zapocniPisanje();
				try {
					pise();
				} finally {
					// TODO: handle finally clause
					baza.zavrsiPisanje();
				}
				
			} catch (Exception e) {
				// TODO: handle exception
				stopGracefully();
			}
			
			// Sinhronizacija
		}
	}

	@AutoCreate(5)
	protected class Citalac extends Thread {

		@Override
		protected void step() {
			radiNestoDrugo();
			try {
				baza.zapocniCitanje();
				try {
					cita();
				} finally {
					baza.zavrsiCitanje();
					// TODO: handle finally clause
				}
				
			} catch (Exception e) {
				// TODO: handle exception
				stopGracefully();
			}
			
		
		}
	}

	// ------------------- //
	//    Sistemski deo    //
	// ------------------- //
	// Ne dirati kod ispod //
	// ------------------- //

	protected final Container pisci   = box("??????????").color(MAROON);
	protected final Container citaoci = box("??????????????").color(NAVY);
	protected final Container resurs  = box("????????").color(ROYAL);
	protected final Container main    = column(row(pisci, citaoci), resurs);
	protected final Operation pisac   = init().name("?????????? %d").color(ROSE).container(pisci);
	protected final Operation citalac = init().name("?????????????? %d").color(AZURE).container(citaoci);
	protected final Operation pisanje = duration("5??2").text("????????").container(resurs).textAfter("??????????????").update(this::azuriraj);;
	protected final Operation citanje = duration("5??2").text("????????").container(resurs).textAfter("??????????????").update(this::azuriraj);;
	protected final Operation posao   = duration("7??2").text("????????").textAfter("????????");

	protected void pise() {
		pisanje.performUninterruptibly();
	}

	protected void cita() {
		citanje.performUninterruptibly();
	}

	protected void radiNestoDrugo() {
		posao.performUninterruptibly();
	}

	protected void azuriraj(Item item) {
		long brP = resurs.stream(Pisac.class).count();
		long brC = resurs.stream(Citalac.class).count();
		resurs.setText(String.format("%d : %d", brP, brC));
		if (brP == 0 && brC == 0) {
			resurs.setColor(NEUTRAL_GRAY);
		} else if (brP > 0 && brC == 0) {
			resurs.setColor(MAROON);
		} else if (brP == 0 && brC > 0) {
			resurs.setColor(NAVY);
		} else {
			resurs.setColor(ROYAL);
		}
	}

	@Override
	protected void initialize() {
		azuriraj(null);
	}

	public static void main(String[] arguments) {
		launch("?????????? ?? ??????????????");
	}
}
package klkMilica;

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
 * Data je simulacija izvodjenja vojnih vezbi. U toku ovih vezbi tenkisti i
 * artiljetrci vezbaju gadjanje nepokretnih meta na poligonu. U toku cele
 * simulacije oni naizmenicno malo odmaraju, pa malo vezbaju, pa ponovo malo
 * odmaraju, pa ponovo vezbaju, itd. 
 * 
 * Ako neko prekine vojnika u odmaranju, on odmah odlazi kod narednika i time
 * napusta simulaciju. Ako neko prekine vojnika dok ceka da vezba, on preskace
 * vezbanje, vraca se na odmaranje i potom pokusava da vezba kao da ga niko
 * nije ni prekidao. Ako neko prekine vojnika u toku vezbanja, on se na to ne
 * obazire i dalje nastavlja normalno svoju rutinu, kao da ga niko nikada nije
 * ni prekinuo.
 * 
 * Sinhronizovati niti pomocu semafora tako da postuju dole izlozene uslove i
 * korektno obradjuju prekide. Takodje, ne blokirati niti ako po pomenutim
 * uslovima mogu da nastave svoj rad.
 * 
 * A) Deset poena
 * 
 * Vojnici treniraju kombinovane borbene akcije sto podrazumeva da jedan
 * tenkista i jedan artiljerac vezbaju u paru. Ne dozvoliti tenkisti da stupi
 * na poligon bez da i jedan artiljerac to ucini, i ne dozvoliti artiljercu da
 * posne vezbu ako to ne ucini i jedan tenkista.
 * 
 * B) Deset poena
 * 
 * Kako je poligon za vezbu dosta mali, na njemu ne moze istovremeno vezbati
 * vise od 7 vojnika.
 * 
 * C) Pet poena
 * 
 * Sinhronizovati procese tako da se istovrmeno postuju uslovi izlozeni i pod A)
 * i pod B).
 * 
 */
public class VojneVezbe extends Application {
	
	
	private class A {
		
		private Lock brava = new ReentrantLock();
		private Condition tenkisti = brava.newCondition();
		private Condition artiljerac = brava.newCondition();
		
		private int brT;
		private int brA;
		
		
		
		public void ulazT() {
			brava.lock();
			try {
				brT++;
				
				while(brA ==0 ) {
					artiljerac.awaitUninterruptibly();
				}
				brA--;
				
				tenkisti.signal();
			} finally {
				brava.unlock();
				
			}
		}
		public void ulazA() {
			brava.lock();
			try {
				brA++;
						
				while(brT ==0 ) {
					tenkisti.awaitUninterruptibly();
				}
				brT--;
				artiljerac.signal();
			} finally {
				brava.unlock();
			}
		}
		
		
	}
	private class B{
		private Lock brava = new ReentrantLock();
		private Condition uslov = brava.newCondition();
		
		private int brV;
		
		public void ulaz() {
			brava.lock();
			try {
				while(brV >=7) {
					uslov.awaitUninterruptibly();
				}
				brV++;
			} finally {
				brava.unlock();
			}
		}
		public void izlaz() {
			brava.lock();
			try {
				brV--;
				uslov.signal();
			} finally {
				brava.unlock();
			}
		}
	}
	
	private class AB{
		private A a = new A();
		private B b = new B();
		
		public void ulaz(boolean tenk) {
			if(tenk) {
				b.ulaz();
				a.ulazT();
			}
			else {
				b.ulaz();
				a.ulazA();
			}
		}
		
		public void izlaz() {
			b.izlaz();
		}
	}

	private AB poligonA = new AB();
	@AutoCreate(12)
	protected class Tenkista extends Thread {

		@Override
		protected void step() {
			odmara();
			poligonA.ulaz(true);
			vezba();
			poligonA.izlaz();
			
		}
	}

	@AutoCreate(12)
	protected class Artiljerac extends Thread {

		@Override
		protected void step() {
			odmara();
			poligonA.ulaz(false);
			vezba();
			poligonA.izlaz();
			
		}
	}

	// ------------------- //
	//    Sistemski deo    //
	// ------------------- //
	// Ne dirati kod ispod //
	// ------------------- //

	protected final Container barake     = box("Бараке").color(WARM_GRAY);
	protected final Container magacin    = box("Магацин").color(COOL_GRAY);
	protected final Container poligon    = box("Полигон").color(ARMY);
	protected final Container main       = column(row(barake, magacin), poligon);
	protected final Operation tenkista   = init().container(barake).name("Тенк. %d").color(CHARTREUSE);
	protected final Operation artiljerac = init().container(barake).name("Арт. %d").color(AZURE);

	protected final Operation odmaranje = duration("7±2").text("Одмара").textAfter("Чека");
	protected final Operation vezbanje  = duration("5±2").text("Вежба").container(poligon).update(this::azuriraj);

	protected void odmara() {
		try {
			odmaranje.performInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected void vezba() {
		vezbanje.performUninterruptibly();
	}

	protected void azuriraj(Item item) {
		long brT = poligon.stream(Tenkista.class).count();
		long brA = poligon.stream(Artiljerac.class).count();
		long brM = 7 - brT - brA;
		poligon.setText(String.format("%d / %d / %d", brT, brA, brM));
	}

	@Override
	protected void initialize() {
		azuriraj(null);
	}

	public static void main(String[] arguments) {
		launch("Војне вежбе");
	}
}

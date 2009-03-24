package pt209223.communication;

import java.util.*;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;

public class Radio {
	protected final RobotController rc;             // Kontroler robota
	protected final LinkedList<Message> outgoing;   // Wiadomosci wychodzace
	protected final LinkedList<Message> incoming;   // Wiadomosci przychodzace
	protected int intsTotalLength;                  // liczba pozycji w ints[] do wyslania
	protected int locationsTotalLength;             // liczba pozycji w locations[] do wyslania
	protected int stringsTotalLength;               // liczba pozycji w strings[] do wyslania
	protected static final boolean verbose = false; // Czy wypisywac rozne takie informacje

	/* 
	 * Do uzytku wewnetrznego dla Radio 
	 */

	// Podstawowe indeksy
	protected static final int IDX_MAGIC                = 0; // indeks magicznego pola
	protected static final int IDX_CHECKSUM             = 1; // indeks checksumy
	protected static final int IDX_TEAM                 = 2; // indeks numeru druzyny
	protected static final int IDX_SENDER               = 3; // indeks id nadawcy
	protected static final int IDX_SIZE                 = 4; // indeks liczby wiadomosci
	protected static final int IDX_ROUND                = 5; // indeks rundy wysylki wiadomosci
	protected static final int IDX_LOCATION             = 0; // indeks lokalizacji nadawcy

	// Gdzie sie zaczynaja prawdziwe dane
	protected static final int IDX_START_INTS           = 6; // indeks poczatku danych na ints[]
	protected static final int IDX_START_LOCATIONS      = 1; // indeks poczatku danych na locations[]
	protected static final int IDX_START_STRINGS        = 0; // indeks poczatku danych na strings[]

	// Wielkosci danych dla pojedynczej wiadomosci
	protected static final int OFF_INTS_LENGTH          = 0; // ofset dlugosci ints[] dla wiadomosci
	protected static final int OFF_LOCATIONS_LENGTH     = 1; // ofset dlugosci locations[] ...
	protected static final int OFF_STRINGS_LENGTH       = 2; // ofset dlugosci strings[] ...

	// Kolejne przesuniecie w tablicach
	protected static final int OFF_START_INTS           = 3; // ofset poczatku danych na ints[]
	protected static final int OFF_START_LOCATIONS      = 0; // ofset poczatku danych na locations[]
	protected static final int OFF_START_STRINGS        = 0; // ofset poczatku danych na strings[]

	// Wartosc MAGIC i HASH 
	protected static final int MAGIC_VALUE = 0x4FA31EB7; // Magiczna wartosc
	protected static final int HASH_VALUE = 0xCD2AA1E7; // Hasza wartosc

	/* 
	 * Do ogolnego uzytku 
	 */

	// W otrzymanej (w pojedynczej wiadosci, nie zbiorczej)
	public static final int SENDER           = 0; // indeks nadawcy w wiadomosci
	public static final int RCPT             = 1; // indeks odbiorcy w wiadomosci
	public static final int ROUND            = 2; // indeks rundy w wiadomosci
	public static final int TYPE             = 3; // indeks typu wiadomosci

	// Gdzie sie zaczynaja nie standardowe, dodatkowe dane (tylko dla Radio)
	protected static final int START_INTS       = 4; // indeks poczatku reszy danych na ints[]
	protected static final int START_LOCATIONS  = 0; // --//-- na locations[]
	protected static final int START_STRINGS    = 0; // --//-- na strings[]

	// Wiadomosc moze miec konkretnego adresata, ale tez moze byc do kazdego...
	public static final int RCPT_ANY = -1; // Wiadomosc dla kazdego

	// Typy wiadomosci...
	public static final int HELLO       = 0; // Hello, tu jestem
	public static final int FLUX_FOUND  = 1; // Widze Flux!
	public static final int FLUX_TAKEN  = 2; // Flux przechwycony!
	public static final int ENEMIES     = 3; // Pozycje wroga
	public static final int GOTO        = 4; // Idz, bez dyskusyjnie, tam
	public static final int STAIRS      = 5; // Pozycje schodow (Archon<->Worker)
	public static final int ATTACK      = 6; // Gdzie isc atakowac
	//public static final int MAP         = 7; // Informacje o mapie
	// ... ?

	/* 
	 * Teraz definiujemy gdzie sa pewne dodatkowe indeksy, 
	 * dla konretnych typow wiadomosci 
	 */

	// Indeksy dla wiadomosci MSG_TYPE_HELLO: 
	public static final int HELLO_SENDER                  = START_LOCATIONS + 0;
	public static final int HELLO_SENDER_TYPE             = START_STRINGS   + 0;
	public static final int HELLO_SIZE_OF_INTS            = START_INTS      + 0;
	public static final int HELLO_SIZE_OF_LOCATIONS       = START_LOCATIONS + 1;
	public static final int HELLO_SIZE_OF_STRINGS         = START_STRINGS   + 1;

	// Indeksy dla wiadomosci MSG_TYPE_FLUX_FOUND:
	public static final int FLUX_FOUND_SENDER             = START_LOCATIONS + 0;
	public static final int FLUX_FOUND_FOUND              = START_LOCATIONS + 1;
	public static final int FLUX_FOUND_SIZE_OF_INTS       = START_INTS      + 0;
	public static final int FLUX_FOUND_SIZE_OF_LOCATIONS  = START_LOCATIONS + 2;
	public static final int FLUX_FOUND_SIZE_OF_STRINGS    = START_STRINGS   + 0;

	// Indeksy dla wiadomosci MSG_TYPE_FLUX_TAKEN:
	public static final int FLUX_TAKEN_SENDER             = START_LOCATIONS + 0;
	public static final int FLUX_TAKEN_SIZE_OF_INTS       = START_INTS      + 0;
	public static final int FLUX_TAKEN_SIZE_OF_LOCATIONS  = START_LOCATIONS + 1;
	public static final int FLUX_TAKEN_SIZE_OF_STRINGS    = START_STRINGS   + 0;

	// Indeksy dla wiadomosci MSG_TYPE_ENEMIES:
	public static final int ENEMIES_SENDER                = START_LOCATIONS + 0;
	public static final int ENEMIES_SIZE                  = START_INTS      + 0;
	public static final int ENEMIES_L_START               = START_LOCATIONS + 1;
	public static final int ENEMIES_I_START               = START_INTS      + 1;
	public static final int ENEMIES_S_START               = START_STRINGS   + 0;
	public static final int ENEMIES_SIZE_OF_INTS          = START_INTS      + 1;
	public static final int ENEMIES_SIZE_OF_LOCATIONS     = START_LOCATIONS + 1;
	public static final int ENEMIES_SIZE_OF_STRINGS       = START_STRINGS   + 0;

	// Indeksy dla wiadomosci MSG_TYPE_GOTO:
	public static final int GOTO_TARGET                   = START_LOCATIONS + 0;
	public static final int GOTO_SIZE_OF_INTS             = START_INTS      + 0;
	public static final int GOTO_SIZE_OF_LOCATIONS        = START_LOCATIONS + 1;
	public static final int GOTO_SIZE_OF_STRINGS          = START_STRINGS   + 0;

	// Indeksy dla wiadomosci MSG_TYPE_STAIRS:
	public static final int STAIRS_SIZE                   = START_INTS      + 0;
	public static final int STAIRS_START                  = START_LOCATIONS + 0;
	public static final int STAIRS_SIZE_OF_INTS           = START_INTS      + 1;
	public static final int STAIRS_SIZE_OF_LOCATIONS      = START_LOCATIONS + 0;
	public static final int STAIRS_SIZE_OF_STRINGS        = START_STRINGS   + 0;

	// Indeksy dla wiadomosci MSG_TYPE_ATTACK:
	public static final int ATTACK_TARGET                 = START_LOCATIONS + 0;
	public static final int ATTACK_SENDER                 = START_LOCATIONS + 1;
	public static final int ATTACK_SIZE_OF_INTS           = START_INTS      + 0;
	public static final int ATTACK_SIZE_OF_LOCATIONS      = START_LOCATIONS + 2;
	public static final int ATTACK_SIZE_OF_STRINGS        = START_STRINGS   + 0;

	public Radio(RobotController _rc)
	{
		rc = _rc;
		outgoing = new LinkedList<Message>();
		incoming = new LinkedList<Message>();
		intsTotalLength = IDX_START_INTS;
		locationsTotalLength = IDX_START_LOCATIONS;
		stringsTotalLength = IDX_START_STRINGS;
	}

	/*
	 * Wyslanie odlozonych wiadomosci. Wszystkie wiadomosci jakie czekaly na wyslanie 
	 * sklejamy w jeden MSG i puszczamy (niestety moze byc tak, ze wyslemy jakas 
	 * przeterminowana wiadomosc). Metoda receive() odkodowywuje taka sklejke.
	 */

	public boolean broadcast()
	{
		int bcode = Clock.getBytecodeNum();

		if (outgoing.isEmpty() || rc.hasBroadcastMessage()) return false;

		Message merged = new Message();
		merged.ints = new int[intsTotalLength];
		merged.locations = new MapLocation[locationsTotalLength];
		merged.strings = new String[stringsTotalLength];

		merged.ints[IDX_MAGIC] = MAGIC_VALUE;
		merged.ints[IDX_CHECKSUM] = HASH_VALUE;
		merged.ints[IDX_TEAM] = rc.getTeam().equals(Team.A) ? 0 : 1;
		merged.ints[IDX_SENDER] = rc.getRobot().getID();
		merged.ints[IDX_SIZE] = outgoing.size();
		merged.ints[IDX_ROUND] = Clock.getRoundNum();
		merged.locations[IDX_LOCATION] = rc.getLocation();
		
		merged.ints[IDX_CHECKSUM] ^= merged.ints[IDX_MAGIC];
		merged.ints[IDX_CHECKSUM] ^= merged.ints[IDX_TEAM];
		merged.ints[IDX_CHECKSUM] ^= merged.ints[IDX_SENDER];
		merged.ints[IDX_CHECKSUM] ^= merged.ints[IDX_SIZE];
		merged.ints[IDX_CHECKSUM] ^= merged.ints[IDX_ROUND];
		merged.ints[IDX_CHECKSUM] ^= merged.locations[IDX_LOCATION].hashCode();

		int offset_i = IDX_START_INTS;      // offset na poczatek danych na ints
		int offset_l = IDX_START_LOCATIONS; // offset na poczatek danych na locations
		int offset_s = IDX_START_STRINGS;   // offset na poczatek danych na strings

		for (Message msg : outgoing) {
			// Zapisujemy rozmiar danych pojedynczych wiadomosci
			
			merged.ints[offset_i+OFF_INTS_LENGTH]      = msg.ints.length;
			merged.ints[offset_i+OFF_LOCATIONS_LENGTH] = msg.locations.length;
			merged.ints[offset_i+OFF_STRINGS_LENGTH]   = msg.strings.length;

			// Dalej liczmy checksume...

			merged.ints[IDX_CHECKSUM] ^= merged.ints[offset_i+OFF_INTS_LENGTH];
			merged.ints[IDX_CHECKSUM] ^= merged.ints[offset_i+OFF_LOCATIONS_LENGTH];
			merged.ints[IDX_CHECKSUM] ^= merged.ints[offset_i+OFF_STRINGS_LENGTH];

			// Jest rozmiar, teraz czas na czyste dane...

	 		offset_i += OFF_START_INTS;
			offset_l += OFF_START_LOCATIONS;
			offset_s += OFF_START_STRINGS;

			for (int i = 0; i < msg.ints.length; ++i, ++offset_i) {
				merged.ints[offset_i] = msg.ints[i];
				merged.ints[IDX_CHECKSUM] ^= (msg.ints[i] ^ i);
			}
			for (int i = 0; i < msg.locations.length; ++i, ++offset_l) {
				merged.locations[offset_l] = msg.locations[i];
				merged.ints[IDX_CHECKSUM] ^= (msg.locations[i].hashCode() ^ i);
			}
			for (int i = 0; i < msg.strings.length; ++i, ++offset_s) {
				merged.strings[offset_s] = msg.strings[i];
				merged.ints[IDX_CHECKSUM] ^= (msg.strings[i].hashCode() ^ i);
			}
		}

		try {
			rc.broadcast(merged);
			outgoing.clear();

			intsTotalLength = IDX_START_INTS;
			locationsTotalLength = IDX_START_LOCATIONS;
			stringsTotalLength = IDX_START_STRINGS;

			if (verbose)
				System.out.println("*** SND: " + descr(merged) + " (" + merged.getNumBytes() + " B) Round: " + Clock.getRoundNum() + " BCode: " + (Clock.getBytecodeNum()-bcode) + " ***");

			return true;
		}
		catch (Exception e) {
			if (verbose)
				System.out.println("*** SND: Wyjatek: " + e.toString() + " Round: " + Clock.getRoundNum() + " BCode: " + (Clock.getBytecodeNum()-bcode) + " ***");
		}

		return false;
	}

	/*
	 * Czy jest cos w kolejce incoming/outgoing?
	 * Uwaga, wiadomosci w kolejce moga byc juz przeterminowane, 
	 * stad metoda get() moze zwrocic null.
	 */ 

	public boolean isIncoming() { return !incoming.isEmpty(); }
	public boolean isOutgoing() { return !outgoing.isEmpty(); }

	/*
	 * Dodanie wiadomosci do kolejki (tylko dla Radio)
	 */

	protected void add(Message msg)
	{
		assert(null != msg.ints);
		assert(null != msg.locations);
		assert(null != msg.strings);

		outgoing.add(msg);

		intsTotalLength      += OFF_START_INTS      + msg.ints.length;
		locationsTotalLength += OFF_START_LOCATIONS + msg.locations.length;
		stringsTotalLength   += OFF_START_STRINGS   + msg.strings.length;
	}

	/*
	 * Odbior wszystkich zaslyszanych wiadomosci.
	 */

	public void receive()
	{
		int bcode = Clock.getBytecodeNum();

		// - Odbieramy wszystkie wiadomosci!
		Message[] msgs = rc.getAllMessages();
		if (null == msgs || 0 == msgs.length) return;

		// - Liczba dolaczonych Message'y (byc moze trzeba bedzie cofnac !)
		int app = 0; 

		for (Message m : msgs) {
			// - Radio nie wysyla null-i
			if (null == m.ints || null == m.locations || null == m.strings) continue;

			// - Sprawdzamy minimalne rozmiary tablic
			if (IDX_START_INTS      > m.ints.length      || 
					IDX_START_LOCATIONS > m.locations.length || 
					IDX_START_STRINGS   > m.strings.length) continue;

			// - Zly magic, to napewno nie jest wiadomosc wyslana przez Radio.
			if (MAGIC_VALUE != m.ints[IDX_MAGIC]) continue; 

			// - Nie zgadza sie numer druzyny
			if ((rc.getTeam().equals(Team.A) ? 0 : 1) != m.ints[IDX_TEAM]) continue;

			// - Nie aktualna wiadomosc
			if (Clock.getRoundNum() > m.ints[IDX_ROUND] + 1) continue;

			// - Przeliczamy checksume dla wiadomosci

			int checksum = HASH_VALUE; 
			checksum ^= m.ints[IDX_MAGIC];
			checksum ^= m.ints[IDX_TEAM];
			checksum ^= m.ints[IDX_SENDER];
			checksum ^= m.ints[IDX_SIZE];
			checksum ^= m.ints[IDX_ROUND];
			checksum ^= m.locations[IDX_LOCATION].hashCode();

			int sender = m.ints[IDX_SENDER]; // - nadawca
			int size   = m.ints[IDX_SIZE];   // - liczba pojedynczych wiadomosci

			// - ustawiamy ofsety w tablicach
			int off_i = IDX_START_INTS;
			int off_l = IDX_START_LOCATIONS;
			int off_s = IDX_START_STRINGS;

			int broken = 0;

			// - Odczytuje poszczegolne wiadomosci
			for (int i = 0; i < size; ++i) {
				// - Sprawdzamy rozmiary tablic
				if (m.ints.length      - off_i < OFF_START_INTS ||
						m.ints.length      - off_i < m.ints[ off_i + OFF_INTS_LENGTH      ] ||
						m.locations.length - off_l < m.ints[ off_i + OFF_LOCATIONS_LENGTH ] ||
						m.strings.length   - off_s < m.ints[ off_i + OFF_STRINGS_LENGTH   ]) {
					broken = 1; break;
				}

				checksum ^= m.ints[off_i+OFF_INTS_LENGTH];
				checksum ^= m.ints[off_i+OFF_LOCATIONS_LENGTH];
				checksum ^= m.ints[off_i+OFF_STRINGS_LENGTH];

				int i_sz = m.ints[off_i+OFF_INTS_LENGTH];      // - rozmiar tablicy ints[]
				int l_sz = m.ints[off_i+OFF_LOCATIONS_LENGTH]; // - rozmiar tablicy locations[]
				int s_sz = m.ints[off_i+OFF_STRINGS_LENGTH];   // - rozmiar tablicy strings[]

				// - Przesuwamy offsety na tablicach

				off_i += OFF_START_INTS;
				off_l += OFF_START_LOCATIONS;
				off_s += OFF_START_STRINGS;

				// - Sprawdzamy minimalne wymagania na rozmiary tablic
				if (off_i+i_sz > m.ints.length      ||
						off_l+l_sz > m.locations.length ||
						off_s+s_sz > m.strings.length) { broken = 2; break; }

				// - Sprawdzenie nadawcy
				if (sender != m.ints[off_i+SENDER]) { broken = 3; break; }
				
				boolean good = true; // Czy wiadomosc jest ok.

				// - Filtrujemy wiadomosc, jest nie do nas
				if (RCPT_ANY != m.ints[off_i+RCPT] && 
						rc.getRobot().getID() != m.ints[off_i+RCPT]) good = false;

				// - Nie akceptujemy zbyt starych wiadomosci.
				if (Clock.getRoundNum() > m.ints[off_i+ROUND] + 1) good = false;

				// - Tworzymy wiadomosc, jaka Radio dostarcza

				Message msg = new Message();
				msg.ints = new int[i_sz];
				msg.locations = new MapLocation[l_sz];
				msg.strings = new String[s_sz];

				for (int j = 0; j < i_sz; ++j, ++off_i) {
					msg.ints[j] = m.ints[off_i];
					checksum ^= (m.ints[off_i] ^ j);
				}
				for (int j = 0; j < l_sz; ++j, ++off_l) {
					msg.locations[j] = m.locations[off_l];
					checksum ^= (m.locations[off_l].hashCode() ^ j);
				}
				for (int j = 0; j < s_sz; ++j, ++off_s) {
					msg.strings[j] = m.strings[off_s];
					checksum ^= (m.strings[off_s].hashCode() ^ j);
				}

				if (good) { incoming.add(msg); ++app; }
			}

			if (broken > 0) {
				if (verbose) 
					System.out.println("*** RCV: Cos sie syplo...(" + broken + ") Round: " + Clock.getRoundNum() + " BCode: " + (Clock.getBytecodeNum()-bcode) + " ***)");
				assert(incoming.size() >= app);
				for (int i = 0; i < app; ++i) incoming.removeLast();
			} else if (m.ints[IDX_CHECKSUM] != checksum) {
			// - Na koniec sprawdzam checksume, jak syf, cofam operacje!!
				if (verbose) {
					System.out.println("*** RCV: NIE zdadza sie CHECKSUMA!! (=" + checksum + ") Round: " + Clock.getRoundNum() + " BCode: " + (Clock.getBytecodeNum()-bcode) + " ***");
					System.out.println("*** RCV: " + descr(m) + " ***");
				}
				assert(incoming.size() >= app);
				for (int i = 0; i < app; ++i) incoming.removeLast();
			} else {
				if (verbose)
					System.out.println("*** RCV: Ok Round: " + Clock.getRoundNum() + " BCode: " + (Clock.getBytecodeNum()-bcode) + " ***");
			}
		}
	}

	/*
	 * Odbior wiadomosci, mozemy dostac null jesli kolejka jest pusta, 
	 * lub wiadomosci sa przeterminowane.
	 */

	public Message get()
	{
		while (!incoming.isEmpty()) {
			Message m = incoming.getFirst();
			incoming.removeFirst();
			if (Clock.getRoundNum() > m.ints[ROUND] + 1) continue;
			return m;
		}

		return null;
	}

	/*
	 * Dalej metody do wysylania konkretnych wiadomosci.
	 */

	/*
	 * HELLO
	 */
	public void sayHello()
	{
		Message m = new Message();
		m.ints      = new int[HELLO_SIZE_OF_INTS];
		m.locations = new MapLocation[HELLO_SIZE_OF_LOCATIONS];
		m.strings   = new String[HELLO_SIZE_OF_STRINGS];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = RCPT_ANY; 
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = HELLO;
		m.locations[HELLO_SENDER]    = rc.getLocation();
		m.strings[HELLO_SENDER_TYPE] = rc.getRobotType().toString();

		add(m);
	}

	/*
	 * Archon znalazl fluxa
	 */
	public void sayFluxFound(MapLocation l)
	{
		Message m = new Message();
		m.ints      = new int[FLUX_FOUND_SIZE_OF_INTS];
		m.locations = new MapLocation[FLUX_FOUND_SIZE_OF_LOCATIONS];
		m.strings   = new String[FLUX_FOUND_SIZE_OF_STRINGS];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = RCPT_ANY; 
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = FLUX_FOUND;
		m.locations[FLUX_FOUND_SENDER] = rc.getLocation();
		m.locations[FLUX_FOUND_FOUND]  = l;

		add(m);
	}

	/*
	 * Archon przejal fluxa
	 */
	public void sayFluxTaken()
	{
		Message m = new Message();
		m.ints      = new int[FLUX_TAKEN_SIZE_OF_INTS];
		m.locations = new MapLocation[FLUX_TAKEN_SIZE_OF_LOCATIONS];
		m.strings   = new String[FLUX_TAKEN_SIZE_OF_STRINGS];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = RCPT_ANY; 
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = FLUX_TAKEN;
		m.locations[FLUX_TAKEN_SENDER] = rc.getLocation();

		add(m);
	}

	/*
	 * Informacja o pozycjach przeciwnika
	 */
	public void sayEnemies(List<RInfo> robots)
	{
		Message m = new Message();
		m.ints      = new int[ENEMIES_SIZE_OF_INTS              + robots.size()];
		m.locations = new MapLocation[ENEMIES_SIZE_OF_LOCATIONS + robots.size()];
		m.strings   = new String[ENEMIES_SIZE_OF_STRINGS        + robots.size()];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = RCPT_ANY; 
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = ENEMIES;
		m.ints[ENEMIES_SIZE] = robots.size(); // zeby meczyc sie z .length :P
		m.locations[ENEMIES_SENDER] = rc.getLocation();

		int it_i = ENEMIES_I_START;
		int it_l = ENEMIES_L_START;
		int it_s = ENEMIES_S_START;

		for (RInfo r : robots) { 
			RobotInfo inf     = r.inf;
			m.ints[it_i]      = (int)(r.inf.energonLevel);
			m.locations[it_l] = r.inf.location;
			m.strings[it_s]   = r.inf.type.toString();
			++it_i; ++it_l; ++it_s;
		} 

		add(m);
	}

	/*
	 * Informacja o schodach zbudowanych z blokow przez workerow
	 */
	public void sayStairs(List<MapLocation> list)
	{
		Message m = new Message();
		m.ints      = new int[STAIRS_SIZE_OF_INTS];
		m.locations = new MapLocation[STAIRS_SIZE_OF_LOCATIONS + list.size()];
		m.strings   = new String[STAIRS_SIZE_OF_STRINGS];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = RCPT_ANY;
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = STAIRS;
		m.ints[STAIRS_SIZE] = list.size();

		int i = 0;
		for (MapLocation l : list) { m.locations[i] = l; ++i; }

		add(m);
	}
		
	/*
	 * Instrukcja 'idz' do wskazanego pola.
	 */
	public void sayGoto(int id, MapLocation l)
	{
		Message m = new Message();
		m.ints      = new int[GOTO_SIZE_OF_INTS];
		m.locations = new MapLocation[GOTO_SIZE_OF_LOCATIONS];
		m.strings   = new String[GOTO_SIZE_OF_STRINGS];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = id; 
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = GOTO;
		m.locations[GOTO_TARGET] = l;

		add(m);	
	}

	/*
	 * Sygnal do ataku.
	 */
	public void sayAttack(MapLocation trg)
	{
		Message m = new Message();
		m.ints      = new int[ATTACK_SIZE_OF_INTS];
		m.locations = new MapLocation[ATTACK_SIZE_OF_LOCATIONS];
		m.strings   = new String[ATTACK_SIZE_OF_STRINGS];
		m.ints[SENDER] = rc.getRobot().getID();
		m.ints[RCPT]   = RCPT_ANY; 
		m.ints[ROUND]  = Clock.getRoundNum();
		m.ints[TYPE]   = ATTACK;
		m.locations[ATTACK_TARGET] = trg;
		m.locations[ATTACK_SENDER] = rc.getLocation();

		add(m);	
	}
	
	/*
	 * Wypis MSG
	 */
	public String descr(Message m)
	{
		String di = "ints:";
		if (null == m.ints) di += " null";
		else {
			di += " {";
			for (int i = 0; i < m.ints.length; ++i) 
				di += " " + m.ints[i];
			di += " }";
		}
		String ds = "strings:";
		if (null == m.strings) ds += " null";
		else {
			ds += " {";
			for (int i = 0; i < m.strings.length; ++i) 
				ds += " " + m.strings[i];
			ds += " }";
		}
		String dl = "locations:";
		if (null == m.locations) dl += " null";
		else {
			dl += " {";
			for (int i = 0; i < m.locations.length; ++i) 
				dl += " " + m.locations[i];
			dl += " }";
		}

		return "< " + di + " " + ds + " " + dl + " >";
	}

}


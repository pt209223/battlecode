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

	/* Do uzytku wewnetrznego dla Radio */

	protected static final int IDX_MAGIC                = 0; // indeks magicznego pola
	protected static final int IDX_TEAM                 = 1; // indeks numeru druzyny
	protected static final int IDX_SENDER               = 2; // indeks id nadawcy
	protected static final int IDX_SIZE                 = 3; // indeks liczby wiadomosci
	protected static final int IDX_START_INTS           = 4; // indeks poczatku danych na ints[]
	protected static final int IDX_LOCATION             = 0; // indeks lokalizacji nadawcy
	protected static final int IDX_START_LOCATIONS      = 1; // indeks poczatku danych na locations[]
	protected static final int IDX_START_STRINGS        = 0; // indeks poczatku danych na strings[]

	protected static final int OFF_INTS_LENGTH          = 0; // ofset dlugosci ints[] dla wiadomosci
	protected static final int OFF_LOCATIONS_LENGTH     = 1; // ofset dlugosci locations[] ...
	protected static final int OFF_STRINGS_LENGTH       = 2; // ofset dlugosci strings[] ...
	protected static final int OFF_START_INTS           = 3; // ofset poczatku danych na ints[]
	protected static final int OFF_START_LOCATIONS      = 0; // ofset poczatku danych na locations[]
	protected static final int OFF_START_STRINGS        = 0; // ofset poczatku danych na strings[]

	protected static final int MAGIC_VALUE = 0x4FA31EB7; // Magiczna wartosc

	/* Do ogolnego uzytku */

	public static final int MSG_IDX_SENDER           = 0; // indeks nadawcy w wiadomosci
	public static final int MSG_IDX_RCPT             = 1; // indeks odbiorcy w wiadomosci
	public static final int MSG_IDX_ROUND            = 2; // indeks rundy w wiadomosci
	public static final int MSG_IDX_TYPE             = 3; // indeks typu wiadomosci
	public static final int MSG_IDX_START_INTS       = 4; // indeks poczatku reszy danych na ints[]
	public static final int MSG_IDX_START_LOCATIONS  = 0; // --//-- na locations[]
	public static final int MSG_IDX_START_STRINGS    = 0; // --//-- na strings[]

	public static final int MSG_RCPT_ANY = -1; // Wiadomosc dla kazdego
	//public static final int MSG_ROUND_XOR = 0x432FA190; // XOR na numer rundy

	public static final int MSG_TYPE_HELLO       = 0; // Hello, tu jestem
	public static final int MSG_TYPE_FLUX_FOUND  = 1; // Widze Flux!
	public static final int MSG_TYPE_FLUX_TAKEN  = 2; // Flux przechwycony!
	public static final int MSG_TYPE_ENEMIES     = 3; // Pozycje wroga
	public static final int MSG_TYPE_GOTO        = 4; // Idz, bez dyskusyjnie, tam
	public static final int MSG_TYPE_STAIRS      = 5; // Pozycje schodow (Archon<->Worker)
	public static final int MSG_TYPE_ATTACK      = 6; // Gdzie isc atakowac
	//public static final int MSG_TYPE_MAP         = 5; // Informacje o mapie
	// ... ?

	// Indeksy dla wiadomosci MSG_TYPE_HELLO:
	public static final int MSG_HELLO_L_SENDER = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_HELLO_S_SENDER = MSG_IDX_START_STRINGS + 0;
	public static final int MSG_HELLO_SIZE_OF_INTS = MSG_IDX_START_INTS;
	public static final int MSG_HELLO_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS + 1;
	public static final int MSG_HELLO_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS + 1;

	// Indeksy dla wiadomosci MSG_TYPE_FLUX_FOUND:
	public static final int MSG_FLUX_FOUND_L_SENDER = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_FLUX_FOUND_L_FOUND  = MSG_IDX_START_LOCATIONS + 1;
	public static final int MSG_FLUX_FOUND_SIZE_OF_INTS = MSG_IDX_START_INTS;
	public static final int MSG_FLUX_FOUND_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS + 2;
	public static final int MSG_FLUX_FOUND_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS;

	// Indeksy dla wiadomosci MSG_TYPE_FLUX_TAKEN:
	public static final int MSG_FLUX_TAKEN_L_SENDER = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_FLUX_TAKEN_SIZE_OF_INTS = MSG_IDX_START_INTS;
	public static final int MSG_FLUX_TAKEN_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS + 1;
	public static final int MSG_FLUX_TAKEN_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS;

	// Indeksy dla wiadomosci MSG_TYPE_ENEMIES:
	public static final int MSG_ENEMIES_L_SENDER = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_ENEMIES_I_SIZE = MSG_IDX_START_INTS + 0;
	public static final int MSG_ENEMIES_L_ENEMIES_START = MSG_IDX_START_LOCATIONS + 1;
	public static final int MSG_ENEMIES_I_ENEMIES_START = MSG_IDX_START_INTS + 1;
	public static final int MSG_ENEMIES_S_ENEMIES_START = MSG_IDX_START_STRINGS + 0;
	public static final int MSG_ENEMIES_SIZE_OF_INTS = MSG_IDX_START_INTS + 1;
	public static final int MSG_ENEMIES_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS + 1;
	public static final int MSG_ENEMIES_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS;

	// Indeksy dla wiadomosci MSG_TYPE_GOTO:
	public static final int MSG_GOTO_L_GOTO = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_GOTO_SIZE_OF_INTS = MSG_IDX_START_INTS;
	public static final int MSG_GOTO_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS+1;
	public static final int MSG_GOTO_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS;

	// Indeksy dla wiadomosci MSG_TYPE_STAIRS:
	public static final int MSG_STAIRS_I_SIZE = MSG_IDX_START_INTS + 0;
	public static final int MSG_STAIRS_L_START = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_STAIRS_SIZE_OF_INTS = MSG_IDX_START_INTS + 1;
	public static final int MSG_STAIRS_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_STAIRS_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS;

	// Indeksy dla wiadomosci MSG_TYPE_ATTACK:
	public static final int MSG_ATTACK_L_TARGET = MSG_IDX_START_LOCATIONS + 0;
	public static final int MSG_ATTACK_SIZE_OF_INTS = MSG_IDX_START_INTS;
	public static final int MSG_ATTACK_SIZE_OF_LOCATIONS = MSG_IDX_START_LOCATIONS + 1;
	public static final int MSG_ATTACK_SIZE_OF_STRINGS = MSG_IDX_START_STRINGS;

	public Radio(RobotController _rc)
	{
		rc = _rc;
		outgoing = new LinkedList<Message>();
		incoming = new LinkedList<Message>();
		intsTotalLength = IDX_START_INTS;
		locationsTotalLength = IDX_START_LOCATIONS;
		stringsTotalLength = IDX_START_STRINGS;
	}

	public boolean broadcast()
	{
		if (outgoing.isEmpty() || rc.hasBroadcastMessage()) return false;

		Message merged = new Message();
		merged.ints = new int[intsTotalLength];
		merged.locations = new MapLocation[locationsTotalLength];
		merged.strings = new String[stringsTotalLength];

		merged.ints[IDX_MAGIC] = MAGIC_VALUE;
		merged.ints[IDX_TEAM] = rc.getTeam() == Team.A ? 0 : 1;
		merged.ints[IDX_SENDER] = rc.getRobot().getID();
		merged.ints[IDX_SIZE] = outgoing.size();
		merged.locations[IDX_LOCATION] = rc.getLocation();
		
		int offset_i = IDX_START_INTS;
		int offset_l = IDX_START_LOCATIONS;
		int offset_s = IDX_START_STRINGS;

		for (Message msg : outgoing) {
			merged.ints[offset_i+OFF_INTS_LENGTH] = msg.ints.length;
			merged.ints[offset_i+OFF_LOCATIONS_LENGTH] = msg.locations.length;
			merged.ints[offset_i+OFF_STRINGS_LENGTH] = msg.strings.length;
	 		offset_i += OFF_START_INTS;
			offset_l += OFF_START_LOCATIONS;
			offset_s += OFF_START_STRINGS;
			for (int i = 0; i < msg.ints.length; ++i, ++offset_i)
				merged.ints[offset_i] = msg.ints[i];
			for (int i = 0; i < msg.locations.length; ++i, ++offset_l) 
				merged.locations[offset_l] = msg.locations[i];
			for (int i = 0; i < msg.strings.length; ++i, ++offset_s)
				merged.strings[offset_s] = msg.strings[i];
		}

		try {
			rc.broadcast(merged);
			outgoing.clear();
			intsTotalLength = IDX_START_INTS;
			locationsTotalLength = IDX_START_LOCATIONS;
			stringsTotalLength = IDX_START_STRINGS;

			return true;
		}
		catch (Exception e) {
			System.out.println("Radio.broadcast(): Wyjatek = " + e.toString());
		}

		return false;
	}

	public boolean isIncoming()
	{
		return !incoming.isEmpty();
	}

	public boolean isOutgoing()
	{
		return !outgoing.isEmpty();
	}

	public void add(Message msg)
	{
		outgoing.add(msg);
		intsTotalLength += 
			OFF_START_INTS + ((null == msg.ints) ? 0 : msg.ints.length);
		locationsTotalLength += 
			OFF_START_LOCATIONS + ((null == msg.locations) ? 0 : msg.locations.length);
		stringsTotalLength +=
			OFF_START_STRINGS + ((null == msg.strings) ? 0 : msg.strings.length);
	}

	public void receive()
	{
		Message[] msgs = rc.getAllMessages();
		if (null == msgs || 0 == msgs.length) return;

		for (Message m : msgs) {
			if (null == m.ints || null == m.locations || null == m.strings) { 
				//System.out.println("Radio.receive(): Pusta tablica, ignoruje... " + descr(m));
				continue;
			}

			if (IDX_START_INTS > m.ints.length || 
					IDX_START_LOCATIONS > m.locations.length || 
					IDX_START_STRINGS > m.strings.length) {
				//System.out.println("Radio.receive(): Za malo el. w tablicy, ignoruje... " + descr(m));
				continue;
			}

			if (MAGIC_VALUE != m.ints[IDX_MAGIC]) {
				//System.out.println("Radio.receive(): Nie zgadza sie wartosc MAGIC, ignoruje... " + descr(m));
				continue;
			}

			if ((rc.getTeam() == Team.A ? 0 : 1) != m.ints[IDX_TEAM]) {
				//System.out.println("Radio.receive(): Wiadomosc z innej druzyny, ignoruje... " + descr(m));
				continue;
			}

			int sender = m.ints[IDX_SENDER];
			int size = m.ints[IDX_SIZE];
			int off_i = IDX_START_INTS;
			int off_l = IDX_START_LOCATIONS;
			int off_s = IDX_START_STRINGS;

			for (int i = 0; i < size; ++i) {
				if (m.ints.length      - off_i < OFF_START_INTS ||
						m.ints.length      - off_i < m.ints[ off_i + OFF_INTS_LENGTH      ] ||
						m.locations.length - off_l < m.ints[ off_i + OFF_LOCATIONS_LENGTH ] ||
						m.strings.length   - off_s < m.ints[ off_i + OFF_STRINGS_LENGTH   ]) {
					System.out.println(
							"Radio.receive(): Nieprawidlowe dane (i="+i+"/"+size+
							" length="+m.ints.length+","+m.strings.length+","+m.locations.length+
							" off="+off_i+","+off_s+","+off_l+"), ignoruje... " + descr(m));
					break;
				}

				int i_sz = m.ints[off_i+OFF_INTS_LENGTH];
				int l_sz = m.ints[off_i+OFF_LOCATIONS_LENGTH];
				int s_sz = m.ints[off_i+OFF_STRINGS_LENGTH];
				off_i += OFF_START_INTS;
				off_l += OFF_START_LOCATIONS;
				off_s += OFF_START_STRINGS;

				if (i_sz < MSG_IDX_START_INTS || 
						l_sz < MSG_IDX_START_LOCATIONS || 
						s_sz < MSG_IDX_START_STRINGS) {
					System.out.println("Radio.receive(): Zbyt mala liczba indeksow, ignoruje... " + descr(m));
					break;
				}

				if (sender != m.ints[off_i+MSG_IDX_SENDER]) {
					System.out.println("Radio.receive(): Niepoprawny id nadawcy, ignoruje... " + descr(m));
					break;
				}
				
				if (MSG_RCPT_ANY != m.ints[off_i+MSG_IDX_RCPT] && 
						rc.getRobot().getID() != m.ints[off_i+MSG_IDX_RCPT]) {
					off_i += i_sz; off_l += l_sz; off_s += s_sz;
					continue;
				}

				if (Clock.getRoundNum() > m.ints[off_i+MSG_IDX_ROUND] + 1) {
					off_i += i_sz; off_l += l_sz; off_s += s_sz;
					continue;
				}

				Message msg = new Message();
				msg.ints = new int[i_sz];
				msg.locations = new MapLocation[l_sz];
				msg.strings = new String[s_sz];

				for (int j = 0; j < i_sz; ++j, ++off_i) 
					msg.ints[j] = m.ints[off_i];
				for (int j = 0; j < l_sz; ++j, ++off_l)
					msg.locations[j] = m.locations[off_l];
				for (int j = 0; j < s_sz; ++j, ++off_s)
					msg.strings[j] = m.strings[off_s];

				incoming.add(msg);
			}
		}
	}

	public Message get()
	{
		while (!incoming.isEmpty()) {
			Message m = incoming.getFirst();
			incoming.removeFirst();
			if (Clock.getRoundNum() > m.ints[MSG_IDX_ROUND] + 1) continue;
			return m;
		}

		return null;
	}

	public void sayHello()
	{
		Message m = new Message();
		m.ints = new int[MSG_HELLO_SIZE_OF_INTS];
		m.locations = new MapLocation[MSG_HELLO_SIZE_OF_LOCATIONS];
		m.strings = new String[MSG_HELLO_SIZE_OF_STRINGS];
		m.locations[MSG_HELLO_L_SENDER] = rc.getLocation();
		m.strings[MSG_HELLO_S_SENDER] = rc.getRobotType().toString();
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = MSG_RCPT_ANY; 
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_HELLO;

		add(m);
	}

	public void sayFluxFound(MapLocation l)
	{
		Message m = new Message();
		m.ints = new int[MSG_FLUX_FOUND_SIZE_OF_INTS];
		m.locations = new MapLocation[MSG_FLUX_FOUND_SIZE_OF_LOCATIONS];
		m.strings = new String[MSG_FLUX_FOUND_SIZE_OF_STRINGS];
		m.locations[MSG_FLUX_FOUND_L_SENDER] = rc.getLocation();
		m.locations[MSG_FLUX_FOUND_L_FOUND] = l;
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = MSG_RCPT_ANY; 
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_FLUX_FOUND;

		add(m);
	}

	public void sayFluxTaken()
	{
		Message m = new Message();
		m.ints = new int[MSG_FLUX_TAKEN_SIZE_OF_INTS];
		m.locations = new MapLocation[MSG_FLUX_TAKEN_SIZE_OF_LOCATIONS];
		m.strings = new String[MSG_FLUX_TAKEN_SIZE_OF_STRINGS];
		m.locations[MSG_FLUX_TAKEN_L_SENDER] = rc.getLocation();
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = MSG_RCPT_ANY; 
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_FLUX_TAKEN;

		add(m);
	}

	public void sayEnemies(List<RInfo> robots)
	{
		Message m = new Message();
		m.ints = new int[MSG_ENEMIES_SIZE_OF_INTS + robots.size()];
		m.locations = new MapLocation[MSG_ENEMIES_SIZE_OF_LOCATIONS + robots.size()];
		m.strings = new String[MSG_ENEMIES_SIZE_OF_STRINGS + robots.size()];
		m.locations[MSG_ENEMIES_L_SENDER] = rc.getLocation();
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = MSG_RCPT_ANY; 
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_ENEMIES;
		m.ints[MSG_ENEMIES_I_SIZE] = robots.size();

		int it_i = MSG_ENEMIES_I_ENEMIES_START;
		int it_l = MSG_ENEMIES_L_ENEMIES_START;
		int it_s = MSG_ENEMIES_S_ENEMIES_START;

		for (RInfo r : robots) { 
			RobotInfo inf = r.inf;
			m.ints[it_i] = (int)(r.inf.energonLevel);
			m.locations[it_l] = r.inf.location;
			m.strings[it_s] = r.inf.type.toString();
			++it_i; ++it_l; ++it_s;
		} 

		add(m);
	}

	public void sayStairs(List<MapLocation> list)
	{
		Message m = new Message();
		m.ints = new int[MSG_STAIRS_SIZE_OF_INTS];
		m.locations = new MapLocation[MSG_STAIRS_SIZE_OF_LOCATIONS + list.size()];
		m.strings = new String[MSG_STAIRS_SIZE_OF_STRINGS];
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = MSG_RCPT_ANY;
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_STAIRS;
		m.ints[MSG_STAIRS_I_SIZE] = list.size();

		int i = 0;
		for (MapLocation l : list) { m.locations[i] = l; ++i; }

		add(m);
	}
		
	public void sayGoto(int id, MapLocation l)
	{
		Message m = new Message();
		m.ints = new int[MSG_GOTO_SIZE_OF_INTS];
		m.locations = new MapLocation[MSG_GOTO_SIZE_OF_LOCATIONS];
		m.strings = new String[MSG_GOTO_SIZE_OF_STRINGS];
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = id; 
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_GOTO;
		m.locations[MSG_GOTO_L_GOTO] = l;

		add(m);	
	}

	public void sayAttack(MapLocation trg)
	{
		Message m = new Message();
		m.ints = new int[MSG_ATTACK_SIZE_OF_INTS];
		m.locations = new MapLocation[MSG_ATTACK_SIZE_OF_LOCATIONS];
		m.strings = new String[MSG_ATTACK_SIZE_OF_STRINGS];
		m.ints[MSG_IDX_SENDER] = rc.getRobot().getID();
		m.ints[MSG_IDX_RCPT] = MSG_RCPT_ANY; 
		m.ints[MSG_IDX_ROUND] = Clock.getRoundNum();
		m.ints[MSG_IDX_TYPE] = MSG_TYPE_ATTACK;
		m.locations[MSG_ATTACK_L_TARGET] = trg;

		add(m);	
	}
	
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


};

package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 3/4/15 at  12:39 PM
 */
public class PlatformVersion extends SwampThing {
    public static final String NAME_KEY = "full_name";
    public static final String PLATFORM_UUID_KEY = "platform_uuid";  
    public static final String PLATFORM_VERSION_UUID_KEY = "platform_version_uuid";
    public static final String VERSION_STRING = "version_string";

    protected Platform platform;

	public PlatformVersion(Session session) {
        super(session);
    }
    public PlatformVersion(Session session, Map map) {
        super(session, map);
    }

	public Platform getPlatform() {
		return platform;
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public String getFullName() {
		return this.getConversionMap().getString(NAME_KEY);
	}

	public String getVersionString() {
		return this.getConversionMap().getString(VERSION_STRING);
	}

	public String getPlatformVersionUuid() {
		return this.getConversionMap().getString(PLATFORM_VERSION_UUID_KEY);
	}

	public String getName() {
		return getFullName();
	}
	
    @Override
    public String getIDKey() {
        return PLATFORM_VERSION_UUID_KEY;
    }

    @Override
    protected SwampThing getNewInstance() {
        return new PlatformVersion(getSession());
    }
    
	public enum Bits {
		BITS_32,
		BITS_64;
		
		public String toString () {
			if (this == Bits.BITS_32){
				//return "32-bit";
				return "32";
			}else {
				//return "64-bit";
				return "64";
			}
		}
	}

	
	String shortName;
	String version;
	Bits bits;
	
	protected void standardize (String shortName, String version, Bits bits) {

		this.shortName = shortName;
		this.version = version;
		this.bits = bits;
	}
	
	public void standardize() {
		/*
		 * This provides backward compatability for all the
		 * "old" OS names in the SWAMP before we addressed
		 * this issue and had the swamp start using real
		 * platform names.
		 * It remains so old SWAMPs still work with the plugins.
		 * This could probably switch to using Bolo's new
		 * automagic code to do the translation ... the only
		 * thing needed is to notice 'Linux' in the string,
		 * The duplicate xxx-bit stuff, and the need
		 * to turn MAJOR.MINOR into MAJOR for everything BUT ubuntu
		 */
		String fullName = getFullName();
		switch(fullName) {
		case ("CentOS Linux 5 32-bit 5.11 32-bit"):
			standardize("CentOS", "5", Bits.BITS_32);
		break;
		case ("CentOS Linux 5 64-bit 5.11 64-bit"):
			standardize("CentOS", "5", Bits.BITS_64);
		break;
		case ("CentOS Linux 6 32-bit 6.7 32-bit"):
			standardize("CentOS", "6", Bits.BITS_32);
		break;
		case ("CentOS Linux 6 64-bit 6.7 64-bit"):
			standardize("CentOS", "6", Bits.BITS_64);
		break;
		case ("CentOS Linux 7 64-bit 7.4 64-bit"):
			standardize("CentOS", "7", Bits.BITS_64);
		break;
		case ("CentOS Linux 7 32-bit 7.4 32-bit"):
			standardize("CentOS", "7", Bits.BITS_32);
		break;
		case ("Debian Linux 7.11 64-bit"):
			standardize("Debian", "7", Bits.BITS_64);
		break;
		case ("Debian Linux 8.6 64-bit"):
			standardize("Debian", "8", Bits.BITS_64);
		break;
		case ("Debian Linux 9.3 64-bit"):
			standardize("Debian", "9", Bits.BITS_64);
		break;
		case ("Fedora Linux 18 64-bit"):
			standardize("Fedora", "18", Bits.BITS_64);
		break;
		case ("Fedora Linux 19 64-bit"):
			standardize("Fedora", "19", Bits.BITS_64);
		break;
		case ("Fedora Linux 20 64-bit"):
			standardize("Fedora", "20", Bits.BITS_64);
		break;
		case ("Fedora Linux 21 64-bit"):
			standardize("Fedora", "21", Bits.BITS_64);
		break;
		case ("Fedora Linux 22 64-bit"):
			standardize("Fedora", "22", Bits.BITS_64);
		break;
		case ("Fedora Linux 23 64-bit"):
			standardize("Fedora", "23", Bits.BITS_64);
		break;
		case ("Fedora Linux 24 64-bit"):
			standardize("Fedora", "24", Bits.BITS_64);
		break;
		case ("Fedora Linux 25 64-bit"):
			standardize("Fedora", "25", Bits.BITS_64);
		break;
		case ("Redhat Enterprise Linux 5 32-bit 5.11 32-bit"):
			standardize("CentOS", "5", Bits.BITS_32);
		break;
		case ("Redhat Enterprise Linux 5 64-bit 5.11 64-bit"):
			standardize("CentOS", "5", Bits.BITS_64);
		break;
		case ("Redhat Enterprise Linux 6 32-bit 6.7 32-bit"):
			standardize("CentOS", "6", Bits.BITS_32);
		break;
		case ("Redhat Enterprise Linux 6 64-bit 6.7 64-bit"):
			standardize("CentOS", "6", Bits.BITS_64);
		break;
		case ("Redhat Enterprise Linux 7 64-bit 7.4 64-bit"):
			standardize("CentOS", "7", Bits.BITS_64);
		break;
		case ("Scientific Linux 5 32-bit 5.11 32-bit"):
			standardize("Scientific", "5", Bits.BITS_32);
		break;
		case ("Scientific Linux 5 64-bit 5.11 64-bit"):
			standardize("Scientific", "5", Bits.BITS_64);
		break;
		case ("Scientific Linux 6 32-bit 6.7 32-bit"):
			standardize("Scientific", "6", Bits.BITS_32);
		break;
		case ("Scientific Linux 6 64-bit 6.7 64-bit"):
			standardize("Scientific", "6", Bits.BITS_64);
		break;
		case ("Scientific Linux 7 64-bit 7.4 64-bit"):
			standardize("Scientific", "7", Bits.BITS_64);
		break;
		case ("Ubuntu Linux 10.04 LTS 64-bit Lucid Lynx"):
			standardize("Ubuntu", "10.04", Bits.BITS_64);
		break;
		case ("Ubuntu Linux 12.04 LTS 64-bit Precise Pangolin"):
			standardize("Ubuntu", "12.04", Bits.BITS_64);
		break;
		case ("Ubuntu Linux 14.04 LTS 64-bit Trusty Tahr"):
			standardize("Ubuntu", "14.04", Bits.BITS_64);
		break;
		case ("Ubuntu Linux 16.04 LTS 64-bit Xenial Xerus"):
			standardize("Ubuntu", "16.04", Bits.BITS_64);
		break;
		default:
			/*
			 * decode the parts so any new OSes automatically
			 * work.   THe prior code here broke any asesssment
			 * when a new os was added to a swamp.
			 *
			 * XXX could expand parsing to allow
			 * NaME [Linux] Major [63][42]-bit
			 *
			 * XXX code doesn't deal with
			 * Android on Android on Redhat Enterprise LInux ...
			 * But we should be calling it RHEL anyway; easy
			 * enough to do something about that later.
			 */
			String[] av = fullName.trim().split("\\s+");
			int ac = av.length;
			boolean errors = false;
			Bits bits = Bits.BITS_64;	/* bad class design */
			if (ac == 5) {
				/*
				 * this code is untested, but is usable
				 * incase we go back to rhel as a platform
				 * AND have multi-word name instead of RHEL.
				 */ 
				/* Redhat Enterprise Linux OSver OSbits
				    0      1         2     3     4 */
				boolean rhel = av[0].equals("Redhat")
				    && av[1].equals("Enterprise")
				    && av[2].equals("Linux");
				if (rhel) {
					/* make it look like a real os */
					av[0] = "RHEL";
					av[1] = av[3];
					av[2] = av[4];
					ac = 3;
					/* av.length = 3 would be OK, but
					 * not needed because of ac
					 */
				}
				/* else error propagates correctly */
			}
			else if (ac == 6) {
				/* Android Android on OSname OSver OSbits
				    0      1       2  3      4     5 */
				boolean android = av[0].equals("Android")
				    && av[1].equals("Android")
				    && av[2].equals("on");
				if (android) {
					av[0] = "Android" + "-" + av[3];
					av[1] = av[4];
					av[2] = av[5];
					ac = 3;
				}
				/* else error propagates correctly */
			}
			if (ac == 3) {
				String sbits = av[2];
				/* enum should have a converter */
				if (sbits.equals("32-bit"))
					bits = Bits.BITS_32;
				else if (sbits.equals("64-bit"))
					bits = Bits.BITS_64;
				else {
					System.err.println("Unknown bits format: " + sbits);
					errors = true;
				}
			}
			else {
				System.err.print("Unknown OS format ac=");
				System.err.println(ac);
				errors = true;
			}
			if (!errors) {
				standardize(av[0], av[1], bits);
			}
			else {
				/* Use stderr to try not break stdout parsing */
				System.err.println("Unknown " + fullName);
				standardize(fullName, "", Bits.BITS_64);
			}
			break;
		}
	}
	
	public String getShortName() {
		return shortName;
	}

	public String getVersion() {
		return version;
	}

	public Bits getBits() {
		return bits;
	}

	public String getDisplayString() {
		return String.format("%s-%s-%s", getShortName().toLowerCase(), getVersion(), getBits());
	}
	
	public String toString() {
		return getDisplayString();
	}

	public static String getDisplayString(String platform_name, String platform_version_string) {
	    if(platform_name.equalsIgnoreCase("android")) {
	        return "android-ubuntu-12.04-64";
	    }else {
	        String str = platform_name + "-" + platform_version_string;
	        str = str.toLowerCase().replace(' ', '-');
	        if (str.endsWith("-bit")) {
	            str = str.substring(0, str.lastIndexOf("-bit"));
	        }
	        return str;
	    }
	}
}

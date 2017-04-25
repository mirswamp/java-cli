package org.continuousassurance.swamp.cli.util;

import org.continuousassurance.swamp.api.PlatformVersion;
import org.continuousassurance.swamp.cli.exceptions.UnkownPlatformException;

public class SwampPlatform {

	public enum Bits {
		BITS_32,
		BITS_64;
		
		public String toString () {
			if (this == Bits.BITS_32){
				return "32-bit";
			}else {
				return "64-bit";
			}
		}
	}

	String name;
	String version;
	Bits bits;
	String displayString;
	PlatformVersion pkr_version;

	
	public SwampPlatform(String name, String version, Bits bits, PlatformVersion pkr_version) {
		super();
		this.name = name;
		this.version = version;
		this.bits = bits;
		this.pkr_version = pkr_version;
	}
	
	public static SwampPlatform convertToSwampPackage(PlatformVersion platform_version) throws UnkownPlatformException {
		
		switch(platform_version.getFullName()) {
		case ("Android Android on Ubuntu 12.04 64-bit"):
			return new SwampPlatform("Android Ubuntu", "12.04", Bits.BITS_64, platform_version);
		case ("CentOS Linux 5 32-bit 5.11 32-bit"):
			return new SwampPlatform("CentOS", "5.11", Bits.BITS_32, platform_version);
		case ("CentOS Linux 5 64-bit 5.11 64-bit"):
			return new SwampPlatform("CentOS", "5.11", Bits.BITS_64, platform_version);
		case ("CentOS Linux 6 32-bit 6.7 32-bit"):
			return new SwampPlatform("CentOS", "6.7", Bits.BITS_32, platform_version);
		case ("CentOS Linux 6 64-bit 6.7 64-bit"):
			return new SwampPlatform("CentOS", "6.7", Bits.BITS_64, platform_version);
		case ("Debian Linux 7.11 64-bit"):
			return new SwampPlatform("Debian", "7.11", Bits.BITS_64, platform_version);
		case ("Debian Linux 8.6 64-bit"):
			return new SwampPlatform("Debian", "8.6", Bits.BITS_64, platform_version);
		case ("Fedora Linux 18 64-bit"):
			return new SwampPlatform("Fedora", "18", Bits.BITS_64, platform_version);
		case ("Fedora Linux 19 64-bit"):
			return new SwampPlatform("Fedora", "19", Bits.BITS_64, platform_version);
		case ("Fedora Linux 20 64-bit"):
			return new SwampPlatform("Fedora", "20", Bits.BITS_64, platform_version);
		case ("Fedora Linux 21 64-bit"):
			return new SwampPlatform("Fedora", "21", Bits.BITS_64, platform_version);
		case ("Fedora Linux 22 64-bit"):
			return new SwampPlatform("Fedora", "22", Bits.BITS_64, platform_version);
		case ("Fedora Linux 23 64-bit"):
			return new SwampPlatform("Fedora", "23", Bits.BITS_64, platform_version);
		case ("Fedora Linux 24 64-bit"):
			return new SwampPlatform("Fedora", "24", Bits.BITS_64, platform_version);
		case ("Red Hat Enterprise Linux 6 32-bit 6.7 32-bit"):
			return new SwampPlatform("RHEL", "6.7", Bits.BITS_32, platform_version);
		case ("Red Hat Enterprise Linux 6 64-bit 6.7 64-bit"):
			return new SwampPlatform("RHEL", "6.7", Bits.BITS_64, platform_version);
		case ("Scientific Linux 5 32-bit 5.11 32-bit"):
			return new SwampPlatform("Scientific Linux", "5.11", Bits.BITS_32, platform_version);
		case ("Scientific Linux 5 64-bit 5.11 64-bit"):
			return new SwampPlatform("Scientific Linux", "5.11", Bits.BITS_64, platform_version);
		case ("Scientific Linux 6 32-bit 6.7 32-bit"):
			return new SwampPlatform("Scientific Linux", "6.7", Bits.BITS_32, platform_version);
		case ("Scientific Linux 6 64-bit 6.7 64-bit"):
			return new SwampPlatform("Scientific Linux", "6.7", Bits.BITS_64, platform_version);
		case ("Ubuntu Linux 10.04 LTS 64-bit Lucid Lynx"):
			return new SwampPlatform("Ubuntu", "10.04", Bits.BITS_64, platform_version);
		case ("Ubuntu Linux 12.04 LTS 64-bit Precise Pangolin"):
			return new SwampPlatform("Ubuntu", "12.04", Bits.BITS_64, platform_version);
		case ("Ubuntu Linux 14.04 LTS 64-bit Trusty Tahr"):
			return new SwampPlatform("Ubuntu", "14.04", Bits.BITS_64, platform_version);
		case ("Ubuntu Linux 16.04 LTS 64-bit Xenial Xerus"):
			return new SwampPlatform("Ubuntu", "16.04", Bits.BITS_64, platform_version);
		default:
			throw new UnkownPlatformException(platform_version.getFullName());
		}
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public Bits getBits() {
		return bits;
	}

	public String getDisplayString() {
		return displayString;
	}

	public PlatformVersion getPkr_version() {
		return pkr_version;
	}

	public String toString () {
		return String.format("%s %s %s", getName(), getVersion(), getBits());
	}
}

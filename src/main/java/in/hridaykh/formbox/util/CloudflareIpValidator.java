package in.hridaykh.formbox.util;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CloudflareIpValidator {

	private static final List<CidrMatcher> CF_RANGES = new ArrayList<>();

	static {
		String[] cidrs = {
			"173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
			"141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
			"197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
			"104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
			"2400:cb00::/32", "2606:4700::/32", "2803:f800::/32", "2405:b500::/32",
			"2405:8100::/32", "2a06:98c0::/29", "2c0f:f248::/32"
		};

		for (String cidr : cidrs) {
			try {
				CF_RANGES.add(new CidrMatcher(cidr));
			} catch (Exception e) {
				log.error("Failed to parse Cloudflare CIDR: {}", cidr, e);
			}
		}
	}

	public static boolean contains(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank())
			return false;

		try {
			InetAddress targetAddress = InetAddress.getByName(ipAddress.trim());
			byte[] targetBytes = targetAddress.getAddress();

			for (CidrMatcher matcher : CF_RANGES) {
				if (matcher.matches(targetBytes)) {
					return true;
				}
			}
		} catch (UnknownHostException e) {
			return false;
		}

		return false;
	}

	private static class CidrMatcher {
		private final byte[] networkAddress;
		private final int prefixLength;

		public CidrMatcher(String cidr) throws UnknownHostException {
			String[] parts = cidr.split("/");
			this.networkAddress = InetAddress.getByName(parts[0]).getAddress();
			this.prefixLength = Integer.parseInt(parts[1]);
		}

		public boolean matches(byte[] targetAddress) {
			if (this.networkAddress.length != targetAddress.length) {
				return false;
			}

			int remainingBits = prefixLength;
			for (int i = 0; i < networkAddress.length && remainingBits > 0; i++) {
				int mask = 0xFF00 >> Math.min(remainingBits, 8);
				if ((networkAddress[i] & mask) != (targetAddress[i] & mask)) {
					return false;
				}
				remainingBits -= 8;
			}
			return true;
		}
	}
}
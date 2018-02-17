package no.artorp.upnp_util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import no.artorp.upnp_util.util.Version;

public class App {
	
	private final boolean runWizard;
	
	public App(boolean runWizard) {
		this.runWizard = runWizard;
	}
	
	public void run() {
		System.out.println("upnp-util " + Version.getVersion());
		
		GatewayDevice activeGw = null;
		PortMappingEntry portMapping = new PortMappingEntry();
		
		GatewayDiscover discover = new GatewayDiscover();
		System.out.println("Looking for gateway devices.");
		
		try {
			Map<InetAddress, GatewayDevice> gateways = discover.discover();
			if (gateways.isEmpty()) {
				System.out.println("No gateways found.");
				return;
			}
			int g_size = gateways.size();
			System.out.println(g_size + " gateway" + (g_size == 1 ? "" : "s") + " found");
			
			// convert the map to a list, to make it indexable for e.g. user selection
			List<GatewayDevice> orderedGateways = new ArrayList<>(gateways.values());
			for (int i = 0; i < orderedGateways.size(); i++) {
				GatewayDevice gw = orderedGateways.get(i);
				System.out.println("\nListing gateway details of device #" + i);
				System.out.println(readableGateway(gw));
				
				if (!runWizard) {
					// list all mapping to this gateway
					System.out.println(getGatewayMappings(gw, portMapping));
				}
			}
			
			if (!runWizard)
				return;
			
			if (orderedGateways.size() > 1) {
				// user must select a gateway
				System.out.println("Choose one:");
				for (int i = 0; i < orderedGateways.size(); i++) {
					GatewayDevice gw = orderedGateways.get(i);
					System.out.println("  " + i + " - " + gw.getFriendlyName());
				}
				System.out.println("  \"exit\" to exit");
				Integer chosen = getNumberInRangeFromUser(0, orderedGateways.size() - 1);
				if (chosen == null) {
					System.out.println("Exiting.");
					return;
				}
				activeGw = orderedGateways.get(chosen);
				System.out.println("Chose " + activeGw.getFriendlyName());
			} else {
				activeGw = orderedGateways.get(0);
			}
			
			boolean isLooping = true;
			
			while (isLooping) {
				System.out.println("Active mappings on " + activeGw.getFriendlyName());
				
				System.out.println(getGatewayMappings(activeGw, portMapping));
				
				System.out.println("Options:");
				System.out.println("  0 - create new mapping");
				System.out.println("  1 - remove mapping");
				System.out.println("  \"exit\" to quit");
				Integer actionChoice = getNumberInRangeFromUser(0, 1);
				if (actionChoice == null) {
					isLooping = false;
					continue;
				}
				
				String newMappingDesc = null;
				if (actionChoice.intValue() == 0) {
					System.out.println("Enter a description for the new mapping:");
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					newMappingDesc = br.readLine();
				}
				
				System.out.println("Type of mapping:");
				System.out.println("  0 - TCP");
				System.out.println("  1 - UDP");
				System.out.println("  2 - both");
				System.out.println("  \"exit\" to quit");
				Integer type = getNumberInRangeFromUser(0, 2);
				if (type == null) {
					isLooping = false;
					continue;
				}
				
				if (actionChoice.intValue() == 0) {
					System.out.println("Select internal and external port range,"
							+ " <port> or <internal>:<external>");
					System.out.println("Example:");
					System.out.println("  12345 - map external port 12345 to internal port 12345");
					System.out.println("  12345:87654 - map external port 87654 to internal port 12345");
				} else if (actionChoice.intValue() == 1) {
					System.out.println("Select external port range to remove");
					System.out.println("Example:");
					System.out.println("  12345 - removes port mapping with extermal port 12345");
				}
				System.out.println("or \"exit\" to quit");
				
				int[] ports = getPortRangeFromUser();
				if (ports == null) {
					isLooping = false;
					continue;
				}
				int internal = ports[0];
				int external = ports[1];
				
				InetAddress localAddress = activeGw.getLocalAddress();
				
				if (actionChoice.intValue() == 0) {
					// create new mapping
					System.out.println("Querying device to see if a port mapping already"
							+ " exists for external port " + external);
					boolean conflict = false;
					if (type.intValue() == 0 || type.intValue() == 2) {
						if (activeGw.getSpecificPortMappingEntry(external, "TCP", portMapping)) {
							conflict = true;
							System.out.println("Port " + external + " w/ protocol TCP is already mapped");
						}
					}
					if (type.intValue() == 1 || type.intValue() == 2) {
						if (activeGw.getSpecificPortMappingEntry(external, "UDP", portMapping)) {
							conflict = true;
							System.out.println("Port " + external + " w/ protocol UDP is already mapped");
						}
					}
					
					if (conflict) {
						System.out.println("Aborting port mapping creation.");
						continue;
					}
					
					if (type.intValue() == 0 || type.intValue() == 2) {
						System.out.println("Creating TCP mapping from external port " + external +
								" to internal port " + internal);
						if (activeGw.addPortMapping(external, internal, localAddress.getHostAddress(), "TCP",
								newMappingDesc)) {
							System.out.println("Successfully created TCP mapping");
						} else {
							System.out.println("Couldn't create new TCP mapping");
						}
					}
					
					if (type.intValue() == 1 || type.intValue() == 2) {
						System.out.println("Creating UDP mapping from external port " + external +
								" to internal port " + internal);
						if (activeGw.addPortMapping(external, internal, localAddress.getHostAddress(), "UDP",
								newMappingDesc)) {
							System.out.println("Successfully created UDP mapping");
						} else {
							System.out.println("Couldn't create new UDP mapping");
						}
					}
				} else if (actionChoice.intValue() == 1) {
					// remove mapping
					if (type.intValue() == 0 || type.intValue() == 2) {
						if (activeGw.deletePortMapping(external, "TCP")) {
							System.out.println("Requested removal of TCP mapping on external port " + external);
						} else {
							System.out.println("Couldn't remove TCP mapping on external port " + external);
						}
					}
					if (type.intValue() == 1 || type.intValue() == 2) {
						if (activeGw.deletePortMapping(external, "UDP")) {
							System.out.println("Requested removal of UDP mapping on external port " + external);
						} else {
							System.out.println("Couldn't remove UDP mapping on external port " + external);
						}
					}
				}
			}
			
		} catch (IOException | SAXException | ParserConfigurationException e) {
			System.err.println("Unexpected exception occured.");
			e.printStackTrace();
		}
		
		System.out.println("Exiting.");
	}
	
	private static String readableGateway(GatewayDevice gw) throws IOException, SAXException {
		return "\tFriendly name: " + gw.getFriendlyName() + "\n" +
				"\tPresentation URL: " + gw.getPresentationURL() + "\n" +
				"\tModel name: " + gw.getModelName() + "\n" +
				"\tModel number: " + gw.getModelNumber() + "\n" +
				"\tLocal interface address: " + gw.getLocalAddress().getHostAddress() + "\n" +
				"\tExternal interface address: " + gw.getExternalIPAddress() + "\n";
	}
	
	private static String getGatewayMappings(GatewayDevice gw, PortMappingEntry portMapping) throws IOException, SAXException {
		StringBuilder sb = new StringBuilder();
		sb.append("Port mappings on gateway " + gw.getFriendlyName() + ":\n");
		int mapIndex = 0;
		// create table, columns: index, protocol, description, internal port, external port
		Formatter formatter = new Formatter(sb);
		String format = "%2s | %3s | %5s | %5s | %-14s | %s%n";
		formatter.format(format, "#", "pr.", "int.", "ext.", "client", "description");
		do {
			if (gw.getGenericPortMappingEntry(mapIndex, portMapping)) {
				formatter.format(format, mapIndex,
						portMapping.getProtocol(),
						portMapping.getInternalPort(),
						portMapping.getExternalPort(),
						portMapping.getInternalClient(),
						portMapping.getPortMappingDescription());
			} else {
				break;
			}
			mapIndex++;
		} while (portMapping != null);
		
		try {
			formatter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns a number from user input
	 * 
	 * @param low inclusive lower number
	 * @param high inclusive upper number
	 * @return value chosen, {@code null} if user typed exit
	 * @throws IOException
	 */
	private static Integer getNumberInRangeFromUser(int low, int high) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String errorMsg = "Input must be integer in range " + low + " to " + high;
		String line = null;
		Integer n = null;
		while ((line = br.readLine()) != null) {
			if ("exit".equals(line)){
				n = null;
				break;
			}
			try {
				n = Integer.valueOf(line.trim());
				if (n < low || n > high) {
					System.out.println(errorMsg);
					continue;
				}
				break;
			} catch (NumberFormatException e) {
				System.out.println("Unknown number " + line);
				System.out.println(errorMsg);
			}
		}
		return n;
	}
	
	private static int[] getPortRangeFromUser() throws IOException {
		int[] range = new int[2];
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		while ((line = br.readLine()) != null) {
			if ("exit".equals(line)) {
				range = null;
				break;
			}
			int delimIndex = line.indexOf(':');
			if (delimIndex == -1) {
				// single port value
				try {
					int port = Integer.valueOf(line);
					range[0] = port;
					range[1] = port;
					break;
				} catch (NumberFormatException e) {
					System.out.println("Unknown port number " + line);
					continue;
				}
			} else {
				// two port values
				String[] splat = new String[2];
				splat[0] = line.substring(0, delimIndex);
				splat[1] = line.substring(delimIndex + 1);
				try {
					int internal = Integer.valueOf(splat[0]);
					int external = Integer.valueOf(splat[1]);
					range[0] = internal;
					range[1] = external;
					break;
				} catch (NumberFormatException e) {
					System.out.println("Port range must be in the form <internal>:<external>");
					continue;
				}
			}
		}
		return range;
	}
}

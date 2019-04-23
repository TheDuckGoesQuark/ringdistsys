package node.ringstore;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NodeListFileParser {

    /**
     * Parses node file csv to map of node ids to their ipv4 addresses and ports
     *
     * @param listFilePath path of file containing node list
     * @param logger       logger for debugging
     * @return map of node ids to their ipv4 address and ports
     * @throws IOException if the file cannot be found
     */
    public static Map<Integer, InetSocketAddress> parseNodeFile(String listFilePath, Logger logger) throws IOException {
        final Map<Integer, InetSocketAddress> nodes = new HashMap<>();

        try (CSVReader csvReader = new CSVReader(new FileReader(listFilePath))) {
            // Skip header row
            csvReader.skip(1);

            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                final int nodeid = Integer.parseInt(values[0]);
                final InetSocketAddress socketAddress = new InetSocketAddress(values[1], Integer.parseInt(values[2]));
                nodes.put(nodeid, socketAddress);
            }
        } catch (IOException e) {
            logger.warning("Failed to parse node list file");
            logger.warning(e.getMessage());
            throw e;
        }

        return nodes;
    }

}

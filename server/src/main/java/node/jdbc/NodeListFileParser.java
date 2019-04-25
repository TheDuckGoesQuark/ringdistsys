package node.jdbc;

import com.opencsv.CSVReader;
import node.ringrepository.VirtualNode;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class NodeListFileParser {

    /**
     * Parses node file csv to map of node ids to their ipv4 addresses and ports
     *
     * @param listFilePath path of file containing node list
     * @param logger       logger for debugging
     * @return map of node ids to their ipv4 address and ports
     * @throws IOException if the file cannot be found
     */
    static List<VirtualNode> parseNodeFile(String listFilePath, Logger logger) throws IOException {
        final List<VirtualNode> nodes = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new FileReader(listFilePath))) {
            // Skip header row
            csvReader.skip(1);

            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                final int nodeid = Integer.parseInt(values[0]);
                final String address = values[1];
                final int commsPort = Integer.parseInt(values[2]);
                final int clientPort = Integer.parseInt(values[3]);

                nodes.add(new VirtualNode(address, commsPort, clientPort, nodeid, null, false));
            }
        } catch (IOException e) {
            logger.warning("Failed to parse node list file");
            logger.warning(e.getMessage());
            throw e;
        }

        return nodes;
    }

}

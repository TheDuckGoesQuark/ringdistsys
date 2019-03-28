package config;

import com.opencsv.CSVReader;
import node.AddressTranslator;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NodeListFileParser {

    public static AddressTranslator parseNodeFile(String listFilePath, Logger logger) throws IOException {
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

        return new AddressTranslator(nodes);
    }

}

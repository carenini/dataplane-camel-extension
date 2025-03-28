package org.eclipse.dataspaceconnector.dataplane.extension.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

public class SinkProcessor implements Processor {
    private DataFlowRequest request;

    public void setRequest(DataFlowRequest request) {
        this.request = request;
    }

    @Override
    public void process(Exchange exchange) {
        String destinationPath = request.getDestinationDataAddress().getProperties()
            .getOrDefault("path", "data/output.txt");
        exchange.getIn().setBody(destinationPath);
    }
} 
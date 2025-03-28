package org.eclipse.dataspaceconnector.dataplane.extension.sink;

import org.apache.camel.CamelContext;
import org.eclipse.dataspaceconnector.spi.dataplane.sink.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

public class CamelSinkFactory implements DataSinkFactory {
    private final CamelContext camelContext;

    public CamelSinkFactory(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return "camel".equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public CamelSink createSink(DataFlowRequest request) {
        return new CamelSink(camelContext, request);
    }
} 
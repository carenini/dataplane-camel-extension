package org.eclipse.dataspaceconnector.dataplane.extension.source;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.dataspaceconnector.spi.dataplane.source.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

public class CamelSourceFactory implements DataSourceFactory {
    private final CamelContext camelContext;

    public CamelSourceFactory(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return "camel".equals(request.getSourceDataAddress().getType());
    }

    @Override
    public CamelSource createSource(DataFlowRequest request) {
        return new CamelSource(camelContext, request);
    }
} 
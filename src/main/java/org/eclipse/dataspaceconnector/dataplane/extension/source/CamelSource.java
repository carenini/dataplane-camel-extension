package org.eclipse.dataspaceconnector.dataplane.extension.source;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.eclipse.dataspaceconnector.dataplane.extension.processor.SourceProcessor;
import org.eclipse.dataspaceconnector.spi.dataplane.source.DataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class CamelSource implements DataSource {
    private static final String ROUTE_XML = "routes/source-route.xml";
    private final CamelContext camelContext;
    private final DataFlowRequest request;
    private final Monitor monitor;

    public CamelSource(CamelContext camelContext, DataFlowRequest request) {
        this.camelContext = camelContext;
        this.request = request;
        this.monitor = camelContext.getRegistry().lookupByNameAndType("monitor", Monitor.class);
    }

    @Override
    public CompletableFuture<InputStream> openPartStream() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create a unique route ID for this transfer
                String routeId = "source-" + request.getId();
                
                // Load the route from XML
                var springContext = new ClassPathXmlApplicationContext(ROUTE_XML);
                var sourceContext = springContext.getBean("sourceContext", CamelContext.class);
                
                // Configure the processor with the request
                var processor = springContext.getBean("sourceProcessor", SourceProcessor.class);
                processor.setRequest(request);
                
                // Start the route
                sourceContext.startRoute(routeId);

                // Create a producer template to trigger the route
                ProducerTemplate producer = sourceContext.createProducerTemplate();
                
                // Trigger the route and get the result
                return (InputStream) producer.requestBody("direct:source-route", null);
            } catch (Exception e) {
                monitor.severe("Error in Camel source route", e);
                throw new RuntimeException("Failed to execute Camel source route", e);
            }
        });
    }

    @Override
    public void close() {
        try {
            String routeId = "source-" + request.getId();
            var springContext = new ClassPathXmlApplicationContext(ROUTE_XML);
            var sourceContext = springContext.getBean("sourceContext", CamelContext.class);
            sourceContext.stopRoute(routeId);
            springContext.close();
        } catch (Exception e) {
            monitor.severe("Error closing Camel source route", e);
        }
    }
} 
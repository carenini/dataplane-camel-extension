package org.eclipse.dataspaceconnector.dataplane.extension.sink;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.eclipse.dataspaceconnector.dataplane.extension.processor.SinkProcessor;
import org.eclipse.dataspaceconnector.spi.dataplane.sink.DataSink;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class CamelSink implements DataSink {
    private static final String ROUTE_XML = "routes/sink-route.xml";
    private final CamelContext camelContext;
    private final DataFlowRequest request;
    private final Monitor monitor;

    public CamelSink(CamelContext camelContext, DataFlowRequest request) {
        this.camelContext = camelContext;
        this.request = request;
        this.monitor = camelContext.getRegistry().lookupByNameAndType("monitor", Monitor.class);
    }

    @Override
    public CompletableFuture<Void> transfer(InputStream inputStream) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create a unique route ID for this transfer
                String routeId = "sink-" + request.getId();
                
                // Load the route from XML
                var springContext = new ClassPathXmlApplicationContext(ROUTE_XML);
                var sinkContext = springContext.getBean("sinkContext", CamelContext.class);
                
                // Configure the processor with the request
                var processor = springContext.getBean("sinkProcessor", SinkProcessor.class);
                processor.setRequest(request);
                
                // Start the route
                sinkContext.startRoute(routeId);

                // Create a producer template to trigger the route
                ProducerTemplate producer = sinkContext.createProducerTemplate();
                
                // Trigger the route with the input stream
                producer.sendBody("direct:sink-route", inputStream);
            } catch (Exception e) {
                monitor.severe("Error in Camel sink route", e);
                throw new RuntimeException("Failed to execute Camel sink route", e);
            }
        });
    }

    @Override
    public void close() {
        try {
            String routeId = "sink-" + request.getId();
            var springContext = new ClassPathXmlApplicationContext(ROUTE_XML);
            var sinkContext = springContext.getBean("sinkContext", CamelContext.class);
            sinkContext.stopRoute(routeId);
            springContext.close();
        } catch (Exception e) {
            monitor.severe("Error closing Camel sink route", e);
        }
    }
} 
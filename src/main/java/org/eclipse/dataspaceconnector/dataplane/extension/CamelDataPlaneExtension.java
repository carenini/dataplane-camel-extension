package org.eclipse.dataspaceconnector.dataplane.extension;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.eclipse.dataspaceconnector.dataplane.extension.sink.CamelSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.extension.source.CamelSourceFactory;
import org.eclipse.dataspaceconnector.spi.dataplane.DataPlane;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelDataPlaneExtension implements ServiceExtension {
    private static final String CAMEL_CONTEXT_XML = "camel-context.xml";
    private Monitor monitor;
    private CamelContext camelContext;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        context.getTypeManager().registerTypes(DataPlane.class);
    }

    @Override
    public void start() {
        try {
            // Initialize Spring context
            var springContext = new ClassPathXmlApplicationContext(CAMEL_CONTEXT_XML);
            
            // Create and start Camel context
            camelContext = new SpringCamelContext(springContext);
            
            // Register monitor in Camel registry
            camelContext.getRegistry().bind("monitor", monitor);
            
            // Register source and sink factories
            var dataPlane = camelContext.getRegistry().lookupByNameAndType("dataPlane", DataPlane.class);
            dataPlane.registerSourceFactory(new CamelSourceFactory(camelContext));
            dataPlane.registerSinkFactory(new CamelSinkFactory(camelContext));
            
            camelContext.start();
            monitor.info("Camel DataPlane started successfully");
        } catch (Exception e) {
            monitor.severe("Failed to start Camel DataPlane", e);
            throw new RuntimeException("Failed to start Camel DataPlane", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (camelContext != null) {
                camelContext.stop();
                monitor.info("Camel DataPlane stopped successfully");
            }
        } catch (Exception e) {
            monitor.severe("Failed to stop Camel DataPlane", e);
        }
    }

    private RouteBuilder createTestRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // Test route that reads a file from the filesystem
                from("file:data?noop=true&fileName=test.txt")
                    .routeId("testFileRoute")
                    .log("Processing file: ${file:name}")
                    .process(exchange -> {
                        String content = exchange.getIn().getBody(String.class);
                        monitor.info("File content: " + content);
                    })
                    .to("http://localhost:8080/test");
            }
        };
    }
} 
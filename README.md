# rooty-tooty

An MQ-based message bus for informing various daemons when things need to get done.

## Usage

### Send an event to the bus

    RootyConfiguration config = new RootyConfiguration()
        .withSecret(secretKey)
        .withQueueName(qName);

    RootySender sender = config.getSender();
    
    RootyMessage message = new NewAccountEvent(accountName);
    sender.write(message);

### Receive events from the bus

#### Define a class that extends RootyHandlerBase

    public class MyHandler extends RootyHandlerBase {
        // handlers can have attributes, set in their config (see below)
        private boolean debug;
        public void setDebug (boolean debug) { this.debug = debug; }

        // declare which message types this class can handle
        @Override public boolean accepts(RootyMessage message) { return message instanceof NewAccountEvent; }

        // called when an acceptable message is ready to be processed
        @Override public void process(RootyMessage message) {
            NewAccountEvent event = (NewAccountEvent) message;
            // ... handle the event ...
        }
    }

#### Start the rooty daemon from the command line

Create a YAML config file:

    queueName: someQueueName+dns
    secret: yourSecretKey
    
    handlers:
      MyHandler:   # fully-qualified Java class name here
        params:    # params will be copied to JavaBean-style attributes on the handler class
          debug: true
      # multiple handlers can be listed

Start RootyMain like this:

    java rooty.RootyMain /path/to/config.yml

Ensure your CLASSPATH includes both:

* the appropriate rooty-tooty.jar file
* the jar file that contains the MyHandler class

#### Or, start the rooty daemon from code

    RootyHandlerConfiguration handlerConfig = new RootyHandlerConfiguration()
        .withHandler(MyHandler.class.getName())
        .withParam("debug", true);

    RootyConfiguration config = new RootyConfiguration()
        .withSecret(secretKey)
        .withQueueName(qName)
        .addHandler(handlerConfig);

    RootyMain daemon = new RootyMain();
    daemon.run(config); // async: will start a listener thread and return


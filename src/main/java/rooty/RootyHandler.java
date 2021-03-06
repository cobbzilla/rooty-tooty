package rooty;

import org.cobbzilla.util.mq.MqClient;

public interface RootyHandler {

    public MqClient getMqClient();
    public RootyHandler setMqClient(MqClient mqClient);

    public String getQueueName();
    public RootyHandler setQueueName(String queueName);

    public RootyStatusManager getStatusManager();
    public RootyHandler setStatusManager(RootyStatusManager statusManager);

    public RootySender getSender();
    public RootyHandler setSender(RootySender sender);

    /**
     * @param message A candidate message
     * @return true if this handler wants to process the message
     */
    public boolean accepts(RootyMessage message);

    /**
     * Process a message
     * @param message The message to process
     * @return true if the message was processed successfully by the authoritative processor
     */
    public boolean process (RootyMessage message);

}

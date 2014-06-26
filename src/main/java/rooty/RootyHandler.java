package rooty;

import org.cobbzilla.util.mq.MqClient;

public interface RootyHandler {

    public MqClient getMqClient();
    public void setMqClient(MqClient mqClient);

    public String getQueueName();
    void setQueueName(String queueName);

    public RootyStatusManager getStatusManager();
    void setStatusManager(RootyStatusManager updater);

    /**
     * @param message A candidate message
     * @return true if this handler wants to process the message
     */
    public boolean accepts(RootyMessage message);

    /**
     * Process a message
     * @param message The message to process
     */
    public void process (RootyMessage message);

}

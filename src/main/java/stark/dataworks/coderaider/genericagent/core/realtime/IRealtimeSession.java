package stark.dataworks.coderaider.genericagent.core.realtime;

/**
 * Active realtime model session.
 */
public interface IRealtimeSession extends AutoCloseable
{
    void sendText(String input);

    void addListener(IRealtimeEventListener listener);

    @Override
    void close();
}

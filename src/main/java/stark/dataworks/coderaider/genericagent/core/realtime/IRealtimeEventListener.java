package stark.dataworks.coderaider.genericagent.core.realtime;

/**
 * Listener for realtime session events.
 */
public interface IRealtimeEventListener
{
    void onEvent(RealtimeEvent event);
}

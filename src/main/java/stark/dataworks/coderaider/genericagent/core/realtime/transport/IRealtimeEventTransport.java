package stark.dataworks.coderaider.genericagent.core.realtime.transport;

import stark.dataworks.coderaider.genericagent.core.realtime.RealtimeEvent;

/**
 * Transport-neutral event sink for realtime orchestration.
 */
public interface IRealtimeEventTransport
{
    void publish(RealtimeEvent event);
}

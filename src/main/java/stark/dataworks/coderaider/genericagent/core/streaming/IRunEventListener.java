package stark.dataworks.coderaider.genericagent.core.streaming;

import stark.dataworks.coderaider.genericagent.core.events.RunEvent;

/**
 * RunEventListener implements core runtime responsibilities.
 */
public interface IRunEventListener
{

    /**
     * Handles a published run event.
     *
     * @param event run event.
     */
    void onEvent(RunEvent event);
}

package stark.dataworks.coderaider.gundam.core.session;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import stark.dataworks.coderaider.gundam.core.model.Message;

/**
 * Session implements session persistence and restoration.
 * It keeps this concern isolated so the kernel remains modular and provider-agnostic.
 */
@Getter
@AllArgsConstructor
public class Session
{
    /**
     * Internal state for id; used while coordinating runtime behavior.
     */
    private final String id;

    /**
     * Internal state for messages; used while coordinating runtime behavior.
     */
    private final List<Message> messages;
}

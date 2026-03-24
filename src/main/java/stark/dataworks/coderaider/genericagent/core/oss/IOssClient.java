package stark.dataworks.coderaider.genericagent.core.oss;

/**
 * IOssClient abstracts object storage operations for generated multimodal assets.
 */
public interface IOssClient
{
    /**
     * Puts an object into the object storage.
     *
     * @param key         object key.
     * @param payload     object payload.
     * @param contentType object content type.
     * @return Public URL of the object.
     */
    String putObject(String key, byte[] payload, String contentType);

    /**
     * Gets the public URL of an object.
     *
     * @param key object key.
     * @return Public URL of the object.
     */
    String getPublicUrl(String key);
}

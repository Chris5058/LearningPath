package com.tradeplatform.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends TradePlatformException {

    /**
     * The type of resource that was not found.
     */
    private final String resourceType;

    /**
     * The identifier of the resource that was not found.
     */
    private final String resourceId;

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
        this.resourceType = "Unknown";
        this.resourceId = "Unknown";
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified resource type and identifier.
     *
     * @param resourceType the type of resource that was not found
     * @param resourceId   the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id %s not found", resourceType, resourceId), "RESOURCE_NOT_FOUND");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message, resource type, and identifier.
     *
     * @param message      the detail message
     * @param resourceType the type of resource that was not found
     * @param resourceId   the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String message, String resourceType, String resourceId) {
        super(message, "RESOURCE_NOT_FOUND");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Returns the type of resource that was not found.
     *
     * @return the resource type
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Returns the identifier of the resource that was not found.
     *
     * @return the resource identifier
     */
    public String getResourceId() {
        return resourceId;
    }
}
package com.example.cantonesevoicerecognition.data.repository;

/**
 * Generic callback interface for repository operations
 * @param <T> The type of result returned on success
 */
public interface RepositoryCallback<T> {
    /**
     * Called when the operation completes successfully
     * @param result The result of the operation
     */
    void onSuccess(T result);
    
    /**
     * Called when the operation fails
     * @param error The exception that caused the failure
     */
    void onError(Exception error);
}
package com.tomanagement;

/**
 * Docker images used by Testcontainers in integration tests.
 */
public enum TestContainerImage {

    POSTGRES("postgres:15-alpine");

    private final String imageName;

    TestContainerImage(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}

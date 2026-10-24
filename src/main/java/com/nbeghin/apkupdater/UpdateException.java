package com.nbeghin.apkupdater;

import java.io.IOException;

/**
 * The update server answered, but not with something this library understands, or the downloaded file
 * failed verification. Distinct from a plain {@link IOException} (network down), which is worth retrying.
 */
public class UpdateException extends IOException {
    private static final long serialVersionUID = 1L;

    public UpdateException(String message) {
        super(message);
    }
}

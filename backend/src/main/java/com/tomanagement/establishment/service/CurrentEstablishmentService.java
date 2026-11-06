package com.tomanagement.establishment.service;

import com.tomanagement.establishment.dto.CurrentEstablishmentResponse;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;

/**
 * The establishment of the current session, for its admin.
 */
public interface CurrentEstablishmentService {

    CurrentEstablishmentResponse get();

    CurrentEstablishmentResponse rename(RenameEstablishmentRequest request);
}

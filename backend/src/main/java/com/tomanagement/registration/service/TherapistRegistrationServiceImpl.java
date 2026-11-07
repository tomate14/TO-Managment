package com.tomanagement.registration.service;

import com.tomanagement.establishment.entity.EstablishmentKind;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.establishment.service.EstablishmentCreator;
import com.tomanagement.establishment.service.NewEstablishment;
import com.tomanagement.registration.dto.RegisterTherapistRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TherapistRegistrationServiceImpl implements TherapistRegistrationService {

    private final EstablishmentCreator creator;
    private final EstablishmentRepository establishmentRepository;

    public TherapistRegistrationServiceImpl(EstablishmentCreator creator,
            EstablishmentRepository establishmentRepository) {
        this.creator = creator;
        this.establishmentRepository = establishmentRepository;
    }

    @Override
    @Transactional
    public void register(RegisterTherapistRequest request) {
        String name = PracticeNameAllocator.allocate(request.fullName(), establishmentRepository::existsByNameIgnoreCase);
        creator.create(new NewEstablishment(name, EstablishmentKind.INDIVIDUAL, false, request.fullName(),
                request.email(), request.password()));
    }
}

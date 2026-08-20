package com.example.petManagementService.pet.service;

import com.example.petManagementService.pet.entity.Pet;
import com.example.petManagementService.pet.entity.PetCustodyHistory;
import com.example.petManagementService.pet.enums.PetStatus;
import com.example.petManagementService.pet.enums.TransferType;
import com.example.petManagementService.pet.repository.PetCustodyHistoryRepository;
import com.example.petManagementService.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

// v2 §9's custody module: the ONE place that mutates a pet's owner/custodian and appends
// to pet_custody_history. Intake, adoption and boarding all call in here rather than each
// writing their own ownership mutation — three copies of this would drift, and the custody
// chain is the audit trail the whole design exists to produce.
//
// Every method joins the CALLER's transaction (default REQUIRED propagation), so the
// request's status change and the pet's custody change commit or roll back together.
// Never REQUIRES_NEW here: a half-applied handover — request COMPLETED but pet unmoved,
// or vice versa — is the one outcome this module exists to prevent.
@Service
@RequiredArgsConstructor
public class CustodyService {

    private final PetRepository petRepository;
    private final PetCustodyHistoryRepository petCustodyHistoryRepository;

    // Intake handover (Flow B step 3). Ownership is RELEASED, not moved to another user:
    // the center becomes both holder and de-facto owner. surrenderedByUserId keeps the
    // provenance that ownerUserId is about to lose — nothing else records who handed the
    // animal in.
    @Transactional
    public Pet transferToCenter(UUID petId, UUID toCenterId, UUID requestId, Long adminUserId) {
        Pet pet = load(petId, requestId);
        // PENDING_INTAKE is the correct pre-state once approve() sets it (Step 5); OWNED is
        // still accepted because today approve() doesn't, and an in-flight request created
        // before that change must still be completable.
        requireStatus(pet, PetStatus.OWNED, PetStatus.PENDING_INTAKE);

        Long fromUserId = pet.getOwnerUserId();
        UUID fromCenterId = pet.getCustodianCenterId();

        pet.setOwnerUserId(null);
        pet.setCustodianCenterId(toCenterId);
        pet.setSurrenderedByUserId(fromUserId);
        pet.setStatus(PetStatus.IN_CENTER_CUSTODY);
        petRepository.save(pet);

        appendHistory(pet, fromUserId, fromCenterId, null, toCenterId,
                requestId, TransferType.INTAKE, adminUserId);
        return pet;
    }

    // Adoption handover (Flow D step 3). The mirror image: the center releases custody and
    // a private individual becomes the owner again, so the pet goes back to plain OWNED.
    // surrenderedByUserId is deliberately NOT cleared — it records who originally handed
    // the animal in, which stays true after it's rehomed.
    @Transactional
    public Pet transferToAdopter(UUID petId, Long toUserId, UUID requestId, Long adminUserId) {
        Pet pet = load(petId, requestId);
        // RESERVED is the correct pre-state once approve() sets it (Step 7);
        // AVAILABLE_FOR_ADOPTION accepted for the same transitional reason as above.
        requireStatus(pet, PetStatus.RESERVED, PetStatus.AVAILABLE_FOR_ADOPTION);

        UUID fromCenterId = pet.getCustodianCenterId();
        if (fromCenterId == null) {
            // A pet being adopted must currently be held by a center — otherwise this is a
            // peer-to-peer transfer, which is exactly what v2 forbids.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PET_NOT_IN_CUSTODY: cannot hand over a pet no center is holding");
        }

        pet.setOwnerUserId(toUserId);
        pet.setCustodianCenterId(null);
        pet.setStatus(PetStatus.OWNED);
        petRepository.save(pet);

        appendHistory(pet, null, fromCenterId, toUserId, null,
                requestId, TransferType.ADOPTION, adminUserId);
        return pet;
    }

    // Intake APPROVED (Flow B step 2) — a promise, not a handover. Only the status moves:
    // the owner still owns the animal and no center is holding it yet, which is why there
    // is deliberately NO pet_custody_history row here. That table records physical hops in
    // the chain of responsibility; nothing has physically moved at approval time.
    @Transactional
    public Pet markPendingIntake(UUID petId, UUID requestId) {
        Pet pet = load(petId, requestId);
        requireStatus(pet, PetStatus.OWNED);
        pet.setStatus(PetStatus.PENDING_INTAKE);
        return petRepository.save(pet);
    }

    // Intake CANCELLED after approval (Flow B step 4: "owner doesn't turn up → admin
    // cancels; pet reverts to OWNED"). Exact mirror of markPendingIntake — status only, no
    // history row, because the un-doing of a promise is also not a physical hop.
    @Transactional
    public Pet revertPendingIntake(UUID petId, UUID requestId) {
        Pet pet = load(petId, requestId);
        requireStatus(pet, PetStatus.PENDING_INTAKE);
        pet.setStatus(PetStatus.OWNED);
        return petRepository.save(pet);
    }

    // Listing created (Flow C step 3). Status only: custodianCenterId is untouched because
    // the same center goes on holding the animal — all that changed is that it's now
    // publicly visible. No history row, for the same reason as markPendingIntake.
    @Transactional
    public Pet markAvailableForAdoption(UUID petId) {
        Pet pet = loadPet(petId);
        requireStatus(pet, PetStatus.IN_CENTER_CUSTODY);
        pet.setStatus(PetStatus.AVAILABLE_FOR_ADOPTION);
        return petRepository.save(pet);
    }

    // Delisting, or an approved adopter who never collects (Flow D step 4). RESERVED is
    // accepted as well as AVAILABLE_FOR_ADOPTION so a cancelled adoption can put the pet
    // back on the shelf.
    @Transactional
    public Pet revertToInCenterCustody(UUID petId) {
        Pet pet = loadPet(petId);
        requireStatus(pet, PetStatus.AVAILABLE_FOR_ADOPTION, PetStatus.RESERVED);
        pet.setStatus(PetStatus.IN_CENTER_CUSTODY);
        return petRepository.save(pet);
    }

    // Adoption APPROVED (Flow D step 2). A promise: the pet is earmarked for one adopter
    // and stops accepting applications, but it's still at the center and still owned by
    // nobody. Status only, no history row.
    @Transactional
    public Pet markReserved(UUID petId) {
        Pet pet = loadPet(petId);
        requireStatus(pet, PetStatus.AVAILABLE_FOR_ADOPTION);
        pet.setStatus(PetStatus.RESERVED);
        return petRepository.save(pet);
    }

    // Approved adopter never collected (Flow D step 4): "listing returns to OPEN and pet
    // to AVAILABLE_FOR_ADOPTION". Note this is NOT revertToInCenterCustody — the listing
    // stays live and the pet goes back on the shelf, rather than off it.
    @Transactional
    public Pet revertToAvailableForAdoption(UUID petId) {
        Pet pet = loadPet(petId);
        requireStatus(pet, PetStatus.RESERVED);
        pet.setStatus(PetStatus.AVAILABLE_FOR_ADOPTION);
        return petRepository.save(pet);
    }

    // Boarding check-in (Flow E step 3). THE critical rule of this whole module:
    // ownerUserId is read for the history row and NEVER written. A boarding stay is a
    // custody loan, not a transfer — IN_BOARDING is the only status where both
    // ownerUserId and custodianCenterId are set at once (v2 §6.1).
    @Transactional
    public Pet boardingCheckIn(UUID petId, UUID toCenterId, UUID requestId, Long adminUserId) {
        Pet pet = load(petId, requestId);
        requireStatus(pet, PetStatus.OWNED);

        Long ownerUserId = pet.getOwnerUserId();   // read only

        pet.setCustodianCenterId(toCenterId);
        pet.setStatus(PetStatus.IN_BOARDING);
        petRepository.save(pet);

        // A real physical hop, so it belongs in the chain: the owner handed the animal to
        // the center. from_user + to_center, with ownership unchanged on the pet itself.
        appendHistory(pet, ownerUserId, null, null, toCenterId,
                requestId, TransferType.BOARDING_IN, adminUserId);
        return pet;
    }

    // Boarding check-out (Flow E step 5). Custodian cleared, pet back to OWNED — by the
    // same owner it always had.
    @Transactional
    public Pet boardingCheckOut(UUID petId, UUID requestId, Long adminUserId) {
        Pet pet = load(petId, requestId);
        requireStatus(pet, PetStatus.IN_BOARDING);

        UUID fromCenterId = pet.getCustodianCenterId();
        Long ownerUserId = pet.getOwnerUserId();

        // Defensive, and it encodes the doc's non-negotiable test #3: if ownership went
        // missing during a boarding stay, something wrote owner_user_id that never should
        // have. Fail loudly rather than hand back an ownerless animal.
        if (ownerUserId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "OWNER_LOST_DURING_BOARDING: pet " + petId
                            + " has no owner at check-out; boarding must never clear ownerUserId");
        }

        pet.setCustodianCenterId(null);
        pet.setStatus(PetStatus.OWNED);
        petRepository.save(pet);

        appendHistory(pet, null, fromCenterId, ownerUserId, null,
                requestId, TransferType.BOARDING_OUT, adminUserId);
        return pet;
    }


    private Pet load(UUID petId, UUID requestId) {
        // 500, not 404: the request already passed validation, so a missing pet here means
        // the data is inconsistent, not that the caller asked for something wrong.
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "PET_MISSING_ON_TRANSFER: request " + requestId
                                + " references a pet that no longer exists"));
    }

    private void requireStatus(Pet pet, PetStatus... allowed) {
        if (!Arrays.asList(allowed).contains(pet.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ILLEGAL_CUSTODY_TRANSITION: pet is " + pet.getStatus()
                            + ", expected one of " + Arrays.toString(allowed));
        }
    }

    // 404, unlike load(petId, requestId)'s 500: here the caller supplied the pet id
    // directly in a request body, so an unknown id is client error, not corrupt data.
    private Pet loadPet(UUID petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PET_NOT_FOUND"));
    }

    // The single writer of pet_custody_history. Append-only: rows are never updated or
    // deleted, so the chain of responsibility for an animal stays intact.
    private void appendHistory(Pet pet, Long fromUserId, UUID fromCenterId,
                               Long toUserId, UUID toCenterId, UUID requestId,
                               TransferType transferType, Long adminUserId) {
        PetCustodyHistory history = new PetCustodyHistory();
        history.setPet(pet);
        history.setFromUserId(fromUserId);
        history.setFromCenterId(fromCenterId);
        history.setToUserId(toUserId);
        history.setToCenterId(toCenterId);
        history.setRequestId(requestId);
        history.setTransferType(transferType);
        history.setTransferredAt(Instant.now());
        history.setRecordedBy(adminUserId);
        petCustodyHistoryRepository.save(history);
    }
}
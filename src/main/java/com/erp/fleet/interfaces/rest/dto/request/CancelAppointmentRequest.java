package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @Size(max = 255, message = "{validation.appointment.cancellationReason.size:Reason cannot exceed 255 characters}")
        String reason
) {}

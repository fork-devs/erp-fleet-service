package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.ScheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;

public interface ScheduleAppointmentUseCase {
    AppointmentResult schedule(ScheduleAppointmentCommand command);
}

package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.RescheduleAppointmentCommand;
import com.erp.fleet.application.dto.command.ScheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;
import com.erp.fleet.application.usecase.ManageAppointmentUseCase;
import com.erp.fleet.application.usecase.ScheduleAppointmentUseCase;
import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.exception.AppointmentNotFoundException;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.port.AppointmentRepositoryPort;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AppointmentApplicationService implements ScheduleAppointmentUseCase, ManageAppointmentUseCase {

    private final AppointmentRepositoryPort appointmentRepository;
    private final CustomerRepositoryPort customerRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final UnitOfWork unitOfWork;

    public AppointmentApplicationService(AppointmentRepositoryPort appointmentRepository,
                                         CustomerRepositoryPort customerRepository,
                                         VehicleRepositoryPort vehicleRepository,
                                         UnitOfWork unitOfWork) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public AppointmentResult schedule(ScheduleAppointmentCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            BranchId branchId = new BranchId(cmd.branchId());
            CustomerId customerId = new CustomerId(cmd.customerId());
            VehicleId vehicleId = new VehicleId(cmd.vehicleId());

            if (customerRepository.findById(customerId, tenantId).isEmpty()) {
                throw new CustomerNotFoundException(cmd.customerId().toString());
            }
            if (vehicleRepository.findById(vehicleId, tenantId).isEmpty()) {
                throw new VehicleNotFoundException(cmd.vehicleId().toString());
            }

            Instant start = cmd.scheduledStart();
            Instant end = cmd.scheduledEnd() != null ? cmd.scheduledEnd() : start.plusSeconds(3600);

            if (start.isBefore(Instant.now().minusSeconds(60))) { // allow slight clock drift
                throw new IllegalArgumentException("La fecha de inicio de la cita debe ser presente o futura");
            }
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }

            if (appointmentRepository.hasOverlappingAppointment(tenantId, vehicleId, start, end, null)) {
                throw new AppointmentConflictException("Existe un solapamiento de horario para el vehículo en las fechas solicitadas");
            }

            Appointment appointment = Appointment.create(
                    null, tenantId, branchId, customerId, vehicleId, start, end, cmd.notes()
            );

            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult getById(UUID appointmentId, UUID tenantId) {
        Appointment appointment = appointmentRepository.findById(new AppointmentId(appointmentId), new TenantId(tenantId))
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString()));
        return AppointmentResult.from(appointment);
    }

    @Override
    public List<AppointmentResult> list(UUID tenantId, UUID branchId, UUID customerId, UUID vehicleId, String status) {
        TenantId tId = new TenantId(tenantId);
        BranchId bId = branchId != null ? new BranchId(branchId) : null;
        CustomerId cId = customerId != null ? new CustomerId(customerId) : null;
        VehicleId vId = vehicleId != null ? new VehicleId(vehicleId) : null;
        AppointmentStatus st = (status != null && !status.trim().isEmpty())
                ? AppointmentStatus.valueOf(status.trim().toUpperCase()) : null;

        List<Appointment> list = appointmentRepository.findWithFilters(tId, bId, cId, vId, st);
        return list.stream().map(AppointmentResult::from).toList();
    }

    @Override
    public AppointmentResult reschedule(RescheduleAppointmentCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            AppointmentId appointmentId = new AppointmentId(cmd.appointmentId());

            Appointment appointment = appointmentRepository.findById(appointmentId, tenantId)
                    .orElseThrow(() -> new AppointmentNotFoundException(cmd.appointmentId().toString()));

            Instant start = cmd.scheduledStart();
            Instant end = cmd.scheduledEnd() != null ? cmd.scheduledEnd() : start.plusSeconds(3600);

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }

            if (appointmentRepository.hasOverlappingAppointment(tenantId, appointment.getVehicleId(), start, end, appointmentId)) {
                throw new AppointmentConflictException("Existe un solapamiento de horario para el vehículo en la nueva fecha");
            }

            BranchId newBranchId = cmd.branchId() != null ? new BranchId(cmd.branchId()) : null;
            appointment.reschedule(newBranchId, start, end, cmd.notes());

            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult confirm(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = appointmentRepository.findById(new AppointmentId(appointmentId), new TenantId(tenantId))
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString()));
            appointment.confirm();
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult checkIn(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = appointmentRepository.findById(new AppointmentId(appointmentId), new TenantId(tenantId))
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString()));
            appointment.checkIn();
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult complete(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = appointmentRepository.findById(new AppointmentId(appointmentId), new TenantId(tenantId))
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString()));
            appointment.complete();
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult cancel(UUID appointmentId, UUID tenantId, String reason) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = appointmentRepository.findById(new AppointmentId(appointmentId), new TenantId(tenantId))
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString()));
            appointment.cancel(reason);
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }
}

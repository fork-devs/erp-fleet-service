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
import com.erp.fleet.domain.model.AppointmentResources;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.port.AppointmentRepositoryPort;
import com.erp.fleet.domain.port.AppointmentNotificationPort;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AppointmentApplicationService implements ScheduleAppointmentUseCase, ManageAppointmentUseCase {

    private final AppointmentRepositoryPort appointmentRepository;
    private final CustomerRepositoryPort customerRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final UnitOfWork unitOfWork;
    private final WorkshopAppointmentResources resources;
    private final AppointmentNotificationPort notifications;

    public AppointmentApplicationService(AppointmentRepositoryPort appointmentRepository,
                                         CustomerRepositoryPort customerRepository,
                                         VehicleRepositoryPort vehicleRepository,
                                         UnitOfWork unitOfWork,
                                         WorkshopAppointmentResources resources,
                                         AppointmentNotificationPort notifications) {
        this.resources = resources;
        this.notifications = notifications;
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
            VehicleId vehicleId = new VehicleId(cmd.vehicleId());
            AppointmentId appointmentId = null;
            CustomerId customerId;

            if (cmd.sourceAlertId() == null) {
                customerId = new CustomerId(cmd.customerId());
            } else {
                appointmentRepository.lockSchedule(tenantId, "alert:" + cmd.sourceAlertId());
                String alertKey = "driveos-alert:" + cmd.tenantId() + ":" + cmd.sourceAlertId();
                appointmentId = new AppointmentId(UUID.nameUUIDFromBytes(alertKey.getBytes(StandardCharsets.UTF_8)));
                var existing = appointmentRepository.findById(appointmentId, tenantId);
                if (existing.isPresent()) {
                    if (!existing.get().getVehicleId().equals(vehicleId)) {
                        throw new AppointmentConflictException("La alerta ya tiene una cita para otro vehículo");
                    }
                    if (!Objects.equals(existing.get().getResources(), cmd.resources())) {
                        throw new AppointmentConflictException("La alerta ya tiene otra asignación de recursos");
                    }
                    return AppointmentResult.from(existing.get());
                }
                customerId = vehicleRepository.findById(vehicleId, tenantId)
                        .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()))
                        .getCustomerId();
            }

            var customer = customerRepository.findById(customerId, tenantId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId.getValue().toString()));
            if (cmd.sourceAlertId() == null && vehicleRepository.findById(vehicleId, tenantId).isEmpty()) {
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

            appointmentRepository.lockSchedule(tenantId, "vehicle:" + vehicleId.getValue());
            if (appointmentRepository.hasOverlappingAppointment(tenantId, vehicleId, start, end, null)) {
                throw new AppointmentConflictException("Existe un solapamiento de horario para el vehículo en las fechas solicitadas");
            }

            Appointment appointment = Appointment.create(
                    appointmentId, tenantId, branchId, customerId, vehicleId, start, end, cmd.notes(), cmd.resources()
            );
            if (cmd.resources() != null) {
                resources.reserve(tenantId, branchId, cmd.resources(), start, end, null);
            }

            Appointment saved = appointmentRepository.save(appointment);
            String email = customer.getEmail();
            if (cmd.sourceAlertId() != null && email != null && !email.isBlank()) {
                notifications.enqueue(saved, email);
            }
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult getById(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> appointmentRepository
                .findById(new AppointmentId(appointmentId), new TenantId(tenantId))
                .map(AppointmentResult::from)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString())));
    }

    @Override
    public List<AppointmentResult> list(UUID tenantId, UUID branchId, UUID customerId, UUID vehicleId, String status) {
        TenantId tId = new TenantId(tenantId);
        BranchId bId = branchId != null ? new BranchId(branchId) : null;
        CustomerId cId = customerId != null ? new CustomerId(customerId) : null;
        VehicleId vId = vehicleId != null ? new VehicleId(vehicleId) : null;
        AppointmentStatus st = (status != null && !status.trim().isEmpty())
                ? AppointmentStatus.valueOf(status.trim().toUpperCase()) : null;

        return unitOfWork.callInTransaction(() -> appointmentRepository.findWithFilters(tId, bId, cId, vId, st)
                .stream().map(AppointmentResult::from).toList());
    }

    @Override
    public AppointmentResult reschedule(RescheduleAppointmentCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            AppointmentId appointmentId = new AppointmentId(cmd.appointmentId());
            Appointment appointment = findForUpdate(cmd.appointmentId(), tenantId);

            Instant start = cmd.scheduledStart();
            Instant end = cmd.scheduledEnd() != null ? cmd.scheduledEnd() : start.plusSeconds(3600);

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
            }

            appointmentRepository.lockSchedule(tenantId, "vehicle:" + appointment.getVehicleId().getValue());
            if (appointmentRepository.hasOverlappingAppointment(tenantId, appointment.getVehicleId(), start, end, appointmentId)) {
                throw new AppointmentConflictException("Existe un solapamiento de horario para el vehículo en la nueva fecha");
            }

            BranchId newBranchId = cmd.branchId() != null ? new BranchId(cmd.branchId()) : null;
            if (appointment.getResources() != null) {
                BranchId selectedBranch = newBranchId == null ? appointment.getBranchId() : newBranchId;
                resources.reserve(tenantId, selectedBranch, appointment.getResources(), start, end, appointmentId);
            }
            appointment.reschedule(newBranchId, start, end, cmd.notes());

            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult confirm(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = findForUpdate(appointmentId, new TenantId(tenantId));
            appointment.confirm();
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult checkIn(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = findForUpdate(appointmentId, new TenantId(tenantId));
            appointment.checkIn();
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult complete(UUID appointmentId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = findForUpdate(appointmentId, new TenantId(tenantId));
            appointment.complete();
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    @Override
    public AppointmentResult cancel(UUID appointmentId, UUID tenantId, String reason) {
        return unitOfWork.callInTransaction(() -> {
            Appointment appointment = findForUpdate(appointmentId, new TenantId(tenantId));
            appointment.cancel(reason);
            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResult.from(saved);
        });
    }

    private Appointment findForUpdate(UUID appointmentId, TenantId tenantId) {
        appointmentRepository.lockSchedule(tenantId, "appointment:" + appointmentId);
        return appointmentRepository.findById(new AppointmentId(appointmentId), tenantId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId.toString()));
    }
}

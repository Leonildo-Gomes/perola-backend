/**
 * Booking module: Appointments, AppointmentHistories, Reviews, and Availability Orchestration.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core", "schedule", "business", "identity"}
)
package cv.perola.modules.booking;

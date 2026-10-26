/**
 * Booking module: Appointments, AppointmentHistories, Reviews, and Availability Orchestration.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"schedule", "business", "identity"}
)
package cv.perola.modules.booking;

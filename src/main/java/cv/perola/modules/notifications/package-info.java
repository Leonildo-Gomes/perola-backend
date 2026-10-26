/**
 * Notifications module: Async domain event listeners (alerts, audit logs, notification dispatches).
 * Strictly decoupled from all modules; consumes Spring Modulith application events.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {}
)
package cv.perola.modules.notifications;
